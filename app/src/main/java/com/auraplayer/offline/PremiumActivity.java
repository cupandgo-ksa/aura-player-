package com.auraplayer.offline;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.LruCache;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PremiumActivity extends Activity {
    private static final int PICK_MEDIA = 40, PICK_SUBTITLE = 41, ASK_MEDIA = 42;
    private static final int[] BG = {0xFF050505, 0xFF0B1110, 0xFF100E13, 0xFF150E10};
    private static final int[] ACCENT = {0xFFF7F7F7, 0xFFBFD8D2, 0xFFD9D1E6, 0xFFE8CED9};
    private static final String[] THEMES = {"Soft white", "Sage", "Lilac", "Rose"};
    private static final float[] SPEEDS = {.5f, .75f, 1f, 1.25f, 1.5f, 2f};
    private final ArrayList<Entry> media = new ArrayList<>();
    private final ArrayList<Caption> captions = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private final ExecutorService thumbnails = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> thumbCache = new LruCache<>(32);
    private final java.util.Set<String> unavailableArt = Collections.synchronizedSet(new HashSet<>());
    private final HashSet<String> favorites = new HashSet<>();
    private final ArrayList<String> recentUris = new ArrayList<>();
    private LinearLayout homeList, playerRoot, playerControls, playerTop;
    private FrameLayout stage;
    private VideoView video;
    private MediaPlayer videoEngine;
    private TextView playerTitle, time, captionView, countView, gestureHint, miniTitle;
    private Button play, miniPlay, tabVideo, tabAudio, folderButton, lockButton;
    private SeekBar progress;
    private View miniBar, playerView, homeView;
    private ImageView playerArt, ambientArt;
    private int active = -1, theme, tab = 0, sortMode = 0, resumePosition, collectionMode = 0;
    private String groupFilter = "";
    private boolean onPlayer, videoReady, repeat, shuffle, locked, fullscreen, scrubbing, enteringPip;
    private String pendingAudioUri = "";
    private boolean keepPlayingAfterTheme;
    private float speed = 1f, downX, downY, startBrightness;
    private int startVolume, startPosition;
    private String folder = "All", query = "";
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            AudioService service = AudioService.instance;
            if (service != null && pendingAudioUri.equals(service.uri)) pendingAudioUri = "";
            if (service != null && !service.uri.isEmpty() &&
                (pendingAudioUri.isEmpty() || pendingAudioUri.equals(service.uri)) &&
                (active < 0 || !media.get(active).uri.equals(service.uri))) {
                for (int i = 0; i < media.size(); i++) if (media.get(i).uri.equals(service.uri)) {
                    active = i;
                    if (onPlayer) showPlayer(); else showHome();
                    break;
                }
            }
            int at = position(), total = duration();
            if (progress != null && !scrubbing) { progress.setMax(Math.max(1, total)); progress.setProgress(at); }
            if (time != null) time.setText(format(at) + "  /  " + format(total));
            if (play != null) play.setText(isPlaying() ? "Ⅱ" : "▶");
            if (miniPlay != null) miniPlay.setText(isPlaying() ? "Ⅱ" : "▶");
            if (captionView != null && active >= 0 && media.get(active).video) {
                String line = "";
                for (Caption c : captions) if (at >= c.start && at <= c.end) { line = c.text; break; }
                captionView.setText(line);
            }
            handler.postDelayed(this, 400);
        }
    };

    private static class Entry {
        String uri, name, folder, artist = "Unknown artist", album = "";
        long albumId;
        boolean video, imported;
        long duration;
        Entry(String uri, String name, String folder, boolean video, long duration, boolean imported) {
            this.uri = uri; this.name = name; this.folder = folder; this.video = video;
            this.duration = duration; this.imported = imported;
        }
    }
    private static class CoverDrawable extends Drawable {
        private final String letter;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int tone;
        CoverDrawable(String title, int seed) {
            String clean = title == null ? "" : title.trim();
            letter = clean.isEmpty() ? "A" : clean.substring(0, 1).toUpperCase(Locale.ROOT);
            int[] tones = {0xFF333D47, 0xFF473640, 0xFF313D3A, 0xFF3F3A48, 0xFF454039};
            tone = tones[Math.floorMod(seed, tones.length)];
        }
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            paint.setShader(new android.graphics.LinearGradient(b.left, b.top, b.right, b.bottom,
                tone, 0xFF121416, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(b, paint); paint.setShader(null);
            paint.setColor(0xD9FFFFFF); paint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            paint.setTextSize(Math.min(b.width(), b.height()) * .37f); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(letter, b.centerX(), b.centerY() - (paint.ascent() + paint.descent()) / 2, paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
    private static class Caption {
        int start, end; String text;
        Caption(int start, int end, String text) { this.start = start; this.end = end; this.text = text; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!getPreferences(0).getBoolean("minimal_theme_v3", false)) {
            getPreferences(0).edit().putInt("theme", 0).putBoolean("minimal_theme_v3", true).apply();
        }
        theme = Math.max(0, Math.min(3, getPreferences(0).getInt("theme", 0)));
        loadCollections(); loadImports(); showHome(); handler.post(ticker); requestLibrary();
    }
    private int dp(float v) { return (int)(getResources().getDisplayMetrics().density * v + .5f); }
    private GradientDrawable glass(int color, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{color, (color & 0x00FFFFFF) | 0x26000000});
        g.setCornerRadius(dp(radius)); g.setStroke(dp(1), stroke); return g;
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(1); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private Button button(String name, Runnable action) {
        Button b = new Button(this); b.setText(name); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(Color.WHITE); b.setBackground(glass(0x24FFFFFF, 16, 0x20FFFFFF));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void addSpace(LinearLayout l, int height) { l.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private void inRow(LinearLayout l, View v, float weight, int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(height), weight);
        p.setMargins(dp(3), 0, dp(3), 0); l.addView(v, p);
    }
    private void padded(View v, int h, int w) { v.setPadding(dp(h), dp(w), dp(h), dp(w)); }
    private void base(View v) {
        v.setBackgroundColor(BG[theme]);
        getWindow().setStatusBarColor(BG[theme]); getWindow().setNavigationBarColor(BG[theme]);
    }

    private void showHome() {
        onPlayer = false; playerView = null; playerTitle = null; time = null; progress = null;
        captionView = null; play = null; video = null; stage = null; playerArt = null; ambientArt = null;
        LinearLayout root = column(); base(root); homeView = root; setContentView(root);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column(); padded(body, 19, 14); scroll.addView(body);

        LinearLayout heading = row(); body.addView(heading);
        TextView logo = text("AURA  /  MUSIC", 19, true, Color.WHITE);
        logo.setLetterSpacing(.16f); heading.addView(logo, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button settings = plainButton("⋮", this::homeMenu, 26);
        heading.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));
        addSpace(body, 17);

        EditText search = new EditText(this); search.setSingleLine(true);
        search.setTextColor(Color.WHITE); search.setHintTextColor(0xFF777777);
        search.setTextSize(14); search.setHint("⌕  Search songs, videos, artists");
        search.setPadding(dp(19), 0, dp(14), 0);
        search.setBackground(glass(0x1DFFFFFF, 25, 0x0FFFFFFF));
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(51)));
        search.setText(query);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) { }
            public void onTextChanged(CharSequence s, int st, int before, int count) { query = s.toString(); refreshHome(); }
            public void afterTextChanged(android.text.Editable e) { }
        });
        addSpace(body, 19);

        LinearLayout shortcuts = row(); body.addView(shortcuts);
        shortcut(shortcuts, "♡", "Favorites", 0xFF703A57, () -> { collectionMode = 1; groupFilter = ""; refreshHome(); });
        shortcut(shortcuts, "◷", "Recent", 0xFF36566A, () -> { collectionMode = 2; groupFilter = ""; refreshHome(); });
        shortcut(shortcuts, "▤", "Folders", 0xFF5B524B, this::chooseFolder);
        addSpace(body, 18);

        LinearLayout tabs = row(); body.addView(tabs);
        tabVideo = tabButton(tabs, "Songs", 0);
        tabAudio = tabButton(tabs, "Videos", 1);
        tabButton(tabs, "Artists", 2);
        tabButton(tabs, "Albums", 3);
        addSpace(body, 17);
        LinearLayout toolbar = row(); body.addView(toolbar);
        Button shuffleAll = plainButton("▶", () -> playCollection(true), 18);
        shuffleAll.setTextColor(BG[theme]); shuffleAll.setBackground(glass(ACCENT[theme], 24, 0x44FFFFFF));
        toolbar.addView(shuffleAll, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView headingText = text("  Shuffle playback", 14, true, Color.WHITE);
        toolbar.addView(headingText, new LinearLayout.LayoutParams(0, dp(40), 1));
        headingText.setOnClickListener(v -> playCollection(true));
        toolbar.addView(plainButton("⇅", this::chooseSort, 21), new LinearLayout.LayoutParams(dp(43), dp(40)));
        toolbar.addView(plainButton("＋", this::pickMedia, 22), new LinearLayout.LayoutParams(dp(43), dp(40)));
        addSpace(body, 11);
        countView = text("", 12, false, 0xFF8C8C8C); body.addView(countView);
        addSpace(body, 2);
        homeList = column(); body.addView(homeList); addSpace(body, 10);
        miniBar = miniPlayer(); root.addView(miniBar);
        refreshHome();
    }
    private Button plainButton(String value, Runnable action, int size) {
        Button b = button(value, action); b.setTextSize(size); b.setBackgroundColor(Color.TRANSPARENT);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(0, 0, 0, 0); return b;
    }
    private void shortcut(LinearLayout parent, String icon, String label, int color, Runnable action) {
        LinearLayout card = column(); card.setGravity(Gravity.CENTER_VERTICAL); padded(card, 11, 9);
        card.setBackground(glass(color, 17, 0x24FFFFFF));
        card.addView(text(icon, 24, false, Color.WHITE));
        card.addView(text(label, 12, true, 0xFFE8E8E8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(88), 1);
        lp.setMargins(dp(3), 0, dp(3), 0); parent.addView(card, lp);
        card.setOnClickListener(v -> action.run());
    }
    private Button tabButton(LinearLayout parent, String label, int value) {
        Button b = plainButton(label, () -> { tab = value; groupFilter = ""; groupByAlbum = false; collectionMode = 0; folder = "All"; refreshHome(); }, 14);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(42), 1);
        p.setMargins(dp(2), 0, dp(2), 0); parent.addView(b, p);
        b.setTag(value); return b;
    }
    private void homeMenu() {
        new AlertDialog.Builder(this).setItems(new String[]{"Add local files", "Scan phone", "Choose folder", "Appearance"},
            (d, n) -> { if (n == 0) pickMedia(); else if (n == 1) requestLibrary();
                else if (n == 2) chooseFolder(); else themeDialog(); }).show();
    }
    private View miniPlayer() {
        LinearLayout bar = row(); bar.setPadding(dp(8), dp(5), dp(8), dp(5));
        bar.setBackground(glass(0xDD3A3A3A, 26, 0x39FFFFFF));
        LinearLayout outer = row(); outer.setPadding(dp(11), dp(2), dp(11), dp(10));
        outer.addView(bar, new LinearLayout.LayoutParams(-1, dp(66)));
        if (active >= 0) {
            Entry e = media.get(active);
            ImageView art = new ImageView(this); art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setBackground(glass(0xFF323232, 20, 0));
            bar.addView(art, new LinearLayout.LayoutParams(dp(52), dp(52)));
            loadArt(art, e);
            LinearLayout info = column(); info.setPadding(dp(10), dp(5), dp(2), 0);
            miniTitle = text(e.name, 14, true, Color.WHITE); miniTitle.setSingleLine(true);
            miniTitle.setEllipsize(android.text.TextUtils.TruncateAt.END); info.addView(miniTitle);
            info.addView(text(e.video ? "Video" : e.artist, 11, false, 0xFFBBBBBB));
            bar.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            miniPlay = plainButton(isPlaying() ? "Ⅱ" : "▶", this::toggle, 20);
            bar.addView(miniPlay, new LinearLayout.LayoutParams(dp(42), dp(44)));
            bar.addView(plainButton("▸|", this::next, 18), new LinearLayout.LayoutParams(dp(36), dp(44)));
            bar.setOnClickListener(v -> showPlayer());
        }
        outer.setVisibility(active < 0 ? View.GONE : View.VISIBLE);
        return outer;
    }
    private void refreshHome() {
        if (homeList == null || onPlayer) return;
        LinearLayout tabs = (LinearLayout)tabVideo.getParent();
        for (int i = 0; i < tabs.getChildCount(); i++) {
            Button b = (Button)tabs.getChildAt(i);
            boolean selected = i == tab;
            b.setTextColor(selected ? Color.BLACK : 0xFF999999);
            b.setBackground(selected ? glass(0xFFF5F5F5, 13, 0) : null);
        }
        homeList.removeAllViews();
        ArrayList<Integer> ids = visibleItems();
        String label = collectionMode == 1 ? "FAVORITES" : collectionMode == 2 ? "RECENT" :
            !groupFilter.isEmpty() ? groupFilter : folder.equals("All") ? "ON THIS DEVICE" : folder;
        countView.setText(label + "   ·   " + ids.size());
        if (tab == 2 || tab == 3) { showGroups(ids); return; }
        if (ids.isEmpty()) {
            TextView empty = text("Nothing here yet. Add files or scan your phone.", 14, false, 0xFF929292);
            empty.setPadding(dp(5), dp(30), dp(5), dp(30)); homeList.addView(empty); return;
        }
        for (int j = 0; j < Math.min(ids.size(), 250); j++) {
            int id = ids.get(j); Entry e = media.get(id);
            LinearLayout item = row(); item.setPadding(dp(2), dp(8), dp(2), dp(8));
            ImageView art = new ImageView(this); art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setBackground(glass(0xFF292929, 7, 0));
            art.setClipToOutline(true);
            item.addView(art, new LinearLayout.LayoutParams(dp(53), dp(53)));
            loadArt(art, e);
            LinearLayout info = column(); info.setPadding(dp(12), dp(3), dp(4), 0);
            TextView name = text(e.name, 15, false, Color.WHITE); name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END); info.addView(name);
            TextView by = text(e.video ? e.folder : e.artist + (e.album.isEmpty() ? "" : "  |  " + e.album), 12, false, 0xFF8F8F8F);
            by.setSingleLine(true); by.setEllipsize(android.text.TextUtils.TruncateAt.END); info.addView(by);
            item.addView(info, new LinearLayout.LayoutParams(0, dp(53), 1));
            item.addView(plainButton("⋮", () -> itemMenu(id), 19), new LinearLayout.LayoutParams(dp(37), dp(47)));
            item.setOnClickListener(v -> start(id));
            homeList.addView(item, new LinearLayout.LayoutParams(-1, dp(69)));
        }
        if (ids.size() > 250) homeList.addView(text("Use search to find more files", 12, false, 0xFF888888));
    }
    private ArrayList<Integer> visibleItems() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < media.size(); i++) {
            Entry e = media.get(i);
            if (tab == 1 ? !e.video : e.video) continue;
            if (!folder.equals("All") && !folder.equals(e.folder)) continue;
            if (!groupFilter.isEmpty() && !groupFilter.equals(groupByAlbum ? e.album : e.artist)) continue;
            if (collectionMode == 1 && !favorites.contains(e.uri)) continue;
            if (collectionMode == 2 && !recentUris.contains(e.uri)) continue;
            String match = (e.name + " " + e.artist + " " + e.album).toLowerCase(Locale.ROOT);
            if (!match.contains(query.toLowerCase(Locale.ROOT))) continue;
            ids.add(i);
        }
        if (collectionMode == 2) Collections.sort(ids, (a,b) -> Integer.compare(recentUris.indexOf(media.get(a).uri), recentUris.indexOf(media.get(b).uri)));
        else if (sortMode == 1) Collections.sort(ids, (a,b) -> media.get(a).name.compareToIgnoreCase(media.get(b).name));
        else if (sortMode == 2) Collections.sort(ids, (a,b) -> Long.compare(media.get(b).duration, media.get(a).duration));
        return ids;
    }
    private void showGroups(ArrayList<Integer> ids) {
        ArrayList<String> groups = new ArrayList<>();
        for (int id : ids) {
            Entry e = media.get(id); String group = tab == 2 ? e.artist : e.album;
            if (group.isEmpty()) group = "Unknown";
            if (!groups.contains(group)) groups.add(group);
        }
        Collections.sort(groups);
        for (String group : groups) {
            TextView line = text("♫     " + group + "     ›", 16, false, Color.WHITE);
            line.setSingleLine(true); line.setEllipsize(android.text.TextUtils.TruncateAt.END);
            line.setPadding(dp(8), dp(8), dp(8), dp(8));
            homeList.addView(line, new LinearLayout.LayoutParams(-1, dp(60)));
            int fromTab = tab;
            line.setOnClickListener(v -> { groupFilter = group; tab = 0; collectionMode = 0;
                // The selected group type is retained when filtering songs.
                groupByAlbum = fromTab == 3; refreshHome(); });
        }
    }
    private boolean groupByAlbum;
    private void playCollection(boolean randomOrder) {
        ArrayList<Integer> ids = visibleItems(); if (ids.isEmpty()) return;
        int chosen = randomOrder ? ids.get(random.nextInt(ids.size())) : ids.get(0);
        start(chosen);
    }
    private void loadArt(ImageView view, Entry entry) {
        String uri = entry.uri;
        view.setTag(uri);
        if (view != ambientArt) view.setImageDrawable(new CoverDrawable(entry.name, uri.hashCode()));
        Bitmap cached;
        synchronized (thumbCache) { cached = thumbCache.get(uri); }
        if (cached != null) { view.setImageBitmap(cached); return; }
        if (unavailableArt.contains(uri)) return;
        thumbnails.execute(() -> {
            Bitmap bmp = null;
            try {
                if (entry.video && Build.VERSION.SDK_INT >= 29) {
                    bmp = getContentResolver().loadThumbnail(Uri.parse(uri), new Size(dp(300), dp(300)), null);
                } else if (!entry.video) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(this, Uri.parse(uri));
                        byte[] art = retriever.getEmbeddedPicture();
                        if (art != null) {
                            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(art, 0, art.length, bounds);
                            BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 1;
                            while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 650) options.inSampleSize *= 2;
                            bmp = BitmapFactory.decodeByteArray(art, 0, art.length, options);
                        }
                        if (bmp == null && entry.albumId > 0 && Build.VERSION.SDK_INT >= 29) {
                            try {
                                Uri albumArt = Uri.parse("content://media/external/audio/albumart/" + entry.albumId);
                                bmp = getContentResolver().loadThumbnail(albumArt, new Size(600, 600), null);
                            } catch (Exception ignored) { }
                        }
                        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        if (title != null && !title.trim().isEmpty()) entry.name = title.trim();
                        if (artist != null && !artist.trim().isEmpty()) entry.artist = artist.trim();
                        if (album != null && !album.trim().isEmpty()) entry.album = album.trim();
                    } finally { retriever.release(); }
                }
            } catch (Exception ignored) { }
            final Bitmap result = bmp;
            if (result == null) unavailableArt.add(uri);
            else synchronized (thumbCache) { thumbCache.put(uri, result); }
            runOnUiThread(() -> { if (result != null && uri.equals(view.getTag())) view.setImageBitmap(result); });
        });
    }
    private void chooseFolder() {
        HashSet<String> folders = new HashSet<>();
        for (Entry e : media) if (e.video == (tab == 1)) folders.add(e.folder);
        ArrayList<String> names = new ArrayList<>(folders); Collections.sort(names); names.add(0, "All");
        new AlertDialog.Builder(this).setTitle("Choose folder").setItems(names.toArray(new String[0]),
            (d, which) -> { folder = names.get(which); refreshHome(); }).show();
    }
    private void chooseSort() {
        new AlertDialog.Builder(this).setTitle("Sort files")
            .setItems(new String[]{"Recently found", "Name A–Z", "Longest first"},
                (d, which) -> { sortMode = which; refreshHome(); }).show();
    }
    private void itemMenu(int id) {
        Entry e = media.get(id);
        new AlertDialog.Builder(this).setTitle(e.name).setItems(new String[]{"Play", favorites.contains(e.uri) ? "Remove favorite" : "Add favorite", "Share file", "Remove from this list"},
            (d, which) -> {
                if (which == 0) start(id);
                else if (which == 1) { toggleFavorite(e.uri); refreshHome(); }
                else if (which == 2) {
                    Intent s = new Intent(Intent.ACTION_SEND); s.setType(e.video ? "video/*" : "audio/*");
                    s.putExtra(Intent.EXTRA_STREAM, Uri.parse(e.uri));
                    s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(Intent.createChooser(s, "Share with"));
                } else {
                    if (!e.imported) { Toast.makeText(this, "Scanned files return after the next scan. The original file is untouched.", Toast.LENGTH_LONG).show(); }
                    if (id == active) { savePosition(); release(); active = -1; }
                    else if (id < active) active--;
                    media.remove(id); saveImports(); refreshHome();
                }
            }).show();
    }
    private void themeDialog() {
        new AlertDialog.Builder(this).setTitle("AURA themes").setSingleChoiceItems(THEMES, theme,
            (d, which) -> {
                if (theme != which) {
                    keepPlayingAfterTheme = isPlaying(); savePosition();
                    theme = which; getPreferences(0).edit().putInt("theme", theme).apply();
                    if (onPlayer) showPlayer(); else showHome();
                }
                d.dismiss();
            }).show();
    }

    private void requestLibrary() {
        String[] permissions = Build.VERSION.SDK_INT >= 33
            ? new String[]{Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO}
            : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        ArrayList<String> needed = new ArrayList<>();
        for (String p : permissions) if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) needed.add(p);
        if (needed.isEmpty()) scanMedia(); else requestPermissions(needed.toArray(new String[0]), ASK_MEDIA);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == ASK_MEDIA) {
            boolean any = false; for (int g : grants) if (g == PackageManager.PERMISSION_GRANTED) any = true;
            if (any) scanMedia(); else Toast.makeText(this, "Use Add files to select media without library access.", Toast.LENGTH_LONG).show();
        }
    }
    private void scanMedia() {
        new Thread(() -> {
            ArrayList<Entry> found = new ArrayList<>();
            scanType(found, true); scanType(found, false);
            runOnUiThread(() -> {
                HashSet<String> known = new HashSet<>(); for (Entry e : media) known.add(e.uri);
                for (Entry e : found) if (known.add(e.uri)) media.add(e);
                refreshHome();
            });
        }).start();
    }
    private void scanType(ArrayList<Entry> found, boolean videos) {
        Uri collection = videos ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] cols = videos
            ? new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DURATION, Build.VERSION.SDK_INT >= 29
                    ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA}
            : new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DURATION, Build.VERSION.SDK_INT >= 29
                    ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID};
        try (Cursor c = getContentResolver().query(collection, cols, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (c == null) return;
            while (c.moveToNext()) {
                long id = c.getLong(0); String name = c.getString(1), path = c.getString(3);
                if (name == null) continue;
                String folderName = path == null || path.isEmpty() ? "Device" : path.replaceAll("/$", "");
                if (Build.VERSION.SDK_INT < 29 && folderName.contains("/"))
                    folderName = folderName.substring(0, folderName.lastIndexOf('/'));
                String title = videos ? cleanName(name) : c.getString(4);
                Entry item = new Entry(ContentUris.withAppendedId(collection, id).toString(),
                    title == null || title.trim().isEmpty() ? cleanName(name) : title.trim(),
                    folderName, videos, c.getLong(2), false);
                if (!videos) {
                    String artist = c.getString(5), album = c.getString(6);
                    if (artist != null && !artist.trim().isEmpty() && !artist.equals("<unknown>")) item.artist = artist;
                    if (album != null && !album.trim().isEmpty() && !album.equals("<unknown>")) item.album = album;
                    item.albumId = c.getLong(7);
                }
                found.add(item);
            }
        } catch (Exception ignored) { }
    }
    private void pickMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "audio/*"});
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_MEDIA);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        if (request == PICK_SUBTITLE && data.getData() != null) { loadSubtitle(data.getData()); return; }
        if (request != PICK_MEDIA) return;
        if (data.getClipData() != null) {
            for (int n = 0; n < data.getClipData().getItemCount(); n++) addMedia(data.getClipData().getItemAt(n).getUri());
        } else if (data.getData() != null) addMedia(data.getData());
        saveImports(); refreshHome();
    }
    private void addMedia(Uri uri) {
        for (Entry e : media) if (e.uri.equals(uri.toString())) return;
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) { }
        String name = uri.getLastPathSegment(), mime = getContentResolver().getType(uri);
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) { }
        if (name == null) name = "Local file";
        boolean isVideo = mime != null && mime.startsWith("video/") || name.matches("(?i).*\\.(mp4|mkv|webm|avi|mov|3gp|m4v)$");
        media.add(new Entry(uri.toString(), cleanName(name), "Added files", isVideo, 0, true));
    }
    private void saveImports() {
        JSONArray arr = new JSONArray();
        for (Entry e : media) if (e.imported) {
            JSONObject o = new JSONObject();
            try { o.put("uri", e.uri); o.put("name", e.name); o.put("video", e.video); arr.put(o); }
            catch (Exception ignored) { }
        }
        getPreferences(0).edit().putString("imported", arr.toString()).apply();
    }
    private void loadImports() {
        try {
            // Keep files selected in the earlier AURA version.
            String saved = getPreferences(0).getString("imported", getPreferences(0).getString("library", "[]"));
            JSONArray arr = new JSONArray(saved);
            for (int n = 0; n < arr.length(); n++) {
                JSONObject o = arr.getJSONObject(n);
                media.add(new Entry(o.getString("uri"), cleanName(o.getString("name")), "Added files", o.getBoolean("video"), 0, true));
            }
        } catch (Exception ignored) { }
    }

    private String cleanName(String name) {
        return name.replaceFirst("(?i)\\.(mp3|m4a|aac|wav|flac|ogg|opus|mp4|mkv|webm|avi|mov|3gp|m4v)$", "")
            .replace('_', ' ').replaceAll("\\s+", " ").trim();
    }
    private void loadCollections() {
        try {
            JSONArray f = new JSONArray(getPreferences(0).getString("favorites", "[]"));
            for (int i = 0; i < f.length(); i++) favorites.add(f.getString(i));
            JSONArray r = new JSONArray(getPreferences(0).getString("recent", "[]"));
            for (int i = 0; i < r.length(); i++) recentUris.add(r.getString(i));
        } catch (Exception ignored) { }
    }
    private void saveCollections() {
        getPreferences(0).edit().putString("favorites", new JSONArray(favorites).toString())
            .putString("recent", new JSONArray(recentUris).toString()).apply();
    }
    private void start(int id) {
        if (id < 0 || id >= media.size()) return;
        String uri = media.get(id).uri;
        recentUris.remove(uri); recentUris.add(0, uri);
        while (recentUris.size() > 40) recentUris.remove(recentUris.size() - 1);
        saveCollections(); prepare(id, true); showPlayer();
    }
    private void prepare(int id, boolean auto) {
        if (id < 0 || id >= media.size()) return;
        if (active != id) savePosition();
        release(); active = id;
        resumePosition = getPreferences(0).getInt("pos_" + media.get(id).uri.hashCode(), 0);
        if (miniBar != null && !onPlayer) {
            miniBar.setVisibility(View.VISIBLE);
            if (miniTitle != null) miniTitle.setText(media.get(id).name);
        }
        if (playerTitle != null) playerTitle.setText("NOW PLAYING");
        captions.clear(); if (captionView != null) captionView.setText("");
        Uri uri = Uri.parse(media.get(id).uri);
        try {
            if (media.get(id).video) {
                if (AudioService.instance != null) startService(new Intent(this, AudioService.class).setAction(AudioService.STOP));
                if (video == null) return;
                video.setVisibility(View.VISIBLE);
                video.setOnPreparedListener(mp -> {
                    videoEngine = mp; videoReady = true;
                    video.seekTo(resumePosition);
                    if (auto) { video.start(); applySpeed(); }
                });
                video.setOnCompletionListener(mp -> completed());
                video.setOnErrorListener((mp, what, extra) -> { error(); return true; });
                video.setVideoURI(uri);
            } else {
                if (video != null) video.setVisibility(View.GONE);
                JSONArray queue = new JSONArray();
                for (Entry item : media) if (!item.video) {
                    JSONObject track = new JSONObject();
                    track.put("uri", item.uri); track.put("title", item.name); track.put("artist", item.artist);
                    queue.put(track);
                }
                pendingAudioUri = uri.toString();
                Intent command = new Intent(this, AudioService.class).setAction(AudioService.PLAY)
                    .putExtra("uri", pendingAudioUri).putExtra("title", media.get(id).name)
                    .putExtra("artist", media.get(id).artist).putExtra("position", resumePosition)
                    .putExtra("queue", queue.toString());
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(command); else startService(command);
            }
        } catch (Exception ex) { error(); }
    }
    private void showPlayer() {
        if (active < 0 || active >= media.size()) return;
        onPlayer = true; homeList = null; miniBar = null; miniTitle = null; miniPlay = null;
        Entry e = media.get(active);
        FrameLayout background = new FrameLayout(this); background.setBackgroundColor(BG[theme]);
        ambientArt = new ImageView(this); ambientArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ambientArt.setAlpha(Build.VERSION.SDK_INT >= 31 ? .27f : .12f);
        if (Build.VERSION.SDK_INT >= 31) ambientArt.setRenderEffect(RenderEffect.createBlurEffect(dp(40), dp(40), Shader.TileMode.CLAMP));
        background.addView(ambientArt, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xB9000000, 0xC9000000, 0xF7000000}));
        background.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        playerRoot = column(); playerView = playerRoot;
        background.addView(playerRoot, new FrameLayout.LayoutParams(-1, -1));
        setContentView(background);
        if (!e.video) loadArt(ambientArt, e);

        playerTop = row(); playerTop.setPadding(dp(19), dp(7), dp(19), dp(4)); playerRoot.addView(playerTop);
        Button close = plainButton("⌄", this::leavePlayer, 31);
        playerTop.addView(close, new LinearLayout.LayoutParams(dp(42), dp(48)));
        playerTitle = text("NOW PLAYING", 11, true, 0xFFCFCFCF);
        playerTitle.setGravity(Gravity.CENTER);
        playerTop.addView(playerTitle, new LinearLayout.LayoutParams(0, dp(48), 1));
        playerTop.addView(plainButton("⋮", this::playerMenu, 27), new LinearLayout.LayoutParams(dp(42), dp(48)));
        addSpace(playerRoot, 17);

        int width = getResources().getDisplayMetrics().widthPixels;
        int albumSize = Math.min(width - dp(44), dp(380));
        int stageHeight = e.video ? Math.round(albumSize * .62f) : albumSize;
        stage = new FrameLayout(this); stage.setBackground(glass(0xFF181818, 15, 0x19FFFFFF));
        stage.setClipToOutline(true);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, stageHeight);
        stageParams.setMargins(dp(22), 0, dp(22), 0); playerRoot.addView(stage, stageParams);
        if (e.video) {
            video = new VideoView(this); stage.addView(video, new FrameLayout.LayoutParams(-1, -1));
            video.setOnTouchListener(this::gesture);
            captionView = text("", 17, true, Color.WHITE);
            captionView.setGravity(Gravity.CENTER); captionView.setShadowLayer(4, 0, 2, Color.BLACK);
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            cp.setMargins(dp(16), 0, dp(16), dp(16)); stage.addView(captionView, cp);
        } else {
            playerArt = new ImageView(this); playerArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
            stage.addView(playerArt, new FrameLayout.LayoutParams(-1, -1));
            loadArt(playerArt, e);
        }
        gestureHint = text("", 15, true, Color.WHITE); gestureHint.setGravity(Gravity.CENTER);
        stage.addView(gestureHint, new FrameLayout.LayoutParams(-1, dp(55), Gravity.CENTER));
        lockButton = button("Unlock", () -> setLocked(false));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(85), dp(44), Gravity.TOP | Gravity.RIGHT);
        lp.setMargins(0, dp(12), dp(12), 0); stage.addView(lockButton, lp);
        lockButton.setVisibility(View.GONE);
        addSpace(playerRoot, 22);

        playerControls = column(); playerControls.setPadding(dp(25), 0, dp(25), dp(24));
        playerRoot.addView(playerControls, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout info = row(); playerControls.addView(info);
        LinearLayout names = column();
        TextView song = text(e.name, 23, false, Color.WHITE); song.setSingleLine(true);
        song.setEllipsize(android.text.TextUtils.TruncateAt.END); names.addView(song);
        TextView artist = text(e.video ? e.folder : e.artist, 15, false, 0xFFAAAAAA);
        artist.setSingleLine(true); artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        names.addView(artist);
        info.addView(names, new LinearLayout.LayoutParams(0, dp(68), 1));
        if (!e.video) {
            Button star = plainButton(favorites.contains(e.uri) ? "★" : "☆", () -> toggleFavorite(e.uri), 30);
            star.setTag(e.uri); info.addView(star, new LinearLayout.LayoutParams(dp(48), dp(52)));
            favoriteButton = star;
        } else favoriteButton = null;
        View spacer = new View(this); playerControls.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        progress = new SeekBar(this); progress.setPadding(0, 0, 0, 0);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT[theme]));
        progress.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT[theme]));
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar b) { scrubbing = true; }
            public void onProgressChanged(SeekBar b, int v, boolean user) { if (user && time != null) time.setText(format(v) + "   /   " + format(duration())); }
            public void onStopTrackingTouch(SeekBar b) { scrubbing = false; jump(b.getProgress()); }
        });
        playerControls.addView(progress);
        time = text("0:00   /   0:00", 12, false, 0xFF999999);
        time.setGravity(Gravity.RIGHT); playerControls.addView(time);
        addSpace(playerControls, 20);
        LinearLayout transport = row(); playerControls.addView(transport);
        inRow(transport, plainButton("⟲", () -> { repeat = !repeat; toast("Repeat " + (repeat ? "on" : "off")); }, 27), 1, 62);
        inRow(transport, plainButton("⏮", this::previous, 25), 1, 62);
        play = button("▶", this::toggle); play.setTextSize(29);
        play.setBackground(glass(0x54FFFFFF, 40, 0x30FFFFFF));
        inRow(transport, play, 1.4f, 70);
        inRow(transport, plainButton("⏭", this::next, 25), 1, 62);
        inRow(transport, plainButton("≡", this::queueDialog, 29), 1, 62);
        if (!e.video && !e.uri.equals(pendingAudioUri) &&
            (AudioService.instance == null || !e.uri.equals(AudioService.instance.uri))) prepare(active, true);
        else if (e.video && video != null && videoEngine == null) prepare(active, true);
    }
    private Button favoriteButton;
    private void toggleFavorite(String uri) {
        if (!favorites.add(uri)) favorites.remove(uri);
        saveCollections();
        if (favoriteButton != null) favoriteButton.setText(favorites.contains(uri) ? "★" : "☆");
    }
    private void queueDialog() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < media.size(); i++) if (active >= 0 && media.get(i).video == media.get(active).video) ids.add(i);
        String[] labels = new String[Math.min(ids.size(), 100)];
        for (int n = 0; n < labels.length; n++) labels[n] = media.get(ids.get(n)).name;
        new AlertDialog.Builder(this).setTitle("Play queue").setItems(labels, (d, which) -> {
            savePosition(); release(); active = ids.get(which); showPlayer();
        }).show();
    }
    private void playerMenu() {
        Entry e = media.get(active);
        String[] names = e.video ? new String[]{"Playback speed", "Subtitles (.srt)", "Picture-in-Picture", "Fit / Zoom", "Lock controls", "Full screen", "Appearance"}
            : new String[]{"Playback speed", "Shuffle", "Add files", "Appearance"};
        new AlertDialog.Builder(this).setItems(names, (d, n) -> {
            if (e.video) {
                if (n == 0) speedDialog(); else if (n == 1) pickSubtitle();
                else if (n == 2) pictureInPicture(); else if (n == 3) toggleZoom();
                else if (n == 4) setLocked(true); else if (n == 5) toggleFullscreen();
                else themeDialog();
            } else {
                if (n == 0) speedDialog(); else if (n == 1) { shuffle = !shuffle; toast("Shuffle " + (shuffle ? "on" : "off")); }
                else if (n == 2) pickMedia(); else themeDialog();
            }
        }).show();
    }
    private void leavePlayer() {
        if (fullscreen) toggleFullscreen();
        savePosition();
        if (videoReady && video != null) video.pause();
        // VideoView belongs to this screen, so release it before returning home.
        if (active >= 0 && media.get(active).video) release();
        showHome();
    }
    private void setLocked(boolean value) {
        locked = value; if (playerTop != null) playerTop.setVisibility(value ? View.GONE : View.VISIBLE);
        if (playerControls != null) playerControls.setVisibility(value ? View.GONE : View.VISIBLE);
        if (lockButton != null) lockButton.setVisibility(value ? View.VISIBLE : View.GONE);
        toast(value ? "Controls locked" : "Controls unlocked");
    }
    private void speedDialog() {
        String[] labels = new String[SPEEDS.length];
        for (int i = 0; i < SPEEDS.length; i++) labels[i] = SPEEDS[i] + "×";
        new AlertDialog.Builder(this).setTitle("Playback speed").setItems(labels,
            (d, which) -> { speed = SPEEDS[which]; applySpeed(); toast("Speed " + labels[which]); }).show();
    }
    private void applySpeed() {
        try {
            if (active >= 0 && !media.get(active).video) {
                if (AudioService.instance != null) AudioService.instance.speed(speed);
                return;
            }
            MediaPlayer mp = videoReady ? videoEngine : null;
            if (mp == null) return;
            boolean playing = mp.isPlaying();
            mp.setPlaybackParams(new PlaybackParams().setSpeed(speed));
            if (!playing) mp.pause();
        } catch (Exception ignored) { }
    }
    private boolean zoomed;
    private void toggleZoom() {
        if (video == null) return;
        zoomed = !zoomed; video.setScaleX(zoomed ? 1.3f : 1f); video.setScaleY(zoomed ? 1.3f : 1f);
        toast(zoomed ? "Zoom" : "Fit");
    }
    private void toggleFullscreen() {
        if (stage == null || active < 0 || !media.get(active).video) return;
        fullscreen = !fullscreen;
        getWindow().getDecorView().setSystemUiVisibility(fullscreen ? 5894 : 0);
        if (playerTop != null) playerTop.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        if (playerControls != null) playerControls.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        stage.getLayoutParams().height = fullscreen ? getResources().getDisplayMetrics().heightPixels : dp(235);
        stage.requestLayout();
    }
    private void pictureInPicture() {
        if (Build.VERSION.SDK_INT >= 26 && videoReady) {
            try { enteringPip = enterPictureInPictureMode(new PictureInPictureParams.Builder().build()); }
            catch (Exception ex) { enteringPip = false; toast("Pop-up playback isn't available on this device"); }
        }
    }
    @Override public void onPictureInPictureModeChanged(boolean pip, Configuration config) {
        super.onPictureInPictureModeChanged(pip, config);
        enteringPip = pip;
        if (playerTop != null) playerTop.setVisibility(pip ? View.GONE : View.VISIBLE);
        if (playerControls != null) playerControls.setVisibility(pip ? View.GONE : View.VISIBLE);
    }
    private boolean gesture(View v, MotionEvent event) {
        if (locked) return true;
        AudioManager manager = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX(); downY = event.getY(); startPosition = position();
            startVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            startBrightness = getWindow().getAttributes().screenBrightness;
            if (startBrightness < 0) startBrightness = .5f;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - downX, dy = event.getY() - downY;
            if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > dp(20)) {
                int target = startPosition + (int)(dx / Math.max(1, v.getWidth()) * Math.min(180000, duration()));
                gestureHint.setText("Seek  " + format(target));
            } else if (Math.abs(dy) > dp(20)) {
                float fraction = -dy / Math.max(1, v.getHeight());
                if (downX > v.getWidth() / 2f) {
                    int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int volume = Math.max(0, Math.min(max, startVolume + (int)(fraction * max)));
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                    gestureHint.setText("Volume  " + Math.round(volume * 100f / Math.max(1, max)) + "%");
                } else {
                    WindowManager.LayoutParams p = getWindow().getAttributes();
                    p.screenBrightness = Math.max(.02f, Math.min(1f, startBrightness + fraction));
                    getWindow().setAttributes(p); gestureHint.setText("Brightness  " + Math.round(p.screenBrightness * 100) + "%");
                }
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX, dy = event.getY() - downY;
            if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > dp(35)) {
                jump(startPosition + (int)(dx / Math.max(1, v.getWidth()) * Math.min(180000, duration())));
            } else if (Math.abs(dx) < dp(15) && Math.abs(dy) < dp(15)) toggle();
            handler.postDelayed(() -> { if (gestureHint != null) gestureHint.setText(""); }, 650);
            return true;
        }
        return true;
    }
    private void pickSubtitle() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,
            new String[]{"application/x-subrip", "text/plain", "text/srt", "application/octet-stream"});
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivityForResult(i, PICK_SUBTITLE);
    }
    private void loadSubtitle(Uri uri) {
        ArrayList<Caption> parsed = new ArrayList<>();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("Empty file");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line; int bytes = 0, start = -1, end = -1; StringBuilder block = new StringBuilder();
            while ((line = reader.readLine()) != null && bytes < 2_000_000) {
                bytes += line.length();
                if (line.contains("-->") && line.contains(":")) {
                    String[] halves = line.split("-->");
                    start = stamp(halves[0].trim()); end = stamp(halves[1].trim()); block.setLength(0);
                } else if (line.trim().isEmpty()) {
                    if (start >= 0 && block.length() > 0) parsed.add(new Caption(start, end, block.toString().trim()));
                    start = -1; block.setLength(0);
                } else if (start >= 0) block.append(line).append('\n');
            }
            if (start >= 0 && block.length() > 0) parsed.add(new Caption(start, end, block.toString().trim()));
            captions.clear(); captions.addAll(parsed); toast(parsed.size() + " subtitle lines loaded");
        } catch (Exception ex) { toast("Could not read this subtitle file"); }
    }
    private int stamp(String value) {
        try {
            String[] t = value.replace(',', '.').split("[:.]");
            return ((Integer.parseInt(t[0]) * 3600 + Integer.parseInt(t[1]) * 60 + Integer.parseInt(t[2])) * 1000)
                + Integer.parseInt((t[3] + "000").substring(0, 3));
        } catch (Exception ex) { return -1; }
    }
    private void error() { toast("This file or codec could not play on this phone"); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private void completed() {
        if (active >= 0) getPreferences(0).edit().putInt("pos_" + media.get(active).uri.hashCode(), 0).apply();
        if (repeat) { jump(0); if (videoReady) video.start(); }
        else next();
    }
    private void toggle() {
        if (active < 0) return;
        try {
            if (!media.get(active).video) {
                if (AudioService.instance != null) AudioService.instance.toggle();
            } else if (videoReady) { if (video.isPlaying()) video.pause(); else { video.start(); applySpeed(); } }
        } catch (Exception ignored) { }
    }
    private void next() {
        if (media.isEmpty()) return;
        if (active >= 0 && !media.get(active).video && AudioService.instance != null) {
            AudioService.instance.next(); return;
        }
        int selected = -1;
        if (shuffle && media.size() > 1) {
            selected = random.nextInt(media.size() - 1);
            if (selected >= active) selected++;
        } else {
            for (int n = active + 1; n < media.size(); n++) if (media.get(n).video == media.get(active).video) { selected = n; break; }
        }
        if (selected >= 0) {
            if (onPlayer) { savePosition(); release(); active = selected; showPlayer(); }
            else { prepare(selected, true); refreshHome(); }
        }
    }
    private void previous() {
        if (active < 0) return;
        if (!media.get(active).video && AudioService.instance != null) {
            AudioService.instance.previous(); return;
        }
        for (int n = active - 1; n >= 0; n--) if (media.get(n).video == media.get(active).video) {
            if (onPlayer) { savePosition(); release(); active = n; showPlayer(); }
            else { prepare(n, true); refreshHome(); }
            return;
        }
        jump(0);
    }
    private void jump(int ms) {
        int target = Math.max(0, Math.min(duration(), ms));
        try { if (active >= 0 && !media.get(active).video && AudioService.instance != null) AudioService.instance.seek(target);
            else if (videoReady) video.seekTo(target); }
        catch (Exception ignored) { }
    }
    private int position() {
        try { return active >= 0 && !media.get(active).video && AudioService.instance != null ? AudioService.instance.position()
            : videoReady && video != null ? video.getCurrentPosition() : 0; }
        catch (Exception ex) { return 0; }
    }
    private int duration() {
        try { return Math.max(0, active >= 0 && !media.get(active).video && AudioService.instance != null ? AudioService.instance.duration()
            : videoReady && video != null ? video.getDuration() : 0); }
        catch (Exception ex) { return 0; }
    }
    private boolean isPlaying() {
        try { return active >= 0 && !media.get(active).video && AudioService.instance != null ? AudioService.instance.isPlaying()
            : videoReady && video != null && video.isPlaying(); }
        catch (Exception ex) { return false; }
    }
    private String format(int ms) {
        int s = Math.max(0, ms / 1000);
        return s >= 3600 ? String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
            : String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }
    private void savePosition() {
        if (active >= 0 && active < media.size())
            getPreferences(0).edit().putInt("pos_" + media.get(active).uri.hashCode(), position()).apply();
    }
    private void release() {
        videoReady = false; videoEngine = null;
        if (video != null) video.stopPlayback();
    }
    @Override public void onBackPressed() {
        if (locked) { setLocked(false); return; }
        if (fullscreen) { toggleFullscreen(); return; }
        if (onPlayer) { leavePlayer(); return; }
        super.onBackPressed();
    }
    @Override protected void onPause() {
        super.onPause(); savePosition();
        if (videoReady && video != null && video.isPlaying() &&
            (Build.VERSION.SDK_INT < 26 || (!isInPictureInPictureMode() && !enteringPip))) video.pause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker); thumbnails.shutdownNow(); release(); super.onDestroy();
    }
}

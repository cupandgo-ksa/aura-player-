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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
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
    private static final int[] BG = {0xFF080D17, 0xFF071916, 0xFF111725, 0xFF1C111B};
    private static final int[] ACCENT = {0xFF70B5FF, 0xFF5CE0BF, 0xFFC9B9FF, 0xFFF3A9C4};
    private static final String[] THEMES = {"Ocean", "Emerald", "Violet", "Rose"};
    private static final float[] SPEEDS = {.5f, .75f, 1f, 1.25f, 1.5f, 2f};
    private final ArrayList<Entry> media = new ArrayList<>();
    private final ArrayList<Caption> captions = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private final ExecutorService thumbnails = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> thumbCache = new LruCache<>(32);
    private LinearLayout homeList, playerRoot, playerControls, playerTop;
    private FrameLayout stage;
    private VideoView video;
    private MediaPlayer audio, videoEngine;
    private TextView playerTitle, time, captionView, countView, gestureHint, miniTitle;
    private Button play, miniPlay, tabVideo, tabAudio, folderButton, lockButton;
    private SeekBar progress;
    private View miniBar, playerView, homeView;
    private int active = -1, theme, tab = 0, sortMode = 0, resumePosition;
    private boolean onPlayer, audioReady, videoReady, repeat, shuffle, locked, fullscreen, scrubbing, enteringPip;
    private boolean keepPlayingAfterTheme;
    private float speed = 1f, downX, downY, startBrightness;
    private int startVolume, startPosition;
    private String folder = "All", query = "";
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
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
        String uri, name, folder;
        boolean video, imported;
        long duration;
        Entry(String uri, String name, String folder, boolean video, long duration, boolean imported) {
            this.uri = uri; this.name = name; this.folder = folder; this.video = video;
            this.duration = duration; this.imported = imported;
        }
    }
    private static class Caption {
        int start, end; String text;
        Caption(int start, int end, String text) { this.start = start; this.end = end; this.text = text; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        theme = Math.max(0, Math.min(3, getPreferences(0).getInt("theme", 0)));
        loadImports(); showHome(); handler.post(ticker); requestLibrary();
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
        b.setTextColor(Color.WHITE); b.setBackground(glass(0x4750667D, 15, 0x3EFFFFFF));
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
        captionView = null; play = null; video = null; stage = null;
        LinearLayout root = column(); base(root); homeView = root; setContentView(root);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column(); padded(body, 18, 18); scroll.addView(body);
        LinearLayout header = row(); body.addView(header);
        LinearLayout brand = column();
        brand.addView(text("AURA  /  PLAYER", 27, true, Color.WHITE));
        brand.addView(text("YOUR OFFLINE CINEMA & MUSIC", 10, true, ACCENT[theme]));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button options = button("◈", this::themeDialog);
        header.addView(options, new LinearLayout.LayoutParams(dp(48), dp(48)));
        addSpace(body, 22);

        LinearLayout banner = column(); padded(banner, 20, 20);
        banner.setBackground(glass(0x774277A0, 25, 0x6CFFFFFF)); body.addView(banner);
        banner.addView(text("ALL YOUR MEDIA", 11, true, 0xFFE8ECFF));
        addSpace(banner, 7);
        banner.addView(text("One beautiful place\nfor every moment.", 25, true, Color.WHITE));
        addSpace(banner, 15);
        LinearLayout bannerActions = row();
        Button importButton = button("＋  Add files", this::pickMedia);
        importButton.setBackground(glass(ACCENT[theme], 15, Color.WHITE)); importButton.setTextColor(BG[theme]);
        inRow(bannerActions, importButton, 1, 44);
        inRow(bannerActions, button("↻  Scan", this::requestLibrary), 1, 44);
        banner.addView(bannerActions);
        addSpace(body, 23);

        LinearLayout tabs = row(); body.addView(tabs);
        tabVideo = button("▣  VIDEOS", () -> { tab = 0; folder = "All"; refreshHome(); });
        tabAudio = button("♫  MUSIC", () -> { tab = 1; folder = "All"; refreshHome(); });
        inRow(tabs, tabVideo, 1, 47); inRow(tabs, tabAudio, 1, 47);
        addSpace(body, 15);
        LinearLayout tools = row(); body.addView(tools);
        folderButton = button("All folders ▾", this::chooseFolder);
        inRow(tools, folderButton, 2, 43);
        inRow(tools, button("Sort ⇅", this::chooseSort), 1, 43);
        addSpace(body, 13);
        EditText search = new EditText(this); search.setSingleLine(true); search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xFFAFBBD0); search.setTextSize(14);
        search.setHint("⌕   Search files by name"); padded(search, 14, 3);
        search.setBackground(glass(0x38547090, 17, 0x33FFFFFF));
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(49)));
        search.setText(query);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) { }
            public void onTextChanged(CharSequence s, int st, int before, int count) { query = s.toString(); refreshHome(); }
            public void afterTextChanged(android.text.Editable e) { }
        });
        addSpace(body, 19);
        countView = text("", 12, true, ACCENT[theme]); body.addView(countView);
        addSpace(body, 9);
        homeList = column(); body.addView(homeList);
        addSpace(body, 16);
        miniBar = miniPlayer(); root.addView(miniBar);
        refreshHome();
    }
    private View miniPlayer() {
        LinearLayout outer = row(); padded(outer, 13, 9);
        outer.setBackground(glass(0xDD23324B, 17, 0x55FFFFFF));
        TextView symbol = text("♫", 25, true, ACCENT[theme]); outer.addView(symbol, new LinearLayout.LayoutParams(dp(38), -1));
        miniTitle = text("", 14, true, Color.WHITE); miniTitle.setSingleLine(true);
        miniTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        outer.addView(miniTitle, new LinearLayout.LayoutParams(0, -1, 1));
        miniPlay = button("▶", this::toggle); outer.addView(miniPlay, new LinearLayout.LayoutParams(dp(47), dp(47)));
        outer.setOnClickListener(v -> { if (active >= 0) showPlayer(); });
        outer.setVisibility(active < 0 ? View.GONE : View.VISIBLE);
        if (active >= 0) miniTitle.setText(media.get(active).name);
        return outer;
    }
    private void refreshHome() {
        if (homeList == null || onPlayer) return;
        tabVideo.setBackground(glass(tab == 0 ? ACCENT[theme] : 0x42546986, 16, 0x40FFFFFF));
        tabAudio.setBackground(glass(tab == 1 ? ACCENT[theme] : 0x42546986, 16, 0x40FFFFFF));
        tabVideo.setTextColor(tab == 0 ? BG[theme] : Color.WHITE);
        tabAudio.setTextColor(tab == 1 ? BG[theme] : Color.WHITE);
        folderButton.setText(folder.equals("All") ? "All folders ▾" : folder + " ▾");
        homeList.removeAllViews();
        ArrayList<Integer> ids = new ArrayList<>();
        for (int i = 0; i < media.size(); i++) {
            Entry e = media.get(i);
            if (e.video != (tab == 0) || !folder.equals("All") && !folder.equals(e.folder)) continue;
            if (!e.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            ids.add(i);
        }
        if (sortMode == 1) Collections.sort(ids, (a,b) -> media.get(a).name.compareToIgnoreCase(media.get(b).name));
        if (sortMode == 2) Collections.sort(ids, (a,b) -> Long.compare(media.get(b).duration, media.get(a).duration));
        countView.setText(ids.size() + (tab == 0 ? " VIDEOS" : " AUDIO TRACKS"));
        if (ids.isEmpty()) {
            LinearLayout empty = column(); padded(empty, 25, 28);
            empty.setBackground(glass(0x31526C89, 20, 0x35FFFFFF));
            empty.addView(text("No files here yet", 19, true, Color.WHITE));
            addSpace(empty, 7);
            empty.addView(text("Allow a scan or add files from your phone.", 13, false, 0xFFC7D2E1));
            homeList.addView(empty); return;
        }
        for (int j = 0; j < Math.min(ids.size(), 250); j++) {
            int id = ids.get(j); Entry e = media.get(id);
            LinearLayout item = row(); padded(item, 10, 9);
            item.setBackground(glass(id == active ? 0x605C86AF : 0x3554667E, 18,
                id == active ? ACCENT[theme] : 0x29FFFFFF));
            ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(glass(0x705B799A, 12, 0x33FFFFFF));
            if (e.video) thumbnail(image, e.uri);
            else image.setImageDrawable(null);
            FrameLayout art = new FrameLayout(this); art.addView(image, new FrameLayout.LayoutParams(-1, -1));
            if (!e.video) {
                TextView note = text("♫", 30, true, ACCENT[theme]); note.setGravity(Gravity.CENTER);
                art.addView(note, new FrameLayout.LayoutParams(-1, -1));
            }
            item.addView(art, new LinearLayout.LayoutParams(dp(69), dp(63)));
            LinearLayout info = column(); info.setPadding(dp(12), dp(4), dp(5), 0);
            TextView name = text(e.name, 14, true, Color.WHITE); name.setMaxLines(2);
            info.addView(name);
            info.addView(text(e.folder + "   •   " + format((int)e.duration), 11, false, 0xFFBAC9DC));
            item.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            item.addView(text("›", 25, false, ACCENT[theme]));
            item.setOnClickListener(v -> start(id));
            item.setOnLongClickListener(v -> { itemMenu(id); return true; });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.bottomMargin = dp(9); homeList.addView(item, p);
        }
        if (ids.size() > 250) homeList.addView(text("Showing first 250 results. Use search to find more.", 12, false, 0xFFAFBED0));
    }
    private void thumbnail(ImageView view, String uri) {
        view.setTag(uri);
        Bitmap cached = thumbCache.get(uri);
        if (cached != null) { view.setImageBitmap(cached); return; }
        if (Build.VERSION.SDK_INT < 29) return;
        thumbnails.execute(() -> {
            try {
                Bitmap bmp = getContentResolver().loadThumbnail(Uri.parse(uri), new Size(dp(120), dp(100)), null);
                thumbCache.put(uri, bmp);
                runOnUiThread(() -> { if (uri.equals(view.getTag())) view.setImageBitmap(bmp); });
            } catch (Exception ignored) { }
        });
    }
    private void chooseFolder() {
        HashSet<String> folders = new HashSet<>();
        for (Entry e : media) if (e.video == (tab == 0)) folders.add(e.folder);
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
        new AlertDialog.Builder(this).setTitle(e.name).setItems(new String[]{"Play", "Share file", "Remove from this list"},
            (d, which) -> {
                if (which == 0) start(id);
                else if (which == 1) {
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
                    keepPlayingAfterTheme = isPlaying(); savePosition(); release();
                    theme = which; getPreferences(0).edit().putInt("theme", theme).apply();
                    showHome(); if (active >= 0) prepare(active, keepPlayingAfterTheme);
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
        String[] cols = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DURATION, Build.VERSION.SDK_INT >= 29
                ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA};
        try (Cursor c = getContentResolver().query(collection, cols, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (c == null) return;
            while (c.moveToNext()) {
                long id = c.getLong(0); String name = c.getString(1), path = c.getString(3);
                if (name == null) continue;
                String folderName = path == null || path.isEmpty() ? "Device" : path.replaceAll("/$", "");
                if (Build.VERSION.SDK_INT < 29 && folderName.contains("/"))
                    folderName = folderName.substring(0, folderName.lastIndexOf('/'));
                found.add(new Entry(ContentUris.withAppendedId(collection, id).toString(), name,
                    folderName, videos, c.getLong(2), false));
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
        media.add(new Entry(uri.toString(), name, "Added files", isVideo, 0, true));
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
                media.add(new Entry(o.getString("uri"), o.getString("name"), "Added files", o.getBoolean("video"), 0, true));
            }
        } catch (Exception ignored) { }
    }

    private void start(int id) { prepare(id, true); showPlayer(); }
    private void prepare(int id, boolean auto) {
        if (id < 0 || id >= media.size()) return;
        if (active != id) savePosition();
        release(); active = id;
        resumePosition = getPreferences(0).getInt("pos_" + media.get(id).uri.hashCode(), 0);
        if (miniBar != null && !onPlayer) {
            miniBar.setVisibility(View.VISIBLE); miniTitle.setText(media.get(id).name);
        }
        if (playerTitle != null) playerTitle.setText(media.get(id).name);
        captions.clear(); if (captionView != null) captionView.setText("");
        Uri uri = Uri.parse(media.get(id).uri);
        try {
            if (media.get(id).video) {
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
                audio = new MediaPlayer(); audio.setAudioStreamType(AudioManager.STREAM_MUSIC);
                audio.setDataSource(this, uri);
                audio.setOnPreparedListener(mp -> {
                    audioReady = true; mp.seekTo(resumePosition);
                    if (auto) { mp.start(); applySpeed(); }
                });
                audio.setOnCompletionListener(mp -> completed());
                audio.setOnErrorListener((mp, what, extra) -> { error(); return true; });
                audio.prepareAsync();
            }
        } catch (Exception ex) { error(); }
    }
    private void showPlayer() {
        if (active < 0 || active >= media.size()) return;
        onPlayer = true; homeList = null; miniBar = null; miniTitle = null; miniPlay = null;
        Entry e = media.get(active);
        playerRoot = column(); base(playerRoot); playerView = playerRoot; setContentView(playerRoot);
        playerTop = row(); padded(playerTop, 13, 10); playerRoot.addView(playerTop);
        Button back = button("‹", this::leavePlayer); playerTop.addView(back, new LinearLayout.LayoutParams(dp(47), dp(43)));
        playerTitle = text(e.name, 17, true, Color.WHITE); playerTitle.setSingleLine(true);
        playerTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(43), 1);
        titleParams.leftMargin = dp(10); playerTop.addView(playerTitle, titleParams);
        if (e.video && Build.VERSION.SDK_INT >= 26) {
            playerTop.addView(button("▢", this::pictureInPicture), new LinearLayout.LayoutParams(dp(47), dp(43)));
        }
        addSpace(playerRoot, 11);
        stage = new FrameLayout(this); stage.setBackground(glass(0xFF07101F, 18, 0x4CFFFFFF));
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, e.video ? dp(235) : dp(300));
        stageParams.setMargins(dp(12), 0, dp(12), 0); playerRoot.addView(stage, stageParams);
        if (e.video) {
            video = new VideoView(this); stage.addView(video, new FrameLayout.LayoutParams(-1, -1));
            video.setOnTouchListener(this::gesture);
            captionView = text("", 17, true, Color.WHITE);
            captionView.setGravity(Gravity.CENTER); captionView.setShadowLayer(4, 0, 2, Color.BLACK);
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            cp.setMargins(dp(16), 0, dp(16), dp(16)); stage.addView(captionView, cp);
        } else {
            TextView artwork = text("♫", 110, true, ACCENT[theme]); artwork.setGravity(Gravity.CENTER);
            artwork.setBackground(glass(0x815174A0, 18, 0x50FFFFFF));
            stage.addView(artwork, new FrameLayout.LayoutParams(-1, -1));
        }
        gestureHint = text("", 15, true, Color.WHITE); gestureHint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hint = new FrameLayout.LayoutParams(-1, dp(55), Gravity.CENTER);
        stage.addView(gestureHint, hint);
        lockButton = button("Unlock", () -> setLocked(false));
        FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(dp(85), dp(46), Gravity.TOP | Gravity.RIGHT);
        lockParams.setMargins(0, dp(12), dp(12), 0); stage.addView(lockButton, lockParams);
        lockButton.setVisibility(View.GONE);
        if (e.video) {
            Button exit = button("⛶", this::toggleFullscreen);
            FrameLayout.LayoutParams exitP = new FrameLayout.LayoutParams(dp(48), dp(43), Gravity.TOP | Gravity.LEFT);
            exitP.setMargins(dp(12), dp(12), 0, 0); stage.addView(exit, exitP);
        }
        addSpace(playerRoot, 17);
        ScrollView sc = new ScrollView(this); playerRoot.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));
        playerControls = column(); padded(playerControls, 19, 0); sc.addView(playerControls);
        playerControls.addView(text(e.video ? "NOW PLAYING  /  VIDEO" : "NOW PLAYING  /  MUSIC", 11, true, ACCENT[theme]));
        addSpace(playerControls, 7);
        TextView title = text(e.name, 23, true, Color.WHITE); title.setMaxLines(2); playerControls.addView(title);
        playerControls.addView(text(e.folder, 12, false, 0xFFB7C7D9)); addSpace(playerControls, 18);
        progress = new SeekBar(this); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT[theme]));
        progress.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT[theme]));
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar b) { scrubbing = true; }
            public void onProgressChanged(SeekBar b, int v, boolean user) { if (user && time != null) time.setText(format(v) + " / " + format(duration())); }
            public void onStopTrackingTouch(SeekBar b) { scrubbing = false; jump(b.getProgress()); }
        });
        playerControls.addView(progress);
        time = text("0:00  /  0:00", 12, false, 0xFFC5D2E4); time.setGravity(Gravity.RIGHT);
        playerControls.addView(time); addSpace(playerControls, 13);
        LinearLayout transport = row(); playerControls.addView(transport);
        inRow(transport, button("−10", () -> jump(position() - 10000)), 1, 53);
        inRow(transport, button("◀◀", this::previous), 1, 53);
        play = button("▶", this::toggle); play.setTextSize(22);
        play.setTextColor(BG[theme]); play.setBackground(glass(ACCENT[theme], 20, Color.WHITE));
        inRow(transport, play, 1.3f, 56);
        inRow(transport, button("▶▶", this::next), 1, 53);
        inRow(transport, button("+10", () -> jump(position() + 10000)), 1, 53);
        addSpace(playerControls, 22);
        LinearLayout options = row(); playerControls.addView(options);
        inRow(options, button("Speed " + speed + "×", this::speedDialog), 1, 47);
        inRow(options, button("Repeat", () -> { repeat = !repeat; toast("Repeat " + (repeat ? "on" : "off")); }), 1, 47);
        inRow(options, button("Shuffle", () -> { shuffle = !shuffle; toast("Shuffle " + (shuffle ? "on" : "off")); }), 1, 47);
        addSpace(playerControls, 9);
        LinearLayout more = row(); playerControls.addView(more);
        if (e.video) {
            inRow(more, button("Subtitles", this::pickSubtitle), 1, 47);
            inRow(more, button("Lock", () -> setLocked(true)), 1, 47);
            inRow(more, button("Fit / Zoom", this::toggleZoom), 1, 47);
        } else {
            inRow(more, button("Themes", this::themeDialog), 1, 47);
            inRow(more, button("Add files", this::pickMedia), 1, 47);
        }
        addSpace(playerControls, 30);
        if (audio == null && !videoReady) prepare(active, true);
        else if (e.video && video != null && videoEngine == null) prepare(active, true);
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
            MediaPlayer mp = audioReady ? audio : videoReady ? videoEngine : null;
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
        if (repeat) { jump(0); if (audioReady) audio.start(); else if (videoReady) video.start(); }
        else next();
    }
    private void toggle() {
        if (active < 0) return;
        try {
            if (audioReady) { if (audio.isPlaying()) audio.pause(); else { audio.start(); applySpeed(); } }
            else if (videoReady) { if (video.isPlaying()) video.pause(); else { video.start(); applySpeed(); } }
        } catch (Exception ignored) { }
    }
    private void next() {
        if (media.isEmpty()) return;
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
        for (int n = active - 1; n >= 0; n--) if (media.get(n).video == media.get(active).video) {
            if (onPlayer) { savePosition(); release(); active = n; showPlayer(); }
            else { prepare(n, true); refreshHome(); }
            return;
        }
        jump(0);
    }
    private void jump(int ms) {
        int target = Math.max(0, Math.min(duration(), ms));
        try { if (audioReady) audio.seekTo(target); else if (videoReady) video.seekTo(target); }
        catch (Exception ignored) { }
    }
    private int position() {
        try { return audioReady ? audio.getCurrentPosition() : videoReady && video != null ? video.getCurrentPosition() : 0; }
        catch (Exception ex) { return 0; }
    }
    private int duration() {
        try { return Math.max(0, audioReady ? audio.getDuration() : videoReady && video != null ? video.getDuration() : 0); }
        catch (Exception ex) { return 0; }
    }
    private boolean isPlaying() {
        try { return audioReady ? audio.isPlaying() : videoReady && video != null && video.isPlaying(); }
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
        videoReady = false; videoEngine = null; audioReady = false;
        if (video != null) video.stopPlayback();
        if (audio != null) { audio.release(); audio = null; }
    }
    @Override public void onBackPressed() {
        if (locked) { setLocked(false); return; }
        if (fullscreen) { toggleFullscreen(); return; }
        if (onPlayer) { leavePlayer(); return; }
        super.onBackPressed();
    }
    @Override protected void onPause() {
        super.onPause(); savePosition();
        if (audioReady && audio.isPlaying()) audio.pause();
        if (videoReady && video != null && video.isPlaying() &&
            (Build.VERSION.SDK_INT < 26 || (!isInPictureInPictureMode() && !enteringPip))) video.pause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker); thumbnails.shutdownNow(); release(); super.onDestroy();
    }
}

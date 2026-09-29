package com.auraplayer.offline;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int PICK = 42;
    private static final int[] BACKGROUNDS = {0xFF0B1020, 0xFF071A18, 0xFF263044, 0xFF241625};
    private static final int[] ACCENTS = {0xFF94B8FF, 0xFF79E5C3, 0xFFDEE8FF, 0xFFF4B5D4};
    private static final String[] THEMES = {"Midnight", "Emerald", "Pearl", "Rose"};
    private final ArrayList<Entry> library = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private VideoView video;
    private MediaPlayer audio;
    private FrameLayout stage;
    private LinearLayout list;
    private TextView title, details, clock;
    private Button playButton, shuffleButton, repeatButton;
    private SeekBar seek;
    private int index = -1, theme = 0;
    private boolean shuffle = false, repeat = false, seeking = false, videoReady = false, audioReady = false;
    private boolean fullScreen = false;
    private int pendingPosition = 0;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            int length = duration(), at = position();
            if (!seeking && seek != null) { seek.setMax(Math.max(1, length)); seek.setProgress(at); }
            if (clock != null) clock.setText(format(at) + " / " + format(length));
            if (playButton != null) playButton.setText(isPlaying() ? "Ⅱ  Pause" : "▶  Play");
            handler.postDelayed(this, 450);
        }
    };

    private static class Entry {
        String uri, name;
        boolean video;
        Entry(String u, String n, boolean v) { uri = u; name = n; video = v; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        theme = getPreferences(0).getInt("theme", 0);
        theme = Math.max(0, Math.min(3, theme));
        loadLibrary();
        draw();
        handler.post(tick);
    }

    private int dp(float n) { return (int)(getResources().getDisplayMetrics().density * n + .5f); }
    private GradientDrawable panel(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{color, (color & 0x00FFFFFF) | 0x18000000});
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }
    private TextView label(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }
    private Button button(String value, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackground(panel(0x405A6782, 0x55FFFFFF, 16));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(1); return l; }
    private void pad(View v, int n) { v.setPadding(dp(n), dp(n), dp(n), dp(n)); }
    private void gap(LinearLayout p, int h) { View v = new View(this); p.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private void rowAdd(LinearLayout row, View child, int weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), weight);
        p.setMargins(dp(3), 0, dp(3), 0); row.addView(child, p);
    }
    private void draw() {
        int accent = ACCENTS[theme];
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUNDS[theme]);
        LinearLayout root = vertical(); pad(root, 18); scroll.addView(root);
        setContentView(scroll);

        TextView eyebrow = label("YOUR PRIVATE MEDIA SPACE", 11, true, accent);
        root.addView(eyebrow); gap(root, 5);
        root.addView(label("AURA Player", 32, true, Color.WHITE));
        TextView subtitle = label("Offline • Ad free • Yours", 13, false, 0xFFBBC4D7);
        root.addView(subtitle); gap(root, 18);

        LinearLayout themes = new LinearLayout(this);
        for (int i = 0; i < THEMES.length; i++) {
            final int chosen = i;
            Button chip = button(THEMES[i], () -> {
                if (theme == chosen) return;
                int old = position(); boolean wasPlaying = isPlaying();
                theme = chosen; getPreferences(0).edit().putInt("theme", theme).apply();
                if (index >= 0) savePosition(index, old);
                release(); draw();
                if (index >= 0) open(index, wasPlaying);
            });
            chip.setTextSize(11);
            chip.setTextColor(i == theme ? BACKGROUNDS[theme] : Color.WHITE);
            if (i == theme) chip.setBackground(panel(accent, 0xDDFFFFFF, 15));
            themes.addView(chip, new LinearLayout.LayoutParams(0, dp(42), 1));
        }
        root.addView(themes); gap(root, 19);

        LinearLayout card = vertical(); pad(card, 14);
        card.setBackground(panel(0x324E6080, 0x66FFFFFF, 25));
        root.addView(card);

        stage = new FrameLayout(this); stage.setBackground(panel(0x55405674, 0x33FFFFFF, 19));
        card.addView(stage, new LinearLayout.LayoutParams(-1, dp(210)));
        TextView mark = label("AURA", 42, true, 0xA9FFFFFF); mark.setGravity(Gravity.CENTER);
        stage.addView(mark, new FrameLayout.LayoutParams(-1, -1));
        video = new VideoView(this); video.setVisibility(View.GONE);
        stage.addView(video, new FrameLayout.LayoutParams(-1, -1));

        gap(card, 16);
        title = label("Choose your local files", 19, true, Color.WHITE); card.addView(title);
        details = label("Audio and video live together here", 12, false, 0xFFC4CEE0);
        card.addView(details); gap(card, 10);
        seek = new SeekBar(this); seek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) { seeking = true; }
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { if (fromUser) clock.setText(format(p) + " / " + format(duration())); }
            @Override public void onStopTrackingTouch(SeekBar s) { seeking = false; jumpTo(s.getProgress()); }
        });
        card.addView(seek);
        clock = label("0:00 / 0:00", 12, false, 0xFFD5DCEE); clock.setGravity(Gravity.RIGHT);
        card.addView(clock); gap(card, 8);

        LinearLayout controls = new LinearLayout(this);
        rowAdd(controls, button("−10s", () -> jumpTo(position() - 10000)), 1);
        rowAdd(controls, button("◀◀", () -> previous()), 1);
        playButton = button("▶  Play", () -> toggle()); rowAdd(controls, playButton, 2);
        rowAdd(controls, button("▶▶", () -> next()), 1);
        rowAdd(controls, button("+10s", () -> jumpTo(position() + 10000)), 1);
        card.addView(controls); gap(card, 8);

        LinearLayout modes = new LinearLayout(this);
        shuffleButton = button("Shuffle: Off", () -> { shuffle = !shuffle; updateModes(); });
        repeatButton = button("Repeat: Off", () -> { repeat = !repeat; updateModes(); });
        rowAdd(modes, shuffleButton, 1); rowAdd(modes, repeatButton, 1);
        rowAdd(modes, button("⛶ Fullscreen", () -> toggleFullScreen()), 1);
        card.addView(modes); gap(root, 18);

        Button add = button("＋  Add local files", this::pickFiles);
        add.setTextColor(BACKGROUNDS[theme]); add.setBackground(panel(accent, Color.WHITE, 18));
        root.addView(add, new LinearLayout.LayoutParams(-1, dp(56)));
        gap(root, 18);
        root.addView(label("MY LIBRARY", 12, true, accent)); gap(root, 8);
        list = vertical(); root.addView(list); refreshList();
    }

    private void updateModes() {
        shuffleButton.setText("Shuffle: " + (shuffle ? "On" : "Off"));
        repeatButton.setText("Repeat: " + (repeat ? "On" : "Off"));
    }
    private void refreshList() {
        list.removeAllViews();
        if (library.isEmpty()) { list.addView(label("No files yet. Add music or videos above.", 14, false, 0xFFC5CDDA)); return; }
        for (int i = 0; i < library.size(); i++) {
            final int selected = i; Entry entry = library.get(i);
            LinearLayout item = vertical(); pad(item, 14);
            item.setBackground(panel(i == index ? 0x665E77A1 : 0x304E6080,
                    i == index ? ACCENTS[theme] : 0x33FFFFFF, 17));
            item.addView(label((entry.video ? "▣  " : "♫  ") + entry.name, 15, i == index, Color.WHITE));
            item.addView(label(entry.video ? "VIDEO" : "AUDIO", 10, false, ACCENTS[theme]));
            item.setOnClickListener(v -> open(selected, true));
            item.setOnLongClickListener(v -> {
                new android.app.AlertDialog.Builder(this).setTitle("Remove from library?")
                    .setMessage(entry.name + "\nThe original file stays on your device.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Remove", (d, w) -> {
                        if (selected == index) { release(); index = -1; title.setText("Choose your local files"); details.setText("Audio and video live together here"); }
                        else if (selected < index) index--;
                        library.remove(selected); saveLibrary(); refreshList();
                    }).show();
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8); list.addView(item, lp);
        }
    }

    private void pickFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK || resultCode != RESULT_OK || data == null) return;
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) addUri(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) addUri(data.getData());
        saveLibrary(); refreshList();
    }
    private void addUri(Uri uri) {
        for (Entry e : library) if (e.uri.equals(uri.toString())) return;
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) { /* Provider may only grant temporary access. */ }
        String name = uri.getLastPathSegment(), mime = getContentResolver().getType(uri);
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        } catch (Exception ignored) { }
        if (name == null) name = "Local media";
        boolean isVideo = (mime != null && mime.startsWith("video/")) || name.matches("(?i).*\\.(mp4|mkv|webm|3gp|mov)$");
        library.add(new Entry(uri.toString(), name, isVideo));
    }

    private void open(int selected, boolean autoplay) {
        if (selected < 0 || selected >= library.size()) return;
        if (index >= 0 && index != selected) savePosition(index, position());
        release(); index = selected;
        Entry e = library.get(index);
        title.setText(e.name); details.setText(e.video ? "LOCAL VIDEO" : "LOCAL AUDIO");
        video.setVisibility(e.video ? View.VISIBLE : View.GONE);
        pendingPosition = getPreferences(0).getInt("pos_" + e.uri.hashCode(), 0);
        try {
            if (e.video) {
                videoReady = false;
                video.setOnPreparedListener(mp -> {
                    videoReady = true;
                    video.seekTo(pendingPosition);
                    if (autoplay) video.start();
                });
                video.setOnCompletionListener(mp -> completed());
                video.setOnErrorListener((mp, what, extra) -> { failed(); return true; });
                video.setVideoURI(Uri.parse(e.uri));
            } else {
                audio = new MediaPlayer();
                audio.setDataSource(this, Uri.parse(e.uri));
                audio.setOnPreparedListener(mp -> {
                    audioReady = true;
                    mp.seekTo(pendingPosition);
                    if (autoplay) mp.start();
                });
                audio.setOnCompletionListener(mp -> completed());
                audio.setOnErrorListener((mp, what, extra) -> { failed(); return true; });
                audio.prepareAsync();
            }
        } catch (Exception exception) { failed(); }
        refreshList();
    }
    private void failed() { Toast.makeText(this, "Can't play this file. Check that it is stored on this device and supported by Android.", Toast.LENGTH_LONG).show(); }
    private void completed() {
        if (index >= 0) savePosition(index, 0);
        if (repeat) { jumpTo(0); if (audio != null) audio.start(); else if (videoReady) video.start(); }
        else if (index + 1 < library.size()) open(index + 1, true);
    }
    private void toggle() {
        if (index < 0) { if (!library.isEmpty()) open(0, true); else pickFiles(); return; }
        if (audio != null && audioReady) { if (audio.isPlaying()) audio.pause(); else audio.start(); }
        else if (videoReady) { if (video.isPlaying()) video.pause(); else video.start(); }
    }
    private void next() {
        if (library.isEmpty()) return;
        int n = shuffle && library.size() > 1 ? random.nextInt(library.size() - 1) : -1;
        if (n >= 0 && n >= index) n++;
        open(n < 0 ? (index + 1) % library.size() : n, true);
    }
    private void previous() { if (!library.isEmpty()) open((index - 1 + library.size()) % library.size(), true); }
    private void jumpTo(int ms) {
        int value = Math.max(0, Math.min(duration(), ms));
        if (audio != null && audioReady) { try { audio.seekTo(value); } catch (Exception ignored) { } }
        else if (videoReady) video.seekTo(value);
    }
    private boolean isPlaying() {
        try { return audio != null ? audioReady && audio.isPlaying() : videoReady && video.isPlaying(); }
        catch (Exception ignored) { return false; }
    }
    private int position() {
        try { return audio != null ? audioReady ? audio.getCurrentPosition() : 0 : videoReady ? video.getCurrentPosition() : 0; }
        catch (Exception ignored) { return 0; }
    }
    private int duration() {
        try { return Math.max(0, audio != null ? audioReady ? audio.getDuration() : 0 : videoReady ? video.getDuration() : 0); }
        catch (Exception ignored) { return 0; }
    }
    private String format(int ms) {
        int s = Math.max(0, ms / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }
    private void savePosition(int i, int ms) {
        if (i >= 0 && i < library.size()) getPreferences(0).edit()
            .putInt("pos_" + library.get(i).uri.hashCode(), Math.max(0, ms)).apply();
    }
    private void release() {
        videoReady = false;
        audioReady = false;
        if (video != null) video.stopPlayback();
        if (audio != null) { audio.release(); audio = null; }
    }
    private void toggleFullScreen() {
        if (index < 0 || !library.get(index).video) return;
        fullScreen = !fullScreen;
        if (fullScreen) {
            getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            stage.getLayoutParams().height = getResources().getDisplayMetrics().heightPixels - dp(20);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(0);
            stage.getLayoutParams().height = dp(210);
        }
        stage.requestLayout();
    }
    @Override public void onConfigurationChanged(Configuration c) { super.onConfigurationChanged(c); }
    @Override protected void onPause() {
        super.onPause();
        if (index >= 0) savePosition(index, position());
        if (audio != null && audioReady && audio.isPlaying()) audio.pause();
        if (videoReady && video.isPlaying()) video.pause();
    }
    @Override protected void onDestroy() { handler.removeCallbacks(tick); release(); super.onDestroy(); }

    private void saveLibrary() {
        JSONArray items = new JSONArray();
        for (Entry e : library) {
            JSONObject obj = new JSONObject();
            try { obj.put("uri", e.uri); obj.put("name", e.name); obj.put("video", e.video); items.put(obj); }
            catch (Exception ignored) { }
        }
        getPreferences(0).edit().putString("library", items.toString()).apply();
    }
    private void loadLibrary() {
        try {
            JSONArray items = new JSONArray(getPreferences(0).getString("library", "[]"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.getJSONObject(i);
                library.add(new Entry(obj.getString("uri"), obj.getString("name"), obj.getBoolean("video")));
            }
        } catch (Exception ignored) { library.clear(); }
    }
}

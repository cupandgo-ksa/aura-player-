package com.auraplayer.offline;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
import android.util.Log;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Locale;

public class GlassActivity extends Activity {
    private static final int PERMISSION = 10, PICK = 11;
    private static final String ORIGIN = "https://aura.local";
    private WebView web;
    private FrameLayout root;
    private volatile String queueJson = "[]";
    private volatile boolean shuffleMode = false;
    private volatile String repeatMode = "all";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF080B12);
        getWindow().setNavigationBarColor(0xFF080B12);
        root = new FrameLayout(this);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        web.setBackgroundColor(0xFF080B12);
        web.addJavascriptInterface(new Bridge(), "AuraNative");
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                Log.d("AURA_WEB", message.messageLevel() + ": " + message.message());
                return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"aura.local".equals(uri.getHost()))
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                try {
                    if ("/cover".equals(uri.getPath())) return cover(uri.getQueryParameter("u"));
                    String path = uri.getPath();
                    path = path == null || path.equals("/") ? "index.html" : path.replaceFirst("^/", "");
                    if (path.contains("..")) return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                    InputStream stream = getAssets().open(path);
                    Log.d("AURA_WEB", "Asset " + path);
                    String mime = URLConnection.guessContentTypeFromName(path);
                    if (path.endsWith(".js")) mime = "application/javascript";
                    if (path.endsWith(".css")) mime = "text/css";
                    return new WebResourceResponse(mime == null ? "application/octet-stream" : mime, "UTF-8", stream);
                } catch (Exception error) {
                    Log.e("AURA_WEB", "Asset failed: " + uri, error);
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e("AURA_WEB", "WebView error " + request.getUrl() + " " + error.getDescription());
            }
            @Override public void onPageFinished(WebView view, String url) {
                Log.d("AURA_WEB", "Page finished: " + url);
                new android.os.Handler(getMainLooper()).postDelayed(() ->
                    web.evaluateJavascript("document.getElementById('root')?.innerHTML.length ?? -1",
                        result -> Log.d("AURA_WEB", "Root length " + result)), 8000);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"aura.local".equals(request.getUrl().getHost());
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        try (InputStream input = getAssets().open("index.html")) {
            byte[] bytes = new byte[input.available()];
            int size = input.read(bytes);
            web.loadDataWithBaseURL(ORIGIN + "/", new String(bytes, 0, size, java.nio.charset.StandardCharsets.UTF_8),
                "text/html", "UTF-8", null);
        } catch (Exception error) { Log.e("AURA_WEB", "Could not open interface", error); }
        new android.os.Handler(getMainLooper()).postDelayed(() -> web.evaluateJavascript(
            "JSON.stringify({url:location.href,ready:document.readyState,root:document.getElementById('root')?.innerHTML.length,script:document.scripts.length})",
            result -> Log.d("AURA_WEB", "Web state " + result)), 11000);
        requestMedia();
    }

    private void requestMedia() {
        String[] permissions = Build.VERSION.SDK_INT >= 33
            ? new String[]{Manifest.permission.READ_MEDIA_AUDIO}
            : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        boolean missing = false;
        for (String p : permissions) if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing = true;
        if (missing) requestPermissions(permissions, PERMISSION);
    }
    @Override public void onRequestPermissionsResult(int request, String[] p, int[] grants) {
        super.onRequestPermissionsResult(request, p, grants);
        if (request == PERMISSION) notifyWeb();
    }
    private void notifyWeb() {
        runOnUiThread(() -> web.evaluateJavascript("window.dispatchEvent(new Event('auraLibraryChanged'))", null));
    }

    private JSONObject track(String uri, String title, String artist, String album, String folder, boolean video, long duration, long size) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", uri); o.put("url", uri); o.put("uri", uri);
            o.put("title", title == null ? "Unknown" : title);
            o.put("artist", artist == null || artist.equals("<unknown>") ? "Local media" : artist);
            o.put("album", album == null ? "" : album);
            o.put("duration", duration / 1000);
            o.put("type", video ? "video" : "audio"); o.put("size", size);
            o.put("format", video ? "VIDEO" : "AUDIO"); o.put("addedAt", System.currentTimeMillis());
            o.put("isFavorite", false); o.put("folder", folder);
            o.put("coverUrl", ORIGIN + "/cover?u=" + Uri.encode(uri));
        } catch (Exception ignored) { }
        return o;
    }
    private String mediaJson() {
        JSONArray all = new JSONArray();
        HashSet<String> seen = new HashSet<>();
        readCollection(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, all, seen);
        try {
            JSONArray added = new JSONArray(getPreferences(0).getString("added", "[]"));
            for (int i = 0; i < added.length(); i++) {
                String value = added.getString(i);
                if (!seen.add(value)) continue;
                Uri uri = Uri.parse(value);
                String name = uri.getLastPathSegment();
                long size = 0;
                try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                    if (c != null && c.moveToFirst()) { name = c.getString(0); size = c.getLong(1); }
                } catch (Exception ignored) { }
                String mime = getContentResolver().getType(uri);
                if (mime != null && !mime.startsWith("audio/")) continue;
                String title = name == null ? "Local file" : name.replaceFirst("(?i)\\.[^.]+$", "").replace('_', ' ');
                JSONObject item = track(value, title, "Local media", "", "Added files", false, 0, size);
                all.put(item);
            }
        } catch (Exception ignored) { }
        queueJson = all.toString();
        return all.toString();
    }
    private void readCollection(Uri collection, JSONArray all, HashSet<String> seen) {
        if (checkSelfPermission(Build.VERSION.SDK_INT >= 33
            ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) return;
        String[] cols = {MediaStore.MediaColumns._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.MediaColumns.DURATION, MediaStore.MediaColumns.SIZE,
            Build.VERSION.SDK_INT >= 29 ? MediaStore.MediaColumns.RELATIVE_PATH : MediaStore.MediaColumns.DATA,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};
        try (Cursor c = getContentResolver().query(collection, cols, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (c == null) return;
            while (c.moveToNext()) {
                String uri = ContentUris.withAppendedId(collection, c.getLong(0)).toString();
                if (!seen.add(uri)) continue;
                String name = c.getString(1);
                if (name == null) continue;
                String folder = c.getString(4);
                all.put(track(uri, name.replaceFirst("(?i)\\.[^.]+$", "").replace('_', ' '),
                    c.getString(5), c.getString(6), folder == null ? "On this device" : folder,
                    false, c.getLong(2), c.getLong(3)));
            }
        } catch (Exception ignored) { }
    }

    private WebResourceResponse cover(String value) {
        if (value != null && value.startsWith("content://")) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(this, Uri.parse(value));
                byte[] bytes = retriever.getEmbeddedPicture();
                if (bytes != null) return new WebResourceResponse("image/jpeg", null, new ByteArrayInputStream(bytes));
            } catch (Exception ignored) { }
            finally { try { retriever.release(); } catch (Exception ignored) { } }
        }
        String letter = "A";
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 300 300'><defs><linearGradient id='g' x2='1' y2='1'><stop stop-color='#554375'/><stop offset='1' stop-color='#141a29'/></linearGradient></defs><rect width='300' height='300' fill='url(#g)'/><circle cx='150' cy='150' r='94' fill='none' stroke='#ffffff' stroke-opacity='.25'/><text x='150' y='176' fill='#ffffff' text-anchor='middle' font-family='sans-serif' font-size='88'>" + letter + "</text></svg>";
        return new WebResourceResponse("image/svg+xml", "UTF-8", new ByteArrayInputStream(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    private void pickFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("audio/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK || result != RESULT_OK || data == null) return;
        try {
            JSONArray items = new JSONArray(getPreferences(0).getString("added", "[]"));
            int count = data.getClipData() == null ? 1 : data.getClipData().getItemCount();
            for (int n = 0; n < count; n++) {
                Uri uri = data.getClipData() == null ? data.getData() : data.getClipData().getItemAt(n).getUri();
                if (uri == null) continue;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) { }
                if (!items.toString().contains(uri.toString())) items.put(uri.toString());
            }
            getPreferences(0).edit().putString("added", items.toString()).apply();
        } catch (Exception ignored) { }
        notifyWeb();
    }
    private class Bridge {
        @JavascriptInterface public String getMedia() { return mediaJson(); }
        @JavascriptInterface public void pickMedia() { runOnUiThread(() -> pickFiles()); }
        @JavascriptInterface public void setQueue(String json) {
            if (json == null) return;
            queueJson = json;
            runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.updateQueue(json); });
        }
        @JavascriptInterface public void mode(boolean shuffle, String repeat) {
            shuffleMode = shuffle; repeatMode = repeat;
            runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.setPlaybackMode(shuffle, repeat); });
        }
        @JavascriptInterface public void play(String uri, String title, String artist) {
            if (uri == null || !uri.startsWith("content://")) return;
            Intent i = new Intent(GlassActivity.this, AudioService.class).setAction(AudioService.PLAY)
                .putExtra("uri", uri).putExtra("title", title).putExtra("artist", artist)
                .putExtra("queue", queueJson).putExtra("shuffle", shuffleMode).putExtra("repeat", repeatMode);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        }
        @JavascriptInterface public void pause() { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.pause(); }); }
        @JavascriptInterface public void resume() { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.resume(); }); }
        @JavascriptInterface public void seek(int ms) { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.seek(ms); }); }
        @JavascriptInterface public void speed(double speed) { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.speed((float)speed); }); }
        @JavascriptInterface public void volume(double value) { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.volume((float)value); }); }
        @JavascriptInterface public void band(int index, double value) { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.setBand(index, (float)value); }); }
        @JavascriptInterface public void bass(double value) { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.setBass((float)value); }); }
        @JavascriptInterface public void treble(double value) { runOnUiThread(() -> { if (AudioService.instance != null) AudioService.instance.setTreble((float)value); }); }
        @JavascriptInterface public String state() {
            AudioService s = AudioService.instance;
            JSONObject o = new JSONObject();
            try {
                o.put("uri", s == null ? "" : s.uri);
                o.put("playing", s != null && s.isPlaying());
                o.put("position", s == null ? 0 : s.position());
                o.put("duration", s == null ? 0 : s.duration());
            } catch (Exception ignored) { }
            return o.toString();
        }
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){var open=!!document.querySelector('[data-audio-menu], [data-header-menu=open], [data-player-popover], [class*=fixed][class*=inset-0]');window.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));return open})()", value -> {
            if (!"true".equals(value)) moveTaskToBack(true);
        });
    }
    @Override protected void onDestroy() {
        web.destroy(); super.onDestroy();
    }
}

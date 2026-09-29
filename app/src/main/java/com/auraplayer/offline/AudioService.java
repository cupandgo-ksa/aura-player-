package com.auraplayer.offline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/** Owns music playback independently of the Activity and its screen lifecycle. */
public class AudioService extends Service {
    public static final String PLAY = "com.auraplayer.offline.PLAY";
    public static final String TOGGLE = "com.auraplayer.offline.TOGGLE";
    public static final String NEXT = "com.auraplayer.offline.NEXT";
    public static final String PREVIOUS = "com.auraplayer.offline.PREVIOUS";
    public static final String STOP = "com.auraplayer.offline.STOP";
    private static final String CHANNEL = "aura_music";
    private static final int NOTIFICATION = 19;
    public static AudioService instance;
    public String uri = "", title = "", artist = "";
    private final ArrayList<Track> queue = new ArrayList<>();
    private MediaPlayer player;
    private Equalizer equalizer;
    private final float[] bands = new float[5];
    private float bass, treble;
    private boolean ready, requestedPlay;
    private int queueIndex = -1;
    private boolean shuffleMode;
    private String repeatMode = "all";
    private float speed = 1f;
    private float volume = .85f;
    private MediaSession session;
    private AudioManager audioManager;
    private BroadcastReceiver noisy;

    private static class Track {
        String uri, title, artist;
        Track(String u, String t, String a) { uri = u; title = t; artist = a; }
    }

    @Override public void onCreate() {
        super.onCreate(); instance = this;
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        NotificationManager notifications = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26)
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "Music playback", NotificationManager.IMPORTANCE_LOW));
        session = new MediaSession(this, "A S media-player");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { next(); }
            @Override public void onSkipToPrevious() { previous(); }
            @Override public void onSeekTo(long pos) { seek((int)pos); }
        });
        session.setActive(true);
        noisy = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { pause(); }
        };
        registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (PLAY.equals(action)) {
            readQueue(intent.getStringExtra("queue"));
            setPlaybackMode(intent.getBooleanExtra("shuffle", false), intent.getStringExtra("repeat"));
            String target = intent.getStringExtra("uri");
            if (target == null) { stopSelf(); return START_NOT_STICKY; }
            title = intent.getStringExtra("title"); artist = intent.getStringExtra("artist"); uri = target;
            if (title == null) title = "A S media-player";
            if (artist == null) artist = "Offline music";
            queueIndex = -1;
            for (int n = 0; n < queue.size(); n++) if (queue.get(n).uri.equals(uri)) { queueIndex = n; break; }
            startForeground(NOTIFICATION, notification());
            play(target, intent.getIntExtra("position", 0));
        } else if (TOGGLE.equals(action)) toggle();
        else if (NEXT.equals(action)) next();
        else if (PREVIOUS.equals(action)) previous();
        else if (STOP.equals(action)) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
        return START_NOT_STICKY;
    }

    private void readQueue(String json) {
        if (json == null) return;
        queue.clear();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                queue.add(new Track(o.getString("uri"), o.optString("title", "Music"), o.optString("artist", "")));
            }
        } catch (Exception ignored) { }
    }

    private void play(String target, int position) {
        releasePlayer(); ready = false; requestedPlay = true;
        try {
            audioManager.requestAudioFocus(focus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            MediaPlayer next = new MediaPlayer(); player = next;
            next.setAudioStreamType(AudioManager.STREAM_MUSIC);
            next.setDataSource(this, Uri.parse(target));
            next.setOnPreparedListener(mp -> {
                if (mp != player) return;
                ready = true;
                mp.setVolume(volume, volume);
                attachEqualizer();
                if (position > 0) mp.seekTo(position);
                if (requestedPlay) resume(); else updateNotification();
            });
            next.setOnCompletionListener(mp -> { if (mp == player) { if ("one".equals(repeatMode)) { seek(0); resume(); } else nextAfterCompletion(); } });
            next.setOnErrorListener((mp, what, extra) -> {
                if (mp == player) { releasePlayer(); updateNotification(); }
                return true;
            });
            next.prepareAsync();
        } catch (Exception ex) { releasePlayer(); updateNotification(); }
    }

    private final AudioManager.OnAudioFocusChangeListener focus = change -> {
        if (change <= 0) pause();
    };

    public void toggle() { if (isPlaying()) pause(); else resume(); }
    public void pause() {
        requestedPlay = false;
        try { if (ready && player != null) player.pause(); } catch (Exception ignored) { }
        updateNotification();
    }
    public void resume() {
        requestedPlay = true;
        try {
            if (ready && player != null) {
                player.start();
                if (speed != 1f) player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
            }
        } catch (Exception ignored) { }
        updateNotification();
    }
    public void updateQueue(String json) {
        readQueue(json);
        queueIndex = -1;
        for (int n = 0; n < queue.size(); n++)
            if (queue.get(n).uri.equals(uri)) { queueIndex = n; break; }
    }
    public void setPlaybackMode(boolean shuffle, String repeat) {
        shuffleMode = shuffle;
        repeatMode = "one".equals(repeat) || "off".equals(repeat) ? repeat : "all";
    }
    private void nextAfterCompletion() {
        if (queue.isEmpty()) { pause(); return; }
        if (!shuffleMode && "off".equals(repeatMode) && queueIndex >= queue.size() - 1) { pause(); return; }
        next();
    }
    public void next() {
        if (queue.isEmpty()) { pause(); return; }
        if (shuffleMode && queue.size() > 1) {
            int index = queueIndex;
            while (index == queueIndex) index = (int)(Math.random() * queue.size());
            changeTrack(index);
        } else changeTrack(queueIndex + 1 < queue.size() ? queueIndex + 1 : 0);
    }
    public void previous() {
        if (position() > 4000) seek(0);
        else if (!queue.isEmpty()) changeTrack(queueIndex > 0 ? queueIndex - 1 : queue.size() - 1);
    }
    private void changeTrack(int index) {
        if (index < 0 || index >= queue.size()) { pause(); return; }
        Track track = queue.get(index); queueIndex = index;
        uri = track.uri; title = track.title; artist = track.artist;
        updateNotification(); play(uri, 0);
    }
    public void seek(int ms) { try { if (ready && player != null) player.seekTo(ms); } catch (Exception ignored) { } }
    public int position() { try { return ready && player != null ? player.getCurrentPosition() : 0; } catch (Exception ex) { return 0; } }
    public int duration() { try { return ready && player != null ? player.getDuration() : 0; } catch (Exception ex) { return 0; } }
    public boolean isPlaying() { try { return ready && player != null && player.isPlaying(); } catch (Exception ex) { return false; } }
    public void speed(float value) {
        speed = value;
        if (!ready || player == null) return;
        try { boolean wasPlaying = isPlaying(); player.setPlaybackParams(player.getPlaybackParams().setSpeed(value)); if (!wasPlaying) player.pause(); }
        catch (Exception ignored) { }
    }
    private void attachEqualizer() {
        try {
            if (equalizer != null) equalizer.release();
            equalizer = new Equalizer(0, player.getAudioSessionId());
            equalizer.setEnabled(true);
            for (int i = 0; i < bands.length; i++) applyBand(i);
        } catch (Exception ignored) { equalizer = null; }
    }
    public void setBand(int index, float db) {
        if (index < 0 || index >= bands.length) return;
        bands[index] = Math.max(-12, Math.min(12, db)); applyBand(index);
    }
    public void setBass(float value) { bass = Math.max(0, Math.min(100, value)); applyBand(0); }
    public void setTreble(float value) { treble = Math.max(0, Math.min(100, value)); applyBand(4); }
    private void applyBand(int index) {
        if (equalizer == null) return;
        try {
            short band = (short)Math.min(equalizer.getNumberOfBands() - 1,
                Math.round(index * (equalizer.getNumberOfBands() - 1) / 4f));
            short[] range = equalizer.getBandLevelRange();
            float db = bands[index] + (index == 0 ? bass * .08f : index == 4 ? treble * .08f : 0);
            short level = (short)Math.max(range[0], Math.min(range[1], Math.round(db * 100)));
            equalizer.setBandLevel(band, level);
        } catch (Exception ignored) { }
    }
    public void volume(float value) {
        volume = Math.max(0, Math.min(1, value));
        try { if (player != null) player.setVolume(volume, volume); } catch (Exception ignored) { }
    }

    private PendingIntent action(String value, int request) {
        Intent i = new Intent(this, AudioService.class).setAction(value);
        return PendingIntent.getService(this, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification() {
        Intent launch = new Intent(this, GlassActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist).putLong(MediaMetadata.METADATA_KEY_DURATION, duration()).build());
        long actions = android.media.session.PlaybackState.ACTION_PLAY | android.media.session.PlaybackState.ACTION_PAUSE |
            android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT | android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS |
            android.media.session.PlaybackState.ACTION_SEEK_TO;
        session.setPlaybackState(new android.media.session.PlaybackState.Builder().setActions(actions)
            .setState(isPlaying() ? android.media.session.PlaybackState.STATE_PLAYING : android.media.session.PlaybackState.STATE_PAUSED,
                position(), speed).build());
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        if (Build.VERSION.SDK_INT < 26) builder.setPriority(Notification.PRIORITY_LOW);
        return builder.setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(artist).setContentIntent(open).setOngoing(isPlaying())
            .setColor(Color.DKGRAY).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(new Notification.Action.Builder(R.drawable.ic_music_prev, "Previous", action(PREVIOUS, 2)).build())
            .addAction(new Notification.Action.Builder(isPlaying() ? R.drawable.ic_music_pause : R.drawable.ic_music_play,
                isPlaying() ? "Pause" : "Play", action(TOGGLE, 3)).build())
            .addAction(new Notification.Action.Builder(R.drawable.ic_music_next, "Next", action(NEXT, 4)).build())
            .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
            .build();
    }
    private void updateNotification() {
        if (uri.isEmpty()) return;
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION, notification());
    }
    private void releasePlayer() {
        ready = false;
        if (equalizer != null) { try { equalizer.release(); } catch (Exception ignored) { } equalizer = null; }
        if (player != null) { try { player.reset(); player.release(); } catch (Exception ignored) { } player = null; }
    }
    @Override public void onDestroy() {
        releasePlayer(); audioManager.abandonAudioFocus(focus);
        unregisterReceiver(noisy); session.release(); instance = null;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}

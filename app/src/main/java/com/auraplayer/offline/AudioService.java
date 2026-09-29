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
    private boolean ready, requestedPlay;
    private int queueIndex = -1;
    private float speed = 1f;
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
        session = new MediaSession(this, "AURA Player");
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
            String target = intent.getStringExtra("uri");
            if (target == null) { stopSelf(); return START_NOT_STICKY; }
            title = intent.getStringExtra("title"); artist = intent.getStringExtra("artist"); uri = target;
            if (title == null) title = "AURA Player";
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
                if (position > 0) mp.seekTo(position);
                if (requestedPlay) resume(); else updateNotification();
            });
            next.setOnCompletionListener(mp -> { if (mp == player) this.next(); });
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
    public void next() { changeTrack(queueIndex + 1); }
    public void previous() { if (position() > 4000) seek(0); else changeTrack(queueIndex - 1); }
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

    private PendingIntent action(String value, int request) {
        Intent i = new Intent(this, AudioService.class).setAction(value);
        return PendingIntent.getService(this, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification() {
        Intent launch = new Intent(this, PremiumActivity.class);
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
        return builder.setSmallIcon(com.auraplayer.offline.R.drawable.ic_launcher)
            .setContentTitle(title).setContentText(artist).setContentIntent(open).setOngoing(isPlaying())
            .setColor(Color.DKGRAY).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(new Notification.Action.Builder(com.auraplayer.offline.R.drawable.ic_launcher, "Previous", action(PREVIOUS, 2)).build())
            .addAction(new Notification.Action.Builder(com.auraplayer.offline.R.drawable.ic_launcher, isPlaying() ? "Pause" : "Play", action(TOGGLE, 3)).build())
            .addAction(new Notification.Action.Builder(com.auraplayer.offline.R.drawable.ic_launcher, "Next", action(NEXT, 4)).build())
            .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
            .build();
    }
    private void updateNotification() {
        if (uri.isEmpty()) return;
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION, notification());
    }
    private void releasePlayer() {
        ready = false;
        if (player != null) { try { player.reset(); player.release(); } catch (Exception ignored) { } player = null; }
    }
    @Override public void onDestroy() {
        releasePlayer(); audioManager.abandonAudioFocus(focus);
        unregisterReceiver(noisy); session.release(); instance = null;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}

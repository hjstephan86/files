package de.epp.cfiles;

import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private MediaSession mediaSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(ZipPlugin.class);
        super.onCreate(savedInstanceState);
        requestManageFilesPermission();
        setupMediaSession();
    }

    private void setupMediaSession() {
        // Audio-Fokus anfordern
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) {
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        mediaSession = new MediaSession(this, "cfiles");
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        PlaybackState state = new PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PAUSE |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
            .build();
        mediaSession.setPlaybackState(state);

        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                js("if(typeof playerTogglePause==='function'&&document.getElementById('audioEl').paused)playerTogglePause();");
            }
            @Override
            public void onPause() {
                js("if(typeof playerTogglePause==='function'&&!document.getElementById('audioEl').paused)playerTogglePause();");
            }
            @Override
            public void onSkipToNext() {
                js("if(typeof playerNext==='function')playerNext();");
            }
            @Override
            public void onSkipToPrevious() {
                js("if(typeof playerPrev==='function')playerPrev();");
            }
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent ev = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ev != null && ev.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (ev.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            onSkipToNext(); return true;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            onSkipToPrevious(); return true;
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            js("if(typeof playerTogglePause==='function')playerTogglePause();"); return true;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            onPlay(); return true;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            onPause(); return true;
                    }
                }
                return super.onMediaButtonEvent(intent);
            }
        });

        mediaSession.setActive(true);
    }

    private void js(final String script) {
        getBridge().getWebView().post(new Runnable() {
            @Override public void run() {
                getBridge().getWebView().evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }

    private void requestManageFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            }
        }
    }
}

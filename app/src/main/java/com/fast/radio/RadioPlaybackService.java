package com.fast.radio;

import androidx.annotation.Nullable; import androidx.media3.common.*; import androidx.media3.datasource.DefaultHttpDataSource; import androidx.media3.exoplayer.*; import androidx.media3.session.*;

public class RadioPlaybackService extends MediaSessionService {
 private ExoPlayer player; private MediaSession session;
 @Override public void onCreate(){super.onCreate();
  DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setUserAgent("Fast Radio/8.0 (Android)").setConnectTimeoutMs(12000).setReadTimeoutMs(20000);
  DefaultLoadControl load=new DefaultLoadControl.Builder().setBufferDurationsMs(5000,10000,1000,2000).build();
  player=new ExoPlayer.Builder(this).setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http)).setLoadControl(load).setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true).build();
  session=new MediaSession.Builder(this,player).build();
 }
 @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo info){return session;}
 @Override public void onDestroy(){if(session!=null){session.release();session=null;}if(player!=null){player.release();player=null;}super.onDestroy();}
}

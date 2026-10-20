package com.serein.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.media.session.*;
import android.os.*;
import android.graphics.Bitmap;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {
    public static final String PLAY="play", PAUSE="pause", TOGGLE="toggle", REFRESH="refresh", PRESET="preset", TIMER="timer", CYCLE="cycle", QUICK_SLEEP="quick_sleep", STOP="stop";
    public static final String STATE="com.serein.app.STATE";
    private static final int NOTIFICATION=71;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<Integer,Track> tracks=new HashMap<>();
    private SessionState state;
    private MediaSession session;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private boolean focused=false, resumeOnGain=false;
    private float duck=1f;
    private long lastNotice=0,lastWidgetMinute=-1;
    private Bitmap artwork;
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){resumeOnGain=false;pausePlayback(true);}};
    private final Runnable idleStop=()->{if(!state.playing)stopSelf();};
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!state.playing)return;
        long remaining=state.remainingNow();
        if(state.timerMinutes>0&&remaining<=0){state.timerMinutes=0;state.remaining=0;state.deadline=0;state.save();pausePlayback(true);return;}
        updateVolumes();
        if(SystemClock.elapsedRealtime()-lastNotice>=10000){updateNotification();lastNotice=SystemClock.elapsedRealtime();}
        long minute=(remaining+59999)/60000;if(minute!=lastWidgetMinute){SereinWidget.updateAll(PlaybackService.this);lastWidgetMinute=minute;}
        handler.postDelayed(this,state.timerMinutes>0&&remaining<=state.fadeSeconds*1000L?100:1000);
    }};
    private static final class Track { final MediaPlayer player; boolean ready; Track(MediaPlayer p){player=p;} }

    @Override public void onCreate(){
        super.onCreate();state=SessionState.get(this);audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        NotificationChannel channel=new NotificationChannel("playback","Playback",NotificationManager.IMPORTANCE_LOW);channel.setSound(null,null);channel.setShowBadge(false);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        session=new MediaSession(this,"Serein");session.setSessionActivity(openApp(this));
        session.setCallback(new MediaSession.Callback(){@Override public void onPlay(){beginForeground();play();}@Override public void onPause(){resumeOnGain=false;pausePlayback(true);}@Override public void onStop(){resumeOnGain=false;pausePlayback(true);stopSelf();}});
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(this,handler).setWillPauseWhenDucked(false).build();
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        artwork=Symbols.artwork(512);
    }
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        beginForeground();
        String action=intent.getAction()==null?REFRESH:intent.getAction();
        switch(action){
            case PLAY:play();break;
            case TOGGLE:if(state.playing){resumeOnGain=false;pausePlayback(true);}else play();break;
            case PAUSE:case STOP:resumeOnGain=false;pausePlayback(true);if(STOP.equals(action))stopSelf();break;
            case PRESET:state.preset(intent.getIntExtra("preset",0));play();break;
            case QUICK_SLEEP:if(state.playing){pausePlayback(true);}else{state.preset(0);if(state.timerMinutes==0)state.setTimer(30);play();}break;
            case TIMER:state.setTimer(intent.getIntExtra("minutes",0));refresh();break;
            case CYCLE:int current=state.timerMinutes;state.setTimer(current==0?15:current<30?30:current<45?45:current<60?60:current<90?90:0);refresh();break;
            default:refresh();break;
        }
        return START_NOT_STICKY;
    }
    private void beginForeground(){startForeground(NOTIFICATION,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);}
    private void play(){
        handler.removeCallbacks(idleStop);resumeOnGain=false;
        if(state.active()==0)state.preset(0);
        if(!focused){focused=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!focused){state.error="Audio is unavailable";Toast.makeText(this,state.error,Toast.LENGTH_SHORT).show();pausePlayback(true);return;}}
        if(!state.playing){state.playing=true;if(state.timerMinutes>0){if(state.remaining<=0)state.remaining=state.timerMinutes*60000L;state.deadline=SystemClock.elapsedRealtime()+state.remaining;}}
        state.error="";session.setActive(true);syncTracks();updateNotification();announce();handler.removeCallbacks(tick);handler.post(tick);
    }
    private void refresh(){
        if(state.playing){if(state.active()==0){pausePlayback(true);return;}syncTracks();updateVolumes();updateNotification();}
        else{stopForeground(STOP_FOREGROUND_REMOVE);handler.removeCallbacks(idleStop);handler.postDelayed(idleStop,1000);}
        state.save();announce();
    }
    private void syncTracks(){
        for(Integer i:new ArrayList<>(tracks.keySet()))if(state.levels[i]<=0)release(i);
        for(int i=0;i<12;i++){
            if(state.levels[i]<=0)continue;
            Track existing=tracks.get(i);if(existing!=null){if(existing.ready&&!existing.player.isPlaying())existing.player.start();continue;}
            final int index=i;MediaPlayer player=new MediaPlayer();Track track=new Track(player);tracks.put(i,track);
            try{
                player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                player.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);player.setLooping(true);player.setVolume(0f,0f);
                AssetFileDescriptor fd=getAssets().openFd("audio/"+SessionState.IDS[i]+".ogg");player.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());fd.close();
                player.setOnPreparedListener(p->{Track active=tracks.get(index);if(active!=track)return;track.ready=true;if(state.playing&&state.levels[index]>0){updateVolumes();p.start();}});
                player.setOnErrorListener((p,what,extra)->{if(tracks.get(index)==track){release(index);state.levels[index]=0;state.error="Could not play "+SessionState.NAMES[index];Toast.makeText(this,state.error,Toast.LENGTH_SHORT).show();if(state.active()==0)pausePlayback(true);else announce();}return true;});
                player.prepareAsync();
            }catch(Exception e){release(index);state.levels[index]=0;state.error="Could not load "+SessionState.NAMES[index];if(state.active()==0)pausePlayback(true);}
        }
        updateVolumes();
    }
    private void updateVolumes(){
        float fade=state.timerMinutes>0?MixMath.fade(state.remainingNow(),state.fadeSeconds*1000L):1f;
        for(Map.Entry<Integer,Track> entry:tracks.entrySet()){Track t=entry.getValue();if(t.ready){float gain=MixMath.gain(state.levels[entry.getKey()],state.master,state.active(),fade,duck);try{t.player.setVolume(gain,gain);}catch(IllegalStateException ignored){}}}
    }
    private void pausePlayback(boolean abandon){
        if(state.deadline>0)state.remaining=state.remainingNow();state.deadline=0;state.playing=false;
        handler.removeCallbacks(tick);for(Track track:tracks.values())if(track.ready)try{track.player.pause();}catch(IllegalStateException ignored){}
        if(abandon&&focused){audio.abandonAudioFocusRequest(focus);focused=false;}
        duck=1f;state.save();updateNotification();stopForeground(STOP_FOREGROUND_DETACH);announce();handler.removeCallbacks(idleStop);handler.postDelayed(idleStop,60000);
    }
    private void release(int index){Track track=tracks.remove(index);if(track!=null){track.player.setOnPreparedListener(null);track.player.setOnErrorListener(null);track.player.release();}}
    private Notification notification(){
        String detail=state.summary();if(state.timerMinutes>0)detail+=" · "+MixMath.time(state.remainingNow());
        Notification.Builder builder=new Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_notification).setContentTitle(state.title()).setContentText(detail).setContentIntent(openApp(this)).setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(state.playing).setShowWhen(false).setColor(0xffa8ba95).setLargeIcon(artwork);
        builder.addAction(new Notification.Action.Builder(state.playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,state.playing?"Pause":"Play",action(this,TOGGLE,70)).build());
        builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Stop",action(this,STOP,71)).build());
        if(session!=null)builder.setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1));
        if(state.playing&&state.timerMinutes>0)builder.setShowWhen(true).setWhen(System.currentTimeMillis()+state.remainingNow()).setUsesChronometer(true).setChronometerCountDown(true);
        return builder.build();
    }
    private void updateNotification(){
        String displaySummary=state.summary();if(state.timerMinutes>0)displaySummary+=" · "+MixMath.time(state.remainingNow());
        if(session!=null){
            session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP).setState(state.playing?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,PlaybackState.PLAYBACK_POSITION_UNKNOWN,state.playing?1f:0f).build());
            session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,state.title()).putString(MediaMetadata.METADATA_KEY_ARTIST,displaySummary).putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,displaySummary).putString(MediaMetadata.METADATA_KEY_ALBUM,"Serein").putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,artwork).build());
        }
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());
    }
    private void announce(){sendBroadcast(new Intent(STATE).setPackage(getPackageName()));SereinWidget.updateAll(this);}
    @Override public void onAudioFocusChange(int change){
        if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){duck=.2f;updateVolumes();}
        else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){resumeOnGain=state.playing;pausePlayback(false);}
        else if(change==AudioManager.AUDIOFOCUS_LOSS){resumeOnGain=false;pausePlayback(true);}
        else if(change==AudioManager.AUDIOFOCUS_GAIN){duck=1f;if(resumeOnGain){resumeOnGain=false;beginForeground();play();}else updateVolumes();}
    }
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(state.deadline>0)state.remaining=state.remainingNow();state.deadline=0;state.playing=false;for(Integer i:new ArrayList<>(tracks.keySet()))release(i);if(focused)audio.abandonAudioFocusRequest(focus);if(session!=null){session.setActive(false);session.release();}try{unregisterReceiver(noisy);}catch(Exception ignored){}stopForeground(STOP_FOREGROUND_REMOVE);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);state.save();announce();super.onDestroy();}
    public static PendingIntent openApp(Context c){return PendingIntent.getActivity(c,1,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static PendingIntent action(Context c,String action,int request){return PendingIntent.getForegroundService(c,request,new Intent(c,PlaybackService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static void command(Context c,String command){c.startForegroundService(new Intent(c,PlaybackService.class).setAction(command));}
}

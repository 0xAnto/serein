package com.serein.app;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

public class SereinWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)manager.updateAppWidget(id,render(c,false));}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,android.os.Bundle options){manager.updateAppWidget(id,render(c,this instanceof SleepWidget));}
    public static RemoteViews render(Context c,boolean compact){
        SessionState state=SessionState.get(c);RemoteViews views=new RemoteViews(c.getPackageName(),compact?R.layout.widget_sleep:R.layout.widget_player);
        views.setTextViewText(R.id.widget_name,"Serein");views.setTextViewText(R.id.widget_title,compact?"Sleep":state.title());
        views.setImageViewBitmap(R.id.widget_play,Symbols.bitmap(state.playing?"pause":"play",0xff35432c,96));
        views.setContentDescription(R.id.widget_play,state.playing?"Pause":"Play");views.setOnClickPendingIntent(R.id.widget_play,PlaybackService.action(c,compact?PlaybackService.QUICK_SLEEP:PlaybackService.TOGGLE,compact?102:101));
        views.setOnClickPendingIntent(R.id.widget_name,PlaybackService.openApp(c));views.setOnClickPendingIntent(R.id.widget_title,PlaybackService.openApp(c));
        boolean timed=state.timerMinutes>0;views.setViewVisibility(R.id.widget_timer_live,timed?View.VISIBLE:View.GONE);views.setViewVisibility(R.id.widget_timer_label,timed?View.GONE:View.VISIBLE);
        if(timed){views.setChronometerCountDown(R.id.widget_timer_live,true);views.setChronometer(R.id.widget_timer_live,SystemClock.elapsedRealtime()+state.remainingNow(),null,state.playing);if(!state.playing)views.setTextViewText(R.id.widget_timer_live,MixMath.time(state.remainingNow()));}
        views.setOnClickPendingIntent(R.id.widget_timer_live,PlaybackService.action(c,PlaybackService.CYCLE,103));views.setOnClickPendingIntent(R.id.widget_timer_label,PlaybackService.action(c,PlaybackService.CYCLE,103));
        if(!compact){int[] buttons={R.id.widget_sleep,R.id.widget_focus,R.id.widget_unwind};for(int i=0;i<3;i++){Intent intent=new Intent(c,PlaybackService.class).setAction(PlaybackService.PRESET).putExtra("preset",i);views.setOnClickPendingIntent(buttons[i],PendingIntent.getForegroundService(c,200+i,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));}}
        return views;
    }
    public static void updateAll(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);for(int id:manager.getAppWidgetIds(new ComponentName(c,SereinWidget.class)))manager.updateAppWidget(id,render(c,false));for(int id:manager.getAppWidgetIds(new ComponentName(c,SleepWidget.class)))manager.updateAppWidget(id,render(c,true));}
}

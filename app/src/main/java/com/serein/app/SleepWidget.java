package com.serein.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;

public final class SleepWidget extends SereinWidget {
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)manager.updateAppWidget(id,render(c,true));}
}

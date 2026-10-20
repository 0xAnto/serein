package com.serein.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;

public final class SessionState {
    public static final String[] IDS = {"rain","brown","ocean","white","forest","fire","pink","wind","stream","bowl","night","fan"};
    public static final String[] NAMES = {"Rain","Brown noise","Ocean","White noise","Forest","Fireplace","Pink noise","Wind","Stream","Singing bowl","Night","Soft fan"};
    public static final int[] GROUPS = {2,1,2,1,2,2,1,2,2,3,2,1};
    public static final int[] TINTS = {0xff91a8b0,0xffb39b80,0xff88aca8,0xffafb8a0,0xff9aae83,0xffc2977a,0xffc3a0a8,0xffa8b6a4,0xff8daeb8,0xffc5b483,0xffa6a4bf,0xffa8b89b};
    private static SessionState instance;
    public final int[] levels = new int[12];
    public int master = 60;
    public boolean dark = false, playing = false;
    public String name = "Sleep", error = "";
    public int timerMinutes = 0, fadeSeconds = 30;
    public long remaining = 0, deadline = 0;
    public final ArrayList<SavedMix> mixes = new ArrayList<>();
    private final SharedPreferences prefs;
    private SessionState(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("serein.native", Context.MODE_PRIVATE);
        master = MixMath.clamp(prefs.getInt("master",60)); dark = prefs.getBoolean("dark",false);
        name = prefs.getString("name","Sleep"); fadeSeconds = prefs.getInt("fade",30);
        timerMinutes = Math.max(0,Math.min(720,prefs.getInt("timer",0))); remaining = timerMinutes * 60000L;
        levels[0] = 55; levels[1] = 30;
        if (prefs.contains("levels")) try { JSONArray a=new JSONArray(prefs.getString("levels","[]")); for(int i=0;i<12;i++) levels[i]=MixMath.clamp(a.optInt(i,0)); } catch(Exception ignored) {}
        try { JSONArray a=new JSONArray(prefs.getString("mixes","[]")); for(int i=0;i<Math.min(50,a.length());i++) { JSONObject o=a.getJSONObject(i); JSONArray l=o.getJSONArray("levels"); int[] v=new int[12]; for(int j=0;j<12;j++)v[j]=MixMath.clamp(l.optInt(j,0)); mixes.add(new SavedMix(o.optString("name","Mix"),v)); }} catch(Exception ignored) {}
    }
    public static SessionState get(Context c) { if(instance==null)instance=new SessionState(c);return instance; }
    public int active() { int n=0;for(int value:levels)if(value>0)n++;return n; }
    public long remainingNow() { return deadline>0?Math.max(0L,deadline-SystemClock.elapsedRealtime()):remaining; }
    public String title() { if(!name.isEmpty())return name;for(int i=0;i<12;i++)if(levels[i]>0)return NAMES[i];return "Serein"; }
    public String summary() { StringBuilder b=new StringBuilder();for(int i=0;i<12;i++)if(levels[i]>0){if(b.length()>0)b.append(" + ");b.append(NAMES[i]);}return b.length()>0?b.toString():"No sounds selected"; }
    public void preset(int preset) { Arrays.fill(levels,0);if(preset==1){levels[4]=45;levels[9]=30;name="Focus";}else if(preset==2){levels[5]=55;levels[7]=25;name="Unwind";}else{levels[0]=55;levels[1]=30;name="Sleep";}save(); }
    public void setTimer(int minutes) { timerMinutes=Math.max(0,Math.min(720,minutes));remaining=timerMinutes*60000L;deadline=playing&&remaining>0?SystemClock.elapsedRealtime()+remaining:0;save(); }
    public void save() {
        JSONArray a=new JSONArray();for(int level:levels)a.put(MixMath.clamp(level));
        JSONArray m=new JSONArray();try{for(SavedMix mix:mixes){JSONObject o=new JSONObject();o.put("name",mix.name);JSONArray v=new JSONArray();for(int level:mix.levels)v.put(level);o.put("levels",v);m.put(o);}}catch(Exception ignored){}
        prefs.edit().putInt("master",master).putBoolean("dark",dark).putString("levels",a.toString()).putString("name",name).putInt("timer",timerMinutes).putInt("fade",fadeSeconds).putString("mixes",m.toString()).apply();
    }
    public static final class SavedMix {
        public final String name;public final int[] levels;
        public SavedMix(String name,int[] levels){this.name=name;this.levels=levels.clone();}
    }
}

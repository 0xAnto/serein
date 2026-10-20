package com.serein.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public final class Palette {
    public final int bg,surface,ink,muted,line,accent,green,greenInk;
    public Palette(boolean dark){
        bg=Color.parseColor(dark?"#252c28":"#eeeee6");surface=Color.parseColor(dark?"#2d362f":"#f5f4ed");
        ink=Color.parseColor(dark?"#cfd8c8":"#354032");muted=Color.parseColor(dark?"#a1af99":"#75816d");
        line=Color.parseColor(dark?"#46523e":"#d7ddce");accent=Color.parseColor(dark?"#d1a289":"#ad6e4d");
        green=Color.parseColor(dark?"#b5c5a0":"#62774f");greenInk=Color.parseColor(dark?"#293422":"#edf0e5");
    }
    public static GradientDrawable shape(int color,float radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);if(stroke!=0)d.setStroke(1,stroke);return d;}
}

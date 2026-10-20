package com.serein.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class Symbols {
    private Symbols(){}
    public static void draw(Canvas canvas,String name,int color,float size){
        canvas.save();canvas.scale(size/24f,size/24f);Paint p=new Paint(3);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        Path path=new Path();
        switch(name){
            case "home":path.moveTo(3,11);path.lineTo(12,3);path.lineTo(21,11);path.moveTo(5,10);path.lineTo(5,21);path.lineTo(19,21);path.lineTo(19,10);path.moveTo(10,21);path.lineTo(10,14);path.lineTo(14,14);path.lineTo(14,21);canvas.drawPath(path,p);break;
            case "mix":canvas.drawLine(5,3,5,8,p);canvas.drawLine(5,13,5,21,p);canvas.drawCircle(5,10.5f,2.5f,p);canvas.drawLine(12,3,12,13,p);canvas.drawLine(12,18,12,21,p);canvas.drawCircle(12,15.5f,2.5f,p);canvas.drawLine(19,3,19,6,p);canvas.drawLine(19,11,19,21,p);canvas.drawCircle(19,8.5f,2.5f,p);break;
            case "more":p.setStyle(Paint.Style.FILL);canvas.drawCircle(5,12,1.5f,p);canvas.drawCircle(12,12,1.5f,p);canvas.drawCircle(19,12,1.5f,p);break;
            case "close":canvas.drawLine(6,6,18,18,p);canvas.drawLine(18,6,6,18,p);break;
            case "play":p.setStyle(Paint.Style.FILL);path.moveTo(9,5);path.lineTo(19,12);path.lineTo(9,19);path.close();canvas.drawPath(path,p);break;
            case "pause":p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(6,5,10,19,1,1,p);canvas.drawRoundRect(14,5,18,19,1,1,p);break;
            case "moon":path.moveTo(20.5f,14);path.cubicTo(10,17,7,9,10,3.5f);path.cubicTo(1,5,-1,18,9,21);path.cubicTo(14,23,20,20,20.5f,14);canvas.drawPath(path,p);break;
            case "sun":canvas.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p);}break;
            case "timer":canvas.drawCircle(12,13,8,p);canvas.drawLine(9,2,15,2,p);canvas.drawLine(12,2,12,5,p);canvas.drawLine(12,9,12,13,p);canvas.drawLine(12,13,15,15,p);break;
            case "save":path.moveTo(6,21);path.lineTo(6,3);path.lineTo(18,3);path.lineTo(18,21);path.lineTo(12,17);path.close();canvas.drawPath(path,p);break;
            case "widget":for(int i=0;i<4;i++){int x=3+(i%2)*11,y=3+(i/2)*11;canvas.drawRoundRect(x,y,x+7,y+7,1.5f,1.5f,p);}break;
            case "volume":path.moveTo(11,4);path.lineTo(6,8);path.lineTo(3,8);path.lineTo(3,16);path.lineTo(6,16);path.lineTo(11,20);path.close();canvas.drawPath(path,p);canvas.drawArc(9,5,21,19,-55,110,false,p);canvas.drawArc(12,1,25,23,-50,100,false,p);break;
            case "plus":canvas.drawLine(5,12,19,12,p);canvas.drawLine(12,5,12,19,p);break;
            case "check":path.moveTo(5,12);path.lineTo(10,17);path.lineTo(19,7);canvas.drawPath(path,p);break;
            case "trash":canvas.drawLine(4,6,20,6,p);canvas.drawLine(9,3,15,3,p);path.moveTo(6,6);path.lineTo(7,21);path.lineTo(17,21);path.lineTo(18,6);canvas.drawPath(path,p);canvas.drawLine(10,10,10,17,p);canvas.drawLine(14,10,14,17,p);break;
            case "breathe":canvas.drawCircle(12,12,9,p);canvas.drawCircle(12,12,5,p);canvas.drawCircle(12,12,1.5f,p);break;
            case "sunset":canvas.drawLine(2,16,22,16,p);canvas.drawLine(4,20,20,20,p);canvas.drawArc(6,6,18,18,180,180,false,p);canvas.drawLine(12,2,12,4,p);break;
        }
        canvas.restore();
    }
    public static Bitmap bitmap(String name,int color,int pixels){Bitmap b=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);draw(new Canvas(b),name,color,pixels);return b;}
    public static Bitmap artwork(int pixels){Bitmap b=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(0xffdbe3cf);Paint p=new Paint(3);p.setColor(0xff6b7e59);float[] heights={.16f,.32f,.46f,.28f,.14f};for(int i=0;i<5;i++){float x=pixels*(.32f+i*.09f),h=pixels*heights[i];c.drawRoundRect(x-pixels*.021f,(pixels-h)/2,x+pixels*.021f,(pixels+h)/2,pixels*.02f,pixels*.02f,p);}return b;}

    public static final class Icon extends View {
        private String name;private int color;
        public Icon(Context c,String name,int color){super(c);this.name=name;this.color=color;}
        public void symbol(String n,int color){if(!name.equals(n)||this.color!=color){name=n;this.color=color;invalidate();}}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float size=Math.min(getWidth(),getHeight())*.45f;c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);Symbols.draw(c,name,color,size);c.restore();}
    }
    public static final class Sound extends View {
        private final int index;private final Paint p=new Paint(3);
        public Sound(Context c,int i){super(c);index=i;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();float s=Math.min(getWidth(),getHeight())/64f;canvas.translate((getWidth()-64*s)/2,(getHeight()-64*s)/2);canvas.scale(s,s);p.setColor(SessionState.TINTS[index]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);p.setStrokeCap(Paint.Cap.ROUND);Path a=new Path();
            switch(index){
                case 0:canvas.save();canvas.rotate(18,32,32);for(int i=0;i<4;i++){float x=14+12*i;canvas.drawLine(x,9+i%2*6,x,22+i%2*6,p);canvas.drawLine(x,31+i%2*5,x,51+i%2*5,p);}canvas.restore();break;
                case 1:for(int r=25;r>=5;r-=5){p.setAlpha(50+(25-r)*5);p.setStyle(Paint.Style.FILL);canvas.drawCircle(32,32,r,p);}break;
                case 2:for(int y=19;y<48;y+=8){a.reset();a.moveTo(5,y);a.cubicTo(16,y-12,21,y+12,32,y);a.cubicTo(43,y-12,48,y+12,59,y);canvas.drawPath(a,p);}break;
                case 3:p.setStyle(Paint.Style.FILL);for(int y=12;y<=52;y+=5)for(int x=12;x<=52;x+=5){p.setAlpha(80+(x+y)%7*20);canvas.drawCircle(x,y,(x+y)%3==0?1.2f:.8f,p);}break;
                case 4:for(int i=0;i<2;i++){int x=22+i*23;int top=8+i*7;a.reset();a.moveTo(x,top);a.lineTo(x-15,39);a.lineTo(x-7,39);a.lineTo(x-17,51);a.lineTo(x+15,51);a.lineTo(x+6,39);a.lineTo(x+14,39);a.close();canvas.drawPath(a,p);canvas.drawLine(x,51,x,59,p);}break;
                case 5:a.moveTo(33,6);a.cubicTo(38,25,53,27,49,44);a.cubicTo(42,66,10,57,14,37);a.cubicTo(16,30,23,24,21,17);a.cubicTo(30,23,27,26,28,29);a.cubicTo(35,21,36,16,33,6);canvas.drawPath(a,p);a.reset();a.moveTo(32,32);a.cubicTo(31,40,18,48,28,53);a.cubicTo(44,58,45,43,32,32);canvas.drawPath(a,p);break;
                case 6:canvas.save();canvas.rotate(-15,32,32);for(int y=12;y<54;y+=3){p.setAlpha(60+(y%15)*12);canvas.drawLine(12,y,52,y,p);}canvas.restore();break;
                case 7:a.moveTo(7,23);a.lineTo(42,23);a.cubicTo(61,23,53,2,42,9);canvas.drawPath(a,p);a.reset();a.moveTo(4,33);a.lineTo(47,33);a.cubicTo(65,33,57,57,44,48);canvas.drawPath(a,p);a.reset();a.moveTo(13,43);a.lineTo(26,43);a.cubicTo(42,43,35,62,25,55);canvas.drawPath(a,p);break;
                case 8:for(int x=17;x<46;x+=13){a.reset();a.moveTo(x,5);a.cubicTo(x+27,16,x-24,24,x+3,35);a.cubicTo(x+26,43,x-17,50,x+19,59);canvas.drawPath(a,p);}break;
                case 9:canvas.drawLine(8,32,56,32,p);canvas.drawArc(8,12,56,53,0,180,false,p);canvas.drawLine(26,54,38,54,p);canvas.drawLine(27,25,37,25,p);canvas.drawLine(19,18,45,18,p);canvas.drawLine(28,10,36,10,p);break;
                case 10:a.moveTo(39,7);a.cubicTo(5,4,1,47,26,55);a.cubicTo(40,61,51,52,55,41);a.cubicTo(30,43,25,18,39,7);canvas.drawPath(a,p);canvas.drawLine(49,8,49,16,p);canvas.drawLine(45,12,53,12,p);break;
                case 11:canvas.drawCircle(32,32,25,p);for(int i=0;i<3;i++){canvas.save();canvas.rotate(i*120,32,32);a.reset();a.moveTo(30,27);a.cubicTo(5,4,48,0,45,19);a.cubicTo(43,26,37,27,30,27);canvas.drawPath(a,p);canvas.restore();}canvas.drawCircle(32,32,4,p);break;
            }
            canvas.restore();
        }
    }
}

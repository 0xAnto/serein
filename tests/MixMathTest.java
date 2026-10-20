import com.serein.app.MixMath;

public final class MixMathTest {
    private static int assertions=0;
    private static void check(boolean condition){assertions++;if(!condition)throw new AssertionError("Check "+assertions);}
    public static void main(String[] args){
        check(MixMath.clamp(-1)==0);check(MixMath.clamp(200)==100);
        check(MixMath.time(0).equals("00:00"));check(MixMath.time(59999).equals("01:00"));check(MixMath.time(3601000).equals("1:00:01"));check(MixMath.time(-5000).equals("00:00"));
        check(MixMath.fade(15000,30000)==.5f);check(MixMath.fade(60000,30000)==1f);check(MixMath.fade(0,30000)==0f);check(MixMath.fade(1000,0)==1f);
        check(MixMath.gain(100,0,3,1f,1f)==0f);check(MixMath.gain(100,100,1,1f,1f)==1f);check(MixMath.gain(100,100,4,1f,1f)==.5f);check(MixMath.gain(100,100,1,.5f,.2f)==.1f);
        for(int n=1;n<=12;n++)check(MixMath.gain(100,100,n,1f,1f)<=1f);
        System.out.println(assertions+" playback and countdown checks passed.");
    }
}

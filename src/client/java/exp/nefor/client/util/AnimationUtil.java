package exp.nefor.client.util;

import net.minecraft.util.math.MathHelper;

public final class AnimationUtil {
    private AnimationUtil(){}
    public static float lerp(float a, float b, float t){ return a + (b-a)* MathHelper.clamp(t,0,1); }
    public static float easeOutExpo(float t){
        return t==1?1:1 - (float)Math.pow(2, -10*t);
    }
    public static float easeOutCubic(float t){
        float f=1-t; return 1 - f*f*f;
    }
    public static float animate(float current, float target, float speed){
        float delta = target-current;
        if(Math.abs(delta)<0.01f) return target;
        return current + delta * MathHelper.clamp(speed*0.2f, 0.05f, 0.4f);
    }

    /** Прогресс появления 0→1 с задержкой (для stagger-анимаций). */
    public static float openT(long openMs, long delayMs, long durMs){
        float t = (System.currentTimeMillis() - openMs - delayMs) / (float) durMs;
        return easeOutCubic(MathHelper.clamp(t, 0f, 1f));
    }
}

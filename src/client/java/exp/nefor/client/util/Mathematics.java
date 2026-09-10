package exp.nefor.client.util;

import net.minecraft.util.math.MathHelper;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;





public final class Mathematics {
    private static final AtomicLong seed = new AtomicLong(System.nanoTime() ^ System.identityHashCode(Mathematics.class));
    private static final ThreadLocal<Random> localRandom = ThreadLocal.withInitial(() -> new Random(seed.get() ^ Thread.currentThread().getId()));
    private static volatile float perlinOffset = localRandom.get().nextFloat() * 1000f;
    private static volatile int mutationCounter = 0;

    
    private static volatile float smoothThreshold = 90f;
    private static volatile float maxDeltaThreshold = 28f;

    private Mathematics() {}

    
    private static void mutateIfNeeded() {
        if ((mutationCounter++ & 0xFF) == 0) {
            seed.addAndGet(System.nanoTime() ^ Thread.currentThread().getId());
            perlinOffset = localRandom.get().nextFloat() * 1000f;
            
            smoothThreshold = 85f + localRandom.get().nextFloat() * 15f; 
            maxDeltaThreshold = 26f + localRandom.get().nextFloat() * 4f; 
        }
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0, 1);
    }

    public static float easeOutCubic(float t) {
        t = clamp(t, 0, 1);
        float f = 1 - t;
        return 1 - f * f * f;
    }

    public static float easeInOutQuad(float t) {
        t = clamp(t, 0, 1);
        return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }

    
    private static int hash(int x) {
        mutateIfNeeded();
        int dynamicFactor = (int)(seed.get() & 0xFFFF) | 1; 
        x = ((x << 13) ^ x) * dynamicFactor;
        return (x * (x * x * 15731 + 789221) + 1376312589) & 0x7fffffff;
    }

    
    public static float perlin(float x) {
        mutateIfNeeded();
        int xi = (int) Math.floor(x + perlinOffset);
        float xf = x + perlinOffset - xi;

        
        float u;
        int variation = (int)(seed.get() >> 8) & 3;
        switch(variation) {
            case 0: u = xf * xf * (3 - 2 * xf); break; 
            case 1: u = xf * xf * xf * (xf * (xf * 6 - 15) + 10); break; 
            case 2: u = (float) Math.sin(xf * Math.PI / 2); break; 
            default: u = xf; break; 
        }

        int a = hash(xi), b = hash(xi + 1);
        float va = ((a & 0xFF) / 127.5f) - 1f;
        float vb = ((b & 0xFF) / 127.5f) - 1f;

        return lerp(va, vb, u) * (0.45f + localRandom.get().nextFloat() * 0.1f);
    }

    
    public static float humanNoise(long timeMs, float scale) {
        mutateIfNeeded();

        
        float p = perlin(timeMs * 0.0006f * scale);

        
        float jitter1 = (localRandom.get().nextFloat() - 0.5f) * 0.012f * scale;
        float jitter2 = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.008f * scale;
        float jitter3 = ((System.nanoTime() & 0xFFFF) / 32768f - 1f) * 0.005f * scale;

        
        float w1 = 0.5f + localRandom.get().nextFloat() * 0.2f;
        float w2 = 0.3f + localRandom.get().nextFloat() * 0.1f;
        float w3 = 1f - w1 - w2;

        return p * 0.025f * scale + jitter1 * w1 + jitter2 * w2 + jitter3 * w3;
    }

    public static float wrapDegrees(float v) {
        return MathHelper.wrapDegrees(v);
    }

    
    public static float gcdSnap(float base, float target, float gcd) {
        mutateIfNeeded();
        if (gcd < 0.0001f) return target;

        float delta = wrapDegrees(target - base);
        float steps = delta / gcd;

        
        float jitterAmount = 0.03f + localRandom.get().nextFloat() * 0.02f; 
        steps += (localRandom.get().nextFloat() - 0.5f) * jitterAmount * 2f;

        long rounded = Math.round(steps);

        
        float correction = ((System.nanoTime() & 0xFFF) / 2048f - 1f) * 0.001f * gcd;

        return base + rounded * gcd + correction;
    }

    
    public static float turnSmooth(float current, float target, float maxStep) {
        mutateIfNeeded();

        float delta = wrapDegrees(target - wrapDegrees(current));

        
        if (Math.abs(delta) > smoothThreshold) {
            float factor = 0.25f + localRandom.get().nextFloat() * 0.08f; 
            delta = Math.signum(delta) * Math.min(Math.abs(delta) * factor, maxStep);
        }

        
        if (Math.abs(delta) > maxDeltaThreshold) {
            float noise = perlin(System.nanoTime() * 1e-9f) * 2f;
            float jitter = (localRandom.get().nextFloat() - 0.5f) * 1.5f;
            delta = Math.signum(delta) * (maxDeltaThreshold + noise + jitter);
        }

        
        float microCorrection = humanNoise(System.nanoTime(), 0.3f) * 0.5f;

        return current + delta + microCorrection;
    }

    public static float randomRange(float min, float max) {
        return min + localRandom.get().nextFloat() * (max - min);
    }

    

    


    public static float variedLerp(float a, float b, float t) {
        mutateIfNeeded();
        t = clamp(t, 0, 1);

        int method = (int)(seed.get() >> 12) & 3;
        switch(method) {
            case 0: return lerp(a, b, t);
            case 1: return lerp(a, b, easeOutCubic(t));
            case 2: return lerp(a, b, easeInOutQuad(t));
            default:
                
                t = t * t * (3 - 2 * t);
                return lerp(a, b, t);
        }
    }

    


    public static float organicNoise(long timeMs, float scale) {
        mutateIfNeeded();

        
        float n1 = perlin(timeMs * 0.0004f * scale);
        float n2 = perlin(timeMs * 0.0012f * scale) * 0.5f;
        float n3 = perlin(timeMs * 0.003f * scale) * 0.25f;

        
        float mix1 = 0.5f + localRandom.get().nextFloat() * 0.2f;
        float mix2 = 0.3f + localRandom.get().nextFloat() * 0.1f;
        float mix3 = 1f - mix1 - mix2;

        return (n1 * mix1 + n2 * mix2 + n3 * mix3) * 0.02f * scale;
    }
}
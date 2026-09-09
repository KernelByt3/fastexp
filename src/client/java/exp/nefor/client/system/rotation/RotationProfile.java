package exp.nefor.client.system.rotation;

/**
 * Per-server rotation profile. Tune GCD, noise, smoothing and aim logic.
 */
public enum RotationProfile {

    VANILLA("Vanilla", 0.0f, 1.0f, 12f, false, 0.30f, AimPoint.CHEST),
    HYPIXEL("Hypixel", 0.4f, 0.15f, 6f, true, 0.18f, AimPoint.EYES),
    REALLY_WORLD("ReallyWorld", 0.9f, 1.2f, 10f, true, 0.35f, AimPoint.CHEST),
    FUN_TIME("FunTime", 0.6f, 0.9f, 14f, true, 0.25f, AimPoint.HEAD),
    HOLY_WORLD("HolyWorld", 0.7f, 0.8f, 8f, true, 0.28f, AimPoint.CHEST),
    SPOOKY_TIME("SpookyTime", 1.1f, 1.4f, 16f, false, 0.40f, AimPoint.RANDOM);

    public enum AimPoint { HEAD, EYES, CHEST, STOMACH, RANDOM }

    public final String name;
    public final float noiseYaw;
    public final float noisePitchScale;
    public final float fovCheck; // degrees for isLookingAt
    public final boolean gcdSnap;
    public final float smoothing; // 0..1 lerp factor
    public final AimPoint aimPoint;

    RotationProfile(String name, float noiseYaw, float noisePitchScale, float fovCheck, boolean gcdSnap, float smoothing, AimPoint aimPoint) {
        this.name = name;
        this.noiseYaw = noiseYaw;
        this.noisePitchScale = noisePitchScale;
        this.fovCheck = fovCheck;
        this.gcdSnap = gcdSnap;
        this.smoothing = smoothing;
        this.aimPoint = aimPoint;
    }

    public static RotationProfile byServer(ServerType type) {
        return switch (type) {
            case HYPIXEL -> HYPIXEL;
            case REALLY_WORLD -> REALLY_WORLD;
            case FUN_TIME -> FUN_TIME;
            case HOLY_WORLD -> HOLY_WORLD;
            case SPOOKY_TIME -> SPOOKY_TIME;
            default -> VANILLA;
        };
    }

    public static RotationProfile byName(String name) {
        for (var p : values()) if (p.name.equalsIgnoreCase(name)) return p;
        return VANILLA;
    }
}

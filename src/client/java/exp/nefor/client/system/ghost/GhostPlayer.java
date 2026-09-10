package exp.nefor.client.system.ghost;

/**
 * Тело бота: позиция, взгляд, хп. Физика шага с землёй из GhostWorld.
 */
public class GhostPlayer {

    public double x, y, z;
    public float yaw, pitch;
    public float health = 20f;
    public int food = 20;
    public boolean onGround = true;

    private double vy = 0;

    public GhostPlayer(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Шаг ходьбы к (tx, tz), земля из мира. Возвращает true если дошёл. */
    public boolean walkToward(GhostWorld world, double tx, double tz, double speed) {
        double dx = tx - x, dz = tz - z;
        double hd = Math.hypot(dx, dz);
        if (hd < 0.3) return true;
        double step = Math.min(speed * 0.05, hd);
        x += dx / hd * step;
        z += dz / hd * step;
        yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        pitch = 0f;
        applyGravity(world);
        return false;
    }

    /** Вертикаль: падение + приземление на surfaceY, прыжок при застревании. */
    public void applyGravity(GhostWorld world) {
        double ground = world.surfaceY(x, z);
        if (Double.isNaN(ground)) {
            // чанков нет — висим на месте, сервер скорректирует телепортом
            vy = 0;
            onGround = false;
            return;
        }
        vy -= 0.08;
        if (vy < -3) vy = -3;
        y += vy;
        if (y <= ground) {
            y = ground;
            vy = 0;
            onGround = true;
        } else {
            onGround = y - ground < 0.05;
        }
    }

    public void jump() {
        if (onGround) {
            vy = 0.42;
            onGround = false;
        }
    }

    public double velocityLen() {
        return Math.abs(vy);
    }
}

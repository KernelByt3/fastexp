package exp.nefor.client.system.ghost;




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

    
    public void applyGravity(GhostWorld world) {
        double ground = world.surfaceY(x, z);
        if (Double.isNaN(ground)) {
            
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

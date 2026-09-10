package exp.nefor.client.system.ghost;

import net.minecraft.SharedConstants;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Настоящий гость на сервере: отдельный TCP-коннект с ником.
 * Виден в табе и другим игрокам. Работает на серверах без авторизации
 * (online-mode отвечает "premium only").
 */
public class GhostConnection {

    public final String nick;
    public final String host;
    public final int port;

    public volatile String status = "connecting";
    public volatile boolean playReady = false;
    public volatile boolean dead = false;
    public volatile float health = 20f;

    private ClientConnection conn;
    private boolean handshakeSent = false;
    private long connectMs;
    private long lastMoveMs = 0;

    public double x, y, z;
    public float yaw, pitch;
    public double tx, ty, tz;
    public boolean hasTarget = false;

    public GhostConnection(String nick, String host, int port) {
        this.nick = nick;
        this.host = host;
        this.port = port;
    }

    public void connect() {
        status = "connecting";
        connectMs = System.currentTimeMillis();
        try {
            conn = new ClientConnection(NetworkSide.CLIENTBOUND);
            conn.connect(host, port, new GhostLoginListener(this));
        } catch (Exception e) {
            status = "error";
            dead = true;
        }
    }

    public ClientConnection raw() {
        return conn;
    }

    public boolean isOpen() {
        return conn != null && conn.isOpen();
    }

    public void send(Packet<?> packet) {
        try {
            if (isOpen()) conn.send(packet);
        } catch (Exception ignored) {
        }
    }

    public void setTarget(double x, double y, double z) {
        tx = x;
        ty = y;
        tz = z;
        hasTarget = true;
    }

    public void clearTarget() {
        hasTarget = false;
    }

    /** Тик из модуля: handshake, движение, пакеты. */
    public void tick() {
        if (dead) return;
        if (conn == null || !conn.isOpen()) {
            if (!handshakeSent && System.currentTimeMillis() - connectMs > 15000) {
                status = "timeout";
                dead = true;
            }
            return;
        }
        if (!handshakeSent) {
            handshakeSent = true;
            conn.send(new HandshakeC2SPacket(SharedConstants.getProtocolVersion(), host, port, ConnectionIntent.LOGIN));
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8));
            conn.send(new LoginHelloC2SPacket(nick, uuid));
            status = "login";
        }
        if (!playReady) return;
        long now = System.currentTimeMillis();
        if (now - lastMoveMs < 50) return;
        lastMoveMs = now;

        if (hasTarget) {
            double dx = tx - x, dz = tz - z, dy = ty - y;
            double hd = Math.hypot(dx, dz);
            double step = 4.2 * 0.05;
            if (hd > 0.25) {
                x += dx / hd * Math.min(step, hd);
                z += dz / hd * Math.min(step, hd);
                yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
            }
            if (Math.abs(dy) > 0.2) y += Math.signum(dy) * Math.min(2.0 * 0.05, Math.abs(dy));
            pitch = 0f;
        }
        conn.send(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
        conn.send(ClientTickEndC2SPacket.INSTANCE);
    }

    public void onTeleport(int id, EntityPosition change, Set<PositionFlag> rel) {
        double nx = rel.contains(PositionFlag.X) ? x + change.position().x : change.position().x;
        double ny = rel.contains(PositionFlag.Y) ? y + change.position().y : change.position().y;
        double nz = rel.contains(PositionFlag.Z) ? z + change.position().z : change.position().z;
        float nyaw = rel.contains(PositionFlag.Y_ROT) ? yaw + change.yaw() : change.yaw();
        float npitch = rel.contains(PositionFlag.X_ROT) ? pitch + change.pitch() : change.pitch();
        x = nx;
        y = ny;
        z = nz;
        yaw = nyaw;
        pitch = npitch;
        send(new TeleportConfirmC2SPacket(id));
    }

    public void onSocketClosed(String reason) {
        if (!dead) status = "offline";
        dead = true;
        playReady = false;
    }

    public void onDisconnectText(String reason) {
        status = reason.length() > 40 ? reason.substring(0, 40) : reason;
        dead = true;
        playReady = false;
    }

    public void disconnect() {
        dead = true;
        playReady = false;
        try {
            if (conn != null) conn.disconnect(Text.literal("bye"));
        } catch (Exception ignored) {
        }
    }
}

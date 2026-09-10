package exp.nefor.client.system.ghost;

import net.minecraft.SharedConstants;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.time.Instant;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Настоящий гость на сервере: отдельный TCP-коннект с сессией,
 * своё тело (GhostPlayer) и свой кусок мира (GhostWorld).
 * Виден в табе и другим игрокам. Только серверы без авторизации.
 */
public class GhostConnection {

    public final GhostSession session;
    public final String host;
    public final int port;

    public final GhostPlayer player = new GhostPlayer(0, 64, 0);
    public final GhostWorld world = new GhostWorld();

    public volatile String status = "connecting";
    public volatile boolean playReady = false;
    public volatile boolean dead = false;

    private ClientConnection conn;
    private boolean handshakeSent = false;
    private long connectMs;
    private long lastMoveMs = 0;
    private double lastX, lastZ;
    private long stuckMs = 0;

    public double tx, ty, tz;
    public boolean hasTarget = false;

    public GhostConnection(GhostSession session, String host, int port) {
        this.session = session;
        this.host = host;
        this.port = port;
    }

    public String nick() {
        return session.nick;
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

    public void look(float yaw, float pitch) {
        player.yaw = yaw;
        player.pitch = pitch;
    }

    public void swing() {
        send(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    /** Чат от лица бота (без подписи — примут серверы без secure-chat). */
    public void sendChat(String text) {
        if (text == null || text.isBlank()) return;
        try {
            send(new ChatMessageC2SPacket(text,
                    Instant.now(),
                    ThreadLocalRandom.current().nextLong(),
                    new net.minecraft.network.message.MessageSignatureData(new byte[0]),
                    new net.minecraft.network.message.LastSeenMessageList.Acknowledgment(
                            0, new BitSet(), net.minecraft.network.message.LastSeenMessageList.Acknowledgment.NO_CHECKSUM)));
        } catch (Exception ignored) {
        }
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
            conn.send(new LoginHelloC2SPacket(session.nick, session.uuid));
            status = "login";
        }
        if (!playReady) return;
        long now = System.currentTimeMillis();
        if (now - lastMoveMs < 50) return;
        lastMoveMs = now;

        if (hasTarget) {
            boolean arrived = player.walkToward(world, tx, tz, 4.2);
            // застрял (стена/яма) — подпрыгнуть
            double moved = Math.hypot(player.x - lastX, player.z - lastZ);
            if (moved < 0.05 && player.onGround) {
                if (stuckMs == 0) stuckMs = now;
                if (now - stuckMs > 500) {
                    player.jump();
                    stuckMs = now;
                }
            } else {
                stuckMs = 0;
            }
            lastX = player.x;
            lastZ = player.z;
            if (arrived) hasTarget = false;
        } else {
            player.applyGravity(world);
        }
        conn.send(new PlayerMoveC2SPacket.Full(player.x, player.y, player.z,
                player.yaw, player.pitch, player.onGround, false));
        conn.send(ClientTickEndC2SPacket.INSTANCE);
    }

    public void onTeleport(int id, EntityPosition change, Set<PositionFlag> rel) {
        double nx = rel.contains(PositionFlag.X) ? player.x + change.position().x : change.position().x;
        double ny = rel.contains(PositionFlag.Y) ? player.y + change.position().y : change.position().y;
        double nz = rel.contains(PositionFlag.Z) ? player.z + change.position().z : change.position().z;
        float nyaw = rel.contains(PositionFlag.Y_ROT) ? player.yaw + change.yaw() : change.yaw();
        float npitch = rel.contains(PositionFlag.X_ROT) ? player.pitch + change.pitch() : change.pitch();
        player.x = nx;
        player.y = ny;
        player.z = nz;
        player.yaw = nyaw;
        player.pitch = npitch;
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

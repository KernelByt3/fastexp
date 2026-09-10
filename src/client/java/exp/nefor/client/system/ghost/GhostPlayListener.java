package exp.nefor.client.system.ghost;

/** PLAY фаза гостя: keepalive, телепорты, распаковка бандлов. Остальное игнор. */
public class GhostPlayListener extends GhostPlayBase {

    public GhostPlayListener(GhostConnection ghost) {
        this.ghost = ghost;
    }

    @Override
    public void onKeepAlive(net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket packet) {
        ghost.send(new net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket(packet.getId()));
    }

    @Override
    public void onDisconnect(net.minecraft.network.packet.s2c.common.DisconnectS2CPacket packet) {
        ghost.onDisconnectText(packet.reason().getString());
    }

    @Override
    public void onPlayerPositionLook(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet) {
        ghost.onTeleport(packet.teleportId(), packet.change(), packet.relatives());
    }

    @Override
    public void onGameJoin(net.minecraft.network.packet.s2c.play.GameJoinS2CPacket packet) {
        ghost.status = "online";
        ghost.playReady = true;
    }

    @Override
    public void onGameMessage(net.minecraft.network.packet.s2c.play.GameMessageS2CPacket packet) {
        if (packet.overlay()) return;
        try {
            String text = packet.content().getString();
            if (text.length() > 160) text = text.substring(0, 160);
            exp.nefor.client.util.player.chat.ChatUtil.sendMessage("§7[" + ghost.nick() + "]§r " + text);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onHealthUpdate(net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket packet) {
        ghost.player.health = packet.getHealth();
    }

    @Override
    public void onChunkData(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket packet) {
        try {
            ghost.world.onChunk(packet.getChunkX(), packet.getChunkZ(),
                    packet.getChunkData().getHeightmap());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onUnloadChunk(net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket packet) {
        try {
            ghost.world.onUnload(packet.pos().x, packet.pos().z);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBundle(net.minecraft.network.packet.s2c.play.BundleS2CPacket packet) {
        for (var sub : packet.getPackets()) {
            try {
                sub.apply(this);
            } catch (Exception ignored) {
            }
        }
    }
}

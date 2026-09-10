package exp.nefor.client.system.ghost;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.DynamicRegistryManager;

/** CONFIGURATION фаза: паки, реестры берём у основного клиента (тот же сервер). */
public class GhostConfigListener extends GhostListenerBase
        implements net.minecraft.network.listener.ClientConfigurationPacketListener {

    public GhostConfigListener(GhostConnection ghost) {
        super(ghost);
    }

    @Override
    public void onSelectKnownPacks(net.minecraft.network.packet.s2c.config.SelectKnownPacksS2CPacket packet) {
        ghost.send(new SelectKnownPacksC2SPacket(packet.knownPacks()));
    }

    @Override
    public void onReady(net.minecraft.network.packet.s2c.config.ReadyS2CPacket packet) {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            ghost.status = "no registries";
            ghost.disconnect();
            return;
        }
        DynamicRegistryManager regs = mc.world.getRegistryManager();
        GhostPlayListener play = new GhostPlayListener(ghost);
        ClientConnection c = ghost.raw();
        if (c == null) return;
        c.transitionInbound(PlayStateFactories.S2C.bind(RegistryByteBuf.makeFactory(regs)), play);
        c.send(ReadyC2SPacket.INSTANCE);
        c.transitionOutbound(PlayStateFactories.C2S.bind(RegistryByteBuf.makeFactory(regs),
                (PlayStateFactories.PacketCodecModifierContext) () -> false));
        ghost.status = "online";
        ghost.playReady = true;
    }

    @Override
    public void onCodeOfConduct(net.minecraft.network.packet.s2c.config.CodeOfConductS2CPacket packet) {
    }

    @Override
    public void onDynamicRegistries(net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket packet) {
    }

    @Override
    public void onFeatures(net.minecraft.network.packet.s2c.config.FeaturesS2CPacket packet) {
    }

    @Override
    public void onResetChat(net.minecraft.network.packet.s2c.config.ResetChatS2CPacket packet) {
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
    public void onPing(net.minecraft.network.packet.s2c.common.CommonPingS2CPacket packet) {
    }

    @Override
    public void onCustomPayload(net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket packet) {
    }

    @Override
    public void onResourcePackSend(net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket packet) {
    }

    @Override
    public void onResourcePackRemove(net.minecraft.network.packet.s2c.common.ResourcePackRemoveS2CPacket packet) {
    }

    @Override
    public void onSynchronizeTags(net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket packet) {
    }

    @Override
    public void onStoreCookie(net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket packet) {
    }

    @Override
    public void onServerTransfer(net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket packet) {
    }

    @Override
    public void onCustomReportDetails(net.minecraft.network.packet.s2c.common.CustomReportDetailsS2CPacket packet) {
    }

    @Override
    public void onServerLinks(net.minecraft.network.packet.s2c.common.ServerLinksS2CPacket packet) {
    }

    @Override
    public void onClearDialog(net.minecraft.network.packet.s2c.common.ClearDialogS2CPacket packet) {
    }

    @Override
    public void onShowDialog(net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket packet) {
    }

    @Override
    public void onCookieRequest(net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket packet) {
    }
}

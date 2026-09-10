package exp.nefor.client.system.ghost;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.network.state.ConfigurationStates;

/** LOGIN фаза гостя: offline-hello, compression, переход в конфигурацию. */
public class GhostLoginListener extends GhostListenerBase
        implements net.minecraft.network.listener.ClientLoginPacketListener {

    public GhostLoginListener(GhostConnection ghost) {
        super(ghost);
    }

    @Override
    public void onHello(net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket packet) {
        // сервер просит шифрование = online-mode, без аккаунта не зайти
        ghost.status = "premium only";
        ghost.disconnect();
    }

    @Override
    public void onCompression(net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket packet) {
        ClientConnection c = ghost.raw();
        if (c != null) c.setCompressionThreshold(packet.getCompressionThreshold(), false);
    }

    @Override
    public void onSuccess(net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket packet) {
        ghost.status = "config";
        ClientConnection c = ghost.raw();
        if (c == null) return;
        c.transitionInbound(ConfigurationStates.S2C, new GhostConfigListener(ghost));
        c.send(EnterConfigurationC2SPacket.INSTANCE);
        c.transitionOutbound(ConfigurationStates.C2S);
    }

    @Override
    public void onDisconnect(net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket packet) {
        ghost.onDisconnectText(packet.reason().getString());
    }

    @Override
    public void onQueryRequest(net.minecraft.network.packet.s2c.login.LoginQueryRequestS2CPacket packet) {
        // запросы (бренд и т.п.) игнорим — сервер обычно идёт дальше
    }

    @Override
    public void onCookieRequest(net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket packet) {
    }
}

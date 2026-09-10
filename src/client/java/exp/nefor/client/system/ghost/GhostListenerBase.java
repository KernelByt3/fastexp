package exp.nefor.client.system.ghost;

/**
 * Общая база гостевых листенеров: закрытие, ping, cookie.
 * getPhase()/getSide() берутся из default-методов подинтерфейсов.
 */
public abstract class GhostListenerBase implements net.minecraft.network.listener.ClientPacketListener {

    protected GhostConnection ghost;

    protected GhostListenerBase() {
    }

    protected GhostListenerBase(GhostConnection ghost) {
        this.ghost = ghost;
    }

    @Override
    public void onDisconnected(net.minecraft.network.DisconnectionInfo info) {
        if (ghost != null) ghost.onSocketClosed(String.valueOf(info));
    }

    @Override
    public boolean isConnectionOpen() {
        return ghost != null && ghost.isOpen();
    }
}

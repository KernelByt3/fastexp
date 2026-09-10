package exp.nefor.client.system.ghost;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;





public final class GhostSession {

    private static final String[] PREFIXES = {
            "Steve", "Alex", "Herobrine", "Noob", "Pro", "Ghost", "Shadow",
            "Knight", "Zombie", "Ender", "Creeper", "Miner", "Hunter", "Wolf"
    };
    private static final Random RND = new Random();

    public final String nick;
    public final UUID uuid;

    public GhostSession(String nick) {
        this.nick = nick.length() > 16 ? nick.substring(0, 16) : nick;
        this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.nick).getBytes(StandardCharsets.UTF_8));
    }

    
    public static GhostSession generate() {
        return new GhostSession(PREFIXES[RND.nextInt(PREFIXES.length)] + (1000 + RND.nextInt(9000)));
    }

    
    public static GhostSession of(String nick) {
        if (nick == null || nick.isBlank()) return generate();
        return new GhostSession(nick.trim());
    }
}

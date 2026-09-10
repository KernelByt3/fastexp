package exp.nefor.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class AltsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("nefor/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Random RNG = new Random();

    private static final String[] FIRST = {
            "Shadow", "Neo", "Dark", "Storm", "Ghost", "Blaze", "Frost", "Raven",
            "Vortex", "Cyber", "Nova", "Toxic", "Silent", "Crazy", "Lucky", "Iron"
    };
    private static final String[] SECOND = {
            "Hunter", "King", "Fox", "Wolf", "Slayer", "Lord", "Strike", "Wave",
            "Beast", "Reaper", "Sniper", "Runner", "Mage", "Knight", "PvP", "Pro"
    };

    private static final List<String> ALTS = new ArrayList<>();

    private AltsManager() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getGameDir()
                .resolve("nefor").resolve("config").resolve("alts.json");
    }

    public static void load() {
        try {
            Path path = path();
            ALTS.clear();
            if (Files.exists(path)) {
                List<String> loaded = GSON.fromJson(Files.readString(path), LIST_TYPE);
                if (loaded != null) ALTS.addAll(loaded);
            }
            LOGGER.info("Loaded {} alts", ALTS.size());
        } catch (Exception exception) {
            LOGGER.warn("Failed to load alts", exception);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(ALTS));
        } catch (Exception exception) {
            LOGGER.warn("Failed to save alts", exception);
        }
    }

    public static List<String> getAlts() {
        return List.copyOf(ALTS);
    }

    public static void add(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || ALTS.contains(trimmed)) return;
        ALTS.add(trimmed);
        save();
    }

    public static void remove(String name) {
        ALTS.remove(name);
        save();
    }

    public static String generateNick() {
        String nick = FIRST[RNG.nextInt(FIRST.length)] + SECOND[RNG.nextInt(SECOND.length)]
                + RNG.nextInt(10, 100);
        while (nick.length() > 16 || ALTS.contains(nick)) {
            nick = FIRST[RNG.nextInt(FIRST.length)] + SECOND[RNG.nextInt(SECOND.length)]
                    + RNG.nextInt(10, 999);
        }
        return nick;
    }

    



    private static void setSessionField(MinecraftClient client, Session session) {
        for (String fieldName : new String[]{"session", "field_1724"}) {
            try {
                var field = MinecraftClient.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(client, session);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception exception) {
                LOGGER.warn("Failed to set session via {}", fieldName, exception);
            }
        }
        LOGGER.error("Could not set session: no matching field found");
    }

    public static boolean login(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + trimmed).getBytes(StandardCharsets.UTF_8));

        Session session = new Session(trimmed, uuid, "0", Optional.empty(), Optional.empty());
        LOGGER.info("Session before: {}", client.getSession().getUsername());
        setSessionField(client, session);
        LOGGER.info("Session after: {}", client.getSession().getUsername());

        add(trimmed);
        LOGGER.info("Switched to offline account {}, returning to menu", trimmed);

        
        client.execute(() -> {
            if (client.world != null) {
                client.disconnect(new exp.nefor.client.gui.NeforTitleScreen(), false);
            }
            client.setScreen(new exp.nefor.client.gui.NeforTitleScreen());
        });
        return true;
    }
}

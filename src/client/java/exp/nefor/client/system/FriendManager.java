package exp.nefor.client.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class FriendManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FRIENDS = new LinkedHashSet<>();
    private static Path path(){ return FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("friends.json"); }

    static{ load(); }
    public static void load(){
        try{ if(Files.exists(path())){ var t = new TypeToken<Set<String>>(){}.getType(); Set<String> s = GSON.fromJson(Files.readString(path()), t); if(s!=null){ FRIENDS.clear(); s.forEach(v-> FRIENDS.add(v.toLowerCase())); } } }catch(Exception ignored){}
    }
    public static void save(){ try{ Files.createDirectories(path().getParent()); Files.writeString(path(), GSON.toJson(FRIENDS)); }catch(Exception ignored){} }
    public static boolean isFriend(String name){ return name!=null && FRIENDS.contains(name.toLowerCase()); }
    public static void add(String name){ if(name==null) return; FRIENDS.add(name.toLowerCase()); save(); }
    public static void remove(String name){ if(name==null) return; FRIENDS.remove(name.toLowerCase()); save(); }
    public static void clear(){ FRIENDS.clear(); save(); }
    public static Set<String> getAll(){ return Collections.unmodifiableSet(FRIENDS); }
}

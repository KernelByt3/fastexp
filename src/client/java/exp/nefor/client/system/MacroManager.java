package exp.nefor.client.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import exp.nefor.client.event.api.EventBus;
import exp.nefor.client.event.api.EventHandler;
import exp.nefor.client.event.impl.KeyEvent;
import exp.nefor.client.util.player.chat.ChatUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class MacroManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String,String> MACROS = new LinkedHashMap<>();
    private static Path path(){ return FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("macros.json"); }

    static{
        load();
        EventBus.subscribe(new Object(){
            @EventHandler void onKey(KeyEvent e){
                if(e.getAction()!=GLFW.GLFW_PRESS) return;
                String name = GLFW.glfwGetKeyName(e.getKey(),0);
                if(name==null) return;
                String key = name.toUpperCase();
                String txt = MACROS.get(key);
                if(txt!=null){
                    ChatUtil.sendMessage(txt);
                }
            }
        });
    }
    public static void load(){
        try{ if(Files.exists(path())){ var t=new TypeToken<Map<String,String>>(){}.getType(); Map<String,String> m=GSON.fromJson(Files.readString(path()),t); if(m!=null){ MACROS.clear(); m.forEach((k,v)->MACROS.put(k.toUpperCase(),v)); } } }catch(Exception ignored){}
    }
    public static void save(){ try{ Files.createDirectories(path().getParent()); Files.writeString(path(), GSON.toJson(MACROS)); }catch(Exception ignored){} }
    public static void add(String key,String text){ MACROS.put(key.toUpperCase(), text); save(); }
    public static void remove(String key){ MACROS.remove(key.toUpperCase()); save(); }
    public static void clear(){ MACROS.clear(); save(); }
    public static Map<String,String> getAll(){ return Collections.unmodifiableMap(MACROS); }
    public static void init(){} 
}

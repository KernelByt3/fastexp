package exp.nefor.client.system.neural;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Датасет для нейро-ауры: собирает как ты наводишься мышью на точки в тренировке.
 * Каждая запись = deltaYaw, deltaPitch, dist, velocity -> время реакции
 */
public final class NeuroDataset {
    public record Sample(float deltaYaw, float deltaPitch, float dist, float speed, long reactionMs, boolean hit){}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("neural_dataset.json");
    private static final List<Sample> SAMPLES = new ArrayList<>();

    public static void add(Sample s){ SAMPLES.add(s); save(); }
    public static List<Sample> all(){ return List.copyOf(SAMPLES); }
    public static int size(){ return SAMPLES.size(); }
    public static void clear(){ SAMPLES.clear(); save(); }

    public static void save(){
        try{ Files.createDirectories(PATH.getParent()); Files.writeString(PATH, GSON.toJson(SAMPLES)); }catch(Exception ignored){}
    }
    public static void load(){
        try{ if(Files.exists(PATH)) { var t = new com.google.gson.reflect.TypeToken<List<Sample>>(){}.getType(); List<Sample> l = GSON.fromJson(Files.readString(PATH), t); if(l!=null){ SAMPLES.clear(); SAMPLES.addAll(l);} } }catch(Exception ignored){}
    }
    static{ load(); }
}

package exp.nefor.client.system.neural;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Простая нейро-модель: 3 веса + bias, обучается по датасету.
 * На вход: deltaYaw, deltaPitch, dist -> выход: smoothing factor
 * Обучение: градиентный спуск по epochs, как в python.
 */
public final class NeuroModel {
    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("neural_model.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // веса
    public float wYaw = 0.18f;
    public float wPitch = 0.12f;
    public float wDist = 0.04f;
    public float bias = 0.22f;
    public int epochsTrained = 0;
    public float lastLoss = 999f;

    private static NeuroModel INSTANCE = new NeuroModel();
    public static NeuroModel get(){ return INSTANCE; }

    public float predict(float deltaYaw, float deltaPitch, float dist){
        float v = wYaw*Math.abs(deltaYaw)*0.02f + wPitch*Math.abs(deltaPitch)*0.02f + wDist*dist*0.05f + bias;
        return Math.max(0.08f, Math.min(0.45f, v));
    }

    public void train(int epochs){
        var data = NeuroDataset.all();
        if(data.isEmpty()) return;
        float lr = 0.002f;
        for(int e=0;e<epochs;e++){
            float loss=0;
            for(var s: data){
                float pred = predict(s.deltaYaw(), s.deltaPitch(), s.dist());
                // цель: реакция 120-250мс -> идеальный smoothing 0.18-0.32
                float target = s.hit() ? 0.18f + Math.min(0.14f, s.reactionMs()/1200f) : 0.32f;
                float err = pred - target;
                loss += err*err;
                // градиенты
                wYaw -= lr * err * Math.abs(s.deltaYaw())*0.02f;
                wPitch -= lr * err * Math.abs(s.deltaPitch())*0.02f;
                wDist -= lr * err * s.dist()*0.05f;
                bias -= lr * err * 0.5f;
            }
            lastLoss = loss / data.size();
            epochsTrained++;
            // clamp
            wYaw = Math.max(0.05f, Math.min(0.4f, wYaw));
            wPitch = Math.max(0.05f, Math.min(0.4f, wPitch));
            bias = Math.max(0.12f, Math.min(0.35f, bias));
        }
        save();
    }

    public void save(){
        try{ Files.createDirectories(PATH.getParent()); Files.writeString(PATH, GSON.toJson(this)); }catch(Exception ignored){}
    }
    public static void load(){
        try{ if(Files.exists(PATH)) { var m = GSON.fromJson(Files.readString(PATH), NeuroModel.class); if(m!=null) INSTANCE=m; } }catch(Exception ignored){}
    }
    static{ load(); }
}

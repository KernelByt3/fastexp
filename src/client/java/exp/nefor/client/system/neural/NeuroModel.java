package exp.nefor.client.system.neural;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;






public final class NeuroModel {
    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve("nefor").resolve("neural_model.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    
    public float wYaw = 0.18f;
    public float wPitch = 0.12f;
    public float wDist = 0.04f;
    public float bias = 0.22f;
    public int epochsTrained = 0;
    public float lastLoss = 999f;

    private static NeuroModel INSTANCE = new NeuroModel();
    public static NeuroModel get(){ return INSTANCE; }

    public float predict(float deltaYaw, float deltaPitch, float dist){
        return Math.max(0.12f, Math.min(0.42f, raw(deltaYaw, deltaPitch, dist)));
    }

    private float raw(float deltaYaw, float deltaPitch, float dist){
        return wYaw*Math.abs(deltaYaw)*0.02f + wPitch*Math.abs(deltaPitch)*0.02f + wDist*dist*0.05f + bias;
    }

    public void train(int epochs){
        var data = NeuroDataset.all();
        if(data.isEmpty()) return;
        epochs = Math.min(epochs, 40);
        
        var shuffled = new java.util.ArrayList<>(data);
        java.util.Collections.shuffle(shuffled);
        float lr = 0.004f;
        float best = Float.MAX_VALUE;
        int bad = 0;
        int done = 0;
        for(int e=0;e<epochs;e++){
            float loss=0;
            for(var s: shuffled){
                
                float pred = raw(s.deltaYaw(), s.deltaPitch(), s.dist());
                
                float target = s.hit() ? 0.18f + Math.min(0.16f, s.reactionMs()/1200f) : 0.34f;
                float err = pred - target;
                loss += err*err;
                wYaw -= lr * err * Math.abs(s.deltaYaw())*0.02f;
                wPitch -= lr * err * Math.abs(s.deltaPitch())*0.02f;
                wDist -= lr * err * s.dist()*0.05f;
                bias -= lr * err * 0.5f;
            }
            loss /= shuffled.size();
            
            if (best - loss < 1e-6f) { if (++bad >= 3) { lastLoss = loss; done++; break; } }
            else { bad = 0; best = loss; }
            lastLoss = loss;
            done++;
            epochsTrained++;
            lr *= 0.95f;
            
            wYaw = Math.max(0.05f, Math.min(0.4f, wYaw));
            wPitch = Math.max(0.05f, Math.min(0.4f, wPitch));
            bias = Math.max(0.12f, Math.min(0.35f, bias));
        }
        if (done == 0) epochsTrained++;
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

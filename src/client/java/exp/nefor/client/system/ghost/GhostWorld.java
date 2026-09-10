package exp.nefor.client.system.ghost;

import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;





public class GhostWorld {

    private final Set<Long> loaded = new HashSet<>();
    private final Map<Long, long[]> heights = new HashMap<>();
    private static final int BITS = 9;
    private static final int MASK = 0x1FF;

    public void onChunk(int cx, int cz, Map<net.minecraft.world.Heightmap.Type, long[]> maps) {
        long key = ChunkPos.toLong(cx, cz);
        loaded.add(key);
        if (maps != null) {
            long[] h = maps.get(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING);
            if (h != null) heights.put(key, h);
        }
    }

    public void onUnload(int cx, int cz) {
        long key = ChunkPos.toLong(cx, cz);
        loaded.remove(key);
        heights.remove(key);
    }

    public boolean isLoaded(double x, double z) {
        return loaded.contains(ChunkPos.toLong(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4));
    }

    



    public double surfaceY(double x, double z) {
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        long[] data = heights.get(ChunkPos.toLong(cx, cz));
        if (data == null) return Double.NaN;
        int lx = ((int) Math.floor(x)) & 15;
        int lz = ((int) Math.floor(z)) & 15;
        int idx = lx + lz * 16;
        int bitIdx = idx * BITS;
        int arrIdx = bitIdx >> 6;
        int shift = bitIdx & 63;
        if (arrIdx < 0 || arrIdx >= data.length) return Double.NaN;
        long v = data[arrIdx] >>> shift;
        if (shift + BITS > 64 && arrIdx + 1 < data.length) v |= data[arrIdx + 1] << (64 - shift);
        return (v & MASK);
    }

    public int loadedCount() {
        return loaded.size();
    }
}

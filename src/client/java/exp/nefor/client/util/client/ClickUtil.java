package exp.nefor.client.util.client;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class ClickUtil {

    public static final Queue<Runnable> clickQueue = new LinkedList<>();

    private static final Random RNG = new Random();
    private static long nextClickTime = 0;
    private static boolean pauseScheduled = false;

    private static int minPauseMs = 40;
    private static int maxPauseMs = 70;

    public static void setDefaultPause(int minMs, int maxMs) {
        minPauseMs = Math.max(1, Math.min(minMs, maxMs));
        maxPauseMs = Math.max(minPauseMs, maxMs);
    }

    public static int getMinPause() {
        return minPauseMs;
    }

    public static int getMaxPause() {
        return maxPauseMs;
    }

    public static int rand(int min, int max) {
        return min + RNG.nextInt(max - min + 1);
    }

    public static void pauseMs(int minMs, int maxMs) {
        nextClickTime = System.currentTimeMillis() + rand(minMs, maxMs);
        pauseScheduled = true;
    }

    public static void add(Runnable action) {
        clickQueue.add(action);
    }

    public static void clear() {
        clickQueue.clear();
    }

    public static boolean isEmpty() {
        return clickQueue.isEmpty();
    }

    public static void tick() {
        if (clickQueue.isEmpty()) return;
        if (System.currentTimeMillis() < nextClickTime) return;

        pauseScheduled = false;
        Runnable action = clickQueue.poll();
        action.run();

        if (!pauseScheduled) {
            pauseMs(minPauseMs, maxPauseMs);
        }
    }
}

package exp.nefor.client.util.data;

public class NotificationData {
    public String text;
    public int color;
    public long startTime;
    public long durationMs;

    public NotificationData(String text, int color, long durationMs) {
        this.text = text;
        this.color = color;
        this.startTime = System.currentTimeMillis();
        this.durationMs = durationMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > durationMs;
    }
}

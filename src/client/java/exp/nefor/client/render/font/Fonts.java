package exp.nefor.client.render.font;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Fonts {

    public record FontEntry(String id, String displayName, Path systemPath) {

        public boolean available() {
            return systemPath == null || Files.exists(systemPath);
        }
    }

    private static final List<FontEntry> ALL = List.of(
            new FontEntry("inter", "Inter", null),
            new FontEntry("arial", "Arial", Path.of("C:/Windows/Fonts/arial.ttf")),
            new FontEntry("segoeui", "Segoe UI", Path.of("C:/Windows/Fonts/segoeui.ttf")),
            new FontEntry("tahoma", "Tahoma", Path.of("C:/Windows/Fonts/tahoma.ttf")),
            new FontEntry("consolas", "Consolas", Path.of("C:/Windows/Fonts/consola.ttf")),
            new FontEntry("verdana", "Verdana", Path.of("C:/Windows/Fonts/verdana.ttf"))
    );

    private Fonts() {
    }

    public static List<FontEntry> all() {
        return ALL;
    }

    public static FontEntry get(String id) {
        for (FontEntry font : ALL) {
            if (font.id().equals(id)) return font;
        }
        return ALL.get(0);
    }

    public static String next(String id) {
        List<FontEntry> available = available();
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).id().equals(id)) {
                return available.get((i + 1) % available.size()).id();
            }
        }
        return available.get(0).id();
    }

    public static List<FontEntry> available() {
        return ALL.stream().filter(FontEntry::available).toList();
    }
}

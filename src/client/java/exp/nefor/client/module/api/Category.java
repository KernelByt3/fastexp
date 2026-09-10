package exp.nefor.client.module.api;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    BOT("Bot"),
    RENDER("Render"),
    MISC("Misc"),
    INTERFACE("Interface");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Category byLabel(String label) {
        for (Category c : values()) {
            if (c.label.equalsIgnoreCase(label)) return c;
        }
        return MISC;
    }
}

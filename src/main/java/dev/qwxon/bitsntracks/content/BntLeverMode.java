package dev.qwxon.bitsntracks.content;

public enum BntLeverMode {
    OFFSET("offset"),
    ROUTE("route"),
    TENSION("tension"),
    VISIBILITY("visibility"),
    PHYSICS("physics"),
    WIDTH("width"),
    RESET("reset");

    private static final BntLeverMode[] VALUES = values();

    private final String key;

    BntLeverMode(String key) {
        this.key = key;
    }

    public String translationKey() {
        return "bits_n_tracks.lever_mode." + this.key;
    }

    public BntLeverMode cycle(int direction) {
        return byIndex(this.ordinal() + direction);
    }

    public static BntLeverMode byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}

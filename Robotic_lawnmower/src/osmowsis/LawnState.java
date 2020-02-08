package osmowsis;

public enum LawnState {
    EMPTY   (1, "empty"),
    CRATER  (2, "crater"),
    FENCE   (3, "fence"),
    MOWER   (4, "mower"),
    ENERGY  (5, "energy"),
    GRASS   (6, "grass"),
    UNKNOWN (7, "unknown");

    private final int num;
    private final String str;

    LawnState(int n, String s) {
        this.num = n;
        this.str = s;
    }

    int get_state_num() {
        return this.num;
    }

    String get_state_str() {
        return this.str;
    }
}

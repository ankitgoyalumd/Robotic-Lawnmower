package osmowsis;

public enum MowerState {
    ACTIVE      (1, "active"),
    STALLED     (2, "stalled"),
    CRASHED     (3, "crashed"),
    NOENERGY    ( 4, "noenergy"),
    SCAN        ( 5, "scanning");

    private final int num;
    private final String str;

    MowerState(int n, String s) {
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

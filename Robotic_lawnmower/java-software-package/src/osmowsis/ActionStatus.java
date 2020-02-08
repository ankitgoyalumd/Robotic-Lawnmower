package osmowsis;

public enum ActionStatus {
    OK          (1, "ok"),
    STALL       (2, "stall"),
    TERMINATE   (3, "terminate"),
    CRASH       (4, "crash");

    private final int num;
    private final String str;

    ActionStatus(int d, String s) {
        this.num = d;
        this.str = s;
    }

    int get_status_num() {
        return this.num;
    }

    String get_status_str() {
        return this.str;
    }
}

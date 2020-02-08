package osmowsis;

public enum Direction {
    NORTH       (1, "north",     0, 1),
    NORTHEAST   (2, "northeast", 1, 1),
    EAST        (3, "east",      1, 0),
    SOUTHEAST   (4, "southeast", 1, -1),
    SOUTH       (5, "south",     0, -1),
    SOUTHWEST   (6, "southwest",-1, -1),
    WEST        (7, "west",     -1, 0),
    NORTHWEST   (8, "northwest",-1, 1),
    UNKNOWN     (9, "unknown",  0, 0);

    private final int dir;
    private final String str;
    private final int x_offset;
    private final int y_offset;

    Direction(int d, String s, int x, int y) {
        this.dir = d;
        this.str = s;
        this.x_offset = x;
        this.y_offset = y;
    }

    int get_direction_num() {
        return this.dir;
    }
    String get_direction_str() {
        return this.str;
    }
    int get_x_offset() {
        return this.x_offset;
    }
    int get_y_offset() {
        return this.y_offset;
    }
}


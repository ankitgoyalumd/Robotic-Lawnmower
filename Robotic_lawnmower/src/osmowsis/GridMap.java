package osmowsis;

import java.util.*;

public class GridMap implements Comparator<Cell> {
    /* INTERNALS */
    protected int height;
    protected int width;
    protected int total_cells;
    protected List<Cell> cells;
    //protected TreeMap<String, Cell> cells;

    //-----------------------------------------------------------
    /* FUNCTIONS */

    public GridMap(int w, int h) {
        //cells = new TreeMap<>();
        cells = new ArrayList<>();
        width = w;
        height = h;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                //String cur_key = String.format("%s%s", String.valueOf(x), String.valueOf(y));
                //cells.put(cur_key, new Cell(x, y));
                cells.add(new Cell(x, y));
            }
        }

        total_cells = w * h;
    }

    @Override
    public int compare(Cell c1, Cell c2) {
        return (c1.key.compareTo(c2.key));
    }

    public void set_new_dimensions(int xpos, int ypos) {
        if (xpos > width)
            width = xpos;
        if (ypos > height)
            height = ypos;
    }

    public Cell lookup_cell(int xpos, int ypos) {
        /*String cur_key = String.format("%s%s", String.valueOf(xpos), String.valueOf(ypos));
        if (!cells.containsKey(cur_key)) {
            return (null);
        }
        */
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            if (c.x == xpos && c.y == ypos)
                return (c);
        }

        return (null);
    }

    public int add_cell(int xpos, int ypos) {
        //String cur_key = String.format("%s%s", String.valueOf(xpos), String.valueOf(ypos));
        //if (cells.containsKey(cur_key))
            //return (1);
        //cells.put(cur_key, new Cell(xpos, ypos));

        if (xpos > width)
            width = xpos;
        if (ypos > height)
            height = ypos;

        cells.add(new Cell(xpos, ypos));

        return (0);
    }

    public int del_cell(int xpos, int ypos) {
        cells.remove(lookup_cell(xpos, ypos));
        return (0);
    }

    public Direction xy_to_cardinal(int xpos, int ypos) {
        for (Direction __d : Direction.values()) {
            if (xpos == __d.get_x_offset() && ypos == __d.get_y_offset())
                return (__d);
        }
        return Direction.UNKNOWN;
    }

    public void initialize_cells(LawnState base_state) {
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);

            if (c.state == LawnState.CRATER ||
                    c.state == LawnState.MOWER)
                continue;

            c.set_lawn_state(base_state);
        }
    }

    // find direction toward closest cell with given property
    public Cell find_closest(int x, int y, LawnState s) {
        int distance = 99999;
        int cur_dis;
        Cell c = null;
        Cell target = null;
        int xd, yd, new_x, new_y;
        String ret = "";

        for (int i = 0; i < cells.size(); i++) {
            c = cells.get(i);

            if (c.state != s)
                continue;

            xd = x - c.x;
            yd = y - c.y;
            if (Math.abs(xd) > Math.abs(yd)) {
                cur_dis = xd;
            } else {
                cur_dis = yd;
            }
            if (cur_dis < distance) {
                distance = cur_dis;
                target = c;
            }
        }

        if (target == null) {
            return (null);
        }

        xd = x - target.x;
        if (xd > 0)
            new_x = -1;
        else if (xd == 0)
            new_x = 0;
        else
            new_x = 1;

        yd = y - target.y;
        if (yd > 0)
            new_y = -1;
        else if (yd == 0)
            new_y = 0;
        else
            new_y = 1;

        target.d_param = xy_to_cardinal(new_x, new_y);
        return (target);
    }
}

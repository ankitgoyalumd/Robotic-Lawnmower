package osmowsis;

public class Cell {
    /* INTERNALS */
    protected String key;
    protected int x;
    protected int y;
    /* USER SPECIFIED */
    protected LawnState state;
    protected int mower_id;
    protected boolean recharge;

    protected Direction d_param; // modified by calls to GridMap.find_closest()
    protected int param1;
    protected int param2;

    //-----------------------------------------------------------
    /* FUNCTIONS */
    public Cell(int xpos, int ypos) {
        x = xpos;
        y = ypos;

        key = String.valueOf(x);
        key += String.valueOf(y);

        mower_id = 0;
        recharge = false;
        state = LawnState.UNKNOWN;
    }

    public String toString() {
        return "("+x+","+y+")";
    }

    public int set_state(int p_num, int value) {
        if (p_num == 1)
            param1 = value;
        else if (p_num == 2)
            param2 = value;
        else
            return (1);

        return (0);
    }

    public void set_lawn_state(LawnState s) {
        state = s;
    }

    public void set_mower_id(int id) {
        mower_id = id;
    }

    public void set_recharge() {
        recharge = true;
    }

    public int get_state(int p_num) {
        if (p_num == 1)
            return (param1);
        else if (p_num == 2)
            return (param2);

        return (-1);
    }
}

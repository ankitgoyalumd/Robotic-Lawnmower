package osmowsis;

public class Monitor {
    protected int stall_turns;
    protected GridMap mower_map;
    private Lawn lawn;

    public Monitor(Lawn l, int collision_delay) {
        lawn = l;
        stall_turns = collision_delay;
        mower_map = new GridMap(0, 0);
    }

    public ActionStatus process_mower_turn(Mower m) {
        ActionStatus status;
        Cell c;

        if (m.mower_state == MowerState.STALLED) {
            // update energy, but do not report any results
            // for stalled mowers, per the requirements
            m.update_stall_turns(-1);
            return ActionStatus.OK;
        } else if (m.mower_state == MowerState.CRASHED) {
            // this mower is dead
            return ActionStatus.OK;
        }

        status = m.do_turn(this);
        if (status == ActionStatus.CRASH) {
            return status;
        }

        // if stalled mower, set stall turns
        if (status == ActionStatus.STALL) {
            m.update_stall_turns(stall_turns);
        }

        // check if mower stopped on a charge pad and recharge
        c = lawn.find_mower(m.id);
        if (c.recharge) {
            m.recharge();
        }

        return status;
    }

    /* how many positions in this direction have a particular state (without obstacles between) */
    public int get_valid_moves(Mower m) {
        int x, y;
        int max_loop = 2;
        int positions = 0;
        Cell c;
        Direction d;

        c = lawn.find_mower(m.id);
        x = c.x;
        y = c.y;
        d = m.direction;

        // cant move more than (max_loop)_ spaces
        for (int i = 0; i < max_loop; i++) {

            // modify x based on distance
            x += d.get_x_offset();

            // modify y based on distance
            y += d.get_y_offset();

            // lookup that position
            c = mower_map.lookup_cell(x, y);
            if (c == null) {
                break;
            }
            if (c.state == LawnState.MOWER ||
                    c.state == LawnState.CRATER ||
                    c.state == LawnState.FENCE ||
                    c.state == LawnState.UNKNOWN)
                break;

            positions++;
        }

        return (positions);
    }

    public int count_adjacent_cells(Mower m, LawnState state) {
        int cnt = 0, x, y;
        int new_x, new_y;
        Cell c;

        c = lawn.find_mower(m.id);
        x = c.x; // save off the mower position
        y = c.y;

        for (Direction __d : Direction.values()) {
            if (__d == Direction.UNKNOWN)
                continue;

            new_x = x + __d.get_x_offset();
            new_y = y + __d.get_y_offset();

            c = mower_map.lookup_cell(new_x, new_y);

            if (c == null && state == LawnState.UNKNOWN)
                cnt++;
            else if (c != null && c.state == state)
                cnt++;
        }

        return (cnt);
    }

    public Direction find_adjacent_direction(Mower m, LawnState s) {
        int x, y;
        Cell mower, c;

        mower = lawn.find_mower(m.id);
        x = mower.x;
        y = mower.y;

        for (Direction __d : Direction.values()) {
            x = mower.x + __d.get_x_offset();
            y = mower.y + __d.get_y_offset();

            c = mower_map.lookup_cell(x, y);
            if (c == null)
                continue;

            if (c.state == s)
                return (__d);

            if (__d == Direction.NORTHWEST)
                break;
        }

        return Direction.UNKNOWN;
    }

    public LawnState get_adjacent_state(Mower m, Direction d) {
        int x, y;
        Cell c;

        c = lawn.find_mower(m.id);
        x = c.x + d.get_x_offset();
        y = c.y + d.get_y_offset();

        c = mower_map.lookup_cell(x, y);
        if (c == null)
            return LawnState.UNKNOWN;

        return c.state;
    }

    public Direction get_direction(Mower m, LawnState s) {
        int x, y;
        Cell c, target;

        c = lawn.find_mower(m.id);
        x = c.x;
        y = c.y;

        target = mower_map.find_closest(x, y, s);
        if (target != null)
            return (target.d_param);
        else
            return Direction.UNKNOWN;
    }

    // returns energy units required to reach nearest
    // recharging pad station
    public int recharge_pad_distance(Mower m) {
        int x, y;
        int xd, yd;
        Cell c, target;

        c = lawn.find_mower(m.id);

        // if we're on a charge pad then ignore
        if (c.recharge) {
            return 0;
        }
        x = c.x;
        y = c.y;

        target = mower_map.find_closest(x, y, LawnState.ENERGY);

        xd = Math.abs(c.x - target.x);
        yd = Math.abs(c.y - target.y);
        if (xd > yd) {
            return (xd * 2);
        }

        return (yd * 2);
    }

    public void perform_scan(Mower m) {
        Cell pos, adj;
        int x, y;
        Cell c;
        String report = "";

        // get mower location
        pos = lawn.find_mower(m.id);

        //System.out.println("SCAN_FROM:"+pos.toString());

        System.out.println("mower_"+m.id);
        System.out.println("scan");

        // scan all directions clockwise starting at North
        for (Direction __d : Direction.values()) {
            c = update_scan_cell(pos.x, pos.y, __d);

            if (c == null) {
                report += LawnState.FENCE.get_state_str();
            } else if (c.state == LawnState.MOWER) {
                report += LawnState.MOWER.get_state_str() + "_" + c.mower_id;
            } else {
                report += c.state.get_state_str();
            }

            if (__d == Direction.NORTHWEST) {
                break;
            } else {
                report += ",";
            }
        }

        // report results according to assignment specifications
        System.out.println(report);
    }

    private Cell update_scan_cell(int xpos, int ypos, Direction d) {
        Cell c, data;

        xpos += d.get_x_offset();
        ypos += d.get_y_offset();

        c = mower_map.lookup_cell(xpos, ypos);
        if (c == null) {
            mower_map.add_cell(xpos, ypos);
            c = mower_map.lookup_cell(xpos, ypos);
        }

        data = lawn.scan_cell(xpos, ypos);
        if (data == null) {
            c.set_lawn_state(LawnState.FENCE);
            return null;
        }

        c.set_lawn_state(data.state);
        return data;
    }

    public void set_new_mower_position(int id, int xpos, int ypos) {
        Cell c = mower_map.lookup_cell(xpos, ypos);
        if (c != null) {
            System.out.println("invalid state -- new mower position should be null");
            return;
        }

        mower_map.add_cell(xpos, ypos);
        c = mower_map.lookup_cell(xpos, ypos);

        c.set_recharge();
        c.set_lawn_state(LawnState.MOWER);
        c.set_mower_id(id);
    }

    public void update_mower_map(Cell src) {
        Cell c = mower_map.lookup_cell(src.x, src.y);
        if (c == null) {
            mower_map.add_cell(src.x, src.y);
            c = mower_map.lookup_cell(src.x, src.y);
        }

        c.set_mower_id(src.mower_id);
        c.set_lawn_state(src.state);
        if (src.recharge)
            c.set_recharge();
    }

    public ActionStatus submit_action(Mower m, String action, int move_amount) {
        ActionStatus rc = ActionStatus.OK;

        if (action == "scan") {
            m.set_mower_state(MowerState.SCAN);
            lawn.render();;
            perform_scan(m);
            m.update_energy(-1);
        } else if (move_amount == 0) {
            m.update_energy(-1);
        } else {
            // System.out.println("[submit_action] move_amount="+move_amount);

            for (int i = 0; i < move_amount; i++) {
                rc = lawn.process_move(m);
                if (rc == ActionStatus.CRASH ||
                        rc == ActionStatus.STALL) {
                    break;
                }
            }

            m.update_energy(-2);
        }

        if (m.mower_state == MowerState.CRASHED)
            return ActionStatus.CRASH;

        if (m.mower_state == MowerState.SCAN)
            m.set_mower_state(MowerState.ACTIVE);

        return rc;
    }

    public void report_move_action(Mower m, int distance, Direction dir, ActionStatus status) {
        System.out.println("mower_"+m.id);
        System.out.println("move,"+distance+","+dir.get_direction_str());
        if (status == ActionStatus.STALL)
            System.out.println(status.get_status_str()+","+stall_turns);
        else
            System.out.println(status.get_status_str());
    }
}

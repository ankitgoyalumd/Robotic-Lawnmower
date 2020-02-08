package osmowsis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Lawn {
    /* INTERNALS */
    protected int width;
    protected int height;
    protected int total_cells;
    protected int mower_cnt;
    protected int crater_cnt;
    protected int grass_cut;
    protected int total_grass;
    protected int max_turns;
    protected int max_energy;
    protected int stall_turns;
    protected int turns_remain;
    public int activate_next_action;
    public int button_mode; // 0 == next, 1 == FF
    public int stop;
    public List<Mower> mowers;
    private GridMap map;
    private Monitor monitor;
    private LawnUI ui;
    protected boolean terminated;

    //-----------------------------------------------------------
    /* FUNCTIONS */
    public Lawn() {
        // default ctor
        mowers = new ArrayList<>();
    }
    public Lawn(String config_file) {
        mowers = new ArrayList<>();
        parse_cfg_file(config_file);
    }

    public ActionStatus poll_mower_action(Mower m) {
        ActionStatus rc; // "return code"

        m.active_turn = 1;
        render();

        rc = monitor.process_mower_turn(m);
        if (rc == ActionStatus.CRASH && all_mowers_crashed()) {
            rc = ActionStatus.TERMINATE;
        } else if (all_grass_cut()) {
            rc = ActionStatus.TERMINATE;
        }

        m.active_turn = 0;

        return rc;
    }

    public void next_action_wait() {
        while (true) {

            if (activate_next_action == 1)
                break;

            try {
                sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void mow_lawn() {
        ActionStatus rc;
        int terminate = 0;
        Mower m;

        while (terminate == 0) {
            for (int i = 0; i < mowers.size(); i++) {
                m = mowers.get(i);

                if (button_mode == 0) {
                    next_action_wait();
                }

                //System.out.println("[ACTION] MOWER("+m.id+") TURNS_REMAIN("+turns_remain+")");
                rc = poll_mower_action(m);
                if (rc == ActionStatus.TERMINATE) {
                    terminate = 1;
                    break;
                }

                if (stop == 1) {
                    terminate = 1;
                    break;
                }

                if (button_mode == 0) {
                    activate_next_action = 0;
                }
            }
            turns_remain--;
            if (turns_remain == 0)
                break;
        }

	this.terminated = true;
        print_final_report();
	render();
    }

    public void print_final_report() {
        String report = "";

        report = map.total_cells + ",";
        report += total_grass + ",";
        report += grass_cut + ",";
        report += turns_remain;

        System.out.println(report);
    }

    public Cell find_mower(int id) {
        Cell c;
        String k;

        for (int i = 0; i < map.cells.size(); i++) {
            c = map.cells.get(i);

            if (c.state == LawnState.MOWER && c.mower_id == id) {
                return c;
            }
        }
        return null;
    }

    public Mower get_mower(int id) {
        Mower m;

        for (int i = 0; i < mowers.size(); i++) {
            m = mowers.get(i);
            if (m.id == id) {
                return m;
            }
        }

        return null;
    }

    public boolean all_mowers_crashed() {
        Mower m;

        for (int i = 0; i < mowers.size(); i++) {
            m = mowers.get(i);
            if (m.mower_state != MowerState.CRASHED) {
                return false;
            }
        }
        return true;
    }

    public boolean all_grass_cut() {
        if (grass_cut == total_grass)
            return true;
        return false;
    }

    // from mowers current position, move forward 1 in the direction
    // the mower is currently facing, and report the results
    public ActionStatus process_move(Mower m) {
        Cell src, dest;
        int x, y;

        src = find_mower(m.id);
        x = src.x + m.direction.get_x_offset();
        y = src.y + m.direction.get_y_offset();
        dest = scan_cell(x, y);

        if (dest == null || dest.state == LawnState.CRATER) {
            // FENCE or CRATER condition
            return ActionStatus.CRASH;

        } else if (dest.state == LawnState.MOWER) {
            // COLLISION
            return ActionStatus.STALL;

        } else if (dest.state == LawnState.GRASS) {
            // CUT GRASS
            cut_grass(dest.x, dest.y);
        }

        if (src.recharge) {
            src.set_lawn_state(LawnState.ENERGY);
        } else {
            src.set_lawn_state(LawnState.EMPTY);
        }

        dest.set_lawn_state(LawnState.MOWER);
        dest.set_mower_id(m.id);

        // keep mower positions in sync for the mower_map
        monitor.update_mower_map(src);
        monitor.update_mower_map(dest);

        // render UI for this move
        render();

        //System.out.println("process_move() src="+src.toString()+":"+src.state.get_state_str());
        //System.out.println("process_move() dest="+dest.toString()+":"+dest.state.get_state_str());

        return ActionStatus.OK;
    }

    public void cut_grass(int xpos, int ypos) {
        Cell c = map.lookup_cell(xpos, ypos);

        grass_cut++;
    }

    private void setup_cell(String line, int id) {
        Cell c;
        int x = -1, y = -1;
        String[] values = line.split(",");

        x = Integer.parseInt(values[0]);
        y = Integer.parseInt(values[1]);
        c = map.lookup_cell(x, y);

        if (values.length == 2) {
            c.set_lawn_state(LawnState.CRATER);
            return;
        }

        for (Direction __d : Direction.values()) {
            if (__d.get_direction_str().equalsIgnoreCase(values[2])) {
                c.set_lawn_state(LawnState.MOWER);
                c.set_mower_id(id);
                c.set_recharge();
                mowers.add(new Mower(id, __d, max_energy));
            }
        }
    }

    private void parse_cfg_file(String file) {
        Mower m;
        Cell c;
        BufferedReader r;
        String line;

        try {
            r = new BufferedReader(new FileReader(file));

            // width
            line = r.readLine();
            width = Integer.valueOf(line);

            // height
            line = r.readLine();
            height = Integer.valueOf(line);

            // initialize the lawn map
            map = new GridMap(width, height);

            // mower count
            line = r.readLine();
            mower_cnt = Integer.valueOf(line);

            // collision delay
            line = r.readLine();
            stall_turns = Integer.valueOf(line);

            // set energy capacity
            line = r.readLine();
            max_energy = Integer.valueOf(line);

            // setup mowers
            for (int i = 1; i <= mower_cnt; i++) {
                line = r.readLine();
                setup_cell(line, i);
            }

            // crater count
            line = r.readLine();
            crater_cnt = Integer.valueOf(line);

            for (int i = 0; i < crater_cnt; i++) {
                line = r.readLine();
                setup_cell(line, -1);
            }

            // read max num turns allowed
            line = r.readLine();
            max_turns = turns_remain = Integer.valueOf(line);

            // finish
            r.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        map.initialize_cells(LawnState.GRASS);
        total_grass = (map.total_cells - crater_cnt);

        // account for grass on mower init positions
        grass_cut += mower_cnt;

        monitor = new Monitor(this, stall_turns);
        for (int i = 0; i < mowers.size(); i++) {
            m = mowers.get(i);
            c = find_mower(m.id);
            monitor.set_new_mower_position(m.id, c.x, c.y);
        }

	terminated = false;
        button_mode = 0;
        stop = 0;
        activate_next_action = 0;
        ui = new LawnUI(this);
    }

    public void render() {
        ui.render_lawn(this);
        try {
            sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Cell scan_cell(int xpos, int ypos) {
        return map.lookup_cell(xpos, ypos);
    }
}

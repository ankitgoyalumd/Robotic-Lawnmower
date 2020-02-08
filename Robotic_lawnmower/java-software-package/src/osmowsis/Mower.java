package osmowsis;

public class Mower {
    protected int id;
    protected Direction prev_direction;
    protected Direction direction;
    protected int energy_units;
    protected int max_energy;
    protected MowerState mower_state;
    protected int stall_turns;
    public int active_turn;
    private int rdir_index;
    private int nomove_turns;
    private int noscan_turns;
    private int low_energy;
    private int noaction_turns;
    private int adj_moves;

    public final Direction[] cardinal = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    Mower(int identifier, Direction d, int energy) {

        id = identifier;
        direction = prev_direction = d;
        noscan_turns = 0;
        nomove_turns = 0;
        rdir_index = 0;
        active_turn = 0;
        noaction_turns = 0;
        adj_moves = 0;
        max_energy = energy_units = energy;
        mower_state = MowerState.ACTIVE;
    }

    public void set_mower_state(MowerState s) {
        mower_state = s;
    }

    // recharge back to full battery
    public void recharge() {
        energy_units = max_energy;
        low_energy = 0;
    }

    // positive OR negative number to offset current energy units
    public void update_energy(int offset) {

        if (mower_state == MowerState.CRASHED) {
            return;
        }

        energy_units += offset;
        if (energy_units <= 0) {
            energy_units = 0;
            mower_state = MowerState.CRASHED;
        }

    }

    public void update_stall_turns(int t) {
        stall_turns += t;

        if (t > 0) {
            mower_state = MowerState.STALLED;
            return;
        }

        stall_turns = 0;
        mower_state = MowerState.ACTIVE;
    }

    public ActionStatus do_turn(Monitor mntr) {
        Direction orig_direction = direction;
        LawnState state;
        int unknowns = 0;
        int adj_grass_cnt = 0;
        int distance = 0;
        ActionStatus status;
        int valid_fwd_moves = 0;

        adj_moves = mntr.count_adjacent_cells(this, LawnState.GRASS);
        adj_moves += mntr.count_adjacent_cells(this, LawnState.ENERGY);
        adj_moves += mntr.count_adjacent_cells(this, LawnState.EMPTY);

        // force a scan if we are near 2+ unknown grid positions
        if (low_energy == 0 || adj_moves < 3) {
            unknowns = mntr.count_adjacent_cells(this, LawnState.UNKNOWN);
            if (unknowns > 1 || noscan_turns > 5 || noaction_turns >= 2) {
                // reporting done inline for the scan
                noscan_turns = 0;
                return (mntr.submit_action(this, "scan", 0));
            } else {
                noscan_turns++;
            }
        }

        // how far in front of us can we go?
        valid_fwd_moves = mntr.get_valid_moves(this);
        if (valid_fwd_moves > 0) {
            // grass in front of us?
            state = mntr.get_adjacent_state(this, direction);
            // grass around us?
            adj_grass_cnt = mntr.count_adjacent_cells(this, LawnState.GRASS);
            if (state != LawnState.GRASS && adj_grass_cnt > 0 &&
                    low_energy == 0) {
                // grass next to us, force no-movement this turn
                distance = 0;
            } else if (low_energy == 1 && state == LawnState.ENERGY) {
                distance = 1;
            } else {
                distance = valid_fwd_moves;
            }
        }
        if (distance == 0)
            nomove_turns++;
        else
            nomove_turns = 0;

        // submit move, possibly 0 distance move
        status = mntr.submit_action(this, "move", distance);
        if (status == ActionStatus.CRASH) {
            set_mower_state(MowerState.CRASHED);
            mntr.report_move_action(this, distance, direction, status);
            return status;
        } else if (status == ActionStatus.STALL) {
            set_mower_state(MowerState.STALLED);
            mntr.report_move_action(this, distance, direction, status);
            return status;
        }

        // COMPUTE DIRECTION
        direction = compute_direction(mntr, valid_fwd_moves);
        //System.out.println("new direction:"+direction.get_direction_str());

        if (direction == Direction.UNKNOWN || noaction_turns >= 3) {
            noaction_turns = 0;
            direction = next_random_direction();
        }
        if (distance == 0 && direction == orig_direction) {
            noaction_turns++;
        } else {
            noaction_turns = 0;
        }
        if (orig_direction != direction) {
            prev_direction = orig_direction;
        }

        // REPORT on final move action and return
        mntr.report_move_action(this, distance, direction, status);
        return status;
    }

    private Direction compute_direction(Monitor mntr, int valid_fwd_moves) {
        Direction dir;
        LawnState st;

        // find closest charge pad
        int energy_distance = mntr.recharge_pad_distance(this);

        // check how far the nearest energy is, and how much energy needed to get there
        if ((energy_distance > 0) && (energy_distance - energy_units + 4) > 0) {
            low_energy = 1;
            return mntr.get_direction(this, LawnState.ENERGY);
        } else if (low_energy == 1) {
            return mntr.get_direction(this, LawnState.ENERGY);
        }

        // if no grass forward or adjacent, check to see if we are stuck
        // and need to just pick a direction
        if (adj_moves <= 2 && nomove_turns >= 4) {
            // prioritize energy locations first
            dir = mntr.find_adjacent_direction(this, LawnState.ENERGY);
            if (dir != Direction.UNKNOWN)
                return dir;

            return mntr.find_adjacent_direction(this, LawnState.EMPTY);
        }

        // grass still in front of us?
        st = mntr.get_adjacent_state(this, direction);
        if (st == LawnState.GRASS)
            return direction;

        // look for adjacent grass
        dir = mntr.find_adjacent_direction(this, LawnState.GRASS);
        if (dir != Direction.UNKNOWN) {
            System.out.println("adj_grass:"+dir.get_direction_str());
            return dir;
        }

        // have we scanned grass somewhere else?
        dir = mntr.get_direction(this, LawnState.GRASS);
        if (dir != Direction.UNKNOWN) {
            System.out.println("grass_direction:"+dir.get_direction_str());
            // can we actually move this direction? (unknown==maybe)
            st = mntr.get_adjacent_state(this, dir);
            if (st == LawnState.EMPTY || st == LawnState.ENERGY ||
                    st == LawnState.UNKNOWN)
                return dir;
        }

        return (next_random_direction());
    }

    // ensure we do not randomly cycle through the same few directions
    private Direction next_random_direction() {
        rdir_index++;
        if (rdir_index > 7) // index 8 is Direction==UNKNOWN
            rdir_index = 0;

        return (cardinal[rdir_index]);
    }
}

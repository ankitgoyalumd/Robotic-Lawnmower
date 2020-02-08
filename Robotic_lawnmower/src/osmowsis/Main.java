package osmowsis;

public class Main {

    public static void main(String[] args)
    {
        if (args.length == 0) {
            System.out.println("ERROR: Test scenario file not found.");
            return;
        }
        Lawn l = new Lawn(args[0]);
        l.mow_lawn();
    }
}

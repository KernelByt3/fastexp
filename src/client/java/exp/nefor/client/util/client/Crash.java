package exp.nefor.client.util.client;

import java.util.Random;

public class Crash {
    public static void crash() {
        Random rand = new Random();

        int error = rand.nextInt(150, 10000);
        Runtime.getRuntime().halt(error);
    }
}

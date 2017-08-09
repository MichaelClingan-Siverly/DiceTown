package mike.dicetown;

import java.util.Random;

/**
 * Created by mike on 7/27/2017.
 */

public class Dice {

    public static int roll(int numDice){
        Random rand = new Random();
        int roll = 0;

        /*
             *   Originally was going to use nextInt only once, but with more than one dice
             *   its even distribution is a bad simulation for probability of actual dice rolls
             *   So I make a separate roll for each dice (because a single fair dice IS evenly distributed)
             */
        for(int i = 0; i < numDice; i++){
            roll += rand.nextInt(6)+1;
        }
        return roll;
    }
}

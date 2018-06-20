package mike.cards;

/**
 * Created by mike on 6/5/2018.
 */

public abstract class LowEstablishment extends Establishment{
    void setNumCopies(int numPlayers){
        numCopies = Math.max(6, numPlayers + 1);
    }
}
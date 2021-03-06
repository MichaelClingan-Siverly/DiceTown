package mike.cards;

/**
 * Created by mike on 7/29/2017.
 */

public abstract class Establishment extends Card {
    public void addCopy(){
        numCopies++;
    }
    public void addRenovatedCopy(){
        numCopies++;
        numRenovated++;
    }
    abstract void setNumCopies(int numPlayers);

    public void removeCopy(){
        if(numCopies > 0){
            if(numRenovated == numCopies)
                numRenovated--;
            numCopies--;
        }
    }
    public void removeRenovatedCopy(){
        if(numCopies > 0){
            numCopies--;
            if(numRenovated > 0)
                numRenovated--;
        }
    }

//    public void removeCopy(){
//        if(numCopies > 0){
//            if(numRenovated > 0)
//                numRenovated--;
//            numCopies--;
//        }
//    }
//    public void removeCopyFromOpponent(){
//        if(numCopies > 0){
//            if(numRenovated == numCopies)
//                numRenovated--;
//            numCopies--;
//        }
//    }

    /**
     *
     * @param roll the value of the dice roll that may activate this card
     * @param myTurn indicate if it is this player's turn
     * @return the value this card is worth given the parameters. Returns 0 if this card was not activated
     */
    public abstract int checkIfActivated(int roll, boolean myTurn);
    public abstract <T> T accept(CardVisitor<T> visitor);
}

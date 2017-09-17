package mike.cards;

import android.support.annotation.NonNull;

/**
 * Created by mike on 7/26/2017.
 * provides basic functionality for all cards
 */

public abstract class Card implements CardInterface, Comparable<Card>{
    protected int numCopies = 1;
    protected int numRenovated = 0;

    public int getNumCopies(){
        return numCopies;
    }

    public void closeForRenovation(){
        numRenovated = numCopies;
    }
    public void finishRenovation(){
        numRenovated = 0;
    }
    public int getNumAvailable(){
        return numCopies-numRenovated;
    }

    public abstract int getCost();
    //cards supply their own image, so I don't have to figure it out with tons of instanceof when drawing them
    public abstract int getFullImageId();
    //full image does not show how many copies or if a card is under construction, plus its larger
    public abstract int getGridImageId();

    //I'm pretty lazy with overriding these in the subclasses, because there should only be
    // one of each class in each structure.
    // Since there should only be one of each card, I'm able to compare by hashCode pretty easily
    @Override
    abstract public int hashCode();

    @Override
    abstract public boolean equals(Object o);

    @Override
    public int compareTo(@NonNull Card c){
        return Integer.compare(hashCode(), c.hashCode());
    }

    public int getNumRenovatedResId(){
        switch(numRenovated){
            case 1:
                return R.drawable.under_construction1;
            case 2:
                return R.drawable.under_construction2;
            case 3:
                return R.drawable.under_construction3;
            case 4:
                return R.drawable.under_construction4;
            case 5:
                return R.drawable.under_construction5;
            case 6:
                return R.drawable.under_construction6;
            default:
                return R.drawable.transparent;
        }
    }
    public int getNumOwnedResId(){
        switch(numCopies){
            case 2:
                return R.drawable.x2;
            case 3:
                return R.drawable.x3;
            case 4:
                return R.drawable.x4;
            case 5:
                return R.drawable.x5;
            case 6:
                return R.drawable.x6;
            default:
                return R.drawable.transparent;
        }
    }
}

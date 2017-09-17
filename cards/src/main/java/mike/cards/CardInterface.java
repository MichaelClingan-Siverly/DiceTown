package mike.cards;

import android.widget.ImageView;

/**
 * Created by mike on 7/30/2017.
 */

public interface CardInterface{
    int getNumCopies();
    void closeForRenovation();
    void finishRenovation();
    int getNumAvailable();
    int getCost();
    String getCode();
    int getFullImageId();
    //full image does not show how many copies or if a card is under construction, plus its larger
    int getGridImageId();
    //these two are here because like the imageIDs, the drawables are in this module...
    // so I don't expect (or really want) other stuff to know about it
    int getNumRenovatedResId();
    int getNumOwnedResId();
}

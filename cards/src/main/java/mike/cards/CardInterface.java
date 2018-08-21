package mike.cards;

import android.widget.FrameLayout;

/**
 * Created by mike on 7/30/2017.
 */

public interface CardInterface extends Comparable<CardInterface>{
    int getNumCopies();
    void closeForRenovation();
    void finishRenovation();
    int getNumAvailable();
    int getCost();
    String getCode();
    //cards supply their own image, so I don't have to figure it out with tons of instanceof when drawing them
    int getFullImageId();
    //full image does not show how many copies or if a card is under construction, plus its larger
    int getGridImageId();
    /**
     * used for displaying a card's number of copies under construction
     * @param frame the frame the representation will be added to
     * @param increaseMargin indicate if a margin should be used
     */
    void setNumRenovatedImage(FrameLayout frame, boolean increaseMargin);

    /**
     * used for displaying a card's number of copies owned
     * @param frame the frame the representation will be added to
     * @param increaseMargin indicate if a margin should be used
     */
    void setnumOwnedImage(FrameLayout frame, boolean increaseMargin);
}

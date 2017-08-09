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
}

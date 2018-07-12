package mike.cards;

import android.widget.FrameLayout;

/**
 * Created by mike on 7/30/2017.
 */

public abstract class ConstructibleLandmark extends Card implements Landmark{
    public ConstructibleLandmark(){
        closeForRenovation();
    }

    @Override
    public void setNumRenovatedImage(FrameLayout frame, boolean increaseMargin){
        /* landmarks don't really need to show this, since only calling this on
         * any landmark would already mean its under construction
         */
    }
}

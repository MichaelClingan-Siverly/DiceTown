package mike.cards;

/**
 * Created by mike on 7/30/2017.
 */

public abstract class ConstructibleLandmark extends Card implements Landmark{
    public ConstructibleLandmark(){
        closeForRenovation();
    }
}

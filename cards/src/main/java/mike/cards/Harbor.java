package mike.cards;

import android.content.res.Resources;

/**
 * Created by mike on 7/26/2017.
 */

public class Harbor extends ConstructibleLandmark {

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public String getCode() {
        return "H";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.harbor;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.harbor;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

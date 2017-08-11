package mike.cards;

import android.content.res.Resources;

/**
 * Created by mike on 7/26/2017.
 */

public class Airport extends ConstructibleLandmark {

    @Override
    public int getCost() {
        return 30;
    }

    @Override
    public String getCode() {
        return "A";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.airport;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.airport;
    }

    @Override
    public int hashCode() {
        return 6;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

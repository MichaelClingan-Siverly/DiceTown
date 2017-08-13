package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class AmusementPark extends ConstructibleLandmark{

    @Override
    public int getCost() {
        return 16;
    }

    @Override
    public String getCode() {
        return "AP";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.amusement_park;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.amusement_park_icon;
    }

    @Override
    public int hashCode() {
        return 4;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

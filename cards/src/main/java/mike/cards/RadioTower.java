package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class RadioTower extends ConstructibleLandmark{

    @Override
    public int getCost() {
        return 22;
    }

    @Override
    public String getCode() {
        return "RT";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.radio_tower;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.radio_tower_icon;
    }

    @Override
    public int hashCode() {
        return 5;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

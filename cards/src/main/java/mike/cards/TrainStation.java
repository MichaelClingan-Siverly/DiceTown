package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class TrainStation extends ConstructibleLandmark {

    @Override
    public int getCost() {
        return 4;
    }

    @Override
    public String getCode() {
        return "TR";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.train_station;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.train_station_icon;
    }

    @Override
    public int hashCode() {
        return 2;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

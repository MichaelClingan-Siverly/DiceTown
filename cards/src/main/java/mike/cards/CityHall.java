package mike.cards;


import android.content.res.Resources;

/**
 * Created by mike on 7/26/2017.
 */

public class CityHall extends Card implements Landmark{
    //the city hall is the only landmark which is constructed when the game begins

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public String getCode() {
        return "CH";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.city_hall;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.city_hall;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

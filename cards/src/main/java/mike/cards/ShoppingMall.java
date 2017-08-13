package mike.cards;

import android.content.res.Resources;

/**
 * Created by mike on 7/26/2017.
 */

public class ShoppingMall extends ConstructibleLandmark{

    @Override
    public int getCost() {
        return 10;
    }

    @Override
    public String getCode() {
        return "SM";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.shopping_mall;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.shopping_mall_icon;
    }

    @Override
    public int hashCode() {
        return 3;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

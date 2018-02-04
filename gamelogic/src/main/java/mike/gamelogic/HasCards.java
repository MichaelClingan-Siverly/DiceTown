package mike.gamelogic;

import android.support.v4.util.ArraySet;

import mike.cards.Establishment;
import mike.cards.Landmark;

/**
 * Created by mike on 8/5/2017.
 */

public interface HasCards {
    Landmark[] getLandmarks();
    Establishment[] getCity();
    ArraySet<Establishment> getCitySet();
    String getName();
    int getMoney();
}

package mike.dicetown;



import android.util.ArrayMap;

import java.util.LinkedList;
import java.util.Map;

import mike.cards.Card;

/**
 * Created by mike on 7/12/2017.
 * class that shows everything about a particular player
 */

public class Player {
    //TODO implement card classes
    private ArrayMap<Integer,LinkedList<Card>> myCity;
    private int money;

    public Player(){
        myCity = new ArrayMap<>(14);
        money = 3;
    }

    private void initCity(){
        //TODO initialize the city with the LinkedLists and the default cards
    }

}

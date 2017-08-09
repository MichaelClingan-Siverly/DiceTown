package mike.gamelogic;



import android.support.v4.util.ArraySet;

import mike.cards.Airport;
import mike.cards.AmusementPark;
import mike.cards.Bakery;
import mike.cards.CityHall;
import mike.cards.ConstructibleLandmark;
import mike.cards.Harbor;
import mike.cards.Landmark;
import mike.cards.Establishment;
import mike.cards.RadioTower;
import mike.cards.ShoppingMall;
import mike.cards.TrainStation;
import mike.cards.WheatField;

/**
 * Created by mike on 7/12/2017.
 * class that does everything a particular player might do
 */

public class Player implements HasCards{
    private ArraySet<Establishment> myCity;
    private Landmark[] myLandmarks;
    private int money;
    private String name;

    public Player(String townName){
        name = townName;
        myCity = new ArraySet<>(5);
        myCity.add(new Bakery());
        myCity.add(new WheatField());
        //add all the landmarks (only City Hall is constructed at start of game)
        myLandmarks = new Landmark[]{new CityHall(), new Harbor(), new TrainStation(),
                new ShoppingMall(), new AmusementPark(), new RadioTower(), new Airport()};
        money = 3;
    }

    public void buyCard(Establishment card){
        if(card.getCost() <= money) {
            money -= card.getCost();
            int i = myCity.indexOf(card);
            //already have a copy of the card
            if (i > -1) {
                myCity.valueAt(i).addCopy();
            } else {
                myCity.add(card);
            }
        }
    }
    //all players own their landmarks, they just need to be built (not added)
    public void buyCard(ConstructibleLandmark card){
        if(card.getCost() <= money){
            money -= card.getCost();
            card.finishRenovation();
        }
    }
    @Override
    public Landmark[] getLandmarks(){
        return myLandmarks;
    }

    //a player wins the game if all their landmarks are constructed
    public boolean checkIfGameWon() {
        for(Landmark card : myLandmarks){
            if(card.getNumAvailable() == 0)
                return false;
        }
        return true;
    }

    public boolean checkIfCardAvailable(Establishment building){
        if(myCity.contains(building)){
            int index = myCity.indexOf(building);
            return myCity.valueAt(index).getNumAvailable() > 0;
        }
        return false;
    }
    public boolean checkIfCardAvailable(Landmark landmark){
        for(int i = 0; i < myLandmarks.length; i++){
            if(myLandmarks[i].equals(landmark)){
                return myLandmarks[i].getNumAvailable() > 0;
            }
        }
        return false;
    }

    public boolean checkIfCardOwned(Establishment building){
        if(myCity.contains(building)){
            int index = myCity.indexOf(building);
            return myCity.valueAt(index).getNumCopies() > 0;
        }
        return false;
    }
    @Override
    public Establishment[] getCity(){
        return myCity.toArray(new Establishment[myCity.size()]);
    }
    @Override
    public ArraySet<Establishment> getCitySet(){
        return myCity;
    }

    public int getMoney(){
        return money;
    }

    public void makeMoney(int moneyEarned){
        this.money += moneyEarned;
    }

    //too similar to makeMoney (with negative moneyEarned), but the intention is more clear here, and checks are better
    public int loseMoney(int moneyPaid){
        int amountLost = Math.min(money, moneyPaid);
        money -= amountLost;
        return amountLost;
    }

    @Override
    public String getName(){
        return name;
    }
}

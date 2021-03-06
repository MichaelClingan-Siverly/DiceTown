package mike.gamelogic;


import android.support.v4.util.ArraySet;

import java.util.Arrays;

import mike.cards.Airport;
import mike.cards.AmusementPark;
import mike.cards.Bakery;
import mike.cards.CardInterface;
import mike.cards.CityHall;
import mike.cards.ConstructibleLandmark;
import mike.cards.Deck;
import mike.cards.Establishment;
import mike.cards.Harbor;
import mike.cards.Landmark;
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
        myCity = new ArraySet<>();
        name = townName;
        myCity.add(new WheatField());
        myCity.add(new Bakery());

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
            }
            else {
                Establishment e = (Establishment)Deck.getCardFromCode(card.getCode());
                if(e != null)
                    myCity.add(e);
            }
        }
    }
    /**
     * all players own their landmarks, they just need to be built (not added)
     * @return true if player has won the game, false if not
     */
    boolean buyCard(ConstructibleLandmark card){
        if(card.getCost() <= money){
            money -= card.getCost();
            for(Landmark landmark : myLandmarks){
                if(landmark.equals(card)){
                    landmark.finishRenovation();
                    //added 1 because City Hall isn't counted as a constructed landmark
                    return getNumConstructedLandmarks()+1 == myLandmarks.length;
                }
            }
        }
        return false;
    }
    @Override
    public Landmark[] getLandmarks(){
        return myLandmarks;
    }

    public boolean checkIfCardAvailable(Establishment building){
        //doesnt use
        if(myCity.contains(building)){
            int index = myCity.indexOf(building);
            return myCity.valueAt(index).getNumAvailable() > 0;
        }
        return false;
    }
    public boolean checkIfCardAvailable(Landmark landmark){
        for (Landmark mark: myLandmarks) {
            if(mark.equals(landmark))
                return mark.getNumAvailable() > 0;
        }
        return false;
    }

    public int checkIfCardOwned(Establishment building){
        if(myCity.contains(building)){
            int index = myCity.indexOf(building);
            return myCity.valueAt(index).getNumCopies();
        }
        return 0;
    }
    @Override
    public Establishment[] getCity(){
        return myCity.toArray(new Establishment[myCity.size()]);
    }
    @Override
    public ArraySet<Establishment> getCitySet(){
        return myCity;
    }

    @Override
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

    public void removeCopyOfEstablishment(Establishment card, boolean renovated){
        int i = myCity.indexOf(card);
        if(i >= 0){
            if(renovated)
                myCity.valueAt(i).removeRenovatedCopy();
            else
                myCity.valueAt(i).removeCopy();

            if(myCity.valueAt(i).getNumCopies() == 0)
                myCity.removeAt(i);
        }
    }

    public int getNumConstructedLandmarks(){
        int myConstructedLandmarks = 0;
        for(int i = 1; i < myLandmarks.length; i++){
            if(myLandmarks[i].getNumAvailable() > 0)
                myConstructedLandmarks++;
        }
        return myConstructedLandmarks;
    }

    Landmark[] getMyConstructedLandmarks(){
        int found = 0;
        Landmark[] constructed = new Landmark[myLandmarks.length];
        for(int i = 1; i < myLandmarks.length; i++){
            if(myLandmarks[i].getNumAvailable() > 0 ){
                constructed[found] = myLandmarks[i];
                found++;
            }
        }
        return Arrays.copyOf(constructed, found);
    }

    public CardInterface[] mergeLandmarksAndMarket(Establishment[] market){
        int landmarksSize = 0;
        for(int i = 1; i < myLandmarks.length; i++){
            Landmark card = myLandmarks[i];
            if(card.getNumAvailable() == 0)
                landmarksSize++;
        }

        int marketLength = 0;
        if(market != null)
            marketLength = market.length;
        CardInterface[] cards = new CardInterface[marketLength + landmarksSize];

        int cardsIndex = 0;
        if(market != null) {
            for (CardInterface card : market) {
                cards[cardsIndex] = card;
                cardsIndex++;
            }
        }
        for(int j = 1; j < myLandmarks.length; j++){
            Landmark card = myLandmarks[j];
            if(card.getNumAvailable() == 0) {
                cards[cardsIndex] = card;
                cardsIndex++;
            }
        }
        return cards;
    }
}

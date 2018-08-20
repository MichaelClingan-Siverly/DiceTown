package mike.gamelogic;

import mike.cards.AppleOrchard;
import mike.cards.Bakery;
import mike.cards.BottlingPlant;
import mike.cards.BurgerStand;
import mike.cards.BusinessCenter;
import mike.cards.Cafe;
import mike.cards.CardVisitor;
import mike.cards.CheeseFactory;
import mike.cards.ConvenienceStore;
import mike.cards.ConventionCenter;
import mike.cards.CornField;
import mike.cards.DemoCompany;
import mike.cards.Establishment;
import mike.cards.ExclusiveClub;
import mike.cards.FamilyRestaurant;
import mike.cards.FlowerOrchard;
import mike.cards.FlowerShop;
import mike.cards.FoodWarehouse;
import mike.cards.Forest;
import mike.cards.FrenchRestaurant;
import mike.cards.FurnitureFactory;
import mike.cards.GeneralStore;
import mike.cards.Harbor;
import mike.cards.LoanOffice;
import mike.cards.MackerelBoat;
import mike.cards.Mine;
import mike.cards.MovingCompany;
import mike.cards.Park;
import mike.cards.PizzaJoint;
import mike.cards.ProduceMarket;
import mike.cards.Publisher;
import mike.cards.Ranch;
import mike.cards.RenoCompany;
import mike.cards.Restaurant;
import mike.cards.ShoppingMall;
import mike.cards.Stadium;
import mike.cards.SushiBar;
import mike.cards.TaxOffice;
import mike.cards.TechStartup;
import mike.cards.TunaBoat;
import mike.cards.TvStation;
import mike.cards.Vineyard;
import mike.cards.WheatField;
import mike.cards.Winery;

/**
 * Created by mike on 7/27/2017.
 * There are so many cards that depend on other cards when activated (18) that I
 * want some structure containing them while constructing.
 *
 *
 */

class ActivationVisitor implements CardVisitor {
    private boolean myTurn = false;
    private int roll;
    private Player[] players;
    private int activeIndex;
    private int myIndex;
    private int myConstructedLandmarks = 0;
    private int activesConstructedLandmarks = 0;
    private int activationCode;
    private int tunaRoll = 0;

    static final int ACTIVATE_RESTAURANTS = 0;
    static final int ACTIVATE_INDUSTRY = 1;
    static final int FORCE_ACTIVATE = 2;

    ActivationVisitor(int diceRoll, Player[] allPlayers, int activePlayerOrder, int myPlayerOrder, int activationCode, int tunaRoll){
        roll = diceRoll;
        players = allPlayers;
        activeIndex = activePlayerOrder;
        myIndex = myPlayerOrder;
        this.activationCode = activationCode;
        this.tunaRoll = tunaRoll;
        if(activeIndex == myIndex)
            myTurn = true;
        //don't count cityHall as a constructed landmark
        myConstructedLandmarks = players[myPlayerOrder].getNumConstructedLandmarks();
        if(myTurn)
            activesConstructedLandmarks = myConstructedLandmarks;
        else
            activesConstructedLandmarks = players[activePlayerOrder].getNumConstructedLandmarks();
    }

    @Override
    public Integer visit(Establishment e){
        return (Integer)e.accept(this);
    }

    @Override
    public Integer visit(AppleOrchard orchard) {
        if(roll == 10 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total = orchard.getNumAvailable() * 3;
            orchard.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(Bakery bakery) {
        if(myTurn && (roll == 2 || roll == 3) && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int value = 1;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = bakery.getNumAvailable() * value;
            bakery.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(BottlingPlant sodaPlant) {
        if((myTurn && roll == 11 && activationCode == ACTIVATE_INDUSTRY) || activationCode == FORCE_ACTIVATE){
            int total = 0;
            for(Player p : players){
                for(Establishment e : p.getCity()){
                    if(e instanceof Restaurant)
                        total+= e.getNumCopies();
                }
            }
            return total * sodaPlant.getNumAvailable();
        }
        return 0;
    }

    @Override
    public Integer visit(BurgerStand burgerStand) {
        if(!myTurn && roll == 8 && activationCode == ACTIVATE_RESTAURANTS) {
            int value = 1;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = burgerStand.getNumAvailable() * value;
            burgerStand.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            burgerStand.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(BusinessCenter businessCenter) {
        return 0;
    }

    @Override
    public Integer visit(Cafe cafe) {
        if(!myTurn && roll == 3 && activationCode == ACTIVATE_RESTAURANTS) {
            int value = 1;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = cafe.getNumAvailable() * value;
            cafe.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            cafe.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(CheeseFactory cheeseFactory) {
        if(myTurn && roll == 7 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE){
            int total = cheeseFactory.getNumAvailable() * 3 * getNumLivestock(players[myIndex]);
            cheeseFactory.finishRenovation();
            return total;
        }
        return 0;
    }

    //I have an interface for Livestock,
    //but Player's binary search would be faster than iterating through all cards
    private int getNumLivestock(Player p){
        return p.checkIfCardOwned(new Ranch());
    }

    @Override
    public Integer visit(ConvenienceStore convenienceStore) {
        if(myTurn && roll == 4 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE){
            int value = 3;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = convenienceStore.getNumAvailable() * value;
            convenienceStore.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(ConventionCenter conventionCenter) {
        return 0;
    }

    @Override
    public Integer visit(CornField cornField) {
        if(((roll == 3 || roll == 4) && activationCode == ACTIVATE_INDUSTRY ||
                activationCode == FORCE_ACTIVATE) && myConstructedLandmarks < 2) {
            int total = cornField.getNumAvailable();
            cornField.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(DemoCompany demoCompany) {
//        if(myTurn && roll == 4 || activationCode == FORCE_ACTIVATE) {
//            int total = 8 * Math.min(demoCompany.getNumAvailable(), myConstructedLandmarks);
//            demoCompany.finishRenovation();
//            return total;
//        }
        return 0;
    }

    @Override
    public Integer visit(ExclusiveClub club) {
        //I don't bother checking for shoppingMall here since active player owes everything they have anyway
        if (!myTurn && roll >= 12 && roll <= 14 && activesConstructedLandmarks >= 3 && activationCode == ACTIVATE_RESTAURANTS){
            int total = 0;
            if(club.getNumAvailable() >= 1)
                total = players[activeIndex].getMoney();
            club.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            club.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(FamilyRestaurant familyRestaurant) {
        if(!myTurn && (roll == 9 || roll == 10) && activationCode == ACTIVATE_RESTAURANTS) {
            int value = 2;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = familyRestaurant.getNumAvailable() * value;
            familyRestaurant.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            familyRestaurant.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(FlowerShop shop) {
        if((roll == 6 && myTurn && activationCode == ACTIVATE_INDUSTRY  ||
                activationCode == FORCE_ACTIVATE) && players[myIndex].getCitySet().contains(new FlowerOrchard())){
            int index = players[myIndex].getCitySet().indexOf(new FlowerOrchard());
            int numCopies = players[myIndex].getCitySet().valueAt(index).getNumCopies();
            int value = 1;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = shop.getNumAvailable() * numCopies * value;
            shop.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(FlowerOrchard orchard) {
        if(roll == 4 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total = orchard.getNumAvailable();
            orchard.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(FoodWarehouse warehouse) {
        if(myTurn && (roll == 12 || roll == 13) && activationCode == ACTIVATE_INDUSTRY ||
                activationCode == FORCE_ACTIVATE){
            int total = 0;
            for(Establishment e : players[myIndex].getCity()){
                if(e instanceof Restaurant)
                    total+= e.getNumCopies();
            }
            total = total * 2 * warehouse.getNumAvailable();
            warehouse.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(Forest forest) {
        if(roll == 5 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total = forest.getNumAvailable();
            forest.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(FrenchRestaurant restaurant) {
        if(!myTurn && roll == 5 && activesConstructedLandmarks >= 2 && activationCode == ACTIVATE_RESTAURANTS){
            int value = 5;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = restaurant.getNumAvailable() * value;
            restaurant.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            restaurant.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(FurnitureFactory factory) {
        if(myTurn && roll == 8 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE){
            int total = getNumNaturalResources(players[myIndex]) * factory.getNumAvailable() * 3;
            factory.finishRenovation();
            return total;
        }
        return 0;
    }

    //I have a NaturalResources interface. I can hardcode all possible natural resources, or use instanceof
    private int getNumNaturalResources(Player p){
        int total = 0;
        total += p.checkIfCardOwned(new Mine());
        total += p.checkIfCardOwned(new Forest());
        return total;
    }

    @Override
    public Integer visit(GeneralStore store) {
        if((roll == 2 && myTurn && activationCode == ACTIVATE_INDUSTRY ||
                activationCode == FORCE_ACTIVATE) && myConstructedLandmarks < 2) {
            int value = 2;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = value * store.getNumAvailable();
            store.finishRenovation();
            return total;
        }
        return 0;
    }
    @Override
    public Integer visit(LoanOffice loanOffice) {
        if(myTurn && (roll == 5 || roll == 6) && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE){
            int money = players[myIndex].getMoney();
            int total = Math.min(money, 2*loanOffice.getNumAvailable());
            loanOffice.finishRenovation();
            return -total;
        }
        return 0;
    }

    @Override
    public Integer visit(MackerelBoat boat) {
        if((roll == 8 && activationCode == ACTIVATE_INDUSTRY ||
                activationCode == FORCE_ACTIVATE) && players[myIndex].checkIfCardAvailable(new Harbor())) {
            int total = boat.getNumAvailable() * 3;
            boat.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(Mine mine) {
        if(roll == 9 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total = mine.getNumAvailable() * 5;
            mine.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(MovingCompany company) {
//        if((roll == 9 || roll == 10) && myTurn || activationCode == FORCE_ACTIVATE) {
//            int total = company.getNumAvailable() * 4;
//            company.finishRenovation();
//            return total;
//        }
        return 0;
    }

    @Override
    public Integer visit(Park park) {
        return 0;
    }

    @Override
    public Integer visit(PizzaJoint pizzaJoint) {
        if(!myTurn && roll == 7 && activationCode == ACTIVATE_RESTAURANTS) {
            int value = 1;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total = pizzaJoint.getNumAvailable() * value;
            pizzaJoint.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            pizzaJoint.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(ProduceMarket market) {
        if(myTurn && (roll == 11 || roll == 12) || activationCode == FORCE_ACTIVATE){
            int total = getNumCrops(players[myIndex]);
            total = total * 2 * market.getNumAvailable();
            market.finishRenovation();
            return total;
        }
        return 0;
    }

    //So...this is similar to Natural Resources, I have an interface for crops.
    private int getNumCrops(Player p){
        int total = 0;
        total += p.checkIfCardOwned(new AppleOrchard());
        total += p.checkIfCardOwned(new Vineyard());
        total += p.checkIfCardOwned(new FlowerOrchard());
        total += p.checkIfCardOwned(new CornField());
        total += p.checkIfCardOwned(new WheatField());
        return total;
    }

    @Override
    public Integer visit(Publisher publisher) {
        return 0;
    }

    @Override
    public Integer visit(Ranch ranch) {
        if(roll == 2 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total =  ranch.getNumAvailable();
            ranch.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(RenoCompany company) {
        return 0;
    }

    @Override
    public Integer visit(Stadium stadium) {
        return 0;
    }

    @Override
    public Integer visit(SushiBar bar) {
        //if I have a harbor
        if(!myTurn && roll == 1 && players[myIndex].checkIfCardAvailable(new Harbor()) && activationCode == ACTIVATE_RESTAURANTS){
            int value = 3;
            if(players[myIndex].checkIfCardAvailable(new ShoppingMall()))
                value++;
            int total =  bar.getNumAvailable() * value;
            bar.finishRenovation();
            return total;
        }
        else if(activationCode == FORCE_ACTIVATE)
            bar.finishRenovation();
        return 0;
    }

    @Override
    public Integer visit(TaxOffice office) {
        return 0;
    }

    @Override
    public Integer visit(TechStartup startup) {
        return 0;
    }

    @Override
    public Integer visit(TunaBoat boat) {
        if ((roll >= 12 && activationCode == ACTIVATE_INDUSTRY  || activationCode == FORCE_ACTIVATE)
                && players[myIndex].checkIfCardAvailable(new Harbor())){
            int total = boat.getNumAvailable() * tunaRoll;
            boat.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(TvStation station) {
        return 0;
    }

    @Override
    public Integer visit(Vineyard vineyard) {
        if(roll == 7 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total = vineyard.getNumAvailable() * 3;
            vineyard.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(WheatField field) {
        if(roll == 1 && activationCode == ACTIVATE_INDUSTRY || activationCode == FORCE_ACTIVATE) {
            int total = field.getNumAvailable();
            field.finishRenovation();
            return total;
        }
        return 0;
    }

    @Override
    public Integer visit(Winery winery) {
        if(myTurn && roll == 9 || activationCode == FORCE_ACTIVATE){
            int numCopies = players[myIndex].checkIfCardOwned(new Vineyard());
            int numToRenovate = winery.getNumAvailable();
            int total = numToRenovate * (6 * numCopies);
            numCopies = winery.getNumCopies();
            for(int i = 0; i < numCopies; i++){
                winery.removeCopy();
            }
            for(int i = 0; i < numToRenovate; i++){
                winery.addRenovatedCopy();
            }
            for(int i = 0; i < numCopies - numToRenovate; i++){
                winery.addCopy();
            }
            return total;
        }
        return 0;
    }
}

package mike.cards;

import android.support.v4.util.ArraySet;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by mike on 8/2/2017.
 * Represents a deck of cards split into three different piles; one for major establishments,
 * one for establishments which activate from 1-6, one for establishments which activate from 7-14
 */

public class Deck {
    private class Piles{
        LinkedList<Establishment> majorEstablishments = new LinkedList<>();
        LinkedList<Establishment> lowEstablishments = new LinkedList<>();
        LinkedList<Establishment> highEstablishments = new LinkedList<>();
    }

    public static final int NUM_ESTABLISHMENTS = 38;
    private Piles piles = new Piles();

    public Deck(int numPlayers){
        Establishment[] establishments = createCards();
        addCardsToPiles(establishments, numPlayers);
    }

    private void addCardsToPiles(Establishment[] establishments, int numPlayers){
        addCopiesToPool(establishments, numPlayers);

        Random rand = new Random();
        int poolLength = establishments.length;
        while(poolLength > 0){
            int poolIndex = rand.nextInt(poolLength);
            try {
                Establishment newCard = establishments[poolIndex].getClass().newInstance();
                establishments[poolIndex].removeCopy();
                //eventually all copies of all cards will be removed from the pool
                if(establishments[poolIndex].getNumCopies() == 0)
                    poolLength = removeFromPool(establishments, poolIndex, poolLength);

                LinkedList<Establishment> list;
                if(newCard instanceof MajorEstablishment)
                    list = piles.majorEstablishments;
                else if(newCard instanceof LowEstablishment)
                    list = piles.lowEstablishments;
                else
                    list = piles.highEstablishments;

                list.push(newCard);
            } catch (InstantiationException | IllegalAccessException e) {
                //all the cards have no parametrized constructors, so instantiation is good
                //all the cards are public, so illegalAccess is good too
                e.printStackTrace();
            }
        }
    }

    private Establishment[] createCards(){
        return new Establishment[]{new AppleOrchard(), new Bakery(),
                new BottlingPlant(), new BurgerStand(), new BusinessCenter(), new Cafe(),
                new CheeseFactory(), new ConvenienceStore(), new ConventionCenter(), new CornField(),
                new DemoCompany(), new ExclusiveClub(), new FamilyRestaurant(),
                new FlowerOrchard(), new FlowerShop(), new FoodWarehouse(), new Forest(),
                new FrenchRestaurant(), new FurnitureFactory(), new GeneralStore(),
                new LoanOffice(), new MackerelBoat(), new Mine(), new MovingCompany(), new Park(),
                new PizzaJoint(), new ProduceMarket(), new Publisher(), new Ranch(),
                new RenoCompany(), new Stadium(), new SushiBar(), new TaxOffice(), new TechStartup(),
                new TunaBoat(), new TvStation(), new Vineyard(), new WheatField(), new Winery()};
    }
//    private ArraySet<String> createMajorCodes(){
//        return new ArraySet<>(Arrays.asList(
//                new BusinessCenter().getCode(), new ConventionCenter().getCode(), new Park().getCode(),
//                new Publisher().getCode(), new RenoCompany().getCode(), new Stadium().getCode(),
//                new TaxOffice().getCode(), new TechStartup().getCode(), new TvStation().getCode()));
//    }
//    private ArraySet<String> createCheapCodes(){
//        return new ArraySet<>(Arrays.asList(
//                new WheatField().getCode(), new Bakery().getCode(), new Ranch().getCode(),
//                new Cafe().getCode(), new ConvenienceStore().getCode(), new Forest().getCode(),
//                new SushiBar().getCode(), new FlowerOrchard().getCode(), new FlowerShop().getCode(),
//                new GeneralStore().getCode(), new CornField().getCode(), new DemoCompany().getCode(),
//                new FrenchRestaurant().getCode(), new LoanOffice().getCode()));
//    }

    private void addCopiesToPool(Establishment[] establishments, int numPlayers){
        //this was longer before I moved the work to the card.
        for(Establishment c : establishments){
            c.setNumCopies(numPlayers);
        }
    }

    private int removeFromPool(Establishment[] establishments, int indexToRemove, int poolLength){
        if(indexToRemove != poolLength-1)
            establishments[indexToRemove] = establishments[poolLength-1];
        return poolLength-1;
    }


    public final static int pileCodeMajor = 1;
    public final static int pileCodeLow = 2;
    public final static int pileCodeHigh = 3;
    /**
     * Draw a card from the deck
     * @param pileCode 1 for major establishments, 2 for establishments costing < 7,
     *                 else for those costing > 6.
     * @return a card belonging to the indicated pile, or null if there are no more cards.
     */
    public Establishment draw(int pileCode){
        LinkedList<Establishment> pile;
        switch(pileCode){
            case pileCodeMajor:
                pile = piles.majorEstablishments;
                break;
            case pileCodeLow:
                pile = piles.lowEstablishments;
                break;
            default:
                pile = piles.highEstablishments;

        }
        if(pile.size() > 0)
            return pile.pop();
        else
            return null;
    }

//    /**
//     * Check if a deck pile is empty
//     * @param pileCode 1 for major establishments, 2 for establishments costing < 7,
//     *                 3 for ones costing > 6
//     * @return a card belonging to the indicated pile, or null if there are no more cards.
//     */
//    public boolean isEmpty(int pileCode){
//        return deck.isEmpty();
//    }

    public static Card getCardFromCode(String cardCode){
        switch(cardCode){
            case "A":
                return new Airport();
            case "AP":
                return new AmusementPark();
            case "AO":
                return new AppleOrchard();
            case "B":
                return new Bakery();
            case "BP":
                return new BottlingPlant();
            case "BS":
                return new BurgerStand();
            case "BC":
                return new BusinessCenter();
            case "C":
                return new Cafe();
            case "CF":
                return new CheeseFactory();
            case "CH":
                return new CityHall();
            case "CS":
                return new ConvenienceStore();
            case "CC":
                return new ConventionCenter();
            case "CO": //cornfield
                return new CornField();
            case "DC":
                return new DemoCompany();
            case "EC":
                return new ExclusiveClub();
            case "FA": //familyRestaurant
                return new FamilyRestaurant();
            case "FO":
                return new FlowerOrchard();
            case "FS":
                return new FlowerShop();
            case "FW":
                return new FoodWarehouse();
            case "F":
                return new Forest();
            case "FR":
                return new FrenchRestaurant();
            case "FF":
                return new FurnitureFactory();
            case "GS":
                return new GeneralStore();
            case "H":
                return new Harbor();
            case "LO":
                return new LoanOffice();
            case "MB":
                return new MackerelBoat();
            case "M":
                return new Mine();
            case "MC":
                return new MovingCompany();
            case "P":
                return new Park();
            case "PJ":
                return new PizzaJoint();
            case "PM":
                return new ProduceMarket();
            case "PB": //publisher
                return new Publisher();
            case "RT":
                return new RadioTower();
            case "R":
                return new Ranch();
            case "RC":
                return new RenoCompany();
            case "SM":
                return new ShoppingMall();
            case "S":
                return new Stadium();
            case "SB":
                return new SushiBar();
            case "TO":
                return new TaxOffice();
            case "TS":
                return new TechStartup();
            case "TR": //trainStation
                return new TrainStation();
            case "TB":
                return new TunaBoat();
            case "TV":
                return new TvStation();
            case "V":
                return new Vineyard();
            case "WF":
                return new WheatField();
            case "W":
                return new Winery();
            default:
                return null;
        }
    }

    public static String getCardNameFromCode(String cardCode){
        switch(cardCode){
            case "A":
                return "Airport";
            case "AP":
                return "AmusementPark";
            case "AO":
                return "AppleOrchard";
            case "B":
                return "Bakery";
            case "BP":
                return "BottlingPlant";
            case "BS":
                return "BurgerStand";
            case "BC":
                return "BusinessCenter";
            case "C":
                return "Cafe";
            case "CF":
                return "CheeseFactory";
            case "CH":
                return "CityHall";
            case "CS":
                return "ConvenienceStore";
            case "CC":
                return "ConventionCenter";
            case "CO": //cornfield
                return "CornField";
            case "DC":
                return "DemoCompany";
            case "EC":
                return "ExclusiveClub";
            case "FA": //familyRestaurant
                return "FamilyRestaurant";
            case "FO":
                return "FlowerOrchard";
            case "FS":
                return "FlowerShop";
            case "FW":
                return "FoodWarehouse";
            case "F":
                return "Forest";
            case "FR":
                return "FrenchRestaurant";
            case "FF":
                return "FurnitureFactory";
            case "GS":
                return "GeneralStore";
            case "H":
                return "Harbor";
            case "LO":
                return "LoanOffice";
            case "MB":
                return "MackerelBoat";
            case "M":
                return "Mine";
            case "MC":
                return "MovingCompany";
            case "P":
                return "Park";
            case "PJ":
                return "PizzaJoint";
            case "PM":
                return "ProduceMarket";
            case "PB": //publisher
                return "Publisher";
            case "RT":
                return "RadioTower";
            case "R":
                return "Ranch";
            case "RC":
                return "RenoCompany";
            case "SM":
                return "ShoppingMall";
            case "S":
                return "Stadium";
            case "SB":
                return "SushiBar";
            case "TO":
                return "TaxOffice";
            case "TS":
                return "TechStartup";
            case "TR": //trainStation
                return "TrainStation";
            case "TB":
                return "TunaBoat";
            case "TV":
                return "TvStation";
            case "V":
                return "Vineyard";
            case "WF":
                return "WheatField";
            default:
                return "Winery";
        }
    }
}

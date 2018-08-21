package mike.cards;

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

    public static String nullCode = "null";

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
                return "Amusement Park";
            case "AO":
                return "Apple Orchard";
            case "B":
                return "Bakery";
            case "BP":
                return "Bottling Plant";
            case "BS":
                return "Burger Joint";
            case "BC":
                return "Business Center";
            case "C":
                return "Cafe";
            case "CF":
                return "Cheese Factory";
            case "CH":
                return "City Hall";
            case "CS": //convenience store
                return "Mini-Mart";
            case "CC":
                return "Convention Center";
            case "CO": //cornfield
                return "Corn Field";
            case "DC":
                return "Demo Company";
            case "EC": //members only club
                return "Exclusive Club";
            case "FA": //familyRestaurant
                return "Sports Bar";
            case "FO":
                return "Flower Orchard";
            case "FS":
                return "Flower Shop";
            case "FW":
                return "Food Warehouse";
            case "F":
                return "Forest";
            case "FR":
                return "French Bistro";
            case "FF":
                return "Furniture Factory";
            case "GS":
                return "General Store";
            case "H": //harbor
                return "Wharf";
            case "LO": //loan office
                return "Payday Lender";
            case "MB":
                return "Fishing Boat";
            case "M": //mine
                return "Gold Mine";
            case "MC":
                return "Moving Company";
            case "P": //park
                return "Worker Cooperative";
            case "PJ":
                return "Pizza Shop";
            case "PM":
                return "Farmers Market";
            case "PB": //publisher
                return "Publisher";
            case "RT": //radio tower
                return "Cell Tower";
            case "R":
                return "Ranch";
            case "RC": //reno company
                return "Home Remodeler";
            case "SM":
                return "Shopping Mall";
            case "S":
                return "Stadium";
            case "SB":
                return "Sushi Bar";
            case "TO":
                return "Tax Office";
            case "TS":
                return "Tech Startup";
            case "TR": //trainStation
                return "Train Depot";
            case "TB":
                return "Tuna Boat";
            case "TV":
                return "TV Station";
            case "V":
                return "Vineyard";
            case "WF":
                return "Wheat Field";
            case "W":
                return "Winery";
            default:
                return nullCode;
        }
    }
}

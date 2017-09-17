package mike.cards;

import android.support.v4.util.ArraySet;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by mike on 8/2/2017.
 */

public class Deck {
    public static final int NUM_ESTABLISHMENTS = 38;
    private LinkedList<Establishment> deck;

//    public static final ArraySet<String> primaryIndustryCodes = new ArraySet<>(Arrays.asList("MB", "TB"));
//    public static final ArraySet<String> secondaryIndustryCodes = new ArraySet<>(Arrays.asList("CF", "FF", "PM", "FW", "DC", "LO", "W", "MC", "BP"));
//    public static final ArraySet<String> cropCodes = new ArraySet<>(Arrays.asList("CO", "AO", "FO", "WF", "V"));
//    public static final ArraySet<String> livestockCodes = new ArraySet<>();
    private final ArraySet<String> majorEstablishmentCodes = new ArraySet<>(Arrays.asList("S", "TV", "BC", "PB", "TO", "RC", "TS", "P", "CC"));
//    public static final ArraySet<String> naturalResourceCodes = new ArraySet<>(Arrays.asList("M", "F"));
//    public static final ArraySet<String> shopCodes = new ArraySet<>(Arrays.asList("B", "CS", "FS", "GS"));
//    public static final ArraySet<String> restaurantCodes = new ArraySet<>(Arrays.asList("FA", "C", "SB", "PJ", "BS", "FR", "EC"));

    public Deck(int numPlayers){
//        livestockCodes.add("R");
//        primaryIndustryCodes.addAll(livestockCodes);
//        primaryIndustryCodes.addAll(cropCodes);
//        primaryIndustryCodes.addAll(naturalResourceCodes);
//        secondaryIndustryCodes.addAll(shopCodes);
        deck = new LinkedList<>();
        Establishment[] establishments = new Establishment[]{new AppleOrchard(), new Bakery(),
                new BottlingPlant(), new BurgerStand(), new BusinessCenter(), new Cafe(),
                new CheeseFactory(), new ConvenienceStore(), new ConventionCenter(), new CornField(),
                new DemoCompany(), new ExclusiveClub(), new FamilyRestaurant(),
                new FlowerOrchard(), new FlowerShop(), new FoodWarehouse(), new Forest(),
                new FrenchRestaurant(), new FurnitureFactory(), new GeneralStore(),
                new LoanOffice(), new MackerelBoat(), new Mine(), new MovingCompany(), new Park(),
                new PizzaJoint(), new ProduceMarket(), new Publisher(), new Ranch(),
                new RenoCompany(), new Stadium(), new SushiBar(), new TaxOffice(), new TechStartup(),
                new TunaBoat(), new TvStation(), new Vineyard(), new WheatField(), new Winery()};

        addCopiesToPool(establishments, numPlayers);

        Random rand = new Random();
        int poolLength = establishments.length;
        while(poolLength > 0){
            int poolIndex = rand.nextInt(poolLength);
            try {
                Establishment newCard = establishments[poolIndex].getClass().newInstance();
                establishments[poolIndex].removeCopy();
                if(establishments[poolIndex].getNumCopies() == 0)
                    poolLength = removeFromPool(establishments, poolIndex, poolLength);
                deck.push(newCard);
            } catch (InstantiationException | IllegalAccessException e) {
                //all the cards have no parametrized constructors, so instantiation is good
                //all the cards are public, so illegalAccess is good too
                e.printStackTrace();
            }
        }
    }

    private void addCopiesToPool(Establishment[] establishments, int numPlayers){
        final int nonMajorEstablishmentCopiesInDeck = 6;
        for(Establishment c : establishments){
            //major establishments get one copy for each player
            if(majorEstablishmentCodes.contains(c.getCode())){
                c.setNumCopies(numPlayers);
            }
            //other establishments get 6 copies each regardless of amount of players
            else{
                c.setNumCopies(nonMajorEstablishmentCopiesInDeck);
            }
        }
    }

    private int removeFromPool(Establishment[] establishments, int indexToRemove, int poolLength){
        if(indexToRemove != poolLength-1)
            establishments[indexToRemove] = establishments[poolLength-1];
        return poolLength-1;
    }

    public Establishment draw(){
        if(deck.size() > 0)
            return deck.pop();
        else return null;
    }

    public boolean isEmpty(){
        return deck.isEmpty();
    }

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

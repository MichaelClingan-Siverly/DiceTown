package mike.cards;

/**
 * Created by mike on 7/26/2017.
 * generic interface for the various card visitors
 * Did I really have to use the visitor pattern? well, no...but I can, and it was good practice
 * In the end, I decided to only use the visitor for a few operations where I'd be iterating over
 * a bunch of cards, such as for activating cards after a dice roll
 */

public interface CardVisitor<T> {
    T visit(Establishment e);
    T visit(AppleOrchard orchard);
    T visit(Bakery bakery);
    T visit(BottlingPlant sodaPlant);
    T visit(BurgerStand burgerStand);
    T visit(BusinessCenter businessCenter);
    T visit(Cafe cafe);
    T visit(CheeseFactory cheeseFactory);
    T visit(ConvenienceStore convenienceStore);
    T visit(ConventionCenter conventionCenter);
    T visit(CornField cornField);
    T visit(DemoCompany demoCompany);
    T visit(ExclusiveClub club);
    T visit(FamilyRestaurant familyRestaurant);
    T visit(FlowerShop shop);
    T visit(FlowerOrchard orchard);
    T visit(FoodWarehouse warehouse);
    T visit(Forest forest);
    T visit(FrenchRestaurant restaurant);
    T visit(FurnitureFactory factory);
    T visit(GeneralStore store);
    T visit(LoanOffice loanOffice);
    T visit(MackerelBoat boat);
    T visit(Mine mine);
    T visit(MovingCompany company);
    T visit(Park park);
    T visit(PizzaJoint pizzaJoint);
    T visit(ProduceMarket market);
    T visit(Publisher publisher);
    T visit(Ranch ranch);
    T visit(RenoCompany company);
    T visit(Stadium stadium);
    T visit(SushiBar bar);
    T visit(TaxOffice office);
    T visit(TechStartup startup);
    T visit(TunaBoat boat);
    T visit(TvStation station);
    T visit(Vineyard vineyard);
    T visit(WheatField field);
    T visit(Winery winery);
}

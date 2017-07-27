package mike.cards;

/**
 * Created by mike on 7/26/2017.
 * visitor for
 */

public class CardTypeVisitor implements CardVisitor {
    public static int TYPE_LANDMARK = 0;
    public static int TYPE_PRIMARY_INDUSTRY = 1;
    public static int TYPE_PRIMARY_INDUSTRY_CROP = 2;
    public static int TYPE_PRIMARY_INDUSTRY_LIVESTOCK = 3;
    public static int TYPE_PRIMARY_INDUSTRY_NATURAL_RESOURCES = 4;
    public static int TYPE_SECONDARY_INDUSTRY = 5;
    public static int TYPE_SECONDARY_INDUSTRY_FOOD = 6;
    public static int TYPE_RESTAURANT = 7;
    public static int TYPE_MAJOR_ESTABLSHMENT = 8;

    @Override
    public Integer visit(Card card){
        //TODO
        return 1;
    }
}

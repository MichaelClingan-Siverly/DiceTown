package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class FamilyRestaurant extends HighEstablishment implements Restaurant {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 3;
    }

    @Override
    public String getCode() {
        return "FA";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.family_restaurant;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.family_restaurant;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 36;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

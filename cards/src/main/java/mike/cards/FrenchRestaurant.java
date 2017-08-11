package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class FrenchRestaurant extends Establishment implements Restaurant {
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
        return "FR";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.french_restaurant;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.french_restaurant;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 18;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

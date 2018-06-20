package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class BurgerStand extends HighEstablishment implements Restaurant {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public String getCode() {
        return "BS";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.burger_stand;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.burger_stand;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

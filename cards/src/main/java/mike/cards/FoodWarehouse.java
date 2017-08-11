package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class FoodWarehouse extends Establishment implements SecondaryIndustry {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 2;
    }

    @Override
    public String getCode() {
        return "FW";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.food_warehouse;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.food_warehouse;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 43;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

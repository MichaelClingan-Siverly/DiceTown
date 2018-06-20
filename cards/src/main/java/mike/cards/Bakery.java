package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class Bakery extends LowEstablishment implements Shop {
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
        return "B";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.bakery;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.bakery;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 11;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

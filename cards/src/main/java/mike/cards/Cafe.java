package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class Cafe extends LowEstablishment implements Restaurant {
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
        return "C";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.cafe;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.cafe;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 12;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

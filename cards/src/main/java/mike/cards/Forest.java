package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class Forest extends Establishment implements NaturalResource {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public String getCode() {
        return "F";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.forest;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.forest;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 17;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

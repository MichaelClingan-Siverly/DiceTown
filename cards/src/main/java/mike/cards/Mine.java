package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class Mine extends HighEstablishment implements NaturalResource {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 6;
    }

    @Override
    public String getCode() {
        return "M";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.mine;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.mine;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 34;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class Winery extends HighEstablishment implements SecondaryIndustry {
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
        return "W";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.winery;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.winery;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 33;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

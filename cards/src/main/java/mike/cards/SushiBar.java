package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class SushiBar extends LowEstablishment implements Restaurant {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 4;
    }

    @Override
    public String getCode() {
        return "SB";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.sushi_bar;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.sushi_bar;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 8;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

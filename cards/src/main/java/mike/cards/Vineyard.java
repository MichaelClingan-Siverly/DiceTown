package mike.cards;


/**
 * Created by mike on 7/26/2017.
 */

public class Vineyard extends Establishment implements Crop {
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
        return "V";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.vineyard;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.vineyard;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 26;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

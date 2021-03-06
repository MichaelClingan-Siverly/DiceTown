package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class CornField extends LowEstablishment implements Crop{
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
        return "CO";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.corn_field;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.corn_field;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 13;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

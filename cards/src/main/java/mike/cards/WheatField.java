package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class WheatField extends LowEstablishment implements Crop {
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
        return "WF";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.wheat_field;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.wheat_field;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 7;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

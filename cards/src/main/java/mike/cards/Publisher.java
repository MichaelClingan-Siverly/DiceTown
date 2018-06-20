package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class Publisher extends MajorEstablishment {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 5;
    }

    @Override
    public String getCode() {
        return "PB";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.publisher;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.publisher;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 24;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

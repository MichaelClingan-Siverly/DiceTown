package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class  AppleOrchard extends HighEstablishment implements Crop {

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
        return "AO";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.apple_orchard;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.apple_orchard;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 39;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

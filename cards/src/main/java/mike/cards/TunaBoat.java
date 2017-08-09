package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class TunaBoat extends Establishment implements PrimaryIndustry {
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
        return "TB";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.tuna_boat;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.tuna_boat;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 44;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

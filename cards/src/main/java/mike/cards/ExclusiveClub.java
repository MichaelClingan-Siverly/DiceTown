package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class ExclusiveClub extends HighEstablishment implements Restaurant {
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
        return "EC";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.exclusive_club;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.exclusive_club;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 45;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

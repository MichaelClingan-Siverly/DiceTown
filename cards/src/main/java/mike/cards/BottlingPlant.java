package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class BottlingPlant extends HighEstablishment implements SecondaryIndustry {
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
        return "BP";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.bottling_plant;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.bottling_plant;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 40;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

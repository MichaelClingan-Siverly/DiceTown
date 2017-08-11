package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class CheeseFactory extends Establishment implements SecondaryIndustry{
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
        return "CF";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.cheese_factory;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.cheese_factory;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 25;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

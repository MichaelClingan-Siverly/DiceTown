package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class DemoCompany extends Establishment implements SecondaryIndustry {
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
        return "DC";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.demo_company;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.demo_company;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 15;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

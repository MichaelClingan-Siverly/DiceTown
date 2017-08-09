package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class LoanOffice extends Establishment implements SecondaryIndustry {
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
        return "LO";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.loan_office;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.loan_office;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 19;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

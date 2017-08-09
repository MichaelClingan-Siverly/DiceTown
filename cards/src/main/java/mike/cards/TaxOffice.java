package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class TaxOffice extends Establishment implements MajorEstablishment {
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
        return "TO";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.tax_office;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.tax_office;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 32;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

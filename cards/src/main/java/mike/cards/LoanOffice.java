package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class LoanOffice extends LowEstablishment implements SecondaryIndustry {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    //loan office has a special  removeCopy because its the only card you want to give away
    // working copies of (with others, its better to give away the ones under renovation)
    public void removeCopy(){
        if(numCopies > 0){
            if(numRenovated > 0)
                numRenovated--;
            numCopies--;
        }
    }
    @Override
    public void removeCopyFromOpponent(){
        if(numCopies > 0){
            if(numRenovated == numCopies)
                numRenovated--;
            numCopies--;
        }
    }

    @Override
    public int getCost() {
        return -5;
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

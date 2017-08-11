package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class TechStartup extends Establishment implements MajorEstablishment {
    private int value = 0;

    public int getValue(){
        return value;
    }

    public void addInvestment(){
        value++;
    }

    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public String getCode() {
        return "TS";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.tech_startup;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.tech_startup;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 38;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }
}

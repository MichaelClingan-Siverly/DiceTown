package mike.cards;

/**
 * Created by mike on 7/26/2017.
 */

public class GeneralStore implements Card {
    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

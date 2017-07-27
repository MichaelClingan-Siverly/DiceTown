package mike.cards;

/**
 * Created by mike on 7/26/2017.
 * interface card element of visitor pattern
 */

public interface Card {
    <T> T accept(CardVisitor<T> visitor);
}

package mike.cards;

/**
 * Created by mike on 7/26/2017.
 * generic interface for the various card visitors
 */

public interface CardVisitor <T>{
    <T> T visit(Card card);
}

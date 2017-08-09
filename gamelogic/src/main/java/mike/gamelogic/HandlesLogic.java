package mike.gamelogic;

import android.os.Message;

import mike.cards.Card;

/**
 * Created by mike on 7/30/2017.
 */

public interface HandlesLogic {
    void diceRolled(int d1, int d2);
    void receiveMessage(int playerOrderWhoSentThis, String dataString);
    void goToNextTown();
    void goToPrevTown();
    void middleButtonPressed();
    void receiveTechChoice(boolean makeInvestment);

    /**
     * indicates that user selected a card for whatever reason they needed to select a card
     * @param card the card that was selected, or null if they made no selection (such as the end of turn buy)
     */
    void selectCard(Card card, String ownerName);
    void selectPlayer(String name);
    void radioReply(boolean reroll, int d1, int d2);
    void replyToAddTwo(boolean addTwo, int d1, int d2);
}

package mike.gamelogic;

import android.support.v4.app.Fragment;

import mike.cards.Card;

/**
 * Created by mike on 7/30/2017.
 * This doesn't have to be a class - having it an interface and letting the implementations
 * decide to be fragments or not would be more flexible.
 * But I want it to extend Fragment, as I want it to be able to work as I expect it to,
 * persist through screen changes and not be remade, so I'm making it a Fragment.
 * I also don't think that anything using an implementation of this should be expected
 * to know that I intend this to be a Fragment
 */

public abstract class HandlesLogic extends Fragment {
    public abstract void initLogic(Player[] players, int myPlayerOrder);
    public abstract void diceRolled(int d1, int d2);
    public abstract void receiveMessage(int playerOrderWhoSentThis, String dataString);
    public abstract void goToNextTown();
    public abstract void goToPrevTown();
    public abstract void middleButtonPressed();
    public abstract void receiveTechChoice(boolean makeInvestment);
    public abstract void receiveMoveRenovatedChoice(boolean moveRenovatedCopy);
    public abstract void setLeaveGameCode(String code);
    public abstract int getPlayerOrder();

    /**
     * indicates that user selected a card for whatever reason they needed to select a card
     * @param card the card that was selected, or null if they made no selection (such as the end of turn buy)
     */
    public abstract void selectCard(Card card, String ownerName);
    public abstract void selectPlayer(String name);
    public abstract void radioReply(boolean reroll, int d1, int d2);
    public abstract void replyToAddTwo(boolean addTwo, int d1, int d2);
}

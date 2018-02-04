package mike.gamelogic;

import android.app.Fragment;
import android.os.Message;

import mike.cards.Card;

/**
 * Created by mike on 7/30/2017.
 * Must be a class since I'm working with Fragments.
 */

public abstract class HandlesLogic extends Fragment {
    public abstract void initLogic(Player[] players, int myPlayerOrder);
    public abstract void diceRolled(int d1, int d2);
    public abstract void receiveMessage(int playerOrderWhoSentThis, String dataString);
    public abstract void goToNextTown();
    public abstract void goToPrevTown();
    public abstract void middleButtonPressed();
    public abstract void receiveTechChoice(boolean makeInvestment);
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

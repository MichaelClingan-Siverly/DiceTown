package mike.dicetown;

import mike.cards.Card;
import mike.cards.CardInterface;
import mike.gamelogic.HasCards;
import mike.gamelogic.Player;

/**
 * Created by mike on 1/13/2018.
 * A container class for the dialog information, to make it easier to
 * remake a dialog after being dismissed. This is necessary, because the user may
 * dismiss a dialog and then bring it back up to make whatever choice was required.
 * This should only contain information used for creating the dialogs,
 * not about the selections made in them.
 * Made it a singleton since there should only be one dialog window active at a time.
 */

class DialogInfo {
    //basic Dialog stuff, used by just about all dialog fragments
    private String title;
    private String message;
    private int code;
    private boolean activeDialog;
    private boolean showing;

    //used while picking players
    private HasCards[] players;
    private String myName;
    private boolean forceChoice;

    //used while picking landmarks or market cards
    private CardInterface[] cards;


    private static final DialogInfo ourInstance = new DialogInfo();

    static DialogInfo getInstance() {
        return ourInstance;
    }

    private DialogInfo() {}

    void setTitle(String newTitle){
        title = newTitle;
    }
    void setMessage(String newMessage){
        message = newMessage;
    }
    void setCode(int newCode){
        code = newCode;
    }


    int getCode(){
        return code;
    }
    String getMessage(){
        return message;
    }
    String getTitle(){
        return title;
    }

    void activateDialog(){
        activeDialog = true;
        showing = true;
    }
    void deactivateDialog(){
        activeDialog = false;
    }
    boolean checkIfDialogActive(){
        return activeDialog;
    }

    void setShowing(boolean showing){
        this.showing = showing;
    }
    boolean isShowing(){
        return showing;
    }


    HasCards[] getPlayers() {
        return players;
    }

    void setPlayers(HasCards[] players) {
        this.players = players;
    }

    String getMyName() {
        return myName;
    }

    void setMyName(String myName) {
        this.myName = myName;
    }

    boolean isForcingChoice() {
        return forceChoice;
    }

    void setForceChoice(boolean force) {
        this.forceChoice = force;
    }


    CardInterface[] getCards() {
        return cards;
    }

    void setCards(CardInterface[] cards) {
        this.cards = cards;
    }
}

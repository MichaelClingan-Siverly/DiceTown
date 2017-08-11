package mike.gamelogic;

import android.support.v4.util.ArraySet;
import android.support.v4.util.SimpleArrayMap;

import java.util.AbstractMap;

import mike.cards.Card;
import mike.cards.CardDisplayable;
import mike.cards.CardInterface;
import mike.cards.Landmark;
import mike.cards.Establishment;

/**
 * Created by mike on 7/30/2017.
 *
 */

public interface UI {
    /**
     * Prompts the user to roll a dice, Only really necessary with the TrainStation, but its nice
     * to let the user have some interaction. Also displays the dice roll
     * @param trainStationOwned indicates whether a Train Station is constructed or not.
     *                          If true, user will be able to pick between one or two dice
     * @param forTunaBoat indicates whether the roll is for TunaBoat activation.
     *                    If this is true, 2 dice will be rolled
     */
    void getDiceRoll(boolean trainStationOwned, boolean forTunaBoat);

    /**
     * called by logic when displaying another player's dice roll
     * @param d1 value of first die
     * @param d2 value of second die
     */
    void displayDiceRoll(int d1, int d2);

    /**
     * Certain cards cause the user to pick from different sets of cards.
     * @param cardOwners arrays of HasCards, so this can display cards arranged by player name
     * @param name name of this user (for things like determining whether to give a renovated card or non-renovated one)
     */
    void pickCard(HasCards cardOwners[], String name, boolean nonMajor);

    /**
     * Allows the user to pick a card with respect to their money owned.
     * Unlike the other pickCard, this also allows a user to not select a card.
     * See other pickCard for more info
     * @param money amount of player's money. A card will not be pickable if it costs more than this
     */
    void pickCard(Establishment[] market, Landmark[]myLandmarks, int money, ArraySet<Establishment> myCity);

    /**
     * Display all the information a user may want out of a town screen
     * @param townName the name of the town
     * @param money amount of money owned by the player who owns this town
     * @param cityCards array of all Establishment cards in this town
     * @param landmarks array of Landmarks in this town, both built and under construction
     * @param myTown true if the town being displayed is owned by this player, false if not
     */
    void displayTown(String townName, int money, Establishment[] cityCards, Landmark[] landmarks, boolean myTown);

    //TODO decide if I want the enlarged picture to include the number of copies and number renovated
    /**
     * Displays an enlarged picture of a card
     * @param card the card to be displayed
     */
    void displayCard(CardDisplayable card);

    void pickPlayer(Player[] players, int myIndex);

    /**
     * sends a message across the socket(s)
     */
    void sendMessage(String data, int indexToSendTo, int indexToSkip);

    void makeToast(String text);

    /**
     * Easy way of changing how much money is displayed in the current city without redrawing EVERYTHING
     * @param newMoney amount of money to be shown
     */
    void changeMoney(int newMoney);

    boolean showDialog();
    void pickCard(Landmark[]myLandmarks, String myName);

    void getTechChoice();

    void askIfAddTwo(final int d1, final int d2);

    void askIfReroll(final int d1, final int d2);
}
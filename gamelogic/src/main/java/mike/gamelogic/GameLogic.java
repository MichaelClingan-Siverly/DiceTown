package mike.gamelogic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;

import mike.cards.Airport;
import mike.cards.AmusementPark;
import mike.cards.BusinessCenter;
import mike.cards.Card;
import mike.cards.ConstructibleLandmark;
import mike.cards.ConventionCenter;
import mike.cards.Deck;
import mike.cards.DemoCompany;
import mike.cards.Establishment;
import mike.cards.Harbor;
import mike.cards.Landmark;
import mike.cards.LowEstablishment;
import mike.cards.MajorEstablishment;
import mike.cards.MovingCompany;
import mike.cards.Park;
import mike.cards.Publisher;
import mike.cards.RadioTower;
import mike.cards.RenoCompany;
import mike.cards.Stadium;
import mike.cards.TaxOffice;
import mike.cards.TechStartup;
import mike.cards.TrainStation;
import mike.cards.TunaBoat;
import mike.cards.TvStation;

/**
 * Created by mike on 7/31/2017.
 */

public class GameLogic extends HandlesLogic {
    public static final String TAG_LOGIC_FRAGMENT = "logicFragment";

    private UI ui;
    private boolean initialized = false;
    private int myPlayerOrder;
    private Player[] players;
    private int townIndexBeingShown;
    private Deck deck;
    private ArraySet<Establishment> market;
    private final int marketTownIndex = -1;
    //only really used by the host
    private int playersInGame = 0;
    private int activePlayer = 0;
    private boolean beginTurnRoll = false;
    private final int SELECT_BUY_CARD = 1;
    private final int SELECT_PLAYER_TV = 2;
    private final int SELECT_PLAYER_MOVE = 3;
    private final int SELECT_CARD_MOVE = 4;
    private final int SELECT_BC_MINE = 5;
    private final int SELECT_BC_THEIRS = 6;
    private final int SELECT_RENO = 7;
    private final int SELECT_DEMO = 8;
    private final int SELECT_CONVENTION = 9;
    private final int NOT_REROLL = 0;
    private int selectCardCode;
    private int numCardsToPick = 0;
    private int originalRoll = 0;
    private Card selectedCard = null;
    private Card tradeMeForSelected = null;
    private boolean activateForConvention = false;
    private boolean rolledDoubles = false;
    private boolean radioRerollAvailable = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment. it's pretty large, and saving everything seems liek a hassle
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        ui = (UI)context;
    }
    //because the other onAttach is added in API 23
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        ui = (UI)activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        ui.finishAttachingLogic();
    }

    @Override
    public void onDetach (){
        super.onDetach();
        ui = null;
    }

    public void initLogic(Player[] players, int myPlayerOrder){
        if(initialized) {
            displayTown(townIndexBeingShown);
            return;
        }

        initialized = true;
        this.players = players;
        this.myPlayerOrder = myPlayerOrder;

        market = new ArraySet<>(10);
        //host waits for players to join the game
        if(myPlayerOrder == 0) {
            playersInGame++;
            //tell clients to begin game after host is in to ensure that clients wont join first
            ui.sendMessage("BG:0", -1, -1);
            deck = new Deck(players.length);
        }
        else
            ui.sendMessage(IN_GAME+":"+myPlayerOrder, 0, -1);

        displayTown(myPlayerOrder);
    }

    @Override
    public int getPlayerOrder(){
        return myPlayerOrder;
    }

    private void attemptDrawingCard() {
        int[] marketPileUniques = getNumUniqueCardsPerMarketPile();
        boolean depletedMajor = false;
        boolean depletedLow = false;
        boolean depletedHigh = false;
        while (true) {
            Establishment card;

            if(!depletedLow && marketPileUniques[1] < 5){
                card = deck.draw(Deck.pileCodeLow);
                if(card == null)
                    depletedLow = true;
                else if(!market.contains(card))
                    marketPileUniques[1]++;
            }
            else if(!depletedHigh && marketPileUniques[2] < 5){
                card = deck.draw(Deck.pileCodeHigh);
                if(card == null)
                    depletedHigh = true;
                else if(!market.contains(card))
                    marketPileUniques[2]++;
            }
            else{
                card = deck.draw(Deck.pileCodeMajor);
                if(card == null)
                    depletedMajor = true;
                else if(!market.contains(card))
                    marketPileUniques[0]++;
            }

            if(card != null) {
                addToMarket(card);
                ui.sendMessage(ADD_TO_MARKET + ":" + card.getCode(), -1, -1);
            }
            if (market.size() == 12 || depletedMajor && depletedLow && depletedHigh)
                return;
        }
    }

    /**
     * checks market to do what its name implies...
     * @return index 0 is num major establishments, 1 is num low (dice roll) establishments,
     * 2 is num high establishments
     */
    private int[] getNumUniqueCardsPerMarketPile(){
        int[] arr = new int[]{0,0,0};
        for(Establishment e : market){
            if(e instanceof MajorEstablishment)
                arr[0]++;
            else if(e instanceof LowEstablishment)
                arr[1]++;
            else
                arr[2]++;
        }
        return arr;
    }

    private void addToMarket(Establishment card){
        if(card == null)
            return;
        if(market.contains(card)){
            int index = market.indexOf(card);
            market.valueAt(index).addCopy();
        }
        else
            market.add(card);
    }

    private void removeFromMarket(Establishment card){
        if(market.contains(card)){
            int index = market.indexOf(card);
            Establishment e = market.valueAt(index);
            if(e != null && e.getNumCopies() > 1)
                e.removeCopy();
            else {
                market.removeAt(index);
                if(myPlayerOrder == 0)
                    attemptDrawingCard();
            }
        }
    }

    private void beginTurnDiceRoll(int prevNumDice){
        resetIndicators(prevNumDice);
        boolean trainAvailable = players[myPlayerOrder].checkIfCardAvailable(new TrainStation());
        ui.getDiceRoll(trainAvailable, false, prevNumDice);
    }

    private void resetIndicators(int prevNumDice){
        numCardsToPick = 0;
        originalRoll = 0;
        selectedCard = null;
        tradeMeForSelected = null;
        activateForConvention = false;
        rolledDoubles = false;
        beginTurnRoll = true;
        radioRerollAvailable = players[activePlayer].checkIfCardAvailable(new RadioTower()) && !(prevNumDice == 1 || prevNumDice == 2);
    }

    @Override
    public void diceRolled(int d1, int d2) {
        if(activePlayer == myPlayerOrder) {
            if (beginTurnRoll) {
                beginTurnRoll = false;
                if (d1 == d2)
                    rolledDoubles = true;
                if (radioRerollAvailable) {
                    radioRerollAvailable = false;
                    ui.askIfReroll(d1, d2);
                }
                else
                    radioReply(false, d1, d2);
            }
            else {
                ui.sendMessage(TUNA_REPLY + ":" + d1 + ":d2:" + d2, -1, -1);
                if (myPlayerOrder == 0) {
                    processTunaRoll(d1+d2);
                }
            }
        }
        //wait for everyone's activations to finish if this is for a normal roll
    }

    private void processTunaRoll(int value){
        if(activateForConvention ){
            if(players[activePlayer].checkIfCardAvailable(new Harbor())){
                players[activePlayer].makeMoney(value);
                moneyStuff(activePlayer, value, false);
            }
            beginEndOfTurn();
        }
        else{
            continueIndustry(value);
        }
    }

    @Override
    public void radioReply(boolean reroll, int d1, int d2){
        if(reroll){
            ui.sendMessage(INFORM_OF_REROLL+":"+myPlayerOrder, -1, -1);
            int numDice = d2 != 0 ? 2 : 1;
            beginTurnDiceRoll(numDice);
        }
        else if(d1+d2 >= 10 && players[activePlayer].checkIfCardAvailable(new Harbor())){
            ui.askIfAddTwo(d1, d2);
        }
        else
            replyToAddTwo(false, d1, d2);
    }

    @Override
    public void replyToAddTwo(boolean addTwo, int d1, int d2){
        if(addTwo)
            ui.sendMessage(ROLL + ":" + d1 + ":d2:" + d2+":add2:0", -1, -1);
        else
            ui.sendMessage(ROLL + ":" + d1 + ":d2:" + d2, -1, -1);
        if(myPlayerOrder == 0) {
            originalRoll = d1+d2;
            if(addTwo)
                originalRoll += 2;

            activateRestaurants();
        }
    }

    //start with Restaurants
    private void activateRestaurants(){
        //I'm choosing to have host iterate all players and tell
        //each other player how much money is gained or owed
        if(myPlayerOrder == 0){
            int[] moneyOwed = getRestaurantMoneyOwed();
            int index = activePlayer;
            //set index to previous player (or last player if current is host)
            index = index-1 >= 0 ? index-1 : players.length-1;
            int totalPaid = 0;
            //pay all players in reverse turn order (beginning with the player who goes before you)
            while(index != activePlayer){
                if(moneyOwed[index] > 0){
                    int moneyPaid = players[activePlayer].loseMoney(moneyOwed[index]);
                    //if a player is out of money, don't bother doing any more work...
                    if(moneyPaid == 0)
                        break;
                    totalPaid += moneyPaid;
                    players[index].makeMoney(moneyPaid);
                    moneyStuff(index, moneyPaid, false);
                }
                index = index-1 >= 0 ? index-1 : players.length-1;
            }
            if(totalPaid > 0)
                moneyStuff(activePlayer, totalPaid, true);
            activateIndustry();
        }
    }

    private int[] getRestaurantMoneyOwed(){
        int[] moneyOwed = new int[players.length];
        //for each player
        for(int i = 0; i < players.length; i++){
            moneyOwed[i] = 0;
            if(activePlayer != i) {
                //visit all their Establishments
                ActivationVisitor visitor = new ActivationVisitor(originalRoll, players, activePlayer, i, ActivationVisitor.ACTIVATE_RESTAURANTS, 0);
                for(int j = 0; j < players[i].getCity().length; j++){
                    moneyOwed[i] += visitor.visit(players[i].getCity()[j]);
                }
            }
        }
        return moneyOwed;
    }

    private void activateIndustry(){
        if(myPlayerOrder == 0){
            if(tunaBoatActivated(originalRoll)){
                if(activePlayer == myPlayerOrder){
                    ui.getDiceRoll(false, true, -1);
                }
                else{
                    ui.sendMessage(NEED_TUNA_ROLL+":0", activePlayer, -1);
                }
            }
            else
                continueIndustry(0);
        }
    }

    private void continueIndustry(int tunaRoll){
        int moneyGained[] = getIndustryMoneyGained(tunaRoll);
        for(int i = 0; i < moneyGained.length; i++){
            players[i].makeMoney(moneyGained[i]);
            if(moneyGained[i] != 0) {
                moneyStuff(i, moneyGained[i], false);
                if(i == townIndexBeingShown)
                    displayTown(i);
            }
        }
        //check if roll would activate a movingCompany: this does not activate on same roll as loan office, so I just let it always go last
        if((originalRoll == 9 || originalRoll == 10) && players[activePlayer].checkIfCardAvailable(new MovingCompany())){
            int movingIndex = players[activePlayer].getCitySet().indexOf(new MovingCompany());
            numCardsToPick = players[activePlayer].getCitySet().valueAt(movingIndex).getNumAvailable();
            if(myPlayerOrder == activePlayer)
                activateMovingCompany();
            else
                ui.sendMessage(REQUEST_ACTIVATE_MOVING+":"+numCardsToPick, activePlayer, -1);
        }
        else if(originalRoll == 4 && players[activePlayer].checkIfCardAvailable(new DemoCompany())){
            int index = players[activePlayer].getCitySet().indexOf(new DemoCompany());
            Establishment card = players[activePlayer].getCitySet().valueAt(index);
            selectCardCode = SELECT_DEMO;
            numCardsToPick = card.getNumAvailable();
            int numConstructedLandmarks = players[activePlayer].getNumConstructedLandmarks();
            numCardsToPick = Math.min(numCardsToPick, numConstructedLandmarks);
            if(numCardsToPick > 0) {
                if (myPlayerOrder == activePlayer)
                    activateDemoCompany();
                else
                    ui.sendMessage(REQUEST_ACTIVATE_DEMO + ":" + numCardsToPick, activePlayer, -1);
            }
            else
                activateMajorEstablishments();
        }
        else
            activateMajorEstablishments();
    }

    private void activateDemoCompany(){
        selectCardCode = SELECT_DEMO;
        ui.pickCard(players[myPlayerOrder].getMyConstructedLandmarks(), players[myPlayerOrder].getName());
    }

    private int[] getIndustryMoneyGained(int tunaRoll){
        int[] moneyGained = new int[players.length];
        //for each player check the proper establishments and make money
        for(int i = 0; i < players.length; i++){
            moneyGained[i] = 0;
            ActivationVisitor visitor = new ActivationVisitor(originalRoll, players, activePlayer, i, ActivationVisitor.ACTIVATE_INDUSTRY, tunaRoll);
            for(int j = 0; j < players[i].getCity().length; j++){
                moneyGained[i] += visitor.visit(players[i].getCity()[j]);
            }
        }
        return moneyGained;
    }

    private boolean tunaBoatActivated(int diceTotal){
        if(diceTotal >= 12) {
            for (Player p : players) {
                if(p.checkIfCardAvailable(new TunaBoat()))
                    return true;
            }
        }
        return false;
    }

    private void activateMovingCompany(){
        selectCardCode = SELECT_CARD_MOVE;
        if(numCardsToPick == 0){
            int movingCompanyIndex = players[activePlayer].getCitySet().indexOf(new MovingCompany());
            if(movingCompanyIndex >= 0) {
                players[activePlayer].getCitySet().valueAt(movingCompanyIndex).finishRenovation();
                if(townIndexBeingShown == activePlayer)
                    displayTown(townIndexBeingShown);
            }
        }
        if(numCardsToPick > 0 && activePlayer == myPlayerOrder){
            Player[] pArray = new Player[1];
            pArray[0] = players[myPlayerOrder];
            ui.pickCard(pArray, players[myPlayerOrder].getName(), "Moving Company", true);
        }
        else if(myPlayerOrder == 0){
            if(activateForConvention)
                beginEndOfTurn();
            else
                activateMajorEstablishments();
        }
    }

    //helpful to remember that major establishments can not be renovated, so I don't have to check
    private void activateMajorEstablishments(){
        if(myPlayerOrder != 0){
            return;
        }
        int totalGained = 0;
        if(originalRoll == 6){
            //get 2 coins from all players,
            if(players[activePlayer].getCitySet().contains(new Stadium())){
                int moneyOwedToMe;
                for(int i = 0; i < players.length; i++){
                    if(i != activePlayer) {
                        moneyOwedToMe = players[i].loseMoney(2);
                        totalGained += moneyOwedToMe;
                        moneyStuff(i, moneyOwedToMe, true);
                    }
                }
                players[activePlayer].makeMoney(totalGained);
                moneyStuff(activePlayer, totalGained, false);
            }
            //takes coins from a chosen player
            if(players[activePlayer].getCitySet().contains(new TvStation())){
                selectCardCode = SELECT_PLAYER_TV;
                if(activePlayer == myPlayerOrder)
                    ui.pickPlayer(players, activePlayer, "pick player for TV Station");
                else
                    ui.sendMessage(REQUEST_TV_STATION+":0", activePlayer, -1);
            }
            else
                checkBusinessCenter();
        }
        else if(originalRoll == 7 && players[activePlayer].getCitySet().contains(new Publisher())){
            doPublisher();
        }
        else if(originalRoll == 8 || originalRoll == 9){
            if(players[activePlayer].getCitySet().contains(new TaxOffice()))
                doTaxOffice();
            if(originalRoll == 8 && players[activePlayer].getCitySet().contains(new RenoCompany())){
                if(activePlayer != myPlayerOrder && myPlayerOrder == 0)
                    ui.sendMessage(REQUEST_RENO+":0", activePlayer, -1);
                else {
                    selectCardCode = SELECT_RENO;
                    ui.pickCard(players, players[myPlayerOrder].getName(), "Renovation Company", true);
                }
            }
            else {
                beginEndOfTurn();
            }
        }
        else if(originalRoll == 10){
            if(players[activePlayer].getCitySet().contains(new TechStartup())){
                int index = players[activePlayer].getCitySet().indexOf(new TechStartup());
                TechStartup startup = (TechStartup)players[activePlayer].getCitySet().valueAt(index);
                if(startup.getValue() > 0) {
                    int moneyGained = 0;
                    for (int i = 0; i < players.length; i++) {
                        if (i != activePlayer) {
                            int moneyLost = players[i].loseMoney(startup.getValue());
                            moneyStuff(i, moneyLost, true);
                            moneyGained += moneyLost;
                        }
                    }
                    players[activePlayer].makeMoney(moneyGained);
                    moneyStuff(activePlayer, moneyGained, false);
                }
            }
            if(players[activePlayer].getCitySet().contains(new ConventionCenter())){
                selectCardCode = SELECT_CONVENTION;
                if(activePlayer == 0 && myPlayerOrder == 0) {
                    activateForConvention = true;
                    Player[] me = new Player[]{players[0]};
                    ui.pickCard(me, players[myPlayerOrder].getName(), "Convention Center", false);
                }
                else
                    ui.sendMessage(REQUEST_CONVENTION+":0", activePlayer, -1);
            }
            else
                beginEndOfTurn();
        }
        else if(originalRoll >= 11 && originalRoll <= 13 && players[activePlayer].getCitySet().contains(new Park())){
            int totalMoney = 0;
            int[] moneyLostPerPlayer = new int[players.length];
            for(int i = 0; i < players.length; i++){
                moneyLostPerPlayer[i] = players[i].loseMoney(players[i].getMoney());
                totalMoney += moneyLostPerPlayer[i];
            }
            int remainder = totalMoney % players.length;
            totalMoney += remainder;
            int moneyToEachPlayer = totalMoney / players.length;
            for(int i = 0; i < moneyLostPerPlayer.length; i++){
                players[i].makeMoney(moneyToEachPlayer);
                if(moneyToEachPlayer < moneyLostPerPlayer[i])
                    moneyStuff(i, moneyLostPerPlayer[i] - moneyToEachPlayer, true);
                else if(moneyToEachPlayer > moneyLostPerPlayer[i])
                    moneyStuff(i, moneyToEachPlayer - moneyLostPerPlayer[i], false);
            }
            beginEndOfTurn();
        }
        else{
            beginEndOfTurn();
        }
    }

    private void doTaxOffice(){
        int moneyGained = 0;
        for(int i = 0; i < players.length; i++){
            if(i != activePlayer && players[i].getMoney() >= 10){
                int moneyLost = players[i].loseMoney(players[i].getMoney() / 2);
                moneyGained += moneyLost;
                moneyStuff(i, moneyLost, true);
            }
        }
        players[activePlayer].makeMoney(moneyGained);
        moneyStuff(activePlayer, moneyGained, false);
    }

    private void doPublisher(){
        final ArraySet<String> shopCodes = new ArraySet<>(Arrays.asList("B", "CS", "FS", "GS"));
        final ArraySet<String> restaurantCodes = new ArraySet<>(Arrays.asList("FA", "C", "SB", "PJ", "BS", "FR", "EC"));
        int totalGained = 0;
        for(int i = 0; i < players.length; i++){
            if(i != activePlayer){
                int moneyOwed = 0;
                for(Establishment e : players[i].getCity()){
                    if(shopCodes.contains(e.getCode()) || restaurantCodes.contains(e.getCode())){
                        moneyOwed += e.getNumCopies();
                    }
                }
                int moneyPaid = players[i].loseMoney(moneyOwed);
                totalGained += moneyPaid;
                moneyStuff(i, moneyPaid, true);
            }
        }
        players[activePlayer].makeMoney(totalGained);
        moneyStuff(activePlayer, totalGained, false);
        beginEndOfTurn();
    }

    private void checkBusinessCenter(){
        if(originalRoll == 6 &&  players[activePlayer].getCitySet().contains(new BusinessCenter())){
            if(activePlayer == 0 && myPlayerOrder == 0) {
                selectCardCode = SELECT_BC_MINE;
                Player[] me = new Player[]{players[0]};
                ui.pickCard(me, players[myPlayerOrder].getName(), "Business Center: card to give", true);
            }
            else {
                ui.sendMessage(REQUEST_TRADE_CARD+":0", activePlayer, -1);
            }
        }
        else if(myPlayerOrder == 0){
            beginEndOfTurn();
        }
    }

    private void tradeCards(Player me, Establishment myCard, Player them, Establishment theirCard){
        int i;
        //I want to take an unrenovated copy if there is one, unless their card is a loan office
        if(!me.getCitySet().contains(theirCard)) {
            Establishment e = (Establishment)Deck.getCardFromCode(theirCard.getCode());
            if(e != null)
                e.removeCopy();
            me.getCitySet().add(e);
        }
        i = me.getCitySet().indexOf(theirCard);
        if(theirCard.getNumAvailable() > 0 && !theirCard.getCode().equals("LO"))
            me.getCitySet().valueAt(i).addCopy();
        else
            me.getCitySet().valueAt(i).addRenovatedCopy();

        //I want to give a renovated copy if there is one, unless my card is a loan office
        if(!them.getCitySet().contains(myCard)) {
            Establishment e = (Establishment) Deck.getCardFromCode(myCard.getCode());
            if (e != null)
                e.removeCopy();
            them.getCitySet().add(e);
        }
        i = them.getCitySet().indexOf(myCard);
        if(myCard.getNumCopies() > myCard.getNumAvailable() && !myCard.getCode().equals("LO"))
            them.getCitySet().valueAt(i).addRenovatedCopy();
        else
            them.getCitySet().valueAt(i).addCopy();

        me.removeCopyOfEstablishment(myCard, true);
        them.removeCopyOfEstablishment(theirCard, false);

        int meIndex;
        for(meIndex = 0; meIndex < players.length; meIndex++){
            if(players[meIndex].getName().equals(me.getName()))
                break;
        }
        int themIndex;
        for(themIndex = 0; themIndex < players.length; themIndex++){
            if(players[themIndex].getName().equals(them.getName()))
                break;
        }

        if(townIndexBeingShown == meIndex || townIndexBeingShown == themIndex)
            displayTown(townIndexBeingShown);

        if(myPlayerOrder == 0)
            beginEndOfTurn();
    }

    private void finishReno(Establishment demoMe){
        int moneyGained = 0;
        Player p;
        for(int i = 0; i < players.length; i++){
            p = players[i];
            if(p.getCitySet().contains(demoMe)){
                int index = p.getCitySet().indexOf(demoMe);
                int moneyLost = p.getCitySet().valueAt(index).getNumAvailable();
                p.getCitySet().valueAt(index).closeForRenovation();
                //renovation even closes the active player's establishments,
                // but there is no point in making active lose money to themselves
                if(activePlayer != i) {
                    //I reuse moneyLost here. The only need to know how many were available is to see how much is owed
                    moneyLost = p.loseMoney(moneyLost);
                    if(townIndexBeingShown == i)
                        displayTown(townIndexBeingShown);
                    moneyGained += moneyLost;
                }
            }
        }
        players[activePlayer].makeMoney(moneyGained);
        if(townIndexBeingShown == activePlayer)
            displayTown(townIndexBeingShown);
        if(myPlayerOrder == 0)
            beginEndOfTurn();
    }

    private void activateForConvention(Establishment card){
        if(players[activePlayer].getCitySet().contains(card)){
            activateForConvention = true;
            int index = players[activePlayer].getCitySet().indexOf(card);
            Establishment e = players[activePlayer].getCitySet().valueAt(index);
            //if all are being renovated, activating one will only finish its renovation
            if(e.getNumAvailable() == 0){
                e.finishRenovation();
            }
            else if(e.getCode().equals("MC")){ //moving company
                numCardsToPick = e.getNumAvailable();
                if(activePlayer == 0 && myPlayerOrder == 0)
                    activateMovingCompany();
                else
                    ui.sendMessage(REQUEST_ACTIVATE_MOVING + ":" + numCardsToPick, activePlayer, -1);
            }
            else if(e.getCode().equals("DC")){ //demo company
                numCardsToPick = e.getNumAvailable();
                if(myPlayerOrder == activePlayer)
                    activateDemoCompany();
                else{

                    ui.sendMessage(REQUEST_ACTIVATE_DEMO+":"+numCardsToPick, activePlayer, -1);
                }
            }
            else if(e.getCode().equals("TB")){ //tuna boat
                if(activePlayer == myPlayerOrder){
                    ui.getDiceRoll(false, true, -1);
                }
                else{
                    ui.sendMessage(NEED_TUNA_ROLL+":0", activePlayer, -1);
                }
            }
            else{
                ActivationVisitor visitor = new ActivationVisitor(0, players, activePlayer, activePlayer, ActivationVisitor.FORCE_ACTIVATE, 0);
                int moneyGained = visitor.visit(e);
                players[activePlayer].makeMoney(moneyGained);
                moneyStuff(activePlayer, moneyGained, false);
                beginEndOfTurn();
            }
        }
        else
            beginEndOfTurn();
    }

    private void moneyStuff(int playerOrder, int amount, boolean loseMoney){
        if(myPlayerOrder == 0) {
            if (loseMoney)
                ui.sendMessage(LOSE_MONEY + ":p" + playerOrder + ":$:" + amount, -1, -1);
            else
                ui.sendMessage(GAIN_MONEY + ":p" + playerOrder + ":$:" + amount, -1, -1);
        }
        if(townIndexBeingShown == playerOrder)
            displayTown(playerOrder);
    }

    //1st step of end of turn buy
    private void beginEndOfTurn(){
        //if ConventionCenter was activated, it gets returned to the market
        if(activateForConvention) {
            returnActivePlayersConvention();
            ui.sendMessage(RETURN_TO_MARKET+":"+activePlayer, -1, -1);
        }
        //checks City Hall
        if(players[activePlayer].getMoney() == 0){
            players[activePlayer].makeMoney(1);
            ui.makeToast("Gaining money from City Hall");
            moneyStuff(activePlayer, 1, false);
        }
        if(myPlayerOrder == 0){
            endOfTurnBuy();
        }
    }

    private void returnActivePlayersConvention(){
        Establishment conv = new ConventionCenter();
        if(players[activePlayer].getCitySet().contains(conv)){
            players[activePlayer].getCitySet().remove(new ConventionCenter());
            addToMarket(conv);
            if(townIndexBeingShown == activePlayer)
                displayTown(activePlayer);
        }
    }

    //2nd step of end of turn
    private void endOfTurnBuy(){
        selectCardCode = SELECT_BUY_CARD;
        if(myPlayerOrder == activePlayer){
            Player me = players[myPlayerOrder];
            ui.pickCard(market.toArray(new Establishment[market.size()]), me);
        }
        else
            ui.sendMessage(ADD_TO_CITY+":?", activePlayer, -1);
    }

    //3rd step of end of turn
    private void checkTech(){
        if(myPlayerOrder == 0) {
            int index;
            if(activePlayer == 0 && 0 <= (index = players[activePlayer].getCitySet().indexOf(new TechStartup())))
                ui.getTechChoice(((TechStartup)players[activePlayer].getCitySet().valueAt(index)).getValue());
            else if(players[activePlayer].getMoney() > 0 && players[activePlayer].checkIfCardAvailable(new TechStartup()))
                ui.sendMessage(REQUEST_TECH_CHOICE+":0", activePlayer, -1);
            else
                checkAmusementPark();
        }
    }

    //4th step of end of turn
    @Override
    public void receiveTechChoice(boolean makeInvestment){
        if(makeInvestment) {
            ui.sendMessage(REPLY_TECH_CHOICE + ":y", -1, -1);
            if(activePlayer == townIndexBeingShown) {
                Player p = players[activePlayer];
                int index = p.getCitySet().indexOf(new TechStartup());
                if(index >= 0)
                    ((TechStartup)p.getCitySet().valueAt(index)).addInvestment();
                displayTown(activePlayer);
            }
        }
        if(myPlayerOrder == 0)
            checkAmusementPark();
        else
            ui.sendMessage(REPLY_TECH_CHOICE + "n", -1, -1);
    }

    //5th step of end of turn
    private void checkAmusementPark(){
        //shouldn't have to tell anyone but the active player when a new player begins their turn
        if(players[activePlayer].checkIfCardAvailable(new AmusementPark()) && rolledDoubles){
            resetIndicators(NOT_REROLL);
            ui.sendMessage(BEGIN_TURN + ":" + activePlayer, -1, -1);
           if(activePlayer == 0)
               beginTurnDiceRoll(NOT_REROLL);
           else
               notifyOfNewActivePlayer(true);
        }
        else{
            activePlayer = (activePlayer+1) % players.length;
            resetIndicators(NOT_REROLL);
            ui.sendMessage(BEGIN_TURN + ":" + activePlayer, -1, -1);
            if(activePlayer == 0)
                beginTurnDiceRoll(NOT_REROLL);
            else
                notifyOfNewActivePlayer(false);
        }
    }

    private void notifyOfNewActivePlayer(boolean sameAsLastTurn){
        String nameWithSuffix = players[activePlayer].getName();
        if(nameWithSuffix.endsWith("s") || nameWithSuffix.endsWith("z"))
            nameWithSuffix = nameWithSuffix + '\'';
        else
            nameWithSuffix = nameWithSuffix+"'s";
        if(sameAsLastTurn)
            ui.makeToast(nameWithSuffix + " takes another turn");
        else
            ui.makeToast("It is " + nameWithSuffix + " turn");
    }

    //TODO this is a monster of a method...should definitely break it up.
    @Override
    public void receiveMessage(int playerOrderWhoSentThis, String dataString) {
        ArrayList<DataParser.DataMapping> mappings = DataParser.parseIncomingData(dataString);
        for(DataParser.DataMapping map : mappings){
            switch(map.keyWord){
                case INFORM_OF_REROLL: {
                    ui.makeToast(players[playerOrderWhoSentThis].getName() + " rerolled their dice");
                    if (myPlayerOrder == 0)
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                    break;
                }
                case IN_GAME: {
                    playersInGame++;
                    if (playersInGame == players.length && myPlayerOrder == 0) {
                        ui.makeToast("Building the market...");
                        attemptDrawingCard();
                        beginTurnDiceRoll(NOT_REROLL);
                    }
                    break;
                }
                case ADD_TO_MARKET: {
                    Establishment c = (Establishment) Deck.getCardFromCode(map.value);
                    addToMarket(c);
                    break;
                }
                case RETURN_TO_MARKET: {
                    returnActivePlayersConvention();
                    if(myPlayerOrder == 0)
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                    break;
                }
                case BEGIN_TURN: {
                    resetIndicators(NOT_REROLL);
                    int oldActivePlayer = activePlayer;
                    activePlayer = Integer.parseInt(map.value);
                    if (activePlayer == myPlayerOrder)
                        beginTurnDiceRoll(NOT_REROLL);
                    else
                        notifyOfNewActivePlayer(oldActivePlayer == activePlayer);
                    break;
                }
                case TUNA_REPLY: {
                    ui.makeToast("Rolling for Tuna Boat value");
                }
                case ROLL: {
                    int d1 = Integer.parseInt(map.value);
                    int d2 = 0;
                    int index = mappings.indexOf(map);
                    boolean addTwo = false;
                    if (mappings.size() > index + 1) {
                        DataParser.DataMapping nextMap = mappings.get(index + 1);
                        if (nextMap.keyWord.equals("d2"))
                            d2 = Integer.parseInt(nextMap.value);
                        if (mappings.size() > index + 2) {
                            nextMap = mappings.get(index + 2);
                            if (nextMap.keyWord.equals("add2")){
                                addTwo = true;
                                ui.makeToast(players[activePlayer].getName()
                                        + " used Wharf to add 2 to their roll");
                            }
                        }
                    }
                    if (myPlayerOrder == 0) {
                        if (map.keyWord.equals(ROLL)) {
                            String stringToSend = ROLL + ":" + d1 + ":d2:" + d2;
                            originalRoll = d1 + d2;
                            if (addTwo) {
                                originalRoll += 2;
                                stringToSend += ":add2:0";
                            }
                            ui.sendMessage(stringToSend, -1, activePlayer);
                            if (d1 == d2)
                                rolledDoubles = true;
                            activateRestaurants();
                        }
                        else if (map.keyWord.equals(TUNA_REPLY)) {
                            ui.sendMessage(TUNA_REPLY + ":" + d1 + ":d2:" + d2, -1, playerOrderWhoSentThis);
                            processTunaRoll(d1 + d2);
                        }
                    }
                    else{
                        for(int playerIndex = 0; playerIndex < players.length; playerIndex++){
                            Player p = players[playerIndex];
                            int activationCode;
                            if(playerIndex == activePlayer)
                                activationCode = ActivationVisitor.ACTIVATE_RESTAURANTS;
                            else
                                activationCode = ActivationVisitor.ACTIVATE_INDUSTRY;
                            //finish potential renovations, since the host doesn't tell clients that
                            ActivationVisitor v = new ActivationVisitor(d1+d2, players, activePlayer, playerIndex, activationCode, 0);
                            for(Establishment e : p.getCity()) {
                                v.visit(e);
                                if(playerIndex == activePlayer) {
                                    if(e instanceof MovingCompany || e instanceof DemoCompany)
                                        e.finishRenovation();

                                }
                            }
                            displayTown(townIndexBeingShown);
                        }
                    }
                    ui.displayDiceRoll(d1, d2);
                    break;
                }
                case GAIN_MONEY:
                case LOSE_MONEY: {
                    boolean gain = map.keyWord.equals(GAIN_MONEY);
                    int playerAffected = Integer.parseInt(map.value.substring(1));
                    int index = mappings.indexOf(map);
                    if (mappings.size() > index + 1) {
                        DataParser.DataMapping nextMap = mappings.get(index + 1);
                        if (nextMap.keyWord.equals("$")) {
                            int value = Integer.parseInt(nextMap.value);
                            if (gain)
                                players[playerAffected].makeMoney(value);
                            else
                                players[playerAffected].loseMoney(value);
                            if (townIndexBeingShown == playerAffected)
                                ui.changeMoney(players[playerAffected].getMoney());
                        }
                    }
                    break;
                }
                case NEED_TUNA_ROLL: {
                    if (myPlayerOrder != 0) {
                        ui.getDiceRoll(false, true, 0);
                    }
                    break;
                }
                case REPLY_ACTIVATE_MOVING: {
                    int index = mappings.indexOf(map);
                    int toPlayerOrder = Integer.parseInt(map.value);
                    Player p = players[toPlayerOrder];
                    if (mappings.size() > index + 1) {
                        DataParser.DataMapping nextMap = mappings.get(index + 1);
                        Establishment card = (Establishment) Deck.getCardFromCode(nextMap.keyWord);
                        if (players[activePlayer].getCitySet().contains(card)) {
                            players[activePlayer].removeCopyOfEstablishment(card, true);
                        }
                        if (p.getCitySet().contains(card)) {
                            int i = p.getCitySet().indexOf(card);
                            if (Integer.parseInt(nextMap.value) == 0)
                                p.getCitySet().valueAt(i).addCopy();
                            else
                                p.getCitySet().valueAt(i).addRenovatedCopy();
                        }
                        else if(card != null){
                            card.closeForRenovation();
                            p.getCitySet().add(card);
                        }
                        if (townIndexBeingShown == activePlayer || townIndexBeingShown == toPlayerOrder)
                            displayTown(townIndexBeingShown);
                    }
                    if (myPlayerOrder == 0) {
                        numCardsToPick--;
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                        activateMovingCompany();
                    }
                    break;
                }
                case REQUEST_ACTIVATE_MOVING:
                    numCardsToPick = Integer.parseInt(map.value);
                    activateMovingCompany();
                    break;
                case REQUEST_TV_STATION:
                    selectCardCode = SELECT_PLAYER_TV;
                    ui.pickPlayer(players, myPlayerOrder, "pick player for TV Station");
                    break;
                case REPLY_TV_STATION: {
                    selectCardCode = 0;
                    int index = mappings.indexOf(map);
                    int moneyGained = Integer.parseInt(map.value);
                    if (mappings.size() > index + 1) {
                        DataParser.DataMapping nextMap = mappings.get(index + 1);
                        int fromIndex = Integer.parseInt(nextMap.value);
                        Player from = players[fromIndex];
                        Player to = players[activePlayer];
                        from.loseMoney(moneyGained);
                        to.makeMoney(moneyGained);
                        if (townIndexBeingShown == activePlayer || townIndexBeingShown == fromIndex)
                            displayTown(townIndexBeingShown);
                    }
                    if (myPlayerOrder == 0) {
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                        checkBusinessCenter();
                    }
                    break;
                }
                case REQUEST_TRADE_CARD: {
                    selectCardCode = SELECT_BC_MINE;
                    Player[] me = new Player[]{players[myPlayerOrder]};
                    ui.pickCard(me, players[myPlayerOrder].getName(), "Business Center: card to give", true);
                    break;
                }
                case REPLY_TRADE_CARD: {
                    int index = mappings.indexOf(map);
                    if (mappings.size() > index + 2) {
                        DataParser.DataMapping nextMap = mappings.get(index + 1);
                        Player from = players[Integer.parseInt(nextMap.keyWord)];
                        String code = nextMap.value;
                        Establishment mine = (Establishment) Deck.getCardFromCode(code);
                        int i = from.getCitySet().indexOf(mine);
                        mine = from.getCitySet().valueAt(i);

                        nextMap = mappings.get(index + 2);
                        Player to = players[Integer.parseInt(nextMap.keyWord)];
                        code = nextMap.value;
                        Establishment theirs = (Establishment) Deck.getCardFromCode(code);
                        i = to.getCitySet().indexOf(theirs);
                        theirs = to.getCitySet().valueAt(i);

                        if (myPlayerOrder == 0)
                            ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                        tradeCards(from, mine, to, theirs);
                    }
                    break;
                }
                case REQUEST_RENO:
                    selectCardCode = SELECT_RENO;
                    ui.pickCard(players, players[myPlayerOrder].getName(), "Renovation Company", true);
                    break;
                case REPLY_RENO:
                    selectCardCode = 0;
                    Establishment e = (Establishment)Deck.getCardFromCode(map.value);
                    if(myPlayerOrder == 0) {
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                    }
                    finishReno(e);
                    break;
                case REQUEST_ACTIVATE_DEMO: {
                    selectCardCode = SELECT_DEMO;
                    if (players[activePlayer].getCitySet().contains(new DemoCompany())) {
                        int index = players[activePlayer].getCitySet().indexOf(new DemoCompany());
                        Card card = players[activePlayer].getCitySet().valueAt(index);
                        numCardsToPick = Math.min(card.getNumAvailable(), 1);
                        int numConstructedLandmarks = players[activePlayer].getNumConstructedLandmarks();
                        numCardsToPick = Math.min(numCardsToPick, numConstructedLandmarks);
                        activateDemoCompany();
                    }
                    break;
                }
                case REPLY_ACTIVATE_DEMO: {
                    Player p = players[activePlayer];
                    Landmark landmark = (Landmark) Deck.getCardFromCode(map.value);
                    for (Landmark l : p.getLandmarks()) {
                        if (l.equals(landmark)) {
                            l.closeForRenovation();
                            p.makeMoney(8);
                            if (townIndexBeingShown == activePlayer)
                                displayTown(activePlayer);
                            break;
                        }
                    }
                    if (myPlayerOrder == 0) {
                        numCardsToPick--;
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                        if (numCardsToPick == 0) {
                            if (activateForConvention)
                                beginEndOfTurn();
                            else
                                activateMajorEstablishments();
                        }
                    }
                    break;
                }
                case REQUEST_CONVENTION:
                    selectCardCode = SELECT_CONVENTION;
                    if(activePlayer == myPlayerOrder) {
                        Player[] me = new Player[]{players[activePlayer]};
                        ui.pickCard(me, players[myPlayerOrder].getName(), "Convention Center", false);
                    }
                    break;
                case REPLY_CONVENTION:
                    Establishment est = (Establishment)Deck.getCardFromCode(map.value);
                    activateForConvention(est);
                    if(myPlayerOrder == 0)
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                    break;
                case ADD_TO_CITY: {
                    Player p;
                    if (map.value.equals("?")) {
                        selectCardCode = SELECT_BUY_CARD;
                        p = players[myPlayerOrder];
                        ui.pickCard(market.toArray(new Establishment[market.size()]), p);
                    }
                    else if (map.value.equals("0")) {
                        if (players[activePlayer].checkIfCardAvailable(new Airport())) {
                            players[activePlayer].makeMoney(10);
                            ui.makeToast(players[activePlayer].getName() + " makes $10 from Airport");
                        } else {
                            ui.makeToast(players[activePlayer].getName() + " doesn't buy anything");
                        }
                        if (myPlayerOrder == 0) {
                            ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                            checkTech();
                        }
                    }
                    else {
                        p = players[activePlayer];
                        final ArraySet<String> landmarkCodes = new ArraySet<>(Arrays.asList("H", "TR", "SM", "RT", "AP", "A"));
                        ui.makeToast(p.getName() + " buys " + (Deck.getCardNameFromCode(map.value)));
                        if (landmarkCodes.contains(map.value))
                            p.buyCard((ConstructibleLandmark) Deck.getCardFromCode(map.value));
                        else {
                            p.buyCard((Establishment) Deck.getCardFromCode(map.value));
                            removeFromMarket((Establishment) Deck.getCardFromCode(map.value));
                        }
                        if (townIndexBeingShown == activePlayer)
                            displayTown(activePlayer);
                        if (myPlayerOrder == 0) {
                            ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                            checkTech();
                        }
                    }
                    break;
                }
                case REQUEST_TECH_CHOICE: {
                    int index = players[myPlayerOrder].getCitySet().indexOf(new TechStartup());
                    if (index >= 0) {
                        ui.getTechChoice(((TechStartup) players[myPlayerOrder].getCitySet().valueAt(index)).getValue());
                    }
                    break;
                }
                case REPLY_TECH_CHOICE: {
                    if (map.value.equals("y") && players[activePlayer].getCitySet().contains(new TechStartup())) {
                        int index = players[activePlayer].getCitySet().indexOf(new TechStartup());
                        ((TechStartup) players[activePlayer].getCitySet().valueAt(index)).addInvestment();
                        if (activePlayer == townIndexBeingShown)
                            displayTown(activePlayer);
                    }
                    if (myPlayerOrder == 0) {
                        ui.sendMessage(dataString, -1, playerOrderWhoSentThis);
                        checkAmusementPark();
                    }
                    break;
                }
                default:
                    //had to do this for the leave code since its not final (couldn't make it a case)
                    if(map.keyWord.equals(leaveGameCode)){
                        int playerOrderLeaving = Integer.parseInt(map.value);

                        if(myPlayerOrder == 0)
                            ui.sendMessage(leaveGameCode+":"+playerOrderLeaving, -1, -1);
                        else if(playerOrderLeaving == 0){
                            ui.makeToast("host left game");
                            ui.leaveGame(myPlayerOrder);
                            return;
                        }
                        ui.makeToast(players[playerOrderLeaving].getName() + " left the game");
                        removePlayer(playerOrderLeaving);

                        if(townIndexBeingShown == playerOrderLeaving)
                            displayTown(myPlayerOrder);
                        //host needs to handle the case where active player leaves the game
                        if(myPlayerOrder == 0 && playerOrderLeaving == activePlayer){
                            activePlayer = activePlayer % players.length;
                            resetIndicators(NOT_REROLL);
                            ui.sendMessage(BEGIN_TURN + ":" + activePlayer, -1, -1);
                            if(activePlayer == 0)
                                beginTurnDiceRoll(NOT_REROLL);
                            else
                                notifyOfNewActivePlayer(false);
                        }
                    }
            }
        }
    }

    private void removePlayer(int playerOrder){
        while(playerOrder < players.length - 1){
            players[playerOrder] = players[playerOrder + 1];
        }
        if(myPlayerOrder > playerOrder)
            myPlayerOrder--;

        Player[] newArray = new Player[players.length - 1];
        System.arraycopy(players, 0, newArray, 0, players.length - 1);
        players = newArray;
        ui.leaveGame(playerOrder);
    }

    private void displayTown(int playerIndex){
        townIndexBeingShown = playerIndex;
        if(townIndexBeingShown == marketTownIndex)
            ui.displayTown("market", players[myPlayerOrder].getMoney(), market.toArray(new Establishment[market.size()]), new Landmark[0], false);
        else {
            Player p = players[playerIndex];
            ui.displayTown(p.getName(), p.getMoney(), p.getCity(), p.getLandmarks(), townIndexBeingShown == myPlayerOrder);
        }
    }

    @Override
    public void goToNextTown() {
        if(townIndexBeingShown < 0)
            townIndexBeingShown = myPlayerOrder;
        else if(townIndexBeingShown == players.length-1)
            townIndexBeingShown = 0;
        else
            townIndexBeingShown++;
        displayTown(townIndexBeingShown);
    }

    @Override
    public void goToPrevTown() {
        if(townIndexBeingShown < 0)
            townIndexBeingShown = myPlayerOrder;
        else if(townIndexBeingShown == 0)
            townIndexBeingShown = players.length-1;
        else
            townIndexBeingShown--;
        displayTown(townIndexBeingShown);
    }

    @Override
    public void middleButtonPressed() {
        if(townIndexBeingShown != myPlayerOrder)
            displayTown(myPlayerOrder);
        else if(!ui.showDialog()) {
            displayTown(marketTownIndex); //market is not a town, but it is displayed like one
        }
    }

    @Override
    public void selectCard(Card card, String ownerName) {
        selectedCard = null;
        if(numCardsToPick > 0 && selectCardCode == SELECT_CARD_MOVE){
            selectedCard = card;
            selectCardCode = SELECT_PLAYER_MOVE;
            ui.pickPlayer(players, myPlayerOrder, "Pick who to give it to");
        }
        else if(selectCardCode == SELECT_BC_MINE){
            tradeMeForSelected = card;
            selectCardCode = SELECT_BC_THEIRS;
            Player[] p = new Player[players.length-1];
            int index = 0;
            for(int i = 0; i < players.length; i++){
                if(i != activePlayer){
                    p[index] = players[i];
                    index++;
                }

            }
            ui.pickCard(p, players[myPlayerOrder].getName(), "Business Center: card to take", true);
        }
        else if(selectCardCode == SELECT_BC_THEIRS){
            selectedCard = card;
            for(int i = 0; i < players.length; i++){
                if(ownerName.equals(players[i].getName())){
                    ui.sendMessage(REPLY_TRADE_CARD+":0:"+myPlayerOrder+":"+tradeMeForSelected.getCode()+":"+i+":"+selectedCard.getCode(), -1, -1);
                    tradeCards(players[myPlayerOrder], (Establishment)tradeMeForSelected, players[i], (Establishment)selectedCard);
                }
            }
        }
        else if(selectCardCode == SELECT_RENO){
            ui.sendMessage(REPLY_RENO+":"+card.getCode(), -1, -1);
            finishReno((Establishment)card);
        }
        else if(selectCardCode == SELECT_DEMO){
            ui.sendMessage(REPLY_ACTIVATE_DEMO+":"+card.getCode(), -1, -1);
            numCardsToPick--;
            card.closeForRenovation();
            Player p = players[activePlayer];
            p.makeMoney(8);
            //I need to finish my renovation before displaying the town
            if(numCardsToPick == 0 && myPlayerOrder == 0) {
                //when demo is all done, finish any renovations and proceed
                int demoIndex = p.getCitySet().indexOf(new DemoCompany());
                p.getCitySet().valueAt(demoIndex).finishRenovation();
            }
            if(townIndexBeingShown == activePlayer)
                displayTown(activePlayer);
            if(numCardsToPick == 0 && myPlayerOrder == 0) {
                if(activateForConvention) {
                    beginEndOfTurn();
                }
                else
                    activateMajorEstablishments();
            }
            else if(numCardsToPick > 0)
                activateDemoCompany();
        }
        else if(selectCardCode == SELECT_CONVENTION){
            if(myPlayerOrder == 0)
                activateForConvention((Establishment)card);
            else
                ui.sendMessage(REPLY_CONVENTION+":"+card.getCode(), -1, -1);
        }
        else if(selectCardCode == SELECT_BUY_CARD){
            if(card == null){
                if(players[activePlayer].checkIfCardAvailable(new Airport())) {
                    players[activePlayer].makeMoney(10);
                    if(townIndexBeingShown == activePlayer)
                        displayTown(activePlayer);
                }
                ui.sendMessage(ADD_TO_CITY+":0", -1, -1);
            }
            else{
                final ArraySet<String> landmarkCodes = new ArraySet<>(Arrays.asList("H", "TR", "SM", "RT", "AP", "A"));
                if(landmarkCodes.contains(card.getCode()))
                    players[activePlayer].buyCard((ConstructibleLandmark) Deck.getCardFromCode(card.getCode()));
                else {
                    players[activePlayer].buyCard((Establishment) Deck.getCardFromCode(card.getCode()));
                    removeFromMarket((Establishment)Deck.getCardFromCode(card.getCode()));
                }
                ui.sendMessage(ADD_TO_CITY+":"+card.getCode(), -1, -1);
                if(townIndexBeingShown == myPlayerOrder)
                    displayTown(myPlayerOrder);
            }
            if(myPlayerOrder == 0) {
                checkTech();
            }
        }
    }

    @Override
    public void selectPlayer(String name){
        if(numCardsToPick > 0 && selectCardCode == SELECT_PLAYER_MOVE){
            selectPlayerForMovingCompany(name);
        }
        else if(selectCardCode == SELECT_PLAYER_TV){
            selectPlayerTv(name);
        }
    }

    private void selectPlayerTv(String name){
        for(int i = 0; i < players.length; i++){
            if(players[i].getName().equals(name)){
                int moneyGained = players[i].loseMoney(5);
                players[activePlayer].makeMoney(moneyGained);
                ui.sendMessage(REPLY_TV_STATION+":"+moneyGained+":"+activePlayer+":"+i, -1, -1);
                if(activePlayer == townIndexBeingShown || i == townIndexBeingShown) {
                    displayTown(townIndexBeingShown);
                }
            }
        }
        if(myPlayerOrder == 0)
            checkBusinessCenter();
    }

    private void selectPlayerForMovingCompany(String name){
        for(int i = 0; i < players.length; i++){
            Player p = players[i];
            if(p.getName().equals(name)){
                numCardsToPick--;
                ArraySet<Establishment> set = p.getCitySet();
                int  renovated = 0;
                if(set.contains((Establishment)selectedCard)){
                    int index = set.indexOf(selectedCard);
                    //loan office is the only card someone would want to keep renovated copies of
                    if(selectedCard.getNumAvailable() == 0  && selectedCard.getCode().equals("LO")
                            || selectedCard.getNumCopies() != selectedCard.getNumAvailable())
                        renovated = 1;
                    if(renovated == 0)
                        set.valueAt(index).addCopy();
                    else
                        set.valueAt(index).addRenovatedCopy();
                }
                else
                    set.add((Establishment)selectedCard);

                players[activePlayer].removeCopyOfEstablishment((Establishment)selectedCard, true);
                ui.sendMessage(REPLY_ACTIVATE_MOVING+":"+i+":"+selectedCard.getCode()+":"+renovated, -1, -1);
                selectedCard = null;

                p = players[activePlayer];
                p.makeMoney(4);
                moneyStuff(activePlayer,4, false);

                if(activePlayer == townIndexBeingShown || i == townIndexBeingShown) {
                    displayTown(townIndexBeingShown);
                }
                break;
            }
        }
        activateMovingCompany();
    }

    private final String IN_GAME = "in";
    private final String BEGIN_TURN = "begin";
    private final String ADD_TO_MARKET = "add";
    private final String RETURN_TO_MARKET = "return";
    //this should be appended with <their playerOrder>
    private final String ADD_TO_CITY = "city";
    private final String LOSE_MONEY = "lose";
    private final String GAIN_MONEY = "gain";
    private final String ROLL = "d1";
    private final String TUNA_REPLY = "tuna";
    private final String NEED_TUNA_ROLL = "needTuna";
    private final String REQUEST_ACTIVATE_MOVING = "requestMoving";
    private final String REPLY_ACTIVATE_MOVING = "replyMoving";
    private final String REQUEST_TV_STATION = "station";
    private final String REPLY_TV_STATION = "stationReply";
    private final String REQUEST_TRADE_CARD = "trade";
    private final String REPLY_TRADE_CARD = "tradeReply";
    private final String REQUEST_RENO = "reno";
    private final String REPLY_RENO = "renoReply";
    private final String REQUEST_ACTIVATE_DEMO = "demo";
    private final String REPLY_ACTIVATE_DEMO = "demoReply";
    private final String REQUEST_CONVENTION = "conv";
    private final String REPLY_CONVENTION = "convReply";
    private final String REQUEST_TECH_CHOICE = "tech";
    private final String REPLY_TECH_CHOICE = "techReply";
    private final String INFORM_OF_REROLL = "rr";
    private String leaveGameCode = null;

    //not quite dependency injection - didn't need the full module, just this little bit of info.
    @Override
    public void setLeaveGameCode(String code){
        leaveGameCode = code;
    }
}
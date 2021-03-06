package mike.dicetown;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import mike.cards.Card;
import mike.cards.Deck;
import mike.cards.Establishment;
import mike.cards.Landmark;
import mike.gamelogic.GameLogic;
import mike.gamelogic.HandlesLogic;
import mike.gamelogic.HasCards;
import mike.gamelogic.Player;
import mike.gamelogic.UI;
import mike.socketthreading.ReceivesMessages;
import mike.socketthreading.SocketService;

public class InGame extends AppCompatActivity implements UI, ReceivesMessages {
    private SocketService mBoundService;
    private HandlesLogic logic;
    //For things which don't need to persist through screen changes. Stored so I can dismiss it
    private PopupWindow popup;
    //used when users close the pick dialog before a choice is made in order to view towns
    private String lastMidButtonText;
    private boolean attachLogic;

    //requires v to have a tag set with the resource id of the card
    View.OnClickListener cardClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayCard((int)v.getTag());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
            I can't wrap my head around this. I tried making a landscape orientation,
            putting it in layout-land as I should, but it would never be used.
            The only way I could use a different layout was to set it manually
         */
        int orientation = getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_in_game);
        }
        else if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_in_game_landscape);
        }

        if(savedInstanceState != null) {
            attachLogic = false;
            Button b = findViewById(R.id.inGameMiddleButton);
            lastMidButtonText = savedInstanceState.getString("lastText");
            b.setText(savedInstanceState.getString("nowText"));
        }
        else
            attachLogic = true;

        initButtons();
        loadLogicFragment();
        doBindService(new Intent(InGame.this, SocketService.class));
        //shouldn't do stuff that requires the activity here, since its  not blocking
        //the logic should determine when the town should be displayed
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        if(lastMidButtonText != null) {
            outState.putString("lastText", lastMidButtonText);
            outState.putString("nowText", ((Button)findViewById(R.id.inGameMiddleButton)).getText().toString());
        }
//        doUnbindService();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestart(){
        super.onRestart();
        attachLogic = false;
        doBindService(null);
    }
    @Override
    public void onResume(){
        super.onResume();
        if(mBoundService != null)
            mBoundService.resumeMessages();
    }

    //work is done here since onStop is not guaranteed to be called
    @Override
    public void onPause(){
        //moved the popup check into the if-statement because it
        //should still display when the user goes back to the app
        if(isFinishing()){
            killPopup();
            stopService();
        }
        else if(mBoundService != null)
            mBoundService.pauseMessages();
        super.onPause();
    }

    /*
        App is not guaranteed to call this, but I'm sort of at a loss otherwise.
        I'd like to create a proper bound service, but when moving from lobby to
        the game, it kills that service when I want it to stay active the whole time...
     */
    @Override
    protected void onDestroy() {
        if(isFinishing()) {
            stopService();
        }
        PickDialogFrag frag = (PickDialogFrag) getSupportFragmentManager().findFragmentByTag(PickDialogFrag.tag);
        if(frag != null)
            frag.destroyingFragmentHost();
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        if(popup != null){
            killPopup();
        }
        else
            goToMainMenu();
    }

    @Override
    public void leaveGame(int playerOrder) {
        //I'm the one leaving - this should only happen if the host leaving forces me to leave
        if(playerOrder == logic.getPlayerOrder())
            goToMainMenu();
        else{
            mBoundService.removePlayer(playerOrder);
        }
    }

    private void killPopup(){
        if(popup != null) {
            popup.dismiss();
            popup.setContentView(null);
            popup = null;
        }
    }

    private void goToMainMenu(){
        Context context = this;
        Intent intent = new Intent(context, MainMenu.class);
        stopService(); //I know this is called in onDestroy, but that's not guaranteed to be called
        startActivity(intent);
        finish();
    }

    @Override
    public void endGame(String winnerName){
        String message;
        if(winnerName == null)
            message = getString(R.string.youWin);
        else
            message = getString(R.string.otherWin, winnerName);
        PickDialogFrag frag = PickDialogFrag.newInstance(getString(R.string.gameOver), message, PickDialogFrag.NO_PICK_GAME_WON, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }

    private void stopService(){
        if(mBoundService != null){
            int myOrder = logic.getPlayerOrder();
            mBoundService.sendData(SocketService.LEAVE_GAME+":"+myOrder, -1, -1);
            doUnbindService();
            stopService(new Intent(InGame.this, SocketService.class));
            mBoundService = null;
        }
    }

    //this needs to be public because Buttons made in xml use it
    public void middleButton(View view){
        logic.middleButtonPressed();
    }

    //this one is where the user must interact and all that.
    @Override
    public void getDiceRoll(boolean trainStationOwned, boolean forTunaBoat, int rerollDice){
        Dice.getDiceRoll(trainStationOwned, forTunaBoat, rerollDice, this);
    }

    @Override
    public void displayDiceRoll(int d1, int d2){
        Dice.displayDiceRoll(d1, d2, this);
    }

    //allows the Dice helper class to tell me when its finished with its work
    void finishRoll(int roll1, int roll2){
        logic.diceRolled(roll1, roll2);
    }

    void pausePickToViewTowns(){
        Button midButton = findViewById(R.id.inGameMiddleButton);
        String currentMidButtonText = midButton.getText().toString();

        if(!getString(R.string.backToOwnCity).equals(currentMidButtonText)) {
            lastMidButtonText = currentMidButtonText;
            midButton.setText(R.string.backToPick);
        }
    }

    private void loadLogicFragment(){
        //finds the fragments
        FragmentManager manager = getSupportFragmentManager();
        logic = (HandlesLogic) manager.findFragmentByTag(GameLogic.TAG_LOGIC_FRAGMENT);
        // create the fragment and data the first time
        if (logic == null) {
            // add the fragment
            logic = new GameLogic();
            manager.beginTransaction().add(logic, GameLogic.TAG_LOGIC_FRAGMENT).commitNow();
        }
    }

    private void initEstablishmentButtons(int landmarkImageWidth, int orientation){
        GridLayout grid = findViewById(R.id.establishmentGrid);
        int width, height;
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int scrollWidth = screenWidth - landmarkImageWidth;
            width = scrollWidth / grid.getColumnCount();
        }
        else
            width = screenWidth / grid.getColumnCount(); //landmarkbar is full width of screen
        height = (int)(1.4 * width);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        for(int i = 0; i < Deck.NUM_ESTABLISHMENTS; i++){
            FrameLayout frame = new FrameLayout(this);
            frame.setLayoutParams(params);
            //I don't want anything cluttering up the grid while not in use
            frame.setVisibility(View.GONE);
            //I don't need to create or store the IDs since I can just getChildAt(int index) to change the buttons
            grid.addView(frame);
        }
    }

    private void initLandmarkButtons(int size, int orientation){
        LinearLayout landmarkLayout = findViewById(R.id.landmarkBar);
        //this was the easiest way to keep a standard dimension for each (other is set in XML)
        // Without it, buttons with foregrounds would have different height than those with none
        ViewGroup.LayoutParams params = landmarkLayout.getLayoutParams();
        if(orientation == Configuration.ORIENTATION_LANDSCAPE)
            params.width = size;
        else
            params.height = size;

        ImageButton button;
        for(int i = 0; i < landmarkLayout.getChildCount(); i++){
            button = (ImageButton)landmarkLayout.getChildAt(i);
            button.setOnClickListener(cardClickListener);
        }
    }

    private void initButtons(){
        //initialize the landmark buttons
        int orientation = getResources().getConfiguration().orientation;
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int size = displayMetrics.widthPixels;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE)
            size = displayMetrics.heightPixels;
        LinearLayout landmarkLayout = findViewById(R.id.landmarkBar);
        size = size / landmarkLayout.getChildCount();

        initLandmarkButtons(size, orientation);

        //why wait for the landmarks to draw to do this when I can just say how wide I'm making them
        initEstablishmentButtons(size, orientation);
    }

    public void visitTown(View v){
        if(v.getId() == R.id.prevTown)
            logic.goToPrevTown();
        else
            logic.goToNextTown();
    }


    @Override
    public void pickMoveRenovated(String cardName, String message){
        String title = "Move renovated copy of "+cardName+'?';
        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_MOVE_RENOVATED, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }
    void receiveMoveRenovated(boolean choice){
        logic.receiveMoveRenovatedChoice(choice);
    }

    @Override
    public void pickPlayer(HasCards[] players, int myIndex, String title){
        DialogInfo.getInstance().setPlayers(players);
        DialogInfo.getInstance().setMyName(players[myIndex].getName());

        String message = "(name : money)";
        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_PLAYER, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }
    public void selectPlayer(String playerName){
        logic.selectPlayer(playerName);
    }

    @Override
    public void pickCard(HasCards[] cardOwners, String myName, String titleMessage, boolean forceChoice) {
        DialogInfo.getInstance().setPlayers(cardOwners);
        DialogInfo.getInstance().setForceChoice(forceChoice);
        DialogInfo.getInstance().setMyName(myName);

        String title = "Select A Card for "+ titleMessage;
        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_PLAYERS_CARD, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }
    public void selectCard(Card pickedCard, String owner){
        logic.selectCard(pickedCard, owner);
    }

    @Override
    public void pickCard(Establishment[] market, final Player me) {
        DialogInfo.getInstance().setMyName("market");
        DialogInfo.getInstance().setCards(me.mergeLandmarksAndMarket(market));
        DialogInfo.getInstance().setPlayers(new Player[]{me});

        final String title = "Buy A Card    (money: "+me.getMoney()+')';

        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_MARKETS_CARD, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }

    @Override
    public void pickCard(Landmark[]myLandmarks, String myName){
        DialogInfo.getInstance().setCards(myLandmarks);
        DialogInfo.getInstance().setMyName(myName);
        String title = "Choose landmark to demolish";

        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_LANDMARK, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }


    @Override
    public void getTechChoice(int currentInvestment) {
        String title = "Invest \u26051 into your Tech Startup?";
        String message = "You currently have \u2605"+ currentInvestment+ " invested";

        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_TECH, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }
    public void receiveTechChoice(boolean invest){
        logic.receiveTechChoice(invest);
    }

    @Override
    public void askIfAddTwo(final int d1, final int d2) {
        String title = "Add 2 to your roll?";
        String message = "original roll total: "+(d1+d2);
        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_ADD_TWO, d1, d2, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }
    public void receiveAddTwoChoice(boolean addTwo, int d1, int d2){
        logic.replyToAddTwo(addTwo, d1, d2);
    }

    @Override
    public void askIfReroll(final int d1, final int d2) {
        String title = "Would you like to reroll?";
        String message;
        if(d2 != 0)
            message = "original roll: "+d1+", "+d2;
        else
            message = "original roll: "+d1;

        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_REROLL, d1, d2, this);
        frag.showNow(getSupportFragmentManager(), PickDialogFrag.tag);
    }
    public void receiveRerollChoice(boolean reroll, int d1, int d2){
        logic.radioReply(reroll, d1, d2);
    }

    /*
     *I am indeed accepting some jank for a smaller app size.
     * I'm trying to mitigate this downside where possible
     * (i.e. displaying smaller images since they're sharper, etc)
     */
    @Override
    public void displayTown(String townName, int money, Establishment[] cityCards, Landmark[] landmarks, boolean myTown) {
        //Things look the same when I don't sort arrays, so I won't waste the time
        setTownText(townName, money, myTown);
        displayLandmarkIcons(landmarks);
        displayEstablishmentIcons(cityCards);
    }

    private void setTownText(String townName, int money, boolean myTown){
        ((TextView)findViewById(R.id.townName)).setText(townName);
        changeMoney(money);

        DialogInfo info = DialogInfo.getInstance();
        int textResource;
        if(!myTown)
            textResource = R.string.backToOwnCity;
        else if(!info.checkIfDialogActive() || info.getCode() == PickDialogFrag.SHOW_ROLL)
            textResource = R.string.toMarketplace;
        else
            textResource = R.string.backToPick;

        ((Button) findViewById(R.id.inGameMiddleButton)).setText(textResource);
    }

    //all players own one of each landmark, so I don't even check for how many there are
    private void displayLandmarkIcons(Landmark[] landmarks){
        LinearLayout landmarkLayout = findViewById(R.id.landmarkBar);
        for(int i = 0; i < landmarks.length; i++){
            ImageButton button = (ImageButton)landmarkLayout.getChildAt(i);
            if(landmarks[i].getNumAvailable() < 1)
                button.setImageResource(R.drawable.under_construction);
            else
                button.setImageResource(0);
            button.setTag(landmarks[i].getFullImageId());
        }
    }

    private void displayEstablishmentIcons(Establishment[] establishments){
        GridLayout grid = findViewById(R.id.establishmentGrid);
        FrameLayout frame;
        //first clears all views set by previous city visited
        for(int i = 0; i < grid.getChildCount(); i++){
            frame = (FrameLayout)grid.getChildAt(i);
            frame.removeAllViews();
            frame.setVisibility(View.GONE);
        }
        Establishment e;

        for(int i = 0; i < establishments.length; i++) {
            e = establishments[i];
            frame = (FrameLayout) grid.getChildAt(i);
            ImageButton button = new ImageButton(this);
            //card image
            button.setBackgroundResource(e.getGridImageId());
            //set the tag to the card's larger image id
            button.setTag(e.getFullImageId()); //I set a tag for the gridID with its index as a key
            button.setOnClickListener(cardClickListener);
            frame.addView(button);
            e.setNumRenovatedImage(frame, false);
            e.setnumOwnedImage(frame, false);
            frame.setVisibility(View.VISIBLE);
        }
    }



    //shows an enlarged version of the card as a popup - so it will not be retained through lifecycle events
    private void displayCard(int imageID){
        ImageButton button;
        View rootLayout = findViewById(R.id.activeScreen);
        if(popup == null) {
            popup = new PopupWindow(this);
            //I don't want to be spending all my memory on redraws, so I'll set a max card size
            int height = rootLayout.getHeight();
            int width = rootLayout.getWidth();
            popup.setHeight(height);
            popup.setWidth(width);
            popup.setBackgroundDrawable(getDrawable(R.drawable.background));

            int[] dims = Card.getLargeCardDimensions(width, height, getResources().getDisplayMetrics());

            int padWidth = (popup.getWidth() - dims[0]) / 2;
            int padHeight = (popup.getHeight() - dims[1]) / 2;

            /* I didn't want to define another XML layout just for the popup, nor was I able to stick
             * a layout in my popup to do what I wanted (large background, much smaller foreground).
             * In the end, I tried padding the image and it worked just the way I wanted! :D
             */
            button = new ImageButton(this);
            button.setPadding(padWidth, padHeight, padWidth, padHeight);
            button.setScaleType(ImageView.ScaleType.FIT_CENTER);
            button.setBackgroundColor(Color.TRANSPARENT);
            //Whole thing is a button, so clicking anywhere would call this
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    killPopup();
                }
            });
            popup.setContentView(button);
        }
        else
            button = (ImageButton)popup.getContentView();

        button.setImageResource(imageID);

        popup.showAtLocation(rootLayout, Gravity.CENTER|Gravity.TOP, 0, 0);
    }

    @Override
    public void sendMessage(String data, int indexToSendTo, int indexToSkip) {
        mBoundService.sendData(data, indexToSendTo, indexToSkip);
    }

    @Override
    public void makeToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void changeMoney(int newMoney) {
        String s = " "+newMoney;
        ((TextView)findViewById(R.id.coinAmountText)).setText(s);
    }

    //shows a dialog is there is one
    @Override
    public boolean showDialog() {
        if(DialogInfo.getInstance().checkIfDialogActive()){
            if(!DialogInfo.getInstance().isShowing()) {
                reloadDialogFragment();
            }
            //else, it's already showing
            return true;
        }
        return false;
    }

    private void reloadDialogFragment(){
        //finds the fragments
        FragmentManager manager = getSupportFragmentManager();
        PickDialogFrag frag = (PickDialogFrag) manager.findFragmentByTag(PickDialogFrag.tag);
        // create the fragment and data the first time
        if (frag == null) {
            String title = DialogInfo.getInstance().getTitle();
            String message = DialogInfo.getInstance().getMessage();
            int code = DialogInfo.getInstance().getCode();
            // add the fragment
            frag = PickDialogFrag.newInstance(title, message, code, this);
            frag.showNow(manager, PickDialogFrag.tag);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SocketService.LocalBinder)service).getService();
            mBoundService.registerClient(InGame.this);

            if(attachLogic)
                StartLogic();
            else{ //activity recreated (e.g. screen rotation). Pausing and Resuming this activity won't cause this to happen again
                //I can't tell logic to display the current town, but doing this will give the same result
                logic.goToNextTown();
                logic.goToPrevTown();
            }
            mBoundService.resumeMessages();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

    //finds this player's player order and fills the array of Players where each index is their playerOrder
    //All of the setup that may require the bound SocketService should be done here (or after)
    private void StartLogic(){
        Intent intent = getIntent();
        int i = 0;
        int myPlayerOrder = 0;
        String myName = intent.getStringExtra("myName");
        ArrayList<Player> mPlayers = new ArrayList<>();
        //loop starts at player 0 (the host) and iterates for each player order
        while(intent.hasExtra("p"+i)){
            String name = intent.getStringExtra("p"+i);
            //there BETTER be a playerOrder for this player if they got this far...
            if(myName.equals(name))
                myPlayerOrder = i;
            mPlayers.add(new Player(name));
            i++;
        }

        Player[] players = mPlayers.toArray(new Player[mPlayers.size()]);

        logic.initLogic(players, myPlayerOrder);
        logic.setLeaveGameCode(SocketService.LEAVE_GAME);
    }

    //I chose to have this get an intent parameter so I may pass info to the service when creating it
    void doBindService(Intent intent) {
        Intent mIntent;
        if(intent == null)
            mIntent = new Intent(InGame.this, SocketService.class);
        else
            mIntent = intent;
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (mBoundService != null) {
            // Detach our existing connection.
            mBoundService.unregisterClient(InGame.this);
            unbindService(mConnection);
            mBoundService = null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if(msg.what == SocketService.MSG_INCOMING_DATA)
            handleIncomingData(msg.arg1, (String)msg.obj);
    }

    //I use the playerOrder who sent it in case I must reply to that specific user (or all users but that one)
    private void handleIncomingData(int playerOrderWhoSentThis, String dataString){
        logic.receiveMessage(playerOrderWhoSentThis, dataString);
    }
}

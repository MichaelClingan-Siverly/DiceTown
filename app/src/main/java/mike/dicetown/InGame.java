package mike.dicetown;

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import mike.cards.Card;
import mike.cards.ConstructibleLandmark;
import mike.cards.Deck;
import mike.cards.Establishment;
import mike.cards.Landmark;
import mike.cards.MajorEstablishment;
import mike.cards.TechStartup;
import mike.gamelogic.GameLogic;
import mike.gamelogic.HandlesLogic;
import mike.gamelogic.HasCards;
import mike.gamelogic.Player;
import mike.gamelogic.UI;
import mike.socketthreading.SocketService;

public class InGame extends AppCompatActivity implements UI {
    private SocketService mBoundService;
    private boolean mIsBound = false;
    private final Messenger mMessenger = new Messenger(new IncomingHandler(InGame.this));
    private HandlesLogic logic;
    int indexOfLastSelected = -1;
    AlertDialog pickDialog = null;
    String playerPick = null;
    private String owner = null;
    Card pickedCard = null;
    private boolean establishmentsMade;
    PopupWindow popup;

    String lastMidButtonText;

    //requires v to have a tag set with the resource id of the card
    private View.OnClickListener cardClickListener = new View.OnClickListener() {
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
            Button b = (Button)findViewById(R.id.inGameMiddleButton);
            lastMidButtonText = savedInstanceState.getString("lastText");
            b.setText(savedInstanceState.getString("nowText"));
        }

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
        super.onSaveInstanceState(outState);
    }

    //work is done here since onStop is not guaranteed to be called
    @Override
    public void onPause(){
        super.onPause();
        if(isFinishing()){
            stopService();
        }
    }

    /*
        App is not guaranteed to call this, but I'm sort of at a loss otherwise.
        I'd like to create a proper bound service, but when moving from lobby to
        the game, it kills that service when I want it to stay active the whole time...
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing()) {
            stopService();
        }
    }

    @Override
    public void onBackPressed(){
        Context context = this;
        Intent intent = new Intent(context, MainMenu.class);
        startActivity(intent);
        finish();
        //onPause will end up being called and stop the service
    }

    @Override
    public void leaveGame(int playerOrder) {
        //I'm the one leaving - this should only happen if the host leaving forces me to leave
        if(playerOrder == logic.getPlayerOrder())
            onBackPressed();
        else{
            mBoundService.removePlayer(playerOrder);
        }
    }

    private void stopService(){
        if(mIsBound){
            int myOrder = logic.getPlayerOrder();
            mBoundService.sendData(SocketService.LEAVE_GAME+":"+myOrder, -1, -1);
            doUnbindService();
            mIsBound = false; //in case this ends up being called again
        }
    }

    //this needs to be public because Buttons made in xml use it
    public void middleButton(View view){
        resetMidButtonText(true);
        logic.middleButtonPressed();
    }

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

    private void resetMidButtonText(boolean showDialog) {
        Button b = (Button) findViewById(R.id.inGameMiddleButton);
        if (b.getText().equals(getString(R.string.backToPick)) || b.getText().equals(getString(R.string.rollDice))) {
            b.setText(lastMidButtonText);
            lastMidButtonText = null;
        }
        if (showDialog) {
            showDialog();
        }
    }

    //I need both the service bound and the logic fragment attached to proceed.
    //Neither part needs to know whether the other is finished or not
    @Override
    public void finishAttachingLogic(){
        if(logic != null && mBoundService != null && establishmentsMade)
            getExtras();
    }

    //finds this player's player order and fills the array of Players where each index is their playerOrder
    private void getExtras(){
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

    private void loadLogicFragment(){
        //finds the fragments
        FragmentManager manager = getFragmentManager();
        logic = (HandlesLogic) manager.findFragmentByTag(GameLogic.TAG_LOGIC_FRAGMENT);
        // create the fragment and data the first time
        if (logic == null) {
            // add the fragment
            logic = new GameLogic();
            manager.beginTransaction().add(logic, GameLogic.TAG_LOGIC_FRAGMENT).commit();
        }
    }

    private void initButtons(){
        establishmentsMade = false;
        initLandmarkButtons();
    }

    private void initEstablishmentButtons(){
        GridLayout grid = (GridLayout)findViewById(R.id.establishmentGrid);
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        ScrollView sv = (ScrollView)findViewById(R.id.townScroll);
        int scrollWidth = sv.getWidth();
        int width, height;
        if(screenHeight > screenWidth)
            width = scrollWidth / grid.getColumnCount();
        else {
            int landmarkWidth = findViewById(R.id.landmarkBar).getWidth();
            width = (screenWidth - landmarkWidth) / grid.getColumnCount();
        }
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
        establishmentsMade = true;
        finishAttachingLogic();
    }

    private void initLandmarkButtons(){
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        LinearLayout landmarkLayout = (LinearLayout)findViewById(R.id.landmarkBar);
        int size = screenWidth / landmarkLayout.getChildCount();
        ImageButton button;
        for(int i = 0; i < landmarkLayout.getChildCount(); i++){
            button = (ImageButton)landmarkLayout.getChildAt(i);
            button.setAdjustViewBounds(true);
            button.setMaxHeight(size);
            button.setScaleType(ImageView.ScaleType.FIT_END);
            button.setOnClickListener(cardClickListener);
        }
        //I want to have the landmarks made so I can find the right size
        landmarkLayout.post(new Runnable() {
            @Override
            public void run() {
                initEstablishmentButtons();
            }
        });
    }

    public void visitTown(View v){
        if(v.getId() == R.id.prevTown)
            logic.goToPrevTown();
        else
            logic.goToNextTown();
    }



    @Override
    public void pickPlayer(HasCards[] players, int myIndex, String title){
        DialogInfo.getInstance().setPlayers(players);
        DialogInfo.getInstance().setMyName(players[myIndex].getName());

        String message = "(name : money)";
        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_PLAYER);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
    }
    public void selectPlayer(String playerName){
        logic.selectPlayer(playerName);
    }

    @Override
    public void pickCard(HasCards[] cardOwners, String myName, String titleMessage, boolean nonMajor) {
        DialogInfo.getInstance().setPlayers(cardOwners);
        DialogInfo.getInstance().setNonMajor(nonMajor);
        DialogInfo.getInstance().setMyName(myName);

        String title = "Select A Card for "+ titleMessage;
        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_PLAYERS_CARD);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
    }
    public void selectCard(Card pickedCard, String owner){
        logic.selectCard(pickedCard, owner);
    }

    @Override
    public void pickCard(Establishment[] market, final Player me) {
        DialogInfo.getInstance().setMyName("market");
        DialogInfo.getInstance().setCards(me.mergeLandmarksAndMarket(market));
        DialogInfo.getInstance().setPlayers(new Player[]{me});

        String title = "Buy A Card    (money: "+me.getMoney()+')';
        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_MARKETS_CARD);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
    }

    @Override
    public void pickCard(Landmark[]myLandmarks, String myName){
        DialogInfo.getInstance().setCards(myLandmarks);
        DialogInfo.getInstance().setMyName(myName);
        String title = "Choose landmark to demolish";

        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_LANDMARK);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
    }


    @Override
    public void getTechChoice(int currentInvestment) {
        String title = "Invest $1 into your Tech Startup?";
        String message = "You currently have $"+ currentInvestment+ " invested";

        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_TECH);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
    }
    public void receiveTechChoice(boolean invest){
        logic.receiveTechChoice(invest);
    }

    @Override
    public void askIfAddTwo(final int d1, final int d2) {
        String title = "Add 2 to your roll?";

        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, PickDialogFrag.PICK_REROLL, d1, d2);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
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

        PickDialogFrag frag = PickDialogFrag.newInstance(title, message, PickDialogFrag.PICK_REROLL, d1, d2);
        frag.show(getFragmentManager(), PickDialogFrag.tag);
    }
    public void receiveRerollChoice(boolean reroll, int d1, int d2){
        logic.radioReply(reroll, d1, d2);
    }

    @Override
    public void displayTown(String townName, int money, Establishment[] cityCards, Landmark[] landmarks, boolean myTown) {
        Arrays.sort(cityCards);
        Arrays.sort(landmarks);

        ((TextView)findViewById(R.id.townName)).setText(townName);
        changeMoney(money);
        if(!DialogInfo.getInstance().checkIfDialogActive()) {
            if (myTown)
                ((Button) findViewById(R.id.inGameMiddleButton)).setText(R.string.toMarketplace);
            else
                ((Button) findViewById(R.id.inGameMiddleButton)).setText(R.string.backToOwnCity);
        }
        displayLandmarkIcons(landmarks);
        displayEstablishmentIcons(cityCards);
    }

    //all players own one of each landmark, so I don't even check for how many there are
    private void displayLandmarkIcons(Landmark[] landmarks){
        LinearLayout landmarkLayout = (LinearLayout)findViewById(R.id.landmarkBar);
        for(int i = 0; i < landmarks.length; i++){
            ImageButton button = (ImageButton)landmarkLayout.getChildAt(i);
            button.setBackgroundResource(landmarks[i].getGridImageId());
            button.setImageResource(landmarks[i].getNumRenovatedResId());
            button.setTag(landmarks[i].getFullImageId());
        }
    }

    //TODO Use .SVG and note how I'm trading jank for a smaller filesize
    //TODO consider using proper sized images and releasing separate APKs to help solve both issues
    //TODO (strongly) consider setting the frames up in another thread and display when finished
    //Even with bitmaps, this can have some jank if it uses larger bitmaps and displays a lot of them
    private void displayEstablishmentIcons(Establishment[] establishments){
        GridLayout grid = (GridLayout)findViewById(R.id.establishmentGrid);
        FrameLayout frame;
        //first clears all views set by previous city visited
        for(int i = 0; i < grid.getChildCount(); i++){
            frame = (FrameLayout)grid.getChildAt(i);
            frame.removeAllViews();
            frame.setVisibility(View.GONE);
        }
        //I want cards to keep a nice poker card size ratio, and for a full row to take up screen width
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        Establishment e;

        for(int i = 0; i < establishments.length; i++){
            e = establishments[i];
            //I use a FrameLayout because I can't set a foreground without it
            //   (View adds setForeground at a higher API than I'd like)
            frame = (FrameLayout)grid.getChildAt(i);
            ImageButton button = new ImageButton(this);
            //card image
            button.setBackgroundResource(e.getGridImageId());
            //set the tag to the card's larger image id
            button.setTag(e.getFullImageId());
            //I set a tag for the gridID with its index as a key and
            //num renovated
            button.setImageResource(e.getNumRenovatedResId());
            button.setLayoutParams(params);
            button.setOnClickListener(cardClickListener);
            //num owned
            ImageView foreground = new ImageView(this);
            foreground.setImageResource(e.getNumOwnedResId());
            foreground.setLayoutParams(params);

            //almost forgot to add the views to the frame
            frame.addView(button);
            frame.addView(foreground);

            if(e.equals(new TechStartup())){
                int investment = ((TechStartup)e).getValue();
                TextView tv = new TextView(this);
                tv.setText(R.string.invest + investment);
                tv.setGravity(Gravity.BOTTOM | Gravity.END);
                tv.setTextColor(Color.BLACK);
                frame.addView(tv);
            }
            frame.setVisibility(View.VISIBLE);
        }
    }

    private void displayCard(int imageID){
        popup = new PopupWindow(this);
        View rootLayout = findViewById(R.id.activeScreen);
        //I check both height and width since I want to make sure the whole image fits in the screen
        int height = rootLayout.getHeight();
        int width = rootLayout.getWidth();
        if(height < 1.4 * width)
            width = (int)(.714 * height);
        else if(height > 1.4 * width)
            height = (int)(1.4 * width);
        popup.setHeight(height);
        popup.setWidth(width);
        popup.setBackgroundDrawable(getDrawable(R.drawable.background));
        ImageButton b = new ImageButton(this);

        b.setScaleType(ImageView.ScaleType.FIT_XY);
        b.setImageResource(imageID);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(popup != null)
                    popup.dismiss();
                popup = null;
                showArrowBar();
            }
        });
        popup.setContentView(b);
        hideArrowBar();
        popup.showAtLocation(rootLayout, Gravity.CENTER|Gravity.TOP, 0, 0);
    }

    private void hideArrowBar(){
        ViewGroup arrowBar = (RelativeLayout)findViewById(R.id.arrowBar);
        for(int i = 0; i < arrowBar.getChildCount(); i++){
            arrowBar.getChildAt(i).setVisibility(View.GONE);
        }
    }
    private void showArrowBar(){
        ViewGroup arrowBar = (RelativeLayout)findViewById(R.id.arrowBar);
        for(int i = 0; i < arrowBar.getChildCount(); i++){
            arrowBar.getChildAt(i).setVisibility(View.VISIBLE);
        }
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

    @Override
    public boolean showDialog() {
        if(pickDialog != null) {
            pickDialog.show();
            return true;
        }
        else if(DialogInfo.getInstance().checkIfDialogActive()){
            if(!DialogInfo.getInstance().isShowing())
                reloadDialogFragment();
            return true;
        }
        return false;
    }

    private void reloadDialogFragment(){
        //finds the fragments
        FragmentManager manager = getFragmentManager();
        PickDialogFrag frag = (PickDialogFrag) manager.findFragmentByTag(PickDialogFrag.tag);
        // create the fragment and data the first time
        if (frag == null) {
            String title = DialogInfo.getInstance().getTitle();
            String message = DialogInfo.getInstance().getMessage();
            int code = DialogInfo.getInstance().getCode();
            // add the fragment
            frag = PickDialogFrag.newInstance(title, message, code);
            frag.show(manager, PickDialogFrag.tag);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SocketService.LocalBinder)service).getService();
            mBoundService.registerClient(mMessenger);
            //I need both the service bound and the logic fragment attached to proceed. both will try
            finishAttachingLogic();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

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
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            mBoundService.unregisterClient(mMessenger);
            unbindService(mConnection);
            stopService(new Intent(InGame.this, SocketService.class));
            mIsBound = false;
        }
    }

    private static class IncomingHandler extends Handler {
        private final WeakReference<InGame> mActivity;
        IncomingHandler(InGame activity){
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            InGame activity = mActivity.get();
            if (activity != null) {
                switch(msg.what){
                    case SocketService.MSG_INCOMING_DATA:
                        activity.handleIncomingData(msg.arg1, (String)msg.obj);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

    //I use the playerOrder who sent it in case I must reply to that specific user (or all users but that one)
    private void handleIncomingData(int playerOrderWhoSentThis, String dataString){
        logic.receiveMessage(playerOrderWhoSentThis, dataString);
    }
}

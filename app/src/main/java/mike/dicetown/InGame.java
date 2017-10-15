package mike.dicetown;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import java.util.Random;

import mike.cards.Card;
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
    private int indexOfLastSelected = -1;
    private AlertDialog pickDialog = null;
    private String playerPick = null;
    private String owner = null;
    private Card pickedCard = null;
    private PopupWindow popup;
    private int roll1;
    private int roll2;
    private String lastMidButtonText;

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
        setContentView(R.layout.activity_in_game);

        doBindService(new Intent(InGame.this, SocketService.class));
        //shouldn't do stuff that requires the activity here, since its  not blocking
        //the logic should determine when the town should be displayed
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
        the game, it kills that service when I want to to stay active the whole time...
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
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

    private void resetMidButtonText(boolean showDialog){
        Button b = (Button)findViewById(R.id.inGameMiddleButton);
        if(b.getText().equals(getString(R.string.backToPick)) || b.getText().equals(getString(R.string.rollDice))){
            b.setText(lastMidButtonText);
            lastMidButtonText = null;
        }
        if(showDialog && pickDialog != null)
            pickDialog.show();
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
        Player me = players[myPlayerOrder];
        displayTown(me.getName(), me.getMoney(), me.getCity(), me.getLandmarks(), true);

        logic = new GameLogic(InGame.this, players, myPlayerOrder);
        logic.setLeaveGameCode(SocketService.LEAVE_GAME);
    }

    private void initButtons(){
        GridLayout grid = (GridLayout)findViewById(R.id.establishmentGrid);
        //I want cards to keep a nice poker card size ratio, and for a full row to take up screen width
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int width = screenWidth / grid.getColumnCount();
        int height = (int)(1.4 * width);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        for(int i = 0; i < Deck.NUM_ESTABLISHMENTS; i++){
            FrameLayout frame = new FrameLayout(this);
            frame.setLayoutParams(params);
            //I don't want anything cluttering up the grid while not in use
            frame.setVisibility(View.GONE);
            //I don't need to create or store the IDs since I can just getChildAt(int index) to change the buttons
            grid.addView(frame);
        }

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
    }

    public void visitTown(View v){
        if(v.getId() == R.id.prevTown)
            logic.goToPrevTown();
        else
            logic.goToNextTown();
    }

    @Override
    public void getDiceRoll(boolean trainStationOwned, boolean forTunaBoat, int rerollDice) {
        if(popup != null) {
            popup.dismiss();
            popup = null;
        }

        AlertDialog.OnClickListener diceListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                roll1 = 0;
                roll2 = 0;
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        pickDialog = null;
                        roll1 = Dice.roll(1);
                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        pickDialog = null;
                        roll1 = Dice.roll(1);
                        roll2 = Dice.roll(1);
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        lastMidButtonText = ((Button) findViewById(R.id.inGameMiddleButton)).getText().toString();
                        ((Button) findViewById(R.id.inGameMiddleButton)).setText(R.string.rollDice);
                        return;
                }
                displayDiceRoll(roll1, roll2);
            }
        };
        AlertDialog.Builder diceBuilder = new AlertDialog.Builder(InGame.this);
        diceBuilder.setCancelable(false);
        if (forTunaBoat)
            diceBuilder.setTitle("Roll dice for tuna boat");
        else if(rerollDice == 1 || rerollDice == 2)
            diceBuilder.setTitle("Reroll Dice");
        else
            diceBuilder.setTitle("Roll dice to start turn");
        if (((trainStationOwned || forTunaBoat) && rerollDice != 2 && rerollDice != 1) || rerollDice == 2)
            diceBuilder.setPositiveButton(R.string.twoDice, diceListener);

        diceBuilder.setNeutralButton(R.string.cancelDice, diceListener);
        if(rerollDice != 2 && !forTunaBoat)
            diceBuilder.setNegativeButton(R.string.oneDice, diceListener);
        pickDialog = diceBuilder.show();
    }

    @Override
    public void displayDiceRoll(int d1, int d2){
        RelativeLayout diceLayout;
        View rootLayout = findViewById(R.id.inGameLayout);
        AnimationDrawable rollOneAnimation = new AnimationDrawable();
        rollOneAnimation.setOneShot(true);

        AnimationDrawable rollTwoAnimation = new AnimationDrawable();
        rollTwoAnimation.setOneShot(true);

        int frames = new Random().nextInt(3)+12;
        int lastD1Side = 0;
        int lastD2Side = 0;
        for(int i = 0; i < frames; i++){
            lastD1Side = addToDiceAnimation(rollOneAnimation, 0, lastD1Side);
            if(d2 > 0) {
                lastD2Side = addToDiceAnimation(rollTwoAnimation, 0, lastD2Side);
            }
        }
        //I'm doing a bunch of work to make it look all nice and fancy below this comment.
        popup = new PopupWindow(this);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //finds width of the screen
        int size = rootLayout.getWidth();
        size = size/3;
        RelativeLayout.LayoutParams d1Params = new RelativeLayout.LayoutParams(size, size);
        diceLayout = new RelativeLayout(this);
        diceLayout.setFocusable(true);


        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        Button button = new Button(this);
        button.setText(R.string.continuePrompt);
        button.setLayoutParams(buttonParams);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(popup != null)
                    popup.dismiss();
                popup = null;
                logic.diceRolled(roll1, roll2);
            }
        });
        diceLayout.addView(button);

        //make sure that the animation ends on the correct dice value(s) and add them to the layout
        addToDiceAnimation(rollOneAnimation, d1, -1);
        ImageView v1 = new ImageView(this);
        v1.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v1.setImageDrawable(rollOneAnimation);
        d1Params.setMarginStart(size/4);
        d1Params.setMarginEnd(size/4);
        v1.setLayoutParams(d1Params);
        diceLayout.addView(v1);
        if(d2 > 0) {
            RelativeLayout.LayoutParams invisibleParams = new RelativeLayout.LayoutParams(0, 0);
            invisibleParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            TextView tv = new TextView(this);
            tv.setId(View.generateViewId());
            tv.setLayoutParams(invisibleParams);
            diceLayout.addView(tv);

            RelativeLayout.LayoutParams d2Params = new RelativeLayout.LayoutParams(d1Params);
            addToDiceAnimation(rollTwoAnimation, d2, -1);
            ImageView v2 = new ImageView(this);
            v2.setScaleType(ImageView.ScaleType.FIT_CENTER);
            v2.setImageDrawable(rollTwoAnimation);
            d2Params.setMarginEnd(size/4);
            d2Params.addRule(RelativeLayout.RIGHT_OF, v1.getId());
            v2.setLayoutParams(d2Params);
            diceLayout.addView(v2);

            d1Params.addRule(RelativeLayout.LEFT_OF, tv.getId());
            d1Params.addRule(RelativeLayout.CENTER_VERTICAL);
            d2Params.addRule(RelativeLayout.RIGHT_OF, tv.getId());
            d2Params.addRule(RelativeLayout.CENTER_VERTICAL);
        }
        else{
            d1Params.addRule(RelativeLayout.CENTER_IN_PARENT);
        }

        popup.setWidth(rootLayout.getWidth());
        popup.setHeight(rootLayout.getHeight());
        popup.setContentView(diceLayout);
        popup.setBackgroundDrawable(getDrawable(R.drawable.background));

        popup.showAtLocation(rootLayout, Gravity.CENTER, 0, 0);
        rollOneAnimation.start();
        if(d2 > 0)
            rollTwoAnimation.start();
        //TODO add dice roll sounds
        //since its a popup, the user can easily close it when it finishes (I think even before that if they want)
    }

    private int addToDiceAnimation(AnimationDrawable drawable, int roll, int prevRoll){
        final int duration = 83;
        int value = roll;
        int id = 0;
        do{
            if (roll < 1 || roll > 6) {
                value = Dice.roll(1);
            }
        }while(value == prevRoll);

        switch (value){
            case 1:
                id = R.drawable.d1;
                break;
            case 2:
                id = R.drawable.d2;
                break;
            case 3:
                id = R.drawable.d3;
                break;
            case 4:
                id = R.drawable.d4;
                break;
            case 5:
                id = R.drawable.d5;
                break;
            case 6:
                id = R.drawable.d6;
                break;
        }
        Drawable d = getDrawable(id);
        if(d != null)
            drawable.addFrame(d, duration);
        return value;
    }

    @Override
    public void pickPlayer(Player[] players, int myIndex, String title){
        indexOfLastSelected = -1;
        ScrollView outerLayout = (ScrollView)getLayoutInflater().inflate(R.layout.pick_card, null);
        LinearLayout layout = (LinearLayout)outerLayout.getChildAt(0);
        layout.setGravity(Gravity.CENTER);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage("(name : money)");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(playerPick == null) {
                        makeToast("no player selected");
                    }
                    else {
                        dialog.dismiss();
                        pickDialog = null;
                        logic.selectPlayer(playerPick);
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    b.setText(R.string.backToPick);
                }
                playerPick = null;
            }
        };
        View.OnClickListener buttonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = (LinearLayout)pickDialog.findViewById(R.id.pickCardLayout);
                if(layout == null)
                    return;
                int index = layout.indexOfChild(v);
                //check if old index has a border and remove it if it does
                if(indexOfLastSelected >= 0)
                    layout.getChildAt(indexOfLastSelected).setBackgroundResource(android.R.drawable.btn_default);
                indexOfLastSelected = index;
                v.setBackgroundColor(Color.GRAY);
                playerPick = (String)v.getTag();
            }
        };
        for(int i = 0; i < players.length; i++){
            if(i != myIndex){
                Button b = new Button(this);
                String name = players[i].getName();
                b.setText(name + " : " + players[i].getMoney());
                b.setTextSize(20);
                b.setTag(name);
                b.setOnClickListener(buttonListener);
                layout.addView(b);
            }
        }
        builder.setNeutralButton(R.string.viewCities, listener);
        builder.setPositiveButton("ok", listener);
        builder.setView(outerLayout);
        pickDialog = builder.show();
    }

    @Override
    public void pickCard(HasCards cardOwners[], String myName, String message, boolean nonMajor) {
        ScrollView outerLayout = (ScrollView)getLayoutInflater().inflate(R.layout.pick_card, null);
        LinearLayout layout = (LinearLayout)outerLayout.getChildAt(0);
        layout.setGravity(Gravity.CENTER);
        indexOfLastSelected = -1;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener lockInSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(pickedCard == null)
                        pickDialog.show();
                    else {
                        pickDialog = null;
                        logic.selectCard(pickedCard, owner);
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    b.setText(R.string.backToPick);
                }
                pickedCard = null;
                owner = null;
            }
        };
        builder.setPositiveButton("select", lockInSelection);
        builder.setNeutralButton("view cities", lockInSelection);
        String title = "Select A Card for "+ message;
        builder.setTitle(title);
        builder.setCancelable(false);

        for(HasCards owner : cardOwners){
            String playerName = owner.getName();
            TextView tv = new TextView(this);
            tv.setText(playerName);
            tv.setTextSize(20);
            tv.setGravity(Gravity.CENTER);
            layout.addView(tv);
            for(Establishment card : owner.getCity()){
                if(nonMajor ^ card instanceof MajorEstablishment) {
                    FrameLayout frame = new FrameLayout(this);
                    ImageButton button = new ImageButton(this);
                    addCardToFrame(frame, card, button, playerName);
                    try {
                        Establishment e = card.getClass().getConstructor().newInstance();
                        //if I have to select my own card, its probably to give it to someone. Give them cards under renovation first
                        if (card.getNumAvailable() == 0)
                            e.closeForRenovation();
                            //the exception to this are loan offices. Give away constructed ones first
                        else if (card.getNumCopies() - card.getNumAvailable() > 0 && myName.equals(playerName) && !card.getCode().equals("LO"))
                            e.closeForRenovation();
                    } catch (Exception e1) {
                        Log.d("pickCard", "exception caught");
                        e1.printStackTrace();
                    }
                    layout.addView(frame);
                }
            }
        }
        builder.setView(outerLayout);
        pickDialog = builder.show();
    }

    @Override
    public void pickCard(Establishment[] market, Landmark[]myLandmarks, final int money, final ArraySet<Establishment> myCity) {
        DialogInterface.OnClickListener lockInSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(pickedCard == null){
                        makeToast("No card selected");
                        resetMidButtonText(true);
                    }
                    else if(pickedCard.getCost() > money){
                        makeToast("too expensive");
                        resetMidButtonText(true);
                    }
                    else if(pickedCard instanceof MajorEstablishment && myCity.contains((Establishment)pickedCard)){
                        pickDialog.show();
                        makeToast("A city may only contain one of each major establishment");
                        resetMidButtonText(true);
                    }
                    else {
                        resetMidButtonText(false);
                        pickDialog = null;
                        logic.selectCard(pickedCard, owner);
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    lastMidButtonText = b.getText().toString();
                    b.setText(R.string.backToPick);
                }
                else if(which == AlertDialog.BUTTON_NEGATIVE){
                    resetMidButtonText(false);
                    pickDialog = null;
                    logic.selectCard(null, null);
                }
                pickedCard = null;
                owner = null;
            }
        };
        Card[] cards = mergeCardArrays(market, myLandmarks, false);
        String title = "Buy A Card    (money: "+money+')';
        String pos = "ok";
        String neg = "don't buy";
        String neut = "view cities";
        pickCardHelper(cards, lockInSelection, "market", title, pos, neg, neut);
    }

    @Override
    public void pickCard(Landmark[]myLandmarks, String myName){
        DialogInterface.OnClickListener lockInSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(pickedCard == null) {
                        makeToast("No card selected");
                        resetMidButtonText(true);
                    }
                    else {
                        resetMidButtonText(false);
                        pickDialog = null;
                        logic.selectCard(pickedCard, owner);
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    lastMidButtonText = b.getText().toString();
                    b.setText(R.string.backToPick);
                }
                pickedCard = null;
                owner = null;
            }
        };
        Card[] cards = mergeCardArrays(null, myLandmarks, true);
        String title = "Choose landmark to demolish";
        String pos = "demo";
        String neut = "view cities";
        pickCardHelper(cards, lockInSelection, myName, title, pos, null, neut);
    }

    private void pickCardHelper(Card[] cards, DialogInterface.OnClickListener listener,
                                String myName, String title, String positiveName,
                                String negativeName, String neutralName){

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView outerLayout = (ScrollView)getLayoutInflater().inflate(R.layout.pick_card, null);
        LinearLayout layout = (LinearLayout)outerLayout.getChildAt(0);
        layout.setGravity(Gravity.CENTER);
        indexOfLastSelected = -1;

        for(Card c : cards){
            FrameLayout frame = new FrameLayout(this);
            ImageButton button = new ImageButton(this);
            addCardToFrame(frame, c, button, myName);
            layout.addView(frame);
        }

        builder.setCancelable(false);
        builder.setView(outerLayout);
        if(title != null)
            builder.setTitle(title);
        if(positiveName != null)
            builder.setPositiveButton(positiveName, listener);
        if(negativeName != null)
            builder.setNegativeButton(negativeName, listener);
        if(neutralName != null)
            builder.setNeutralButton(neutralName, listener);
        pickDialog = builder.show();
    }

    private void addCardToFrame(FrameLayout frame, Card card, ImageButton button, String playerName){
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = (LinearLayout)pickDialog.findViewById(R.id.pickCardLayout);
                if(layout == null)
                    return;
                int index = layout.indexOfChild((FrameLayout)v.getTag(R.id.frame));
                ImageView border = new ImageView(getApplicationContext());
                border.setScaleType(ImageView.ScaleType.FIT_CENTER);
                border.setImageResource(R.drawable.selected_border);
                //check if old index has a border and remove it if it does
                if(indexOfLastSelected >= 0 &&
                        ((FrameLayout)layout.getChildAt(indexOfLastSelected)).getChildCount() > 2)
                    ((FrameLayout)layout.getChildAt(indexOfLastSelected)).removeViewAt(2);
                //add border to newly selected index
                ((FrameLayout)layout.getChildAt(index)).addView(border);
                owner = (String)v.getTag(R.id.cardOwner);
                pickedCard = (Card)v.getTag(R.id.card);
                indexOfLastSelected = index;
            }
        };
        int width = (int)(Resources.getSystem().getDisplayMetrics().widthPixels * .8);
        int height = (int)(width * 1.4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        frame.setLayoutParams(params);

        button.setLayoutParams(params);
        button.setBackgroundResource(card.getFullImageId());
        button.setImageResource(card.getNumRenovatedResId());
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setOnClickListener(listener);
        frame.addView(button);
        ImageView foreground = new ImageView(this);
        foreground.setImageResource(card.getNumOwnedResId());
        frame.addView(foreground);
        button.setTag(R.id.cardOwner, playerName);
        button.setTag(R.id.card, card);
        button.setTag(R.id.frame, frame);
    }

    @Override
    public void getTechChoice(int currentInvestment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invest 1$ into your Tech Startup?");
        builder.setMessage("You currently have $"+ currentInvestment+ " invested");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == DialogInterface.BUTTON_POSITIVE){
                    dialog.dismiss();
                    logic.receiveTechChoice(true);
                }
                else{
                    dialog.dismiss();
                    logic.receiveTechChoice(false);
                }
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
        builder.show();
    }

    @Override
    public void askIfAddTwo(final int d1, final int d2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add 2 to your roll?");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                logic.replyToAddTwo(which == DialogInterface.BUTTON_POSITIVE, d1, d2);
            }
        };
        builder.setCancelable(false);
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
        builder.show();
    }

    @Override
    public void askIfReroll(final int d1, final int d2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Would you like to reroll?");
        if(d2 != 0)
            builder.setMessage("original roll: "+d1+", "+d2);
        else
            builder.setMessage("original roll: "+d1);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                logic.radioReply(which == DialogInterface.BUTTON_POSITIVE, d1, d2);
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
        builder.setCancelable(false);
        builder.show();
    }

    private Card[] mergeCardArrays(Establishment[] market, Landmark[] landmarks, boolean useConstructedLandmarks){
        int landmarksSize = 0;
        for(int i = 1; i < landmarks.length; i++){
            Landmark card = landmarks[i];
            if(useConstructedLandmarks && card.getNumAvailable() == 1 || card.getNumAvailable() == 0 && !useConstructedLandmarks)
                landmarksSize++;
        }
        int marketLength = 0;
        if(market != null)
            marketLength = market.length;
        Card[] cards = new Card[marketLength + landmarksSize];
        int i = 0;

        if(market != null) {
            for (Card card : market) {
                cards[i] = card;
                i++;
            }
        }
        for(int j = 1; j < landmarks.length; j++){
            Landmark card = landmarks[j];
            if(useConstructedLandmarks && card.getNumAvailable() == 1 || card.getNumAvailable() == 0 && !useConstructedLandmarks) {
                cards[i] = (Card) card;
                i++;
            }
        }
        return cards;
    }

    @Override
    public void displayTown(String townName, int money, Establishment[] cityCards, Landmark[] landmarks, boolean myTown) {
        Arrays.sort(cityCards);
        Arrays.sort(landmarks);
        ((TextView)findViewById(R.id.townName)).setText(townName);
        changeMoney(money);
        if(pickDialog == null) {
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
    private void displayEstablishmentIcons(Establishment[] establishments){
        GridLayout grid = (GridLayout)findViewById(R.id.establishmentGrid);
        grid.setColumnCount(4);
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
            // (View adds setForeground at a higher API than I'd like)
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
                tv.setText("invesement: "+investment);
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

        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
        RelativeLayout arrowBar = (RelativeLayout)findViewById(R.id.arrowBar);
        for(int i = 0; i < arrowBar.getChildCount(); i++){
            arrowBar.getChildAt(i).setVisibility(View.GONE);
        }
    }
    private void showArrowBar(){
        RelativeLayout arrowBar = (RelativeLayout)findViewById(R.id.arrowBar);
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
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SocketService.LocalBinder)service).getService();
            mBoundService.registerClient(mMessenger);
            initButtons();
            getExtras();
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

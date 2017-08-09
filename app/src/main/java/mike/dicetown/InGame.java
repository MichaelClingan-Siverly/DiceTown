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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import mike.cards.Card;
import mike.cards.CardDisplayable;
import mike.cards.Deck;
import mike.cards.Establishment;
import mike.cards.Landmark;
import mike.cards.MajorEstablishment;
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
    private int indexOfLastSelectedCard = -1;
    private int money = -1;
    private AlertDialog pickDialog = null;
    private String playerPick = null;
    private String owner = null;
    private Card pickedCard = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        doBindService(new Intent(InGame.this, SocketService.class));
        getExtras();
        initButtons();
        //the logic should determine when the town should be displayed
    }

    //this needs to be public because Buttons made in xml use it
    public void middleButton(View view){
        Button b = (Button)view;
        if(b.getText().equals(getString(R.string.backToPick))){
            pickDialog.show();
        }
        logic.middleButtonPressed();
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
        logic = new GameLogic(InGame.this, players, myPlayerOrder);
    }

    private void initButtons(){
        GridLayout grid = (GridLayout)findViewById(R.id.establishmentGrid);
        //I want cards to keep a nice poker card size ratio, and for a full row to take up screen width
        int width = grid.getWidth() / grid.getColumnCount();
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
    }

    public void visitTown(View v){
        if(v.getId() == R.id.prevTown)
            logic.goToPrevTown();
        else
            logic.goToNextTown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void getDiceRoll(boolean trainStationOwned, boolean forTunaBoat) {
        AlertDialog.OnClickListener diceListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int roll1 = 0;
                int roll2 = 0;
                switch (which){
                    case DialogInterface.BUTTON_NEGATIVE:
                        ((Button)findViewById(R.id.inGameMiddleButton)).setText(R.string.rollDice);
                        return;
                    case DialogInterface.BUTTON_POSITIVE:
                        roll1 = Dice.roll(1);
                        roll2 = Dice.roll(1);
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        roll1 = Dice.roll(1);
                        break;
                }
                logic.diceRolled(roll1, roll2);
                displayDiceRoll(roll1, roll2);
            }
        };
        AlertDialog.Builder diceBuilder = new AlertDialog.Builder(InGame.this);
        if(trainStationOwned || forTunaBoat)
            diceBuilder.setPositiveButton(R.string.twoDice, diceListener);
        if(!forTunaBoat) {
            diceBuilder.setNeutralButton(R.string.oneDice, diceListener);
            diceBuilder.setNegativeButton(R.string.cancelDice, diceListener);
        }
        diceBuilder.show();
    }

    @Override
    public void displayDiceRoll(int d1, int d2){
        AnimationDrawable rollOneAnimation = new AnimationDrawable();
        rollOneAnimation.setOneShot(true);

        AnimationDrawable rollTwoAnimation = new AnimationDrawable();
        rollTwoAnimation.setOneShot(true);

        int frames = new Random().nextInt(8)+8;
        for(int i = 0; i < frames; i++){
            addToDiceAnimation(rollOneAnimation, 0);
            if(d2 > 0) {
                addToDiceAnimation(rollTwoAnimation, 0);
            }
        }
        //I'm doing a bunch of work to make it look all nice and fancy below this comment.
        PopupWindow popup = new PopupWindow(this);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        LinearLayout layout = new LinearLayout(this);
        //finds width of the screen
        int size = Resources.getSystem().getDisplayMetrics().widthPixels;
        size = size/3;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);

        //make sure that the animation ends on the correct dice value(s) and add them to the layout
        addToDiceAnimation(rollOneAnimation, d1);
        ImageView v1 = new ImageView(this);
        v1.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v1.setImageDrawable(rollOneAnimation);
        params.setMarginStart(size/4);
        params.setMarginEnd(size/2);
        v1.setLayoutParams(params);
        layout.addView(v1);
        if(d2 > 0) {
            addToDiceAnimation(rollTwoAnimation, d2);
            ImageView v2 = new ImageView(this);
            v2.setScaleType(ImageView.ScaleType.FIT_CENTER);
            v2.setImageDrawable(rollTwoAnimation);
            params.setMarginStart(size/2);
            params.setMarginEnd(size/4);
            v2.setLayoutParams(params);
            layout.addView(v2);
        }

        popup.setContentView(layout);
        popup.showAtLocation(getCurrentFocus(), Gravity.CENTER, 0, 0);
        rollOneAnimation.start();
        if(d2 > 0)
            rollTwoAnimation.start();
        //TODO add dice roll sounds
        //since its a popup, the user can easily close it when it finishes (I think even before that if they want)
    }

    private void addToDiceAnimation(AnimationDrawable drawable, int roll){
        final int duration = 83;
        int value = roll;
        int id = 0;
        if(value < 1 || value > 6)
            value = Dice.roll(1);
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
        if(d == null)
            return;
        drawable.addFrame(d, duration);
    }

    @Override
    public void pickPlayer(Player[] players, int myIndex){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick who to give it to");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(playerPick == null)
                        pickDialog.show();
                    else {
                        logic.selectPlayer(playerPick);
                        pickDialog = null;
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    b.setText(R.string.backToPick);
                }
                playerPick = null;
            }
        };
        builder.setNeutralButton(R.string.viewCities, listener);
        LinearLayout layout = (LinearLayout)findViewById(R.id.pickCardLayout);
        View.OnClickListener buttonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playerPick = ((Button)v).getText().toString();
            }
        };
        for(int i = 0; i < players.length; i++){
            if(i != myIndex){
                Button b = new Button(this);
                b.setText(players[i].getName() + " : " + players[i].getMoney());
                b.setTextSize(20);
                b.setOnClickListener(buttonListener);
                layout.addView(b);
            }
        }
        builder.setView(layout);
        pickDialog = builder.show();
    }

    @Override
    public void pickCard(HasCards cardOwners[], String myName, boolean nonMajor) {
        LinearLayout layout = (LinearLayout)findViewById(R.id.pickCardLayout);
        indexOfLastSelectedCard = -1;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener lockInSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(pickedCard == null)
                        pickDialog.show();
                    else {
                        logic.selectCard(pickedCard, owner);
                        pickDialog = null;
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
        builder.setTitle("Select A Card");

        for(HasCards owner : cardOwners){
            String playerName = owner.getName();
            TextView tv = new TextView(this);
            tv.setText(playerName);
            tv.setTextSize(20);
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
                        button.setTag(1, e);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    layout.addView(frame);
                }
            }
        }
        builder.setView(layout);
        pickDialog = builder.show();
    }
    private void addCardToFrame(FrameLayout frame, Card card, ImageButton button, String playerName){
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = (LinearLayout)findViewById(R.id.pickCardLayout);
                int index = layout.indexOfChild(v);
                ImageView border = new ImageView(getApplicationContext());
                border.setScaleType(ImageView.ScaleType.FIT_CENTER);
                border.setImageResource(R.drawable.selected_border);
                //check if old index has a border and remove it if it does
                if(indexOfLastSelectedCard >= 0 &&
                        ((FrameLayout)layout.getChildAt(indexOfLastSelectedCard)).getChildCount() > 2)
                    ((FrameLayout)layout.getChildAt(indexOfLastSelectedCard)).removeViewAt(2);
                //add border to newly selected index
                ((FrameLayout)layout.getChildAt(index)).addView(border);
                owner = (String)v.getTag(0);
                pickedCard = (Card)v.getTag(1);
                indexOfLastSelectedCard = index;
            }
        };
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = (int)(1.4 * width);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        frame.setLayoutParams(params);

        button.getLayoutParams().width = width;
        button.getLayoutParams().height = height;
        button.setBackgroundResource(card.getFullImageId());
        button.setImageResource(card.getNumRenovatedResId());
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setOnClickListener(listener);
        frame.addView(button);
        ImageView foreground = new ImageView(this);
        foreground.setImageResource(card.getNumOwnedResId());
        frame.addView(foreground);
        button.setTag(0, playerName);

    }

    @Override
    public void pickCard(Establishment[] market, Landmark[]myLandmarks, final int money, final ArraySet<Establishment> myCity) {
        this.money = money;
        LinearLayout layout = (LinearLayout)findViewById(R.id.pickCardLayout);
        indexOfLastSelectedCard = -1;
        Card[] cards = mergeCardArrays(market, myLandmarks);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener lockInSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(pickedCard == null || pickedCard.getCost() >= money || (pickedCard instanceof MajorEstablishment && myCity.contains((Establishment)pickedCard)))
                        pickDialog.show();
                    else {
                        logic.selectCard(pickedCard, owner);
                        pickDialog = null;
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    b.setText(R.string.backToPick);
                }
                else if(which == AlertDialog.BUTTON_NEGATIVE){
                    logic.selectCard(null, null);
                    pickDialog = null;
                }
                pickedCard = null;
                owner = null;
            }
        };
        builder.setPositiveButton("select", lockInSelection);
        builder.setNeutralButton("view cities", lockInSelection);
        builder.setNegativeButton("don't buy anything", lockInSelection);
        builder.setTitle("Buy A Card");

        for(Card c : cards){
            FrameLayout frame = new FrameLayout(this);
            ImageButton button = new ImageButton(this);
            addCardToFrame(frame, c, button, "market");
            button.setTag(1, c);
            layout.addView(frame);
        }
    }

    @Override
    public void pickCard(Landmark[]myLandmarks, String myName){
        LinearLayout layout = (LinearLayout)findViewById(R.id.pickCardLayout);
        indexOfLastSelectedCard = -1;
        Card[] cards = (Card[])myLandmarks;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener lockInSelection = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(which == AlertDialog.BUTTON_POSITIVE){
                    if(pickedCard == null)
                        pickDialog.show();
                    else {
                        logic.selectCard(pickedCard, owner);
                        pickDialog = null;
                    }
                }
                else if(which == AlertDialog.BUTTON_NEUTRAL){
                    Button b = (Button)findViewById(R.id.inGameMiddleButton);
                    b.setText(R.string.backToPick);
                }
                else if(which == AlertDialog.BUTTON_NEGATIVE){
                    logic.selectCard(null, null);
                    pickDialog = null;
                }
                pickedCard = null;
                owner = null;
            }
        };
        builder.setPositiveButton("select", lockInSelection);
        builder.setNeutralButton("view cities", lockInSelection);
        builder.setNegativeButton("don't buy anything", lockInSelection);
        builder.setTitle("Buy A Card");

        for(Card c : cards){
            FrameLayout frame = new FrameLayout(this);
            ImageButton button = new ImageButton(this);
            addCardToFrame(frame, c, button, "market");
            button.setTag(1, c);
            layout.addView(frame);
        }
    }

    @Override
    public void getTechChoice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invest 1$ into your Tech Startup?");
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
        builder.setTitle("Invest 1$ into your Tech Startup?");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == DialogInterface.BUTTON_POSITIVE){
                    dialog.dismiss();
                    logic.replyToAddTwo(true, d1, d2);
                }
                else{
                    dialog.dismiss();
                    logic.replyToAddTwo(false, d1, d2);
                }
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
        builder.show();
    }

    @Override
    public void askIfReroll(final int d1, final int d2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Would you like to reroll?");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == DialogInterface.BUTTON_POSITIVE){
                    dialog.dismiss();
                    logic.radioReply(true, d1, d2);
                }
                else{
                    dialog.dismiss();
                    logic.radioReply(false, d1, d2);
                }
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
        builder.show();
    }

    private Card[] mergeCardArrays(Establishment[] market, Landmark[] landmarks){
        Card[] cards = new Card[market.length + landmarks.length-1];
        int i = 0;
        for(Card card : market){
            cards[i] = card;
            i++;
        }
        for(int j = 1; j < landmarks.length; j++){
            cards[i] = (Card)landmarks[i];
        }
        return cards;
    }

    @Override
    public void displayTown(String townName, int money, Establishment[] cityCards, Landmark[] landmarks, boolean myTown) {
        Arrays.sort(cityCards);
        Arrays.sort(landmarks);
        ((TextView)findViewById(R.id.townName)).setText(townName);
        changeMoney(money);
        if(myTown)
            ((Button)findViewById(R.id.inGameMiddleButton)).setText(R.string.toMarketplace);
        else
            ((Button)findViewById(R.id.inGameMiddleButton)).setText(R.string.backToOwnCity);
        displayLandmarkIcons(landmarks);
        displayEstablishmentIcons(cityCards);
    }
    //all players own one of each landmark, so I don't even check for how many there are
    private void displayLandmarkIcons(Landmark[] landmarks){
        LinearLayout landmarkLayout = (LinearLayout)findViewById(R.id.landmarkBar);
        for(int i = 0; i < landmarks.length; i++){
            ImageButton button = (ImageButton)landmarkLayout.getChildAt(i);
            button.setBackgroundResource(landmarks[0].getGridImageId());
            button.setImageResource(landmarks[0].getNumRenovatedResId());
        }
    }
    private void displayEstablishmentIcons(Establishment[] establishments){
        GridLayout grid = (GridLayout)findViewById(R.id.establishmentGrid);
        grid.setColumnCount(4);
        //first clears all views set by previous city visited
        for(int i = 0; i < grid.getChildCount(); i++){
            ((FrameLayout)grid.getChildAt(i)).removeAllViews();
        }
        //I want cards to keep a nice poker card size ratio, and for a full row to take up screen width
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        for(int i = 0; i < establishments.length; i++){
            Establishment e = establishments[i];
            //I use a FrameLayout because I can't set a foreground without it
            // (View adds setForeground at a higher API than I'd like)
            FrameLayout frame = (FrameLayout)grid.getChildAt(i);
            ImageButton button = new ImageButton(this);
            //card image
            button.setBackgroundResource(e.getGridImageId());
            //set the tag to the card's larger image id
            button.setTag(e.getFullImageId());
            //I set a tag for the gridID with its index as a key and
            //num renovated
            button.setImageResource(e.getNumRenovatedResId());
            button.setLayoutParams(params);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //the buttons have tags set to their larger image ID
                    displayCard((int)v.getTag());
                }
            });
            //num owned
            ImageView foreground = new ImageView(this);
            foreground.setImageResource(e.getNumOwnedResId());
            foreground.setLayoutParams(params);

            //almost forgot to add the views to the frame
            frame.addView(button);
            frame.addView(foreground);
        }
    }

    @Override
    public void displayCard(CardDisplayable card) {
        displayCard(card.getFullImageId());
    }

    private void displayCard(int imageID){
        PopupWindow popup = new PopupWindow(getApplicationContext());
        //I check both height and width since I want to make sure the whole image fits in the screen
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        if(height < 1.4 * width)
            width = (int)(.714 * height);
        else if(height > 1.4 * width)
            height = (int)(1.4 * width);
        ImageView v = new ImageView(getApplicationContext());
        popup.setHeight(height);
        popup.setWidth(width);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setImageResource(imageID);
        popup.showAtLocation(getCurrentFocus(), Gravity.CENTER, 0, 0);
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
        ((TextView)findViewById(R.id.coinAmountText)).setText(String.valueOf(money));
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
            mBoundService.stopAcceptingConnections();
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

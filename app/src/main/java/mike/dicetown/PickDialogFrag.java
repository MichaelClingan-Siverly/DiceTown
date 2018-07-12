package mike.dicetown;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import mike.cards.Card;
import mike.cards.CardInterface;
import mike.cards.Establishment;
import mike.cards.MajorEstablishment;
import mike.gamelogic.HasCards;


/**
 * Created by mike on 1/11/2018.
 * Fragment doing a lot of the work for creating various AlertDialogs which would be seen by users
 * There is a small amount of jank (~30-55 frames while in debug mode on my emulators),
 * but I'm having trouble putting all the work into a worker thread.
 * I can get it to work, but only if I don't bother handling lifecycle stuff,
 * and I don't think that trade off is worth it.
 *
 * Also tried showing this from a worker thread (actually expecting to get errors about trying
 * to update UI from non-UI thread), and it worked.
 * But that didn't seem to help the skipping frames either.
 */

public class PickDialogFrag extends DialogFragment {
    public static final int PICK_PLAYER = 0;
    public static final int PICK_PLAYERS_CARD = 1;
    public static final int PICK_MARKETS_CARD = 2;
    public static final int PICK_LANDMARK = 3;
    public static final int PICK_TECH = 4;
    public static final int PICK_ADD_TWO = 5;
    public static final int PICK_REROLL = 6;
    public static final int PICK_ROLL_TWO = 7;
    public static final int PICK_ROLL_ONE = 8;
    public static final int PICK_ROLL_ANY = 9;
    public static final int NO_PICK_GAME_WON = 10;
    public static final String tag = "dialog";


    private boolean enablePosButton = true;
    private String playerName = null;
    private Card pickedCard = null;
    private int d1, d2;
    private int indexOfLastSelection = -1;

    private boolean vertical = true;
    private InGame game;

    int[] metrics;


    /**
     * prepares the dialog to be shown
     * @param title title of the dialog
     * @param message message of the dialog
     * @param code code indicating what is shown so it can set the appropriate buttons and listeners
     * @return a new DialogFragment with the
     */
    public static PickDialogFrag newInstance(String title, String message, int code) {
        return makeInstance(new Bundle(), title, message, code);
    }

    /**
     * Alternative way to prepare the dialog to be shown if one needs to include dice roll values
     * @param title title of the dialog
     * @param message message of the dialog
     * @param code code indicating what is shown so it can set the appropriate buttons and listeners
     * @param d1 value of first dice
     * @param d2 value of second dice
     * @return a new DialogFragment with the
     */
    public static PickDialogFrag newInstance(String title, String message, int code, int d1, int d2){
        Bundle args = new Bundle();
        args.putInt("d1", d1);
        args.putInt("d2", d2);
        return makeInstance(args, title, message, code);
    }

    private static PickDialogFrag makeInstance(Bundle args, String title, String message, int code){
        PickDialogFrag frag = new PickDialogFrag();
        args.putString("title", title);
        args.putString("message", message);
        args.putInt("code", code);
        frag.setArguments(args);

        setDialogInfo(title, message, code);
        return frag;
    }

    private static void setDialogInfo(String title, String message, int code){
        DialogInfo info = DialogInfo.getInstance();
        info.setCode(code);
        info.setMessage(message);
        info.setTitle(title);
    }

    private void findOrientation(){
        DisplayMetrics displayMetrics = game.getResources().getDisplayMetrics();

        //sizes in dp here
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        vertical = width <= height;

//        if(vertical)
//            width = (int)(width * .8);
//        else
//            height = (int)(height * .5);

        metrics = game.getLargeCardDimensions(width, height);
    }

    /*
     *This is called after onCreate, which is called after onAttach
     */
    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        findOrientation();
        setCancelable(false);
        AlertDialog.Builder builder = prepareBuilder();

        //set the DialogInfo to
        DialogInfo.getInstance().activateDialog();

        return builder.create();
    }

    private AlertDialog.Builder prepareBuilder(){
        Bundle args = getArguments();
        int code = -1;
        //I don't want to have the Activity set the buttons,
        //as it would cause problems if the Actiivty is destroyed and re-created.
        AlertDialog.Builder builder = new AlertDialog.Builder(game);
        if(args != null) {
            code = args.getInt("code");

            if (args.getString("title") != null)
                builder.setTitle(args.getString("title"));
            if (args.getString("message") != null)
                builder.setMessage(args.getString("message"));

            //Even if none exists, there won't be problems
            d1 = args.getInt("d1");
            d2 = args.getInt("d2");
        }
        else
            builder.setTitle("something went wrong");

        setDialogContent(builder, code);
        return builder;
    }

    private void setDialogContent(AlertDialog.Builder builder, int code){
        //Set appropriate buttons and layouts, if any
        switch(code){
            case PICK_PLAYER: //expects that caller has set players and myIndex in the DialogInfo
                //positive, neutral
                setPickPlayerOrCardListeners(builder);
                setPlayerLayout(builder);
                break;
            case PICK_PLAYERS_CARD: //expects that caller has set nonMajor, players, and myName in the DialogInfo
                //positive, neutral, negative
                setPickPlayerOrCardListeners(builder);
                if(!DialogInfo.getInstance().isForcingChoice())
                    setMarketNegButton(builder, "no");
                setPickPlayersCardLayout(builder);
                break;
            case PICK_MARKETS_CARD:  //expects caller to have set cards, myName
                //positive, neutral, negative
                setPickPlayerOrCardListeners(builder);
                setMarketNegButton(builder, "don't buy");
                setPickCardLayout(builder);
                break;
            case PICK_LANDMARK: //expects that caller has set cards (to be the market), myName(as "market"), and players (as the buying player)
                //positive, neutral
                setPickPlayerOrCardListeners(builder);
                setPickCardLayout(builder);
                break;
            case PICK_TECH:
                //positive, negative
                setTechChoiceListeners(builder);
                break;
            case PICK_ADD_TWO:
                //positive, negative
                setAddTwoListener(builder);
                break;
            case PICK_REROLL: //expects that caller has set d1 and d2 in the DialogInfo
                //positive, negative
                setRerollListener(builder);
                break;
            case PICK_ROLL_TWO: //tuna boat or re-rolling after two dice is rolled, forces exactly two dice
                //positive, neutral
                setPickDiceTwo(builder);
                setNeutralToViewTown(builder);
                break;
            case PICK_ROLL_ONE:
                //neutral, negative
                setPickDiceOne(builder);
                setNeutralToViewTown(builder);
                break;
            case PICK_ROLL_ANY:
                //positive, neutral, negative
                setPickDiceOne(builder);
                setPickDiceTwo(builder);
                setNeutralToViewTown(builder);
                break;
            case NO_PICK_GAME_WON:
                //positive, neutral
                setNeutralToViewTown(builder);
                setEndGameButton(builder);
                break;
            default: //only viewing towns
                //neutral
        }
    }

    /* Although a number of dialogs don't have an explicit button to dismiss and view towns,
     * I still want users to get the behavior they'd expect from pressing the back button.
     */
    @Override
    public void onResume(){
        super.onResume();
        final AlertDialog dialog = (AlertDialog)getDialog();

        if(!enablePosButton)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogI, int keyCode, KeyEvent event) {
                if ((keyCode ==  android.view.KeyEvent.KEYCODE_BACK))
                {
                    game.pausePickToViewTowns();
                    dismiss();
                    // To dismiss the fragment when the back-button is pressed.
                    return true;
                }
                // Otherwise, do nothing else
                else
                    return false;
            }
        });
    }


    private void setEndGameButton(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                game.onBackPressed();
                destroyFragment();
            }
        };
        builder.setPositiveButton(R.string.endGame, listener);
    }


    private void setMarketNegButton(AlertDialog.Builder builder, String buttonMessage){
        DialogInterface.OnClickListener dontChooseListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                game.selectCard(null, null);
                destroyFragment();
            }
        };
        builder.setNegativeButton(buttonMessage, dontChooseListener);
    }


    private void setAddTwoListener(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                game.receiveAddTwoChoice(which == DialogInterface.BUTTON_POSITIVE, d1, d2);
                destroyFragment();
            }
        };
        setPosAndNegButtons(builder, listener);
    }


    private void setRerollListener(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                game.receiveRerollChoice(which == DialogInterface.BUTTON_POSITIVE, d1, d2);
                destroyFragment();
            }
        };
        setPosAndNegButtons(builder, listener);
    }


    private void setTechChoiceListeners(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                game.receiveTechChoice(which == DialogInterface.BUTTON_POSITIVE);
                destroyFragment();
            }
        };
        setPosAndNegButtons(builder, listener);
    }

    private void setPosAndNegButtons(AlertDialog.Builder builder, DialogInterface.OnClickListener listener){
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
    }

    //the listeners were so similar, I merged them into one. It sets the positive button
    private void setPickPlayerOrCardListeners(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(pickedCard != null)
                    game.selectCard(pickedCard, playerName);
                else
                    game.selectPlayer(playerName);
                destroyFragment();
            }
        };
        builder.setPositiveButton("ok", listener);
        setNeutralToViewTown(builder);
        enablePosButton = false;
    }


    /* I'm ok suppressing this. I use onCreateDialog, and am using it to show an AlertDialog
     * AlertDialogs seem like the one case when a null root is appropriate.
     */
    @SuppressLint("InflateParams")
    private FrameLayout getOuterLayout(){
        //both types of ScrollView are subclasses of FrameLayout
        if(vertical)
            return (ScrollView)game.getLayoutInflater().inflate(R.layout.pick_card, null);
        else
            return (HorizontalScrollView)game.getLayoutInflater().inflate(R.layout.pick_card_landscape, null);
    }

    //main difference between pickCard and player layouts are that the pickCard doesn't have player names in it
    private void setPickCardLayout(AlertDialog.Builder builder){
        FrameLayout outerLayout = getOuterLayout();
        LinearLayout layout = (LinearLayout) outerLayout.getChildAt(0);

        CardInterface[] cards = DialogInfo.getInstance().getCards();
        String myName = DialogInfo.getInstance().getMyName();
        for(CardInterface c : cards){
            FrameLayout frame = new FrameLayout(game);
            ImageButton button = new ImageButton(game);
            addCardToFrame(frame, c, button, myName, makeCardFrameListener());
            layout.addView(frame);
        }
        builder.setView(outerLayout);
    }

    private void setPlayerLayout(AlertDialog.Builder builder){
        FrameLayout outerLayout = getOuterLayout();
        LinearLayout layout = (LinearLayout) outerLayout.getChildAt(0);
        View.OnClickListener buttonListener = makePlayerLayoutListener();

        //if players is null, I'll let it crash
        HasCards[] players = DialogInfo.getInstance().getPlayers();
        String myName = DialogInfo.getInstance().getMyName();

        for(HasCards player : players){
            if(!player.getName().equals(myName)){
                Button b = new Button(game);
                String name = player.getName();
                b.setTag(name);
                //Set tag here or it wont have the incorrect player name, since I manipulate it below
                name += " : " + player.getMoney();
                b.setText(name);
                b.setTextSize(20);
                b.setOnClickListener(buttonListener);
                layout.addView(b);
            }
        }
        builder.setView(outerLayout);
    }

    private View.OnClickListener makePlayerLayoutListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout buttonLayout = getDialog().findViewById(R.id.pickCardLayout);
                if(buttonLayout == null)
                    return;
                int index = buttonLayout.indexOfChild(v);

                //check if old index has a border and remove it if it does
                if(indexOfLastSelection >= 0)
                    buttonLayout.getChildAt(indexOfLastSelection).setBackgroundResource(android.R.drawable.btn_default);
                indexOfLastSelection = index;
                v.setBackgroundColor(Color.GRAY);
                playerName = (String)v.getTag();
                //I want to only enable this button after a choice has been made
                ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }
        };
    }


    private void setPickPlayersCardLayout(AlertDialog.Builder builder){
        FrameLayout outerLayout = getOuterLayout();
        LinearLayout layout = (LinearLayout) outerLayout.getChildAt(0);
        HasCards[] cardOwners = DialogInfo.getInstance().getPlayers();

        //first a player's name and cards are displayed before another player is displayed
        for(HasCards owner : cardOwners){
            String playerName = owner.getName();
            TextView nameView = new TextView(game);
            if(!vertical) {
                setTextViewMargins(nameView);
            }
            nameView.setText(playerName);
            nameView.setTextSize(20);
            nameView.setGravity(Gravity.CENTER);
            layout.addView(nameView);

            //this is displaying a player's cards
            addPlayersCards(layout, owner);
        }
        builder.setView(outerLayout);
    }

    private void setTextViewMargins(TextView view){
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
        if(params == null){
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
        }
        float density = game.getResources().getDisplayMetrics().density;
        params.setMarginEnd((int)(5*density));
        params.setMarginStart((int)(5*density));
    }

    private void addPlayersCards(LinearLayout layout, HasCards owner){
        for(Establishment card : owner.getCity()){
            if(!(card instanceof MajorEstablishment)){
//            if(DialogInfo.getInstance().isNonMajor() ^ card instanceof MajorEstablishment) {
                FrameLayout frame = new FrameLayout(game);
                ImageButton button = new ImageButton(game);
                addCardToFrame(frame, card, button, owner.getName(), makeCardFrameListener());
                String myName = DialogInfo.getInstance().getMyName();
                try {
                    Establishment e = card.getClass().getConstructor().newInstance();
                    //if I have to select my own card, its probably to give it to someone. Give them cards under renovation first
                    if (card.getNumAvailable() == 0)
                        e.closeForRenovation();
                        //the exception to this are loan offices. Give away constructed ones first
                    else if (card.getNumCopies() - card.getNumAvailable() > 0
                            && myName.equals(owner.getName()) && !card.getCode().equals("LO"))
                        e.closeForRenovation();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                layout.addView(frame);
            }
        }
    }

    private void addCardToFrame(FrameLayout frame, CardInterface card, ImageButton button, String owner, View.OnClickListener cardListener){
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(metrics[0], metrics[1]);
        frame.setLayoutParams(params);

        button.setLayoutParams(params);
        button.setImageResource(card.getFullImageId());
        button.setBackground(null);
//        button.setBackgroundResource(card.getFullImageId());
        button.setScaleType(ImageView.ScaleType.FIT_XY);
        button.setOnClickListener(cardListener);
        frame.addView(button);
        //num renovated
        card.setNumRenovatedImage(frame, true);
        //num owned
        card.setnumOwnedImage(frame, true);

        button.setTag(R.id.cardOwner, owner);
        button.setTag(R.id.card, card);
        button.setTag(R.id.frame, frame);
    }

    private View.OnClickListener makeCardFrameListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = getDialog().findViewById(R.id.pickCardLayout);
                if(layout == null)
                    return;
                int index = layout.indexOfChild((FrameLayout)v.getTag(R.id.frame));
                ImageView border = new ImageView(game);
                border.setScaleType(ImageView.ScaleType.FIT_XY);
                border.setImageResource(R.drawable.selected_border);
                //check if old index has a border and remove it if it does
                if(indexOfLastSelection >= 0){
                    int numChildren =((FrameLayout)layout.getChildAt(indexOfLastSelection)).getChildCount();
                    ((FrameLayout)layout.getChildAt(indexOfLastSelection)).removeViewAt(numChildren - 1);
                }

                //add border to newly selected index
                ((FrameLayout)layout.getChildAt(index)).addView(border);
                playerName = (String)v.getTag(R.id.cardOwner);
                pickedCard = (Card) v.getTag(R.id.card);
                indexOfLastSelection = index;

                checkToEnablePosButton();
            }
        };
    }

    private void checkToEnablePosButton(){
        if(DialogInfo.getInstance().getMyName().equals("market")){
            HasCards buyer = DialogInfo.getInstance().getPlayers()[0];
            if(pickedCard.getCost() > buyer.getMoney()) {
                game.makeToast("too expensive");
                ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                return;
            }
            else if(pickedCard instanceof  MajorEstablishment && buyer.getCitySet().contains(pickedCard)) {
                game.makeToast("A city may only contain one of each major establishment");
                ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                return;
            }
        }
        //I want to only enable this button after a choice has been made
        ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
    }


    private void setNeutralToViewTown(AlertDialog.Builder builder){
        DialogInterface.OnClickListener diceListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                game.pausePickToViewTowns();
            }
        };
        builder.setNeutralButton(R.string.viewTowns, diceListener);
    }

    private void setPickDiceOne(AlertDialog.Builder builder){
        builder.setNegativeButton(R.string.oneDice, makeDiceListener(false));
    }

    private void setPickDiceTwo(AlertDialog.Builder builder){
        builder.setPositiveButton(R.string.twoDice, makeDiceListener(true));
    }

    private DialogInterface.OnClickListener makeDiceListener(final boolean twoDice){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Dice.rollValues(twoDice, game);
                destroyFragment();
            }
        };
    }



    private void destroyFragment(){
        DialogInfo.getInstance().deactivateDialog();
        game.getSupportFragmentManager().beginTransaction().remove(PickDialogFrag.this).commitAllowingStateLoss();
    }


    /*
     * Below overrides make things a bit easier for me - I don't have to set them everywhere
     * note, all the deactivateDialogs are still in there. The dialog may not be showing, but still
     * needs to be ready to be shown again. And that boolean is how I know this
     */
    @Override
    public void show (FragmentManager manager, String tag){
        DialogInfo.getInstance().setShowing(true);
        super.show(manager, tag);
    }
    @Override
    public int show (FragmentTransaction transaction, String tag){
        DialogInfo.getInstance().setShowing(true);
        return super.show(transaction, tag);
    }
    @Override
    public void onDismiss (DialogInterface dialog){
        DialogInfo.getInstance().setShowing(false);
        super.onDismiss(dialog);
    }
    @Override
    public void onAttach(Context context){
        game = (InGame)context;
        super.onAttach(context);
    }


    //Removed the interface. This and my Activity are in same package, so there's really no need...
}


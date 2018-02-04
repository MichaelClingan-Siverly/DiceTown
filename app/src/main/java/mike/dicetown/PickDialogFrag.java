package mike.dicetown;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
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
    public static final String tag = "dialog";


    private boolean enablePosButton = true;
    private String playerName = null;
    private Card pickedCard = null;
    private int d1, d2;
    private int indexOfLastSelection = -1;

    private boolean vertical = true;

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
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        vertical = width <= height;

        if(vertical){
            width = (int)(width * .8);
            height = (int)(width * 1.4);
        }
        else{
            height = (int)(height * .5);
            width = (int)(height / 1.4);
        }
        metrics = new int[]{width, height};
    }


    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        findOrientation();
        int code = args.getInt("code");
        //I don't want to have the Activity set the buttons,
        //as it would cause problems if the Actiivty is destroyed and re-created.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if(args.getString("title") != null)
            builder.setTitle(args.getString("title"));
        if(args.getString("message") != null)
            builder.setMessage(args.getString("message"));

        //Even if none exists, there won't be problems
        d1 = args.getInt("d1");
        d2 = args.getInt("d2");


        //Set appropriate buttons and layouts, if any
        switch(code){
            case PICK_PLAYER: //expects that caller has set players and myIndex in the DialogInfo
                //positive, neutral
                setPickPlayerOrCardListeners(builder);
                setPlayerLayout(builder);
                break;
            case PICK_PLAYERS_CARD: //expects that caller has set nonMajor, players, and myName in the DialogInfo
                //positive, neutral
                setPickPlayerOrCardListeners(builder);
                setPickPlayersCardLayout(builder);
                break;
            case PICK_MARKETS_CARD:  //expects caller to have set cards, myName
                //positive, neutral, negative
                setPickPlayerOrCardListeners(builder);
                setMarketNegButton(builder);
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
            default: //only viewing towns
                //neutral
        }

        //set the DialogInfo to
        DialogInfo.getInstance().activateDialog();
        setCancelable(false);

        return builder.create();
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
                    setMidButtonText(R.string.backToPick);
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


    private void setMarketNegButton(AlertDialog.Builder builder){
        DialogInterface.OnClickListener dontBuyListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((InGame)getActivity()).selectCard(null, null);
                destroyFragment();
            }
        };
        builder.setNegativeButton("don't buy", dontBuyListener);
    }


    private void setAddTwoListener(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((InGame)getActivity()).receiveAddTwoChoice(which == DialogInterface.BUTTON_POSITIVE, d1, d2);
                destroyFragment();
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
    }


    private void setRerollListener(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((InGame)getActivity()).receiveRerollChoice(which == DialogInterface.BUTTON_POSITIVE, d1, d2);
                destroyFragment();
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
    }


    private void setTechChoiceListeners(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((InGame)getActivity()).receiveTechChoice(which == DialogInterface.BUTTON_POSITIVE);
                destroyFragment();
            }
        };
        builder.setPositiveButton("yes", listener);
        builder.setNegativeButton("no", listener);
    }

    //the listeners were so similar, I merged them into one. It sets the positive button
    private void setPickPlayerOrCardListeners(AlertDialog.Builder builder){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InGame game = (InGame)getActivity();
                if(pickedCard != null)
                    ((InGame)getActivity()).selectCard(pickedCard, playerName);
                else
                    game.selectPlayer(playerName);
                destroyFragment();
            }
        };

        builder.setPositiveButton("ok", listener);
        setNeutralToViewTown(builder);
        enablePosButton = false;
    }

    //both types of ScrollView are subclasses of FrameLayout
    private FrameLayout getOuterLayout(){
        if(vertical)
            return (ScrollView)getActivity().getLayoutInflater().inflate(R.layout.pick_card, null);
        else
            return (HorizontalScrollView)getActivity().getLayoutInflater().inflate(R.layout.pick_card_landscape, null);
    }

    //main difference between pickCard and player layouts are that the pickCard doesn't have player names in it
    private void setPickCardLayout(AlertDialog.Builder builder){
        FrameLayout outerLayout = getOuterLayout();
        LinearLayout layout = (LinearLayout) outerLayout.getChildAt(0);

        CardInterface[] cards = DialogInfo.getInstance().getCards();
        String myName = DialogInfo.getInstance().getMyName();
        for(CardInterface c : cards){
            FrameLayout frame = new FrameLayout(getActivity());
            ImageButton button = new ImageButton(getActivity());
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
                Button b = new Button(getActivity());
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
                LinearLayout buttonLayout = (LinearLayout)getDialog().findViewById(R.id.pickCardLayout);
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
            TextView nameView = new TextView(getActivity());
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
        float density = getActivity().getResources().getDisplayMetrics().density;
        params.setMarginEnd((int)(5*density));
        params.setMarginStart((int)(5*density));
    }

    private void addPlayersCards(LinearLayout layout, HasCards owner){
        for(Establishment card : owner.getCity()){
            if(DialogInfo.getInstance().isNonMajor() ^ card instanceof MajorEstablishment) {
                FrameLayout frame = new FrameLayout(getActivity());
                ImageButton button = new ImageButton(getActivity());
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
                    Log.d("pickCard", "exception caught");
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
        button.setBackgroundResource(card.getFullImageId());
        button.setImageResource(card.getNumRenovatedResId());
        button.setScaleType(ImageView.ScaleType.FIT_XY);
        button.setOnClickListener(cardListener);

        frame.addView(button);
        ImageView foreground = new ImageView(getActivity());
        foreground.setImageResource(card.getNumOwnedResId());
        frame.addView(foreground);
        button.setTag(R.id.cardOwner, owner);
        button.setTag(R.id.card, card);
        button.setTag(R.id.frame, frame);
    }

    private View.OnClickListener makeCardFrameListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = (LinearLayout)getDialog().findViewById(R.id.pickCardLayout);
                if(layout == null)
                    return;
                int index = layout.indexOfChild((FrameLayout)v.getTag(R.id.frame));
                ImageView border = new ImageView(getActivity());
                border.setScaleType(ImageView.ScaleType.FIT_XY);
                border.setImageResource(R.drawable.selected_border);
                //check if old index has a border and remove it if it does
                if(indexOfLastSelection >= 0 &&
                        ((FrameLayout)layout.getChildAt(indexOfLastSelection)).getChildCount() > 2)
                    ((FrameLayout)layout.getChildAt(indexOfLastSelection)).removeViewAt(2);
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
            if(pickedCard.getCost() > buyer.getMoney())
                ((InGame)getActivity()).makeToast("too expensive");
            else if(pickedCard instanceof  MajorEstablishment && buyer.getCitySet().contains(pickedCard))
                ((InGame)getActivity()).makeToast("A city may only contain one of each major establishment");
            else
                ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        }
        else
        //I want to only enable this button after a choice has been made
        ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
    }


    private void setNeutralToViewTown(AlertDialog.Builder builder){
        DialogInterface.OnClickListener diceListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setMidButtonText(R.string.backToPick);
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
                Dice.rollValues(twoDice, (InGame)getActivity());
                destroyFragment();
            }
        };
    }


    private void setMidButtonText(int textID){
        InGame game = (InGame)getActivity();
        game.lastMidButtonText = ((Button) game.findViewById(R.id.inGameMiddleButton)).getText().toString();
        ((Button) game.findViewById(R.id.inGameMiddleButton)).setText(textID);
    }


    private void destroyFragment(){
        DialogInfo.getInstance().deactivateDialog();
        getFragmentManager().beginTransaction().remove(PickDialogFrag.this).commitAllowingStateLoss();
    }

    public interface AcceptsDialogClicks{
        //TODO declare what functions InGame should implement, which would be called from the listeners in here
        //allows InGame to leave their values as private instead of package-protected

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
}

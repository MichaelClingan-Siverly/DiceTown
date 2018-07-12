package mike.dicetown;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * Created by mike on 7/27/2017.
 * Utility class for the rolling and display of dice.
 */

class Dice {
    static private int roll1;
    static private int roll2;

    private static int roll() {
        Random rand = new Random();
        /*
         *   Originally was going to use nextInt only once, but with more than one dice
         *   its even distribution is a bad simulation for probability of actual dice rolls
         *   So I make a separate roll for each dice (because a single fair dice IS evenly distributed)
         */
        return rand.nextInt(6) + 1;
    }

    static void rollValues(boolean twoDice, InGame game){
        roll1 = roll();
        if(twoDice)
            roll2 = roll();
        else
            roll2 = 0;

        displayDiceRoll(roll1, roll2, game);
    }

    static void getDiceRoll(boolean trainStationOwned, boolean forTunaBoat, int rerollDice, final InGame game){
        if(game.getPopup() != null) {
            game.getPopup().dismiss();
            game.setPopup(null);
        }

        String title;
        boolean oneDice = false;
        boolean twodice = false;
        AlertDialog.Builder diceBuilder = new AlertDialog.Builder(game);
        diceBuilder.setCancelable(false);
        if(forTunaBoat)
            title = "Roll dice for tuna boat";
        else if(rerollDice == 1 || rerollDice == 2)
            title = "Reroll dice";
        else
            title = "Roll dice to start turn";

        if(((trainStationOwned || forTunaBoat) && rerollDice != 2 && rerollDice != 1)|| rerollDice == 2)
            twodice = true;

        if(rerollDice != 2 && !forTunaBoat)
            oneDice = true;

        int code;
        if(oneDice && twodice)
            code = PickDialogFrag.PICK_ROLL_ANY;
        else if(oneDice)
            code = PickDialogFrag.PICK_ROLL_ONE;
        else if(twodice)
            code = PickDialogFrag.PICK_ROLL_TWO;
        else
            code = -1;

        PickDialogFrag frag = PickDialogFrag.newInstance(title, null, code);
        frag.show(game.getSupportFragmentManager(), PickDialogFrag.tag);
    }

    /* This is here and not in the PickDialogFrag because I don't need the user to make
     * any decision. So I just used a PopupWindow for it.
     */
    static void displayDiceRoll(final int d1, final int d2, final InGame game){
        roll1 = d1;
        roll2 = d2;

        RelativeLayout diceLayout;
        diceLayout = new RelativeLayout(game);
        diceLayout.setFocusable(true);

        View rootLayout = game.findViewById(R.id.inGameLayout);

        int frames = new Random().nextInt(3)+12;
        AnimationDrawable rollOneAnimation = createDiceAnimation(frames, d1, game);
        AnimationDrawable rollTwoAnimation = new AnimationDrawable();
        if(d2 > 0) {
            rollTwoAnimation = createDiceAnimation(frames, d2, game);
        }

        //I'm doing a bunch of work to make it look all nice and fancy below this comment.
        PopupWindow popup = new PopupWindow(game);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //finds width of the screen
        int size = rootLayout.getWidth();
        size = size/3;

        diceLayout.addView(makeDiceButton(game));

        RelativeLayout.LayoutParams params;
        ImageView view;
        if(d2 > 0) {
            //creates an invisible item in the center of the layout,
            //which allows me to put the dice views on either side
            RelativeLayout.LayoutParams invisibleParams = new RelativeLayout.LayoutParams(0, 0);
            invisibleParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            TextView tv = new TextView(game);
            tv.setId(View.generateViewId());
            tv.setLayoutParams(invisibleParams);
            diceLayout.addView(tv);

            //add the second dice to the right of the invisible item
            params = getDiceTwoParams(size, tv.getId());
            //rollTwoAnimation will not be null if it gets to this point
            view = createDiceView(rollTwoAnimation, params, game);
            diceLayout.addView(view);

            //tell the first dice to the left of the invisible item
            params = getDiceOneParams(size, true, tv.getId());
        }
        else{
            //first dice needs different parameters depending on whether one or two dice will be shown
            params = getDiceOneParams(size, false, 0); //centerID won't be checked
        }
        //adding the common parts of the first dice to the layout
        view = createDiceView(rollOneAnimation, params, game);
        diceLayout.addView(view);

        popup.setWidth(rootLayout.getWidth());
        popup.setHeight(rootLayout.getHeight());
        popup.setContentView(diceLayout);
        popup.setBackgroundDrawable(game.getDrawable(R.drawable.background));

        popup.showAtLocation(rootLayout, Gravity.CENTER, 0, 0);
        game.setPopup(popup);
        //start the animations after all the layouts are set up and displaying
        rollOneAnimation.start();
        if(d2 > 0)
            rollTwoAnimation.start();
        //TODO add dice roll sounds
        //since its a popup, the user can easily close it when it finishes (I think even before that if they want)
    }

    private static AnimationDrawable createDiceAnimation(int frames, int lastSideDisplayed, InGame game){
        AnimationDrawable animation = new AnimationDrawable();
        animation.setOneShot(true);

        int lastSideAdded = 0;
        for(int i = 0; i < frames; i++){
            lastSideAdded = addToDiceAnimation(animation, lastSideAdded, game);
        }

        addToDiceAnimation(animation, lastSideDisplayed, -1, game);
        return animation;
    }

    private static Button makeDiceButton(final InGame game){
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        Button button = new Button(game);
        button.setText(R.string.continuePrompt);
        button.setLayoutParams(buttonParams);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupWindow popup = game.getPopup();
                if(popup != null)
                    popup.dismiss();
                game.setPopup(null);
                game.finishRoll(roll1, roll2);
            }
        });
        return button;
    }

    //centerID does not need to be set to anything if setForTwoDice is false
    private static RelativeLayout.LayoutParams getDiceOneParams(int size, boolean setForTwoDice, int centerID){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
        params.setMarginStart(size/4);
        params.setMarginEnd(size/4);

        if(setForTwoDice){
            params.addRule(RelativeLayout.LEFT_OF, centerID);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
        }
        else
            params.addRule(RelativeLayout.CENTER_IN_PARENT);

        return params;
    }

    private static RelativeLayout.LayoutParams getDiceTwoParams(int size, int centerID){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
        params.setMarginStart(size/4);
        params.setMarginEnd(size/4);

        params.addRule(RelativeLayout.RIGHT_OF, centerID);
        params.addRule(RelativeLayout.CENTER_VERTICAL);

        return params;
    }

    private static ImageView createDiceView(AnimationDrawable animation, RelativeLayout.LayoutParams params, InGame game){
        ImageView view = new ImageView(game);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setImageDrawable(animation);
        view.setLayoutParams(params);

        return view;
    }

    private static int addToDiceAnimation(AnimationDrawable drawable, int prevRoll, InGame game){
        return addToDiceAnimation(drawable, 0, prevRoll, game);
    }

    private static int addToDiceAnimation(AnimationDrawable drawable, int roll, int prevRoll, InGame game){
        final int duration = 83;
        int value = roll;
        int id = 0;
        do{
            if (roll < 1 || roll > 6) {
                value = Dice.roll();
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
        Drawable d = game.getDrawable(id);
        if(d != null)
            drawable.addFrame(d, duration);
        return value;
    }
}

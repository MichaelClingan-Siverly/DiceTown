package mike.cards;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by mike on 7/26/2017.
 * provides basic functionality for all cards
 */

public abstract class Card implements CardInterface{
    int numCopies = 1;
    int numRenovated = 0;

    @Override
    public int getNumCopies(){
        return numCopies;
    }
    @Override
    public void closeForRenovation(){
        numRenovated = numCopies;
    }
    @Override
    public void finishRenovation(){
        numRenovated = 0;
    }
    @Override
    public int getNumAvailable(){
        return numCopies-numRenovated;
    }

    /**I'm pretty lazy with overriding these in the subclasses, because there should only be
     * one of each class in each structure.
     * Since there should only be one of each card, I'm able to compare by hashCode pretty easily
     */
    @Override
    abstract public int hashCode();

    @Override
    abstract public boolean equals(Object o);

    @Override
    public int compareTo(@NonNull CardInterface c){
        return Integer.compare(hashCode(), c.hashCode());
    }

    @Override
    public void setnumOwnedImage(FrameLayout frame, boolean increaseMargin){
        if(numCopies > 1) {
            TextView foreground = new TextView(frame.getContext());
            foreground.setTextColor(Color.WHITE);
            foreground.setTypeface(foreground.getTypeface(), Typeface.BOLD);
            String text = "x"+numCopies;
            foreground.setText(text);
            foreground.setGravity(Gravity.BOTTOM | Gravity.END);

            setPadding(foreground,increaseMargin,false);
            frame.addView(foreground);
        }
    }

    @Override
    public void setNumRenovatedImage(FrameLayout frame, boolean increaseMargin){
        if (numRenovated > 0) {
            TextView foregroundRenovated = new TextView(frame.getContext());
            foregroundRenovated.setTextColor(Color.rgb(245, 168, 41));
            foregroundRenovated.setTypeface(foregroundRenovated.getTypeface(), Typeface.BOLD);
            String text = "!"+numRenovated;
            foregroundRenovated.setText(text);
            foregroundRenovated.setGravity(Gravity.BOTTOM | Gravity.START);

            setPadding(foregroundRenovated,increaseMargin, true);
            frame.addView(foregroundRenovated);
        }
    }

    //adds padding for the textviews on cards which display number of copies owned or renovated
    private void setPadding(TextView tv, boolean addMargin, boolean forReno){
        int botPad = 2;
        int rightPad = 0;
        int leftPad = 0;
        if(addMargin){
            tv.setTextSize(22);
            botPad = 12;
            if(forReno)
                leftPad = 20;
            else
                rightPad = 20;
        }
        else if(forReno)
            leftPad = 7;
        else
            rightPad = 7;

        float density = tv.getContext().getResources().getDisplayMetrics().density;
        tv.setPadding((int)(density * leftPad),0,(int)(density * rightPad), (int)(density * botPad));
    }
}

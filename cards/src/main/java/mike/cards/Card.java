package mike.cards;

import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
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

    /**
     *
     * @param layoutWidth width of the layout this card will be sized in comparison to
     * @param layoutHeight height of the layout this card will be sized in comparison to
     * @return int array holding card dimensions in pixels,
     * with index 0 having the width, and 1 having the height
     */
    public static int[] getLargeCardDimensions(int layoutWidth, int layoutHeight, DisplayMetrics d){
        //max height and width are 225 and 160 (respectively) dp, converted to px
        int maxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 225, d);
        int maxWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, d);
        int height = layoutHeight;
        int width = layoutWidth;

        //I check both height and width since I want to make sure the whole image fits in the screen
        int ratio = (int) (1.4 * width);
        if (height < ratio)
            width = (int) (.714 * height);
        else if (height > ratio)
            height = ratio;

        //If attempted dims is larger than max for one dim, both will be too large. So only check one
        if (height > maxHeight) {
            height = maxHeight;
            width = maxWidth;
        }
        return new int[]{width, height};
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

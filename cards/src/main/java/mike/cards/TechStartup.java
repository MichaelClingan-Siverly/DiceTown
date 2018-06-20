package mike.cards;

import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by mike on 7/26/2017.
 */

public class TechStartup extends MajorEstablishment {
    private int value = 0;

    public int getValue(){
        return value;
    }

    public void addInvestment(){
        value++;
    }

    @Override
    public <T> T accept(CardVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public String getCode() {
        return "TS";
    }

    @Override
    public int getFullImageId() {
        return R.drawable.tech_startup;
    }

    @Override
    public int getGridImageId() {
        return R.drawable.tech_startup;
    }

    @Override
    public int checkIfActivated(int roll, boolean myTurn) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 38;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }

    //Tech Startup is a major establishment, so a player can only own one of each.
    //So I just use the space normally used to show the number of copies for this.
    @Override
    public void setnumOwnedImage(FrameLayout frame, boolean increaseMargins){
        //players can't pick another player's major establishments.
        //all Tech Startups will have zero value when purchased.
        if(increaseMargins)
            return;

        TextView tv = new TextView(frame.getContext());
        String text = "value:"+value;
        tv.setText(text);
        tv.setGravity(Gravity.BOTTOM | Gravity.END);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11);
        float density = tv.getContext().getResources().getDisplayMetrics().density;
        tv.setPadding(0,0,(int)(density * 5), (int)(density * 2));

        frame.addView(tv);
    }
}

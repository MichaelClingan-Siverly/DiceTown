package mike.dicetown;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;

public class MainMenu extends AppCompatActivity {
    private String userName = "";
    private String townName = "";
    private String hostIP = "";
    private boolean host = false;
    EditText ipEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    public void joinListener(View v){
        getName();
        getTownName();

        if(nameEntered() && townNameEntered()) {
            AlertDialog.OnClickListener dialogListener = new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                    if(which == DialogInterface.BUTTON_POSITIVE){
                        hostIP = ipEdit.getText().toString();
                        try {
                            if(!hostIP.equals("") && InetAddress.getByName(hostIP).isReachable(500)) {
                                dialog.dismiss();
                                goToLobby();
                            }
                            else if(!hostIP.equals(""))
                                makeToast("Could not connect to host.");
                            else
                                makeToast("no IP entered");

                        }
                        catch (IOException e) {
                            makeToast("Unknown host. Is the IP entered correctly?");
                        }
                    }
                }
            };
            final AlertDialog.Builder ipAlert = new AlertDialog.Builder(this);
            ipAlert.setTitle("Join Game");
            ipAlert.setMessage("Please enter host's IP address");
            ipEdit = new EditText(this);
            ipAlert.setView(ipEdit);
            //make sure a userName is entered. If so, attempt to connect to the host
            ipAlert.setPositiveButton("Join", dialogListener);
            //go close the dialog and no nothing more
            ipAlert.setNegativeButton("Cancel", dialogListener);
            ipAlert.show();
        }
        else if(!nameEntered() && !townNameEntered())
            makeToast("no player or town name");
        else if(!nameEntered())
            makeToast("no player name");
        else
            makeToast("no town name");
    }

    public void hostListener(View v){
        getName();
        getTownName();
        if(nameEntered() && townNameEntered()) {
            host = true;
            goToLobby();
        }
        else if(!nameEntered() && !townNameEntered())
            makeToast("no player or town name");
        else if(!nameEntered())
            makeToast("no player name");
        else
            makeToast("no town name");
    }

    private void goToLobby(){
        Context context = this;
        Intent intent = new Intent(context, DiceTown.class);
        intent.putExtra("host", host);
        intent.putExtra("name", userName);
        intent.putExtra("town", townName);
        if(!hostIP.equals(""))
            intent.putExtra("IP", hostIP);
        startActivity(intent);
        finish();
    }

    private void getName(){
        EditText editText = (EditText)findViewById(R.id.mainMenuEditName);
        userName = editText.getText().toString();
    }
    private void getTownName(){
        EditText editText = (EditText)findViewById(R.id.mainMenuEditTownName);
        townName = editText.getText().toString();
    }

    private boolean nameEntered(){
        return !userName.equals("");
    }
    private boolean townNameEntered(){
        return !townName.equals("");
    }

    private void makeToast(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}

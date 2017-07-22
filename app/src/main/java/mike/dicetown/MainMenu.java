package mike.dicetown;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainMenu extends AppCompatActivity {
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
        getTownName();

        if(townNameEntered()) {
            AlertDialog.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        hostIP = ipEdit.getText().toString();
                        new CheckAddressTask(hostIP).execute(null, null);
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
        else
            makeToast("no town name");
    }

    private void continueJoining(String text){
        if(text.equals(CheckAddressTask.successfulReach)){
            goToLobby();
        }
        else
            makeToast(text);
    }

    public void hostListener(View v){
        getTownName();
        if(townNameEntered()) {
            host = true;
            goToLobby();
        }
        else
            makeToast("no town name");
    }

    private void goToLobby(){
        Context context = this;
        Intent intent = new Intent(context, Lobby.class);
        intent.putExtra(Lobby.booleanExtraKeyHost, host);
        intent.putExtra(Lobby.stringExtraKeyName, townName);
        if(!hostIP.equals(""))
            intent.putExtra("IP", hostIP);
        startActivity(intent);
        finish();
    }

    private void getTownName(){
        EditText editText = (EditText)findViewById(R.id.mainMenuEditTownName);
        townName = editText.getText().toString();
    }

    private boolean townNameEntered(){
        return !townName.equals("");
    }

    private void makeToast(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    private class CheckAddressTask extends AsyncTask<Void, Void, Boolean[]>{
        String address;
        private final int UNKNOWN_HOST = 0;  //User wants this to be false (meaning host is known)
        private final int REACHABLE = 1;     //user wants this to be true
        private final int NETWORK_ERROR = 2; //true if a network error occured.
        static final String successfulReach = "Host is reachable";

        CheckAddressTask(String address){
            this.address = address;
        }
        @Override
        protected Boolean[] doInBackground(Void... params) {
            Boolean[] results = new Boolean[]{null, null, null};


            try {
                InetAddress iAddress = InetAddress.getByName(address);
                results[REACHABLE] = iAddress.isReachable(3000);
            }
            catch(UnknownHostException e) {
                results[UNKNOWN_HOST] = true;
            } catch (IOException e) {
                e.getLocalizedMessage();
                results[NETWORK_ERROR] = true;
            }
            return results;
        }

        @Override
        protected void onPostExecute(Boolean[] result){
            if(result[REACHABLE] != null) {
                if(!result[REACHABLE])
                    continueJoining("Could not connect to host");
                else
                    continueJoining(successfulReach);
            }
            else if(result[UNKNOWN_HOST])
                continueJoining("That is not an address");
            else
                continueJoining("A network error occurred");
        }
    }
}

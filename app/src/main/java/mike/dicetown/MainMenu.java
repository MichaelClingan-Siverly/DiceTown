package mike.dicetown;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import mike.socketthreading.SocketService;

public class MainMenu extends AppCompatActivity {
    private String townName = "";
    private String hostIP = "";
    private boolean host = false;
    EditText ipEdit;
    AlertDialog ipDialog;
    private final String savedTextKey = "saved";
    private final String showDialog = "show";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        EditText editText = (EditText)findViewById(R.id.mainMenuEditTownName);
        //setting the flags in the xml wouldn't work for me, setting them here worked
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putBoolean(showDialog, ipDialog != null);
        if(ipEdit != null){
            String text = ipEdit.getText().toString();
            state.putString(savedTextKey, text);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state){
        super.onRestoreInstanceState(state);
        if(state.getBoolean(showDialog)){
            joinListener(null);
            String text = state.getString(savedTextKey);
            ipEdit.setText(text);
        }
    }

    public void joinListener(View v){
        getTownName();

        if(townNameEntered()) {
            AlertDialog.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //make sure a userName is entered. If so, attempt to connect to the host
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        hostIP = ipEdit.getText().toString();
                        new CheckAddressTask(hostIP).execute(null, null);
                    }
                    //if its negative, it closes dialog and does no nothing more
                }
            };
            String message = "Please enter host's IP address";
            displayAlert("Join Game", message, "Join", "Cancel", dialogListener);
        }
    }

    private void displayAlert(String title, String message, String posButton,
                                  String negButton, AlertDialog.OnClickListener listener){
        final AlertDialog.Builder ipAlert = new AlertDialog.Builder(this);
        if(title != null)
            ipAlert.setTitle(title);
        if(message != null)
            ipAlert.setMessage("Please enter host's IP address");
        ipEdit = new EditText(this);
        ipEdit.setInputType(InputType.TYPE_CLASS_PHONE);
        ipEdit.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        ipAlert.setView(ipEdit);
        if(posButton != null)
            ipAlert.setPositiveButton(posButton, listener);
        if(negButton != null)
            ipAlert.setNegativeButton(negButton, listener);

        ipDialog = ipAlert.show();
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
            intent.putExtra(Lobby.stringOptionalExtraKeyIP, hostIP);
        startActivity(intent);
        finish();
    }

    private void getTownName(){
        EditText editText = (EditText)findViewById(R.id.mainMenuEditTownName);
        townName = editText.getText().toString();
    }

    private boolean townNameEntered(){
        if(townName == null || townName.equals("")) {
            makeToast("no town name");
            return false;
        }
        else if(townName.equals("market")) {
            makeToast("sorry, that name is reserved");
            return false;
        }
        return true;
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

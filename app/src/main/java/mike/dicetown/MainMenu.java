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
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;

//TODO leak canary indicates that I sometmes (rarely) encounter a TextView leaking this activity. My only TextView is in the xml, and I don't mess with it at all, so that'll be interesting...
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
        EditText editText = findViewById(R.id.mainMenuEditTownName);
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

    @Override
    public void onBackPressed(){
        finishAffinity();
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
                        //don't even try checking if user doesn't enter anything
                        if(hostIP.equals(""))
                            makeToast("That is not an address");
                        else
                            new CheckAddressTask(MainMenu.this, hostIP).execute(null, null);
                    }
                    //if its negative, it closes dialog and does no nothing more
                }
            };
            displayAlert(dialogListener);
        }
    }

    private void displayAlert(AlertDialog.OnClickListener listener){
        final AlertDialog.Builder ipAlert = new AlertDialog.Builder(this);
        ipAlert.setTitle(getString(R.string.mainMenuJoin));
        ipAlert.setMessage("Please enter host's IP address");
        ipEdit = new EditText(this);
        ipEdit.setInputType(InputType.TYPE_CLASS_PHONE);
        ipEdit.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        ipAlert.setView(ipEdit);
        ipAlert.setPositiveButton(getString(R.string.join), listener);
        ipAlert.setNegativeButton(getString(R.string.cancel), listener);

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
        EditText editText = findViewById(R.id.mainMenuEditTownName);
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

    private static class CheckAddressTask extends AsyncTask<Void, Void, Boolean[]>{
        String address;
        private final int UNKNOWN_HOST = 0;  //User wants this to be false (meaning host is known)
        private final int REACHABLE = 1;     //user wants this to be true
        private final int NETWORK_ERROR = 2; //true if a network error occured.
        static final String successfulReach = "Host is reachable";
        private WeakReference<MainMenu> outerClass;

        CheckAddressTask(MainMenu context, String address){
            outerClass = new WeakReference<>(context);
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
            MainMenu activity = outerClass.get();
            //if outer class is null or already finishing, the user exited the app
            if(activity != null && !activity.isFinishing()){
                if(result[REACHABLE] != null) {
                    if(!result[REACHABLE])
                        activity.continueJoining("Could not connect to host");
                    else
                        activity.continueJoining(successfulReach);
                }
                else if(result[UNKNOWN_HOST])
                    activity.continueJoining("That is not an address");
                else
                    activity.continueJoining("A network error occurred");
            }
        }
    }
}

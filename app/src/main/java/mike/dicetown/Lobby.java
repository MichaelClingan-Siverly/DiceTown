package mike.dicetown;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import mike.socketthreading.SocketService;


/**
 * Created by mike on 7/11/2017.
 * the pre-game lobby where all players must ready-up before the host may start
 *
 * Used Android Developers References a lot for most of the Service stuff.
 * https://developer.android.com/reference/android/app/Service.html
 */

public class Lobby extends AppCompatActivity{
    /** One of the extras that are checked when creating this activity. indicate if player is host*/
    public final static String booleanExtraKeyHost = "host";
    /** One of the extras that are checked when creating this activity. name of the town*/
    public final static String stringExtraKeyName = "townName";
    private String myTownName;

    private boolean host;
    private String hostIP = null;
    private ArrayMap<String, PlayerReadyContainer> allPlayers;

    private void getExtras(){
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null) {
            host = extras.getBoolean(booleanExtraKeyHost, false);
            myTownName = extras.getString(stringExtraKeyName, "a town has no name");
            //player is joining a game. hostIP has been checked before leaving the mainMenu
            if(extras.containsKey("IP"))
                hostIP = extras.getString("IP");
        }

        allPlayers = new ArrayMap<>();
        addPlayerToList(myTownName);
    }

    private void changeReadiness(String townName){
        if(allPlayers.containsKey(townName)){
            PlayerReadyContainer container = allPlayers.get(townName);
            container.ready = !container.ready;
            if(container.ready) {
                container.readyIcon.setImageResource(R.drawable.checkmark);
                Button b = (Button)findViewById(R.id.lobbyButton);
                if(checkIfAllReady() && host) {
                    b.setText(R.string.lobbyStartGame);
                    b.setClickable(true);
                }
                //only change a player's button's function if they're the one who changed readiness
                else if(townName.equals(myTownName) && !host)
                    b.setText(R.string.lobbyUnReady);
            }
            else {
                container.readyIcon.setImageResource(R.drawable.unready);
                Button b = (Button)findViewById(R.id.lobbyButton);
                if(!checkIfAllReady() && host){
                    b.setText("");
                    b.setClickable(false);
                }
                //only change a player's button's function if they're the one who changed readiness
                else if(townName.equals(myTownName) && !host)
                    b.setText(R.string.lobbyReady);
            }
        }
    }

    private boolean checkIfAllReady(){
        for(int i = 0; i < allPlayers.size(); i++){
            PlayerReadyContainer cont = allPlayers.valueAt(i);
            if(!cont.ready)
                return false;
        }
        //not ready if host is the only player
        return allPlayers.size() > 1;
    }

    private void addPlayerToList(String townName){
        allPlayers.put(townName, new PlayerReadyContainer());
        //The left column is for the player's town name
        TextView name = new TextView(getApplicationContext());
        name.setText(townName);
        name.setTextColor(Color.BLACK);
        //I store the text height because I want the icons to be as tall (and wide) as the corresponding text
        name.measure(0,0);
        int textHeight = name.getMeasuredHeight();
        LinearLayout layout = (LinearLayout)findViewById(R.id.lobbyNameLayout);
        layout.addView(name);

        //the right column is for the player's ready status
        ImageView image = allPlayers.get(townName).readyIcon;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(textHeight, textHeight);
        image.setLayoutParams(params);
        layout = (LinearLayout)findViewById(R.id.lobbyIconLayout);
        layout.addView(image);

        if(host && townName.equals(myTownName)){
            changeReadiness(myTownName);
            Button b = (Button)findViewById(R.id.lobbyButton);
            b.setClickable(false);
        }
    }

    private class PlayerReadyContainer{
        boolean ready;
        ImageView readyIcon;
        PlayerReadyContainer(){
            ready = false;
            readyIcon = new ImageView(Lobby.this);
            readyIcon.setImageResource(R.drawable.unready);
            readyIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }

    /**
     * displays host's IP address on bottom of screen.
     * host IP should be set before this is called
     * Intended to only be used by the host, but can be used by others...although it won't mean much
     */
    private void displayHostIP(){
        if(mService != null) {
            Message  msg = Message.obtain();
            msg.what = SocketService.MSG_GET_IP;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.d("displayHostIP", e.getLocalizedMessage());
            }
            //All of this is test stuff to see the IP address. I need the ipByteArray to open a socket
            TextView textView = (TextView) findViewById(R.id.ipAddress);

            String ipAddress = "IP address: " + hostIP;
            textView.append(ipAddress);
        }
    }

    private void startService(){
        Intent intent = new Intent(Lobby.this, SocketService.class);
        if(host) {
            displayHostIP();
        }
        else{
            intent.putExtra(SocketService.MAKE_AS_CLIENT, hostIP);
        }
        //only name in there so far is the player's town name
        intent.putExtra("name", allPlayers.keyAt(0));
        startService(intent);
        doBindService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_layout);
        getExtras();
        //TODO uncomment this
//        startService();
        if(host){
            Button button = (Button)findViewById(R.id.lobbyButton);
            button.setClickable(false);

        }
    }

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //TODO handle messages received from the SocketService (define what they should be)
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        SocketService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
//                msg = Message.obtain(null,
//                        SocketService.MSG_SET_VALUE, this.hashCode(), 0);
//                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            Toast.makeText(Lobby.this, "remove service connected",
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

            // As part of the sample, tell the user what happened.
            Toast.makeText(Lobby.this, "remote service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(Lobby.this,
                SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            SocketService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    @Override
    public void onBackPressed(){
        //TODO close any sockets and go back to MainMenu
    }
}

package mike.dicetown;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import mike.socketthreading.SocketService;


/**
 * Created by mike on 7/11/2017.
 * the pre-game lobby where all players must ready-up before the host may start
 *
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
            else
                displayHostIP();
        }

        allPlayers = new ArrayMap<>();
        addPlayerToList(myTownName);
    }

    public void lobbyButtonListener(View v){
        if(host){
            if(checkIfAllReady()) {
                //TODO do whatever is needed to start game
            }
        }
        else{
            changeReadiness(myTownName);
            //TODO send ready/unready status to host
        }
    }

    private void changeReadiness(String townName){
        if(allPlayers.containsKey(townName)){
            PlayerReadyContainer container = allPlayers.get(townName);
            container.ready = !container.ready;
            if(container.ready) {
                container.readyIcon.setImageResource(R.drawable.checkmark);
                Button b = (Button)findViewById(R.id.lobbyButton);
                /* originally disabled the start button and would re-enable it here, but it
                   wouldnt work even though isClickable was true. so instead I let it stay
                   clickable and do another check when its pressed */
                if(checkIfAllReady() && host)
                    b.setText(R.string.lobbyStartGame);
                //only change a player's button's function if they're the one who changed readiness
                else if(townName.equals(myTownName) && !host)
                    b.setText(R.string.lobbyUnReady);
            }
            else {
                container.readyIcon.setImageResource(R.drawable.unready);
                Button b = (Button)findViewById(R.id.lobbyButton);
                if(!checkIfAllReady() && host)
                    b.setText("");
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
        int desiredIconSize = name.getMeasuredHeight();
        LinearLayout layout = (LinearLayout)findViewById(R.id.lobbyNameLayout);
        layout.addView(name);

        //the right column is for the player's ready status
        ImageView image = allPlayers.get(townName).readyIcon;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(desiredIconSize, desiredIconSize);
        image.setLayoutParams(params);
        layout = (LinearLayout)findViewById(R.id.lobbyIconLayout);
        layout.addView(image);

        //the host is always ready
        if(host && townName.equals(myTownName)){
            changeReadiness(myTownName);
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

    //only called if the user is a game host
    public void displayHostIP(){
        hostIP = getLocalIpAddress();
        TextView textView = (TextView) findViewById(R.id.ipAddress);

        String ipAddress = "IP address: " + hostIP;
        textView.append(ipAddress);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_layout);
        getExtras();
        //don't bother starting the service if there is no host IP
        if(!hostIP.equals("error getting address"))
            startService();
    }

    private void startService(){
        Intent intent = new Intent(Lobby.this, SocketService.class);
        intent.putExtra(SocketService.INTENT_HOST_BOOLEAN, host);
        intent.putExtra(SocketService.INTENT_HOST_IP_STRING, hostIP);
        //binding starts the service, and I'd rather bind since I want to communicate with it
        doBindService(intent);
    }

    @Override
    public void onBackPressed(){
        //TODO tell Service to close any sockets and then go back to MainMenu.
    }

    /* https://stackoverflow.com/a/18638588 */
    // gets the ip address of your phone's network
    private String getLocalIpAddress() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddressInt = manager.getConnectionInfo().getIpAddress();
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddressInt = Integer.reverseBytes(ipAddressInt);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddressInt).toByteArray();

        try{
            InetAddress inet = InetAddress.getByAddress(ipByteArray);
            int cutOff = inet.toString().lastIndexOf('/');
            return inet.toString().substring(cutOff+1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return "error getting address";
    }



    /**
     * Used Android Developers References a lot for most of the Service stuff.
     * https://developer.android.com/reference/android/app/Service.html
     */
    //the SocketService. Communicate with it by calling its methods
    private SocketService mBoundService;
    private boolean mIsBound = false;
    //
    private final Messenger mMessenger = new Messenger(new IncomingHandler(Lobby.this));

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((SocketService.LocalBinder)service).getService();

            //I don't get the Messenger from the service because this runs on the same thread as it
            //Because its on the same thread, I can use mBoundService and interact with it more easily
            //But I do give the service a Messenger so it can interact with me
            mBoundService.registerClient(mMessenger);

            // Tell the user about this for our demo.
            Toast.makeText(Lobby.this, "socket service connected",
                    Toast.LENGTH_SHORT).show();


        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(Lobby.this, "socket service connected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    //I chose to have this get an intent parameter so I may pass info to the service when creating it
    void doBindService(Intent intent) {
        Intent mIntent;
        if(intent == null)
            mIntent = new Intent(Lobby.this, SocketService.class);
        else
            mIntent = intent;
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            mBoundService.stopAcceptingConnections();
            // Detach our existing connection.
            mBoundService.unregisterClient(mMessenger);
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private static class IncomingHandler extends Handler{
        private final WeakReference<Lobby> mActivity;
        IncomingHandler(Lobby activity){
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            Lobby activity = mActivity.get();
            if (activity != null) {
                switch(msg.what){
                    default:
                        super.handleMessage(msg);
                }
                //TODO interact with Lobby through activity
            }
        }
    }
}

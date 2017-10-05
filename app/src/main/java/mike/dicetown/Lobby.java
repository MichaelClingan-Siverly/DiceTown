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
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
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
import java.util.ArrayList;

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
    private int myPlayerOrder;

    private boolean host;
    private boolean moveToGame = false;
    private String hostIP = null;
    private SparseArray<PlayerReadyContainer> allPlayers;

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
        myPlayerOrder = 0;
        allPlayers = new SparseArray<>();
        addPlayerToList(myTownName, myPlayerOrder);
    }

    public void lobbyButtonListener(View v){
        if(host){
            if(checkIfAllReady()) {
                goToGame();
            }
        }
        else{
            //if I'm not a host any my order is 0, it means the host hasn't given me an order yet
            //First thing they do when conencted is give me an order, so it means we're not connected yet
            if(myPlayerOrder != 0) {
                changeReadiness(myPlayerOrder);
                mBoundService.sendData(CHANGE_READINESS + ':' + myPlayerOrder, 0, -1);
            }
        }
    }

    private void goToGame(){
        moveToGame = true;
        doUnbindService();
        Intent intent = new Intent(Lobby.this, InGame.class);
        intent.putExtra("myName", myTownName);
        for(int i = 0; i < allPlayers.size(); i++){
            intent.putExtra("p"+i, allPlayers.get(i).townName);
        }
        startActivity(intent);
        finish();
    }

    private void changeReadiness(int playerOrder){
        if(allPlayers.get(playerOrder) != null){
            PlayerReadyContainer container = allPlayers.get(playerOrder);
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
                else if(playerOrder == myPlayerOrder && !host)
                    b.setText(R.string.lobbyUnReady);
            }
            else {
                container.readyIcon.setImageResource(R.drawable.unready);
                Button b = (Button)findViewById(R.id.lobbyButton);
                if(!checkIfAllReady() && host)
                    b.setText("");
                //only change a player's button's function if they're the one who changed readiness
                else if(playerOrder == myPlayerOrder && !host)
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

    private void addPlayerToList(String townName, int playerOrder){
        allPlayers.put(playerOrder, new PlayerReadyContainer(townName));
        //The left column is for the player's town name
        TextView name = new TextView(getApplicationContext());
        name.setTextSize(20);
        name.setText(townName);
        name.setTextColor(Color.BLACK);
        //I store the text height because I want the icons to be as tall (and wide) as the corresponding text
        name.measure(0,0);
        int desiredIconSize = name.getMeasuredHeight();
        LinearLayout layout = (LinearLayout)findViewById(R.id.lobbyNameLayout);
        layout.addView(name, playerOrder);

        //the right column is for the player's ready status
        ImageView image = allPlayers.get(playerOrder).readyIcon;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(desiredIconSize, desiredIconSize);
        image.setLayoutParams(params);
        layout = (LinearLayout)findViewById(R.id.lobbyIconLayout);
        layout.addView(image, playerOrder);

        //the host is always ready
        if(host && townName.equals(myTownName)){
            changeReadiness(playerOrder);
        }
        if(!host && playerOrder == 0){
            changeReadiness(0);
            changeReadiness(0);
        }
    }

    private void removePlayerFromList(int playerOrder){
        //change my playerOrder
        allPlayers.remove(playerOrder);
        if(myPlayerOrder > playerOrder)
            myPlayerOrder--;
        LinearLayout layout = (LinearLayout)findViewById(R.id.lobbyNameLayout);
        int numChildren = layout.getChildCount();
        for(int i = playerOrder; i < numChildren; i++){
            layout.removeViewAt(i);
            if(i+1 < numChildren && layout.getChildAt(i+1) != null){
                layout.addView(layout.getChildAt(i+1), i);
            }
        }
    }

    private class PlayerReadyContainer{
        boolean ready;
        ImageView readyIcon;
        String townName;
        PlayerReadyContainer(String townName){
            this.townName = townName;
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
        Context context = this;
        Intent intent = new Intent(context, MainMenu.class);
        startActivity(intent);
        finish();
        //onStop will end up being called and stop the service
    }

    @Override
    public void onPause(){
        super.onPause();
        if(isFinishing() && !moveToGame){
            stopService();
        }
    }

    private void stopService(){
        if(mIsBound){
            mBoundService.sendData(SocketService.LEAVE_GAME+":"+myPlayerOrder, -1, -1);
            stopService(new Intent(Lobby.this, SocketService.class));
            doUnbindService();
            mIsBound = false; //in case this ends up being called again
        }
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
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };

    //I chose to have this get an intent parameter so I may pass info to the service when creating it
    void doBindService(Intent intent) {
        Intent mIntent;
        if(intent == null)
            mIntent = new Intent(Lobby.this, SocketService.class);
        else
            mIntent = intent;
        //I start the service first, otherwise the service is destroyed when unbound, which I don't want to do
        startService(mIntent);
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
                    case SocketService.MSG_INCOMING_DATA:
                        activity.handleIncomingData((String)msg.obj);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }


    /**
     * key for data indicating that readiness should be changed
     */
    private final String CHANGE_READINESS = "CR";

    //I use the playerOrder who sent it in case I must reply to that specific user (or all users but that one)
    private void handleIncomingData(String dataString){
        /*
         * key for data indicating that a player's name has been changed due to conflicts
         */
        final String CHANGE_NAME = "CN";
        /*
         * key for data indicating the player's name. The key should be appended by the player order
         */
        final String PLAYER_NAME = "PN";

        ArrayList<DataMapping> list = parseIncomingData(dataString);
        for(DataMapping mapping : list){
            //host telling players what their player order is. Reply with the name of this user's town
            if(mapping.keyWord.contains(SocketService.PLAYER_ORDER)){
                if(!host) {
                    removePlayerFromList(myPlayerOrder);
                    myPlayerOrder = Integer.parseInt(mapping.value);
                    //the keyword includes my player order
                    mBoundService.sendData(PLAYER_NAME+myPlayerOrder+':'+myTownName, 0,-1);
                }
            }
            else if(mapping.keyWord.equals(CHANGE_NAME)){
                allPlayers.get(myPlayerOrder).townName = mapping.value;
                myTownName = mapping.value;
                LinearLayout layout = (LinearLayout)findViewById(R.id.lobbyNameLayout);
                TextView tv = (TextView)layout.getChildAt(myPlayerOrder);
                tv.setText(mapping.value);
            }
            //players telling you what their name is
            else if(mapping.keyWord.contains(PLAYER_NAME)){
                int order = extractInt(mapping.keyWord);
                String name = mapping.value;
                if(host){
                    name = checkName(name);
                }
                addPlayerToList(name,order);
                if(!host && order == myPlayerOrder-1)
                    addPlayerToList(myTownName, myPlayerOrder);
                if(host){
                    //send the new name to all existing players
                    mBoundService.sendData(PLAYER_NAME+':'+name, -1, order);
                    //send existing player names to the new player
                    for(int i = 0; i < order; i++){
                        int otherPlayerOrder = allPlayers.keyAt(i);
                        mBoundService.sendData(PLAYER_NAME+otherPlayerOrder+':'+allPlayers.valueAt(i).townName, order, -1);
                    }
                    if(!name.equals(mapping.value)){
                        mBoundService.sendData(CHANGE_NAME+':'+name, order, -1);
                    }
                }
                //all players know the host (with key 0) is ready, so they change host's readiness automatically
                else if(!allPlayers.get(0).ready){
                    changeReadiness(0);
                }
            }
            else if(mapping.keyWord.contains(CHANGE_READINESS)){
                int order = extractInt(mapping.value);
                changeReadiness(order);
                //If I'm the host, tell all other players that this player is (un)ready
                if(host){
                    mBoundService.sendData(CHANGE_READINESS,-1, order);
                }
            }
            else if(mapping.keyWord.equals("BG")){ //begin game
                goToGame();
            }
            else if(mapping.keyWord.equals(SocketService.LEAVE_GAME)){
                int playerOrderLeaving = Integer.parseInt(mapping.value);
                if(playerOrderLeaving == 0){
                    Toast.makeText(this, "Host left game", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                }
                else{
                    Toast.makeText(this, allPlayers.get(playerOrderLeaving).townName + " left the game",
                            Toast.LENGTH_SHORT).show();
                    mBoundService.removePlayer(playerOrderLeaving);
                    removePlayerFromList(playerOrderLeaving);

                    if(myPlayerOrder == 0){
                        mBoundService.sendData(dataString, -1, -1);
                    }
                }
            }
        }
    }

    private String checkName(String name){
        for(int i = 0; i < allPlayers.size(); i++){
            if(allPlayers.valueAt(i).townName.equals(name)){
                return getNewName(name);
            }
        }
        return name;
    }

    private String getNewName(String name){
        if(name == null || name.equals(""))
            return "noName";
        char c;
        String s = "";

        c = name.charAt(name.length()-1);
        if(c >= '0' && c <= '9'){
            s = c+s;
        }
        else
            return name+2;

        for(int i = name.length()-2; i >= 0; i--){
            c = name.charAt(name.length()-i);
            if(c >= '0' && c <= '9'){
                s = c+s;
            }
            else
                break;
        }
        return name+s;
    }

    private ArrayList<DataMapping> parseIncomingData(String dataString){
        ArrayList<DataMapping> list = new ArrayList<>();
        int i = 0;
        int j = i; //keeps track of the old index
        String a = null;
        while((i = dataString.indexOf(':', i)) != -1){
            if(a != null){
                list.add(new DataMapping(a, dataString.substring(j, i)));
                a = null;
            }
            else{
                a = dataString.substring(j, i);
            }
            i++;
            j = i;
        }
        if(a != null && dataString.length() >= i){
            list.add(new DataMapping(a, dataString.substring(j)));
        }
        return list;
    }

    //returns the first int value found in the string, and Integer.MIN_VALUE if no int can be found
    private int extractInt(String str){
        String s = "";
        for(int i = 0; i < str.length(); i++){
            if(str.charAt(i) >= '0' && str.charAt(i) <= '9'){
                s+=str.charAt(i);
            }
            else if(!s.equals(""))
                break;
        }
        if(s.equals("")) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(s);
    }

    private class DataMapping{
        String keyWord;
        String value;
        DataMapping(String keyword, String value){
            this.keyWord = keyword;
            this.value = value;
        }
    }
}

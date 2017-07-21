package mike.socketthreading;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;

public class SocketService extends Service implements ReceivesNewConnections {
    private ArrayList<PlayerSocketContainer> socketConnections;
    private Messenger client;
    private AcceptConnections serverListenerTask = null;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    /**
     * Registers a (presumably Activity) client so that this service may interact with it
     * @param clientsMessenger a Messenger belonging to the Activity this should be interacting with
     * @return true if there was no other client and this one was registered, false otherwise
     */
    public boolean registerClient(Messenger clientsMessenger){
        Log.d("registeringClient", "what that said");
        if(client == null) {
            client = clientsMessenger;
            return true;
        }
        return false;
    }

    /**
     * Tries to unregister a (presumably Activity) clicnt
     * @param clientsMessenger compared to the client's Messenger stored in class,
     *                         will be unregistered if it is the same
     */
    public void unregisterClient(Messenger clientsMessenger){
        if(client.equals(clientsMessenger))
            client = null;
    }

    /**
     * Tells the SocketServer task - if there is one - to stop accepting connections
     * This does nothing if connections are not being listened for
     */
    public void stopAcceptingConnections(){
        if(serverListenerTask != null){
            serverListenerTask.cancel(true);
        }
    }

    @Override
    public void onCreate() {
        socketConnections = new ArrayList<>();
    }

    /**
     * @param intent checked for INTENT_HOST_IP String extra. If it is not set or null,
     *               then a ServerSocket will be opened and listen for connections until
     *               sTopAcceptingConnections() is called
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkIntent(intent);
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_REDELIVER_INTENT;
    }

    /**
     * @param intent checked for INTENT_HOST_IP String extra. If it is not set or null,
     *               then a ServerSocket will be opened and listen for connections until
     *               sTopAcceptingConnections() is called
     */
    @Override
    public IBinder onBind(Intent intent) {
        checkIntent(intent);
        return mBinder;
    }

    /**
     * used when creating this Service. Required. true if host, false if not
     */
    public final static String INTENT_HOST_BOOLEAN = "host";
    /**
     * used when creating this Service. Required whether a client or server
     */
    public final static String INTENT_HOST_IP_STRING = "IP";

    private void checkIntent(Intent intent){
        String ip = intent.getStringExtra(INTENT_HOST_IP_STRING);
        //user is a host
        if(intent.getBooleanExtra(INTENT_HOST_BOOLEAN, false)) {
            serverListenerTask = new AcceptConnections(SocketService.this, ip);
            serverListenerTask.execute(null, null);
            //TODO run acceptConnections and give the activity my IP
        }
        //user is a client
        else{
            //TODO use the IP and connect to the host
        }
    }

    /**
     * receives new connections to a ServerSocket made in a separate thread
     * @param s the new socket to be added
     */
    @Override
    public void receiveConnection(Socket s) {
        Log.d("receiveConn", "address: " + s.getInetAddress().getHostAddress());
        //TODO add the socket to a set (whatever structure) of sockets
        //TODO rename the town if necessary and inform the client if it has been done
    }

    private class PlayerSocketContainer{
        String townName;
        Socket socket;
        PlayerSocketContainer(String townName, Socket s){
            this.townName = townName;
            socket = s;
        }
    }

    /**
     * Handler of incoming messages from clients.
     * Allows other threads to interact with this Service
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<SocketService> mService;
        IncomingHandler(SocketService service){
            mService = new WeakReference<>(service);
        }
        @Override
        public void handleMessage(Message msg) {
            SocketService service = mService.get();
            if(service != null){
                //TODO read message and interact with outer class through service
            }
        }
    }

}
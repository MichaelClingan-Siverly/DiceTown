package mike.socketthreading;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;

public class SocketService extends Service implements ReceivesNewConnections {
    private ArrayList<PlayerSocketContainer> connectionList;
    private Messenger client;

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
        if(client != null) {
            client = clientsMessenger;
            return true;
        }
        return false;
    }

    public void unregisterClient(Messenger clientsMessenger){
        if(client.equals(clientsMessenger))
            client = null;
    }

    @Override
    public void onCreate() {
        connectionList = new ArrayList<>();
    }

    //TODO check the intent to see if host
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * receives new connections to a ServerSocket made in a separate thread
     * @param s the new socket to be added
     */
    @Override
    public void receiveConnection(Socket s) {
        Log.d("receiveConn", "address: " + s.getInetAddress().getHostAddress());
        //TODO add the socket to a set (whatever structure) of sockets
    }

    @Override
    public void setHostIP(String hostIp){
        //TODO give the IP to the UI so it can be displayed
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
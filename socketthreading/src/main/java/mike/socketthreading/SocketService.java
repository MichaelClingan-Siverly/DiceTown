package mike.socketthreading;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class SocketService extends Service implements ReceivesNewConnections{
    private Messenger client;
    private AcceptConnections serverListenerTask = null;
    private GameClientConnection connections = null;
    private boolean previouslyStarted = false;
    private boolean cantConnectFlag = false;

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
        if(client == null) {
            client = clientsMessenger;
            if (cantConnectFlag)
                kickUser();
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
            serverListenerTask = null;
        }
    }

    /**
     * key for data indicating what playerOrder a player is assigned
     */
    public static final String PLAYER_ORDER = "PO";
    /**
     * key for data indicating that the socket is being closed (player is leaving game)
     */
    public static final String LEAVE_GAME = "LG";

    /**
     * Sends the given data to the correct receiving Sockets
     * @param data a String representing the data to be sent across the sockets.
     *             Strings should be formatted as key:value
     * @param indexToSendTo If set to an index of a socket, data will only be sent to that socket
     *                      otherwise, data will be sent to all sockets
     * @param indexToSkip if set to an index of a socket, data will be sent to all sockets but that one
     */
    public void sendData(String data, int indexToSendTo, int indexToSkip){
        connections.sendData(data, indexToSendTo, indexToSkip);
    }

    /**
     * @param intent checked for INTENT_HOST_IP String extra. If it is not set or null,
     *               then a ServerSocket will be opened and listen for connections until
     *               sTopAcceptingConnections() is called
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!previouslyStarted) {
            previouslyStarted = true;
            checkIntent(intent);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
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
        boolean host = intent.getBooleanExtra(INTENT_HOST_BOOLEAN, false);
        connections = new GameClientConnection(new Messenger(new IncomingHandler(SocketService.this)), host);
        String ip = intent.getStringExtra(INTENT_HOST_IP_STRING);
        //if host, begin listening for connections
        if(host) {
            serverListenerTask = new AcceptConnections(SocketService.this, ip);
            serverListenerTask.execute(null, null);
        }
        //if client, immediately connect to host
        else{
            new Thread(new CreateSocketTask(ip)).start();
        }
    }

    /**
     * receives new connections to a ServerSocket made in a separate thread
     * @param s the new socket to be added
     */
    @Override
    public void receiveConnection(Socket s) {
        int playerOrder = connections.addSocket(s);
        sendData(PLAYER_ORDER+':'+playerOrder, playerOrder, -1); //host is playerOrder zero
    }

    public void removePlayer(int playerOrder){
        connections.removeSocket(playerOrder);
    }

    /**
     * arg1 will be checked for index of what socket sent the message
     */
    public final static int MSG_INCOMING_DATA = 1;
    public final static int MSG_CANT_JOIN_GAME = 2;

    /**
     * Handles incoming messages from the Sockets and forwards them to the bound Activities.
     * Allows other threads (ones listening to Sockets) to interact with this Service
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<SocketService> mService;
        IncomingHandler(SocketService service){
            mService = new WeakReference<>(service);
        }
        @Override
        public void handleMessage(Message msg) {
            SocketService service = mService.get();
            if(service != null && service.client != null){
                switch(msg.what){
                    case MSG_INCOMING_DATA:
                        try {
                            Message message = Message.obtain(msg);
                            service.client.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    }

    //Because Sockets can't be made on the main thread: Used to create client sockets
    private class CreateSocketTask implements Runnable{
        String address;

        CreateSocketTask(String address){
            this.address = address;
        }

        @Override
        public void run() {
            try {
                InetAddress inetAddr = InetAddress.getByName(address);
                Socket s = new Socket(inetAddr, AcceptConnections.SERVERPORT);
                connections.addSocket(s);
            }
            //have lobby kick users out - host ip may be reachable, but not actually hosting a game
            catch(ConnectException e){
                if(client != null){
                    kickUser();
                }
                else{
                    cantConnectFlag = true;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void kickUser(){
        Message m = Message.obtain();
        m.what= MSG_CANT_JOIN_GAME;
        try {
            client.send(m);
        }
        catch(RemoteException re){
            re.printStackTrace();
            //I don't want to let the game crash for a user, but not sure how I should be handling
            //some of these exceptions I don't expect to be getting
        }
    }

    @Override
    public void onDestroy(){
        stopAcceptingConnections();
        connections.quitThread();
        connections = null;
        client = null;
        super.onDestroy();
    }
}
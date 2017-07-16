package mike.socketthreading;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Class to create a socket or server socket in a remove process.
 * To determine whether a client or server socket is to be made,
 * it looks at extras given while starting or binding. (see MAKE_AS_CLIENT)
 *
 * Without Android Developer stuff, I'd be screwed...
 * https://developer.android.com/reference/android/app/Service.html
 *
 * Created by mike on 7/14/2017.
 */
public class SocketService extends Service{
    //helps determine whether socket should be cast to Socket or ServerSocket
    private boolean host;
    //this can stay null if this is not the gmae host,
    // or if the host decides to stop receiving connections
    private ServerSocket serverSocket = null;
    //all sockets that this may be connected to. If I'm not a host, there should only be one in it
    private ArrayList<Socket> sockets = null;
    private AcceptConnections acceptTask = null;
    //TODO should let users change the port if the default one is being used
    private final int PORT = 37875;
    /**
     * Keeps track of all current registered clients.
     * I don't anticipate there will be more than one, but more can be handled
     **/
    ArrayList<Messenger> mClients = new ArrayList<>();
    /** Target we publish for clients to send messages to IncomingHandler. */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, or stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to the service to send a message across the socket.
     * The message must have had data set to it in obj or by setData
     * obj will be checked first and Data will be checked if there is no obj assigned
     * //TODO figure out what kind of data types I expect and what their keys may be
     */
    public static final int MSG_SEND_DATA = 3;

    /**
     * Command to the service to send a message across the socket.
     * only the Message's arg1 and arg2 will be checked
     * //TODO figure out what the args should mean if this is used, maybe to send the value of a dice roll
     */
    public static final int MSG_SEND_DATA_USE_ARGS = 4;

    /**
     * Command to the service to return the IP used by the socket.
     * Not really needed unless the service's client is a game host
     *
     * A Message will be sent to the clients with a what value of 5 and
     * a obj set to a String representation of the IP without the hostname
     */
    public static final int MSG_GET_IP = 5;

    /**
     * Command to the service to have the ServerSocket listen for and accept connections.
     * If the ServerSocket has previously been closed, it will create a new ServerSocket
     */
    public static final int MSG_ACCEPT_CONNECTIONS = 6;

    /**
     * Command to the service to no longer accept connections close the ServerSocket.
     */
    public static final int MSG_DENY_CONNECTIONS = 7;

    /**
     * Command to the service to begin the game.
     * The ServerSocket will no longer accept new connections
     */
    public static final int MSG_BEGIN_GAME = 8;

    /*
     * Indicates that a connection has been accepted by a serverSocket in another thread
     * Expects data to be set with two byte[] arrays, called "myAddress" and "theirAddress"
     */
    final static int MSG_CONNECTION_ACCEPTED = 9;

    /**
     * Key to be used while starting or binding. The corresponding value to this key is
     * expected to be a String representing the host's IP address.
     * It is expected that the IP is valid and reachable or else this will stop itself.
     *
     * The created socket will be connected to the host
     */
    public static final String MAKE_AS_CLIENT = "client";

    public SocketService() {
    }


    /**
     * Possible extras that can be sent: makeAsClient (see makeAsClient).
     * If no extra is sent, it will be assumed that the caller is the game
     * host and a ServerSocket will be created
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //create client socket
        if(intent.hasExtra(MAKE_AS_CLIENT)){
            String hostIP = intent.getStringExtra(MAKE_AS_CLIENT);
            makeSocket(hostIP);
        }
        //create host
        else{
            makeSocket(null);
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Possible extras that will be checked: MAKE_AS_CLIENT (see MAKE_AS_CLIENT).
     * If no extra is sent, this will first check if any sockets already exist;
     * If no sockets exist, a ServerSocket will be created
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        if(serverSocket == null || sockets == null) {
            //create client socket
            if (intent.hasExtra(MAKE_AS_CLIENT)) {
                String hostIP = intent.getStringExtra(MAKE_AS_CLIENT);
                makeSocket(hostIP);
            }
            //create host
            else{
                makeSocket(null);
            }
        }
        return mMessenger.getBinder();
    }

    //makes all sockets used by this Service. This ones used when starting the service
    private void makeSocket(String hostIP){
        //nothing keeping more players from joining a game, but the physical game is up to 5 players
        final int DEFAULT_MAX_PLAYERS = 5;
        try {
            //create client socket
            if(hostIP != null){
                host = false;
                InetAddress address = InetAddress.getByName(hostIP);
                sockets = new ArrayList<>(1);
                //creating a Socket like this will connect it to the remote one
                sockets.add(new Socket(address, PORT));
            }
            //create host
            else{
                host = true;
                if(serverSocket == null || serverSocket.isClosed())
                    serverSocket = new ServerSocket(PORT);
                if(sockets == null)
                    sockets = new ArrayList<>(DEFAULT_MAX_PLAYERS);
            }
        }
        catch (UnknownHostException e) {
            Log.d("unknownHost", "shouldn't happen. Address should be checked before game");
            stopSelf();
        }
        catch (IOException e) {
            Log.d("IOException", "error creating the socket");
        }
    }
    //and this one is used when creating Sockets that have connected to the server
    private void makeSocket(InetAddress myAddress, InetAddress theirAddress){
        try {
            //creating a Socket like this will connect it to the remote one
            Socket s = new Socket(theirAddress, PORT, myAddress, PORT);
            //TODO send a message across the socket and ask for their player/town names (I ask for it to avoid any problems that may arise if they connect and immediately send that info)
            sockets.add(s);

        } catch (IOException e) {
            Log.d("unknownHost", "shouldn't happen. Socket connected to me so address should be good");
            //I don't stopself here like in the other makeSocket because there may be other sockets connected
        }
    }

    class IncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SEND_DATA:
                    //TODO send some data across all registered Sockets
                    break;
                case MSG_SEND_DATA_USE_ARGS:
                    //TODO send some data across all registered Sockets
                    break;
                case MSG_GET_IP:
                    getIP();
                    break;
                case MSG_ACCEPT_CONNECTIONS:
                    if(host){
                        makeSocket(null);
                        acceptTask = new AcceptConnections(serverSocket, mMessenger.getBinder());
                        //puts the task on another thread and starts it
                        new Thread(acceptTask).start();
                    }
                    break;
                case MSG_DENY_CONNECTIONS:
                    denyConnections();
                    break;
                case MSG_BEGIN_GAME:
                    denyConnections();
                    //TODO begin the game...
                    break;
                case MSG_CONNECTION_ACCEPTED:
                    byte[] myAddr = msg.getData().getByteArray("myAddress");
                    byte[] theirAddr = msg.getData().getByteArray("theirAddress");
                    try {
                        InetAddress myAddress = InetAddress.getByAddress(myAddr);
                        InetAddress theirAddress = InetAddress.getByAddress(theirAddr);
                        makeSocket(myAddress, theirAddress);
                    } catch (UnknownHostException e) {
                        Log.d("unknownHostAccepted", "shouldn't happen. Address should be good if it connected to me");
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void denyConnections(){
        if(host && serverSocket != null && !serverSocket.isClosed()){
            if(acceptTask != null) {
                acceptTask.stopThread();
                acceptTask = null;
                //Did I have to get rid of the ServerSocket? No, but I don't
                // intend on allowing a host to let players rejoin once they've quit
                serverSocket = null;
            }
        }
    }

    private void getIP(){
        Message message = Message.obtain();
        String myAddress;
        if(host && serverSocket != null)
            myAddress = serverSocket.getInetAddress().toString();
        else
            myAddress = sockets.get(0).getInetAddress().toString();

        //Cut off the host name if there is one.
        int index = myAddress.lastIndexOf('/');
        myAddress = myAddress.substring(index+1);

        message.what = MSG_GET_IP;
        message.obj = myAddress;

        for(int i = mClients.size()-1; i >= 0; i--){
            try {
                mClients.get(i).send(message);
            }
            catch (RemoteException e) {
                //client is dead and can be removed.
                // Iterating backwards so its safe to do this inside the loop
                mClients.remove(i);
            }
        }
    }
}
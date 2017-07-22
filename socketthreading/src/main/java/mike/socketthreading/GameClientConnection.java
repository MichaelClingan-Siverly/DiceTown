package mike.socketthreading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by mike on 7/21/2017.
 * A class for listening and sending data to all connected Sockets
 */

public class GameClientConnection {
    //the Messenger belonging to whatever created this class. Send data received from Sockets to this
    private Messenger sendToMessenger;
    private HandlerThread myThread;
    //Handler for this class. Left it a Handler so I can post Runnables in addition to receiving Messages
    private Handler mHandler;
    private ArrayList<Socket> sockets;

    GameClientConnection(Messenger sendStuffTo){
        sockets = new ArrayList<>();
        myThread = new HandlerThread("myClientThread");
        myThread.start();
        sendToMessenger = sendStuffTo;
        mHandler = new ClientHandler(GameClientConnection.this);
        sockets = new ArrayList<>();
    }

    void quitThread(){
        myThread.quit();
    }

    void addSocket(Socket s, String testMeString){
        sockets.add(s);
        //TODO post a runnable where the socket is listened to for whatever amount of time.
    }

    private static class ClientHandler extends Handler{
        private final WeakReference<GameClientConnection> mClass;
        ClientHandler(GameClientConnection outerClass){
            super(outerClass.myThread.getLooper());
            mClass = new WeakReference<>(outerClass);
        }
        @Override
        public void handleMessage(Message msg) {
            GameClientConnection outer = mClass.get();
            if(outer != null){
                switch(msg.what){
                    //TODO only thing this class does besides listen to sockets is send data to all of them. The Serice tells me when to do that, so handle the message for it
                }
                //TODO read message and interact with outer class through service
            }
        }
    }
}

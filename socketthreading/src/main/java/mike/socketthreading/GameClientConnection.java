package mike.socketthreading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;

/**
 * Created by mike on 7/21/2017.
 * A class for listening and sending data to all connected Sockets
 */

class GameClientConnection {
    //the Messenger belonging to whatever created this class. Send data received from Sockets to this
    private Messenger sendToMessenger;
    private HandlerThread myThread;
    //Handler for this class. Left it a Handler so I can post Runnables in addition to receiving Messages
    private Handler mHandler;
    private SparseArray<Socket> sockets;
    private int nextSocketIndex = 0;
    //used as a synchronization lock for accesses to the SparseArray itself
    private final Object lock = new Object();

    GameClientConnection(Messenger sendStuffTo){
        myThread = new HandlerThread("myClientThread");
        myThread.start();
        sendToMessenger = sendStuffTo;
        mHandler = new ClientHandler(GameClientConnection.this);
        sockets = new SparseArray<>();
    }

    //TODO quit close the sockets and quit the thread when I end the game
    void quitThread(){
        myThread.quit();
    }

    int addSocket(Socket s){
        synchronized (lock) {
            sockets.put(nextSocketIndex, s);
        }
        int i = nextSocketIndex;
        nextSocketIndex++;
        readData(i);
        return i;
    }

    /**
     *
     * @param dataToBeSent a String to be sent across the sockets
     * @param indexToSendTo If set to an index of a socket, data will only be sent to that socket
     *                      otherwise, data will be sent to all sockets
     * @param indexToSkip if set to an index of a socket, data will be sent to all sockets but that one
     */
    void sendData(String dataToBeSent, int indexToSendTo, int indexToSkip){
        Socket s;
        synchronized (lock){
            s = sockets.get(indexToSendTo);
        }
        if(s != null){
            sendDataHelper(dataToBeSent, s);
        }
        else{
            int length;
            synchronized (lock){
                length = sockets.size();
            }
            for(int i = 0; i < length; i++){
                synchronized (lock){
                    if(sockets.keyAt(i) != indexToSkip)
                        continue;
                    s = sockets.valueAt(i);
                }
                sendDataHelper(dataToBeSent, s);
            }
        }
    }

    private void sendDataHelper(String dataToBeSent, Socket s){
        try {
            synchronized (s.getOutputStream()) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                writer.write(dataToBeSent, 0, dataToBeSent.length());
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * posts a new Runnable to the message queue which listens to the socket,
     * after the Runnable finishes it posts itself again so that the socket will
     * be listened to again once all other messages have been handled
     */

    private void readData(final int index){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Socket s;
                synchronized(lock){
                    s = sockets.get(index);
                }
                if(s != null) {
                    try {
                        synchronized(s.getInputStream()) {
                            while (s.getInputStream().available() > 0) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                String line = reader.readLine();
                                mHandler.sendMessage(Message.obtain(null, SocketService.MSG_INCOMING_DATA, index, -1, line));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //don't close the reader, since it will close the inputStream
                readData(index);
            }
        });
    }

    //creates the Handler from the outer class's Looper.
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
                    case SocketService.MSG_INCOMING_DATA:
                        try {
                            outer.sendToMessenger.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    }
}

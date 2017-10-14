package mike.socketthreading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
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
    private SparseArray<SocketContainer> sockets;
    private int nextSocketKey;
    //used as a synchronization lock for accesses to the SparseArray itself
    private final Object lock = new Object();
    //sockets made to create 2 pings, one now and one at half this time from now.
    //each ping reply creates new pings at this time
    private final int timer = 15000;

    GameClientConnection(Messenger sendStuffTo, boolean host){
        if(host)
            nextSocketKey = 1;
        else
            nextSocketKey = 0;
        myThread = new HandlerThread("myClientThread");
        myThread.start();
        sendToMessenger = sendStuffTo;
        mHandler = new ClientHandler(GameClientConnection.this);
        sockets = new SparseArray<>();
    }

    //I don't really use this, instead I let the thread (and sockets) die when the service is killed
    void quitThread(){
        synchronized (lock) {
            myThread.quit();
        }
    }

    int addSocket(Socket s){
        synchronized (lock) {
            sockets.put(nextSocketKey, new SocketContainer(s));
        }
        int i = nextSocketKey;
        nextSocketKey++;
        readData(i);
        beginKeepAlive(i);
        return i;
    }

    void removeSocket(int playerOrder){
        synchronized (lock) {
            int size = sockets.size();
            //while most things are indexed from zero, this one isn't.
            // (If I'm host, first player to join is key 1 and key 0 is empty)
            for(int i = playerOrder; i <= size; i++){
                sockets.remove(i);
                if(size > i && sockets.get(i+1) != null) {
                    SocketContainer cont = sockets.get(i + 1);
                    sockets.put(i, cont);
                }
            }
            nextSocketKey--;
        }
    }

    /**
     *
     * @param dataToBeSent a String to be sent across the sockets
     * @param keyToSendTo If set to an index of a socket, data will only be sent to that socket
     *                      otherwise, data will be sent to all sockets
     * @param keyToSkip if set to an index of a socket, data will be sent to all sockets but that one
     */
    void sendData(String dataToBeSent, int keyToSendTo, int keyToSkip){
        SocketContainer cont;
        synchronized (lock){
           cont  = sockets.get(keyToSendTo);
        }
        if(cont != null){
            sendDataHelper(dataToBeSent, keyToSendTo);
        }
        else{
            int length;
            synchronized (lock){
                length = sockets.size();
            }
            for(int i = 0; i < length; i++){
                int thisKey;
                synchronized (lock){
                    thisKey = sockets.keyAt(i);
                }
                if(thisKey != keyToSkip)
                    sendDataHelper(dataToBeSent, thisKey);
            }
        }
    }

    private void sendDataHelper(final String dataToBeSent, final int index){
        SocketContainer cont;
        synchronized (lock){
            cont = sockets.get(index);
        }
        if(cont != null && cont.socket != null) {
            Runnable r = new SendDataRunnable(dataToBeSent, index);
            mHandler.post(r);
        }
    }

    private void beginKeepAlive(final int index){
        postKeepAlive(index);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                postKeepAlive(index);
            }
        }, timer / 2);
    }

    //even is used to keep the keepAlive posts from increasing
    private void postKeepAlive(int index){
        SocketContainer cont;
        synchronized (lock){
            cont = sockets.get(index);
        }
        if(cont != null){
            Runnable r = new PingRunnable(index);
            mHandler.postDelayed(r, timer);
        }
    }

    private class PingRunnable implements Runnable{
        private int i;
        PingRunnable(final int index){
            i = index;
        }
        @Override
        public void run() {
            Log.d("ping", "ping sent to " + i);
            SocketContainer cont;
            synchronized (lock){
                cont = sockets.get(i);
            }
            if(cont != null) {
                if (cont.active) {
                    cont.active = false;
                    mHandler.post(new SendDataRunnable("ping", i));
                }
                else if (!cont.killMe) {
                    cont.killMe = true;
                    //inform the client that the player at this index has left. Let it decide if socket should be removed
                    mHandler.sendMessage(Message.obtain(null, SocketService.MSG_INCOMING_DATA, i, -1, SocketService.LEAVE_GAME + ":" + i));
                }
            }
        }
    }

    private class SendDataRunnable implements Runnable{
        String dataToBeSent;
        private int i;
        Socket s;
        SendDataRunnable(final String dataToSend, final int index){
            if(sockets.get(index) != null && sockets.get(index).socket != null)
                s = sockets.get(index).socket;
            dataToBeSent = dataToSend;
            i = index;
        }
        @Override
        public void run() {
            SocketContainer cont;
            synchronized (lock){
                cont = sockets.get(i);
            }
            if(cont != null) {
                try {
                    synchronized (s.getOutputStream()) {
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                        writer.write(dataToBeSent, 0, dataToBeSent.length());
                        writer.newLine();
                        writer.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                Socket s = null;
                synchronized(lock){
                    if(sockets.get(index) != null && sockets.get(index).socket != null)
                        s = sockets.get(index).socket;
                }
                try {
                    if (s != null && s.getInputStream().available() > 0) {
                        synchronized (s.getInputStream()) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                            String line;
                            while (reader.ready()) {
                                line = reader.readLine();
                                switch (line){
                                    case "ping":
                                        sendDataHelper("pong", index);
                                        break;
                                    case "pong":
                                        Log.d("ping", "pong returned from "+ index);
                                        synchronized (lock){
                                            if(sockets.get(index) != null) {
                                                sockets.get(index).active = true;
                                                postKeepAlive(index);
                                            }
                                        }
                                        break;
                                    default:
                                        mHandler.sendMessage(Message.obtain(null, SocketService.MSG_INCOMING_DATA, index, -1, line));
                                }
                            }
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
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
                            Message message = Message.obtain(msg);
                            outer.sendToMessenger.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    }

    private class SocketContainer{
        Socket socket;
        boolean active;
        boolean killMe;
        SocketContainer(Socket s){
            socket = s;
            active = true;
            killMe = false;
        }
    }
}

package mike.socketthreading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

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
    //and now I don't have to deal with synchronizing the modifications
    private ConcurrentHashMap<Integer, SocketContainer> sockets;
    private int nextSocketKey;
    //sockets made to create 2 pings, one now and one at half this time from now.
    //each ping reply creates new pings at this time
    private final int timer = 15000;
    private boolean threadQuit = false;
    //To ensure pauses and resumes are synchronized
    private final Object threadLock = new Object();

    GameClientConnection(Messenger sendStuffTo, boolean host){
        if(host)
            nextSocketKey = 1;
        else
            nextSocketKey = 0;
        myThread = new HandlerThread("myClientThread");
        myThread.start();
        sendToMessenger = sendStuffTo;
        mHandler = new ClientHandler(GameClientConnection.this);
        //and now I don't have to encapsulate the list modifications myself...
        sockets = new ConcurrentHashMap<>();
    }

    /* Well, I didn't think I needed to do this before.
     * Seems like I was naive thinking they would die when the service was killed.
     * Android Profiler made it VERY clear that this was not the case...
     */
    void quitThread(){
        synchronized (threadLock) {
            threadQuit = true;
        }
        mHandler.removeCallbacks(null);
        myThread.quitSafely();
    }

    int addSocket(Socket s){
        sockets.put(nextSocketKey, new SocketContainer(s));
        int i = nextSocketKey;
        nextSocketKey++;
        readData(i);
        beginKeepAlive(i);
        return i;
    }

    void removeSocket(int playerOrder){
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

    /**
     *
     * @param dataToBeSent a String to be sent across the sockets
     * @param keyToSendTo If set to an index of a socket, data will only be sent to that socket
     *                      otherwise, data will be sent to all sockets
     * @param keyToSkip if set to an index of a socket, data will be sent to all sockets but that one
     */
    void sendData(String dataToBeSent, int keyToSendTo, int keyToSkip){
        SocketContainer cont = sockets.get(keyToSendTo);
        if(cont != null){
            sendDataHelper(dataToBeSent, keyToSendTo);
        }
        else{
            for(Enumeration<Integer> e = sockets.keys(); e.hasMoreElements();){
                int i = e.nextElement();
                if(i != keyToSkip)
                    sendDataHelper(dataToBeSent, i);
            }
        }
    }

    private void sendDataHelper(final String dataToBeSent, final int index){
        SocketContainer cont = sockets.get(index);

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
        SocketContainer cont = sockets.get(index);
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
            SocketContainer cont = sockets.get(i);
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
            SocketContainer cont = sockets.get(i);
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

    private class ReadDataRunnable implements Runnable{
        int index;
        ReadDataRunnable(final int index){

            this.index = index;
        }

        @Override
        public void run() {
            Socket s = null;
            if(sockets.get(index) != null && sockets.get(index).socket != null)
                s = sockets.get(index).socket;
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
                                    if(sockets.get(index) != null) {
                                        sockets.get(index).active = true;
                                        postKeepAlive(index);
                                    }
                                    break;
                                default:
                                    synchronized (threadLock) {
                                        if (threadQuit)
                                            return;
                                    }
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
    }

    /**
     * posts a new Runnable to the message queue which listens to the socket,
     * after the Runnable finishes it posts itself again so that the socket will
     * be listened to again once all other messages have been handled
     */
    private void readData(final int index){
        //quick and dirty check to make sure I'm not posting anything on a dead thread
        //Checking if myThread or mHandler's Looper's thread is alive hasn't been working.
        synchronized (threadLock) {
            if (threadQuit)
                return;
            mHandler.post(new ReadDataRunnable(index));
        }
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
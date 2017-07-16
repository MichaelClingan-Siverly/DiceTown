package mike.socketthreading;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class used by the SocketService to allow SocketServers to accept connections on a remote thread.
 * As such, it's intended to only be used when the user is a game host
 *
 * Created by mike on 7/15/2017.
 */

class AcceptConnections implements Runnable{
    private ServerSocket serverSocket;
    //used to send messages back to the service. I hope.
    private Messenger callingThreadMessenger;
    AcceptConnections(ServerSocket serverSocket, IBinder callingThreadBinder){
        this.serverSocket = serverSocket;
        callingThreadMessenger = new Messenger(callingThreadBinder);
    }
    //Only stuff here in run() is executed in a remote thread.

    /**
     * Accepts connections on the given ServerSocket and returns the local and remote address
     * as byte arrays contained in a Bundle in a Message sent to the given IBinder
     * Keys are self-descriptive and are labeled "theirAddress" and "myAddress"
     */
    @Override
    public void run() {
        try {
            //Yup, I know it loops until an exception is caught, but I hear that its a
            // reasonable way to interrupt the accept()
            while(true) {
                Socket s = serverSocket.accept();
                Message msg = Message.obtain();
                msg.what = SocketService.MSG_CONNECTION_ACCEPTED;

                Bundle bundle = new Bundle();
                bundle.putByteArray("theirAddress", s.getInetAddress().getAddress());
                bundle.putByteArray("myAddress", serverSocket.getInetAddress().getAddress());
                msg.setData(bundle);
                //tell SocketService that a connection has been established
                callingThreadMessenger.send(msg);
            }
        } catch (IOException | RemoteException e) {
            //the Android example I used (seen in DiceTown) didn't do anything with this
            //Not sure if I should do anything here or not
        }
    }
    //Since stopping the accept will escape the loop, the thread will finish execution and stop
    void stopThread(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            //don't really care about the exception. I stopped the accept like I wanted to
        }
    }
}
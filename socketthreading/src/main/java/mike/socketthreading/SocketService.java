package mike.socketthreading;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.net.Socket;

public class SocketService extends Service implements ReceivesNewConnections {

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

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

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
        //TODO add the socket to a set (whatever structure) of sockets
    }
    @Override
    public void setHostIP(String hostIp){
        //TODO give the IP to the UI so it can be displayed
    }
}
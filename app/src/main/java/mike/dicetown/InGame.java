package mike.dicetown;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import mike.socketthreading.SocketService;

public class InGame extends AppCompatActivity {
    private SocketService mBoundService;
    private boolean mIsBound = false;
    private final Messenger mMessenger = new Messenger(new IncomingHandler(InGame.this));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        bindService(new Intent(InGame.this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((SocketService.LocalBinder)service).getService();

            //I don't get the Messenger from the service because this runs on the same thread as it
            //Because its on the same thread, I can use mBoundService and interact with it more easily
            //But I do give the service a Messenger so it can interact with me
            mBoundService.registerClient(mMessenger);

            // Tell the user about this for our demo.
            Toast.makeText(InGame.this, "socket service connected",
                    Toast.LENGTH_SHORT).show();


        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(InGame.this, "socket service connected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    //I chose to have this get an intent parameter so I may pass info to the service when creating it
    void doBindService(Intent intent) {
        Intent mIntent;
        if(intent == null)
            mIntent = new Intent(InGame.this, SocketService.class);
        else
            mIntent = intent;
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            mBoundService.stopAcceptingConnections();
            // Detach our existing connection.
            mBoundService.unregisterClient(mMessenger);
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private static class IncomingHandler extends Handler {
        private final WeakReference<InGame> mActivity;
        IncomingHandler(InGame activity){
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            InGame activity = mActivity.get();
            if (activity != null) {
                switch(msg.what){
                    case SocketService.MSG_INCOMING_DATA:
                        activity.handleIncomingData(msg.arg1, (String)msg.obj);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

    //I use the playerOrder who sent it in case I must reply to that specific user (or all users but that one)
    private void handleIncomingData(int playerOrderWhoSentThis, String dataString){

        //TODO Well...parse and handle incoming data...
    }

    private ArrayList<DataMapping> parseIncomingData(String dataString){
        ArrayList<DataMapping> list = new ArrayList<>();
        int i = 0;
        int j = i; //keeps track of the old index
        String a = null;
        String b = null;
        while((i = dataString.indexOf(':', i)) != -1){
            if(a != null){
                b = dataString.substring(j, i);
                list.add(new DataMapping(a, b));
                a = null;
            }
            else{
                a = dataString.substring(j, i);
            }
            i++;
            j = i;
        }
        if(a != null && b == null){
            b = dataString.substring(j);
            list.add(new DataMapping(a,b));
        }
        return list;
    }

    private class DataMapping{
        String keyWord;
        String value;
        DataMapping(String keyword, String value){
            this.keyWord = keyword;
            this.value = value;
        }
    }
}

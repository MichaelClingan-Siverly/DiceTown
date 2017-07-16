package mike.dicetown;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import mike.socketthreading.SocketService;


/**
 * Created by mike on 7/11/2017.
 * the pre-game lobby where all players must ready-up before the host may start
 *
 * Used Android Developers References a lot for most of the Service stuff.
 * https://developer.android.com/reference/android/app/Service.html
 */

public class DiceTown extends AppCompatActivity{
    private boolean host;
    private String hostIP = null;
    public String myName;
    public String myTownName;

    private void getExtras(){
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null) {
            host = extras.getBoolean("host", false);
            myName = extras.getString("name", "a man has no name");
            myTownName = extras.getString("town", "a town has no name");
            //player is joining a game. hostIP has been checked before leaving the mainMenu
            if(extras.containsKey("IP"))
                hostIP = extras.getString("IP");
        }
    }

    /**
     * displays host's IP address on bottom of screen.
     * host IP should be set before this is called
     * Intended to only be used by the host, but can be used by others...although it won't mean much
     */
    private void displayHostIP(){
        if(mService != null) {
            Message  msg = Message.obtain();
            msg.what = SocketService.MSG_GET_IP;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.d("displayHostIP", e.getLocalizedMessage());
            }
            //All of this is test stuff to see the IP address. I need the ipByteArray to open a socket
            TextView textView = (TextView) findViewById(R.id.ipAddress);

            String ipAddress = "IP address: " + hostIP;
            textView.append(ipAddress);
        }
    }

    private void startService(){
        Intent intent = new Intent(DiceTown.this, SocketService.class);
        if(host) {
            displayHostIP();
        }
        else{
            intent.putExtra(SocketService.MAKE_AS_CLIENT, hostIP);
        }
        startService(intent);
        doBindService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getExtras();
        setContentView(R.layout.activity_lobby_layout);
        startService();

        //TODO make a lobby layout - Multiple ImageViews will have to be made
        ImageView readyImage = new ImageView(this);
        readyImage.setImageResource(R.drawable.checkmark);
        readyImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        ImageView unreadyImage = new ImageView(this);
        unreadyImage.setImageResource(R.drawable.unready);
        readyImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /** Some text view we are using to show state information. */
    TextView mCallbackText; //TODO get rid of this. I'm sure my family won't care to know whats up with the Service

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //TODO handle messages received from the SocketService
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            mCallbackText.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        SocketService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
//                msg = Message.obtain(null,
//                        SocketService.MSG_SET_VALUE, this.hashCode(), 0);
//                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            Toast.makeText(DiceTown.this, "remove service connected",
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mCallbackText.setText("Disconnected.");

            // As part of the sample, tell the user what happened.
            Toast.makeText(DiceTown.this, "remote service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(DiceTown.this,
                SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mCallbackText.setText("Binding.");
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            SocketService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            mCallbackText.setText("Unbinding.");
        }
    }

}

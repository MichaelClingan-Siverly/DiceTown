package mike.dicetown;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainMenu extends AppCompatActivity {
    private String userName = "";
    private boolean host = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        EditText editText = (EditText)findViewById(R.id.mainMenuEditName);



        //TODO move this stuff to lobby screen
//        //All of this is test stuff to see the IP address. I need the ipByteArray to open a socket
//        TextView textView = (TextView)findViewById(R.id.ipAddress);
//
//        WifiManager manager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
//        int address = manager.getConnectionInfo().getIpAddress();
//        String ipAddress = "IP address: "+address;
//        textView.append(ipAddress);
//
//        // Convert little-endian to big-endianif needed
//        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
//            address = Integer.reverseBytes(address);
//        }
//
//        String meh = "\n";
//        byte[] ipByteArray = BigInteger.valueOf(address).toByteArray();
//
//        try {
//            InetAddress inet = InetAddress.getByAddress(ipByteArray);
//            meh += inet.toString();
//            textView.append(meh);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
    }

    public void joinListener(View v){
        AlertDialog.Builder ipAlert = new AlertDialog.Builder(this);
        ipAlert.setTitle("Join Game");
        ipAlert.setMessage("Please enter host's IP address");
        EditText ipInput = new EditText(this);
        ipAlert.setView(ipInput);
        //make sure a userName is entered. If so, attempt to connect to the host
        ipAlert.setPositiveButton("Join", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //TODO
            }
        });
        //go close the dialog and no nothing more
        ipAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //TODO
            }
        });
        ipAlert.show();
    }

    public void hostListener(View v){

    }
}

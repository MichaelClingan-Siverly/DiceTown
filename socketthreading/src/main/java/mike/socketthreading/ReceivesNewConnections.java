package mike.socketthreading;

import java.net.Socket;

/**
 * Created by mike on 7/19/2017.
 */

interface ReceivesNewConnections {
    void receiveConnection(Socket s);
    void setHostIP(String hostIP);
}

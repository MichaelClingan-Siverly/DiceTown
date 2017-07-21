package mike.socketthreading;

import java.net.Socket;

/**
 * Created by mike on 7/19/2017.
 * Interface to facilitate communication between AcceptConnections and SocketService
 * (Could have used the Handler from SocketService, and I may do that in the future)
 */

//TODO conside using the Handler from SocketService instead of callbacks
interface ReceivesNewConnections {
    void receiveConnection(Socket s);
}

package mike.socketthreading;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

class AcceptConnections extends AsyncTask <Void, Socket, Void>{
    private ReceivesNewConnections callback;
    // designate a port
    //TODO let users change this if necessary (would have to display it on the host's UI and allow clients to enter it)
    static final int SERVERPORT = 49255;
    private String address;

    private ServerSocket serverSocket = null;

    AcceptConnections(ReceivesNewConnections sendsNewConnectionsToThis, String addressToUse){
        callback = sendsNewConnectionsToThis;
        address = addressToUse;
    }

    private void createServerSocket(){
        try {
            InetAddress addressUsed = InetAddress.getByName(address);
            serverSocket = new ServerSocket(SERVERPORT, 0, addressUsed);

            //I also set a timeout on the accept() so I can occasionally check to see if it should be cancelled
            serverSocket.setSoTimeout(2000); //2 seconds
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        createServerSocket();
        while(!isCancelled() && serverSocket != null && !serverSocket.isClosed()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                //don't particularly care if it times out or if I close the
            }
            //I publish because I want this to keep going until the user decides to cancel it by starting the game
            if(socket != null)
                publishProgress(socket);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Socket... progress) {
        if(callback == null){
            cancel(true);
        }
        else if(progress[0] != null && !isCancelled())
            callback.receiveConnection(progress[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        closeSocket();
    }

    @Override
    public void onCancelled(Void result){
        closeSocket();
    }

    private void closeSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                //*shrugs*
            }
        }
    }
}
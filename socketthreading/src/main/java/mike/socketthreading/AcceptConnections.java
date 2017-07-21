package mike.socketthreading;

import android.os.AsyncTask;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class AcceptConnections extends AsyncTask <Void, Socket, Void>{
    private ReceivesNewConnections callback;
    // designate a port
    //TODO let users change this if necessary (would have to display it on the host's UI and allow clients to enter it)
    public static final int SERVERPORT = 49255;

    private ServerSocket serverSocket = null;

    AcceptConnections(ReceivesNewConnections sendsNewConnectionsToThis){
        callback = sendsNewConnectionsToThis;
    }

    @Override
    protected void onPreExecute(){
        try {
            //creates the socket and give the IP back to the service so it can be displayed
            serverSocket = new ServerSocket(SERVERPORT);
            callback.setHostIP(serverSocket.getInetAddress().getHostAddress());
            //I also set a timeout on the accept() so I can occasionally check to see if it should be cancelled
            serverSocket.setSoTimeout(2000); //2 seconds
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.setHostIP(serverSocket.getInetAddress().getHostAddress());
    }

    @Override
    protected Void doInBackground(Void... params) {
        while(!isCancelled() && serverSocket != null && !serverSocket.isClosed()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                //don't particularly care if it times out
            }
            //I publish because I want this to keep going until the user decides to cancel it by starting the game
            if(socket != null)
                publishProgress(socket);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Socket... progress) {
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
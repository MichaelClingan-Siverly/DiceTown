package mike.socketthreading;

import android.os.Message;

public interface ReceivesMessages {
    void handleMessage(Message msg);
}

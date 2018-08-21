package mike.dicetown;

import android.app.Application;
import android.content.Intent;

//import com.squareup.leakcanary.LeakCanary;
//import com.squareup.leakcanary.RefWatcher;

/**
 * Simple launcher Application that simply moves the user to the main menu.
 * Needed this so I could use Leak Canary (manually checking Android Profiler is troublesome)
 */
public class DiceTown extends Application {
//    private RefWatcher refWatcher;
//    //LeakCanary watches Activities by default. Use this if I want it to watch other stuff
//    public static RefWatcher getRefWatcher(Context context) {
//        DiceTown application = (DiceTown) context.getApplicationContext();
//        return application.refWatcher;
//    }

    @Override
    public void onCreate(){
        super.onCreate();
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        refWatcher = LeakCanary.install(this);
//        // Normal app init code (go to the Main Menu)...
        Intent intent = new Intent(DiceTown.this, MainMenu.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}

package br.com.digitaldesk.pluginscanandnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


 //Created by bruno.klein on 08/11/2016.

public class BootBroadcastReceiver extends BroadcastReceiver {

    private Context context;

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED” start Service
        if (intent.getAction().equals(ACTION)) {
            Log.i("Script", "Entrou BootBroadcast");
            this.context = context;

            Intent serviceIntentScan = new Intent(this.context, BackgroundStartScanService.class);
            this.context.startService(serviceIntentScan);

            Intent serviceIntent = new Intent(context, BackgroundSubscribeIntentService.class);
            this.context.startService(serviceIntent);
        }
    }
}


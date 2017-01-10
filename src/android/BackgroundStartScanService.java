package br.com.digitaldesk.pluginscanandnotification;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.ArrayList;
import java.util.List;


 //Created by bruno.klein on 16/11/2016.
 
public class BackgroundStartScanService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private ArrayAdapter<String> mNearbyMessagesArrayAdapter;

    private RelativeLayout mContainer;

    private boolean mSubscribed = false;

    private List<String> mNearbyMessagesList = new ArrayList<String>();

    private Context context;

    public BackgroundStartScanService() { super("BackgroundStartScanService"); }

    @Override
    protected void onHandleIntent(Intent intent) {
            buildGoogleApiClient();
            Log.i("Script", "Entrou StartScan");

            final List<String> cachedMessages = Utils.getCachedMessages(this);
            if (cachedMessages != null) {
                mNearbyMessagesList.addAll(cachedMessages);
            }
    }

    private void buildGoogleApiClient() {
        Log.i("Script", "GoogleApiClient");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
                            .setPermissions(NearbyPermissions.BLE).build())
                    .addConnectionCallbacks(this)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mContainer != null) {
            Snackbar.make(mContainer, "Exception while connecting to Google Play services: " +
                            connectionResult.getErrorMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("Script", "GoogleApiClient connected");
        subscribe();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(key, Utils.KEY_CACHED_MESSAGES)) {
            mNearbyMessagesList.clear();
            mNearbyMessagesList.addAll(Utils.getCachedMessages(this));
            mNearbyMessagesArrayAdapter.notifyDataSetChanged();
        }
    }
    private void subscribe() {
        if (mSubscribed) {
            Log.i("Script", "Already subscribed.");
            return;
        }

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                /*.setFilter(new MessageFilter.Builder()
                        .includeNamespacedType("edd1ebeac04e5defa017", "candy")
                        .build())*/
                .build();

        Nearby.Messages.subscribe(mGoogleApiClient, getPendingIntent(), options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i("Script", "Subscribed successfully.");
                            startService(getBackgroundSubscribeServiceIntent());
                        } else {
                            Log.e(TAG, "Operation failed. Error: " +
                                    NearbyMessagesStatusCodes.getStatusCodeString(
                                            status.getStatusCode()));
                        }
                    }
                });
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getService(this, 0,
                getBackgroundSubscribeServiceIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Intent getBackgroundSubscribeServiceIntent() {
        return new Intent(this, BackgroundSubscribeIntentService.class);
    }
}
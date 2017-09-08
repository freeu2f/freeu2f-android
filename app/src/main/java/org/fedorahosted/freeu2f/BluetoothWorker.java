package org.fedorahosted.freeu2f;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Background service for handling bluetooth send and receives
 */
public class BluetoothWorker extends IntentService {
    private static final String TAG = "BT_WORKER_SERVICE";
    public static final String ACTION_SCANBLE = "org.fedorahosted.freeu2f.action.SCANBLE";
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothWorker() {
        super("BluetoothWorker");

    }

    public static void StartActionScanBLE(Context context) {
        Intent intent = new Intent(context, BluetoothWorker.class);
        intent.setAction(ACTION_SCANBLE);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action){
                case ACTION_SCANBLE:
                    handleActionScanBLE();
                    break;
                default:
                    Log.d(TAG, String.format("received unknown action: %s", action));
            }
        }
    }

    private void handleActionScanBLE() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        ScanCallback sc = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.d(TAG, result.getScanRecord().getDeviceName());
                super.onScanResult(callbackType, result);
            }
        };
        bluetoothAdapter.getBluetoothLeScanner().startScan(sc);
    }
}

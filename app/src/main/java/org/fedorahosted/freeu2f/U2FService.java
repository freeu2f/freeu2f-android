package org.fedorahosted.freeu2f;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;

public class U2FService extends Service {
    public static final String UI_DATA = "UI_DATA";
    public static final String UPDATE_UI_BROADCAST = "org.fedorahosted.freeu2f.U2FService.updateUI";
    private LocalBroadcastManager localBroadcastManager = null;

    private U2FGattService mU2FGattService = new U2FGattService();
    private BluetoothLeAdvertiser mBtLeAdvertiser = null;
    private BluetoothGattServer mGattServer = null;

    private AdvertiseSettings cfg = new AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
            .build();

    private AdvertiseData data = new AdvertiseData.Builder()
            .addServiceUuid(new ParcelUuid(U2FGattService.U2F_UUID))
            .setIncludeDeviceName(true)
            .build();

    private AdvertiseCallback cb = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            brodadcastUIUpdates(getString(R.string.ADVERTISE_START_SUCCESS));
            Log.d(getClass().getCanonicalName(), getString(R.string.ADVERTISE_START_SUCCESS));
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            brodadcastUIUpdates(getString(R.string.ADVERTISE_START_FAILED));
            Log.d(getClass().getCanonicalName(), getString(R.string.ADVERTISE_START_FAILED));
        }
    };

    private BluetoothGattServerCallback mGattCallback = new BluetoothGattServerCallback() {
        @Override
        public void onCharacteristicReadRequest(
                BluetoothDevice device,
                int requestId,
                int offset,
                BluetoothGattCharacteristic chr) {
            int status = BluetoothGatt.GATT_FAILURE;
            byte[] bytes = null;

            if (offset != 0) {
                status = BluetoothGatt.GATT_INVALID_OFFSET;
                brodadcastUIUpdates("status: GATT_INVALID_OFFSET");
            } else if (chr.equals(U2FGattService.U2F_CONTROL_POINT_LENGTH)) {
                status = BluetoothGatt.GATT_SUCCESS;
                bytes = new byte[] { 0x02, 0x00 }; /* Length == 512, see U2F BT 6.1 */
                brodadcastUIUpdates(String.format("status: GATT_SUCCESS, bytes %s", Arrays.toString(bytes)));
            } else if (chr.equals(U2FGattService.U2F_SERVICE_REVISION_BITFIELD)) {
                status = BluetoothGatt.GATT_SUCCESS;
                bytes = new byte[] { 0x40 };       /* Version == 1.2, see U2F BT 6.1 */
                brodadcastUIUpdates(String.format("status: GATT_SUCCESS, bytes %s", Arrays.toString(bytes)));
            }

            // needs to be detached from block to allow updated check
            if (status == BluetoothGatt.GATT_FAILURE){
                brodadcastUIUpdates("status: GATT_FAILURE");
            }

            String message = (bytes == null)
                    ? "byte length was null"
                    : Integer.valueOf(bytes.length).toString();

            Log.d(getClass().getCanonicalName(), message);
            mGattServer.sendResponse(device, requestId, status, 0, bytes);
        }

        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattCharacteristic chr,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value) {
            int status = BluetoothGatt.GATT_FAILURE;

            if (offset != 0) {
                status = BluetoothGatt.GATT_INVALID_OFFSET;
            } else if (chr.equals(U2FGattService.U2F_CONTROL_POINT)) {
                status = BluetoothGatt.GATT_SUCCESS;
                // TODO
            } else if (chr.equals(U2FGattService.U2F_SERVICE_REVISION_BITFIELD)) {
                status = BluetoothGatt.GATT_SUCCESS;
            }

            mGattServer.sendResponse(device, requestId, status, 0, null);
        }
    };

    private void brodadcastUIUpdates(String text){
        if (localBroadcastManager == null){
            localBroadcastManager = LocalBroadcastManager.getInstance(this);
        }

        Intent intent = new Intent(UPDATE_UI_BROADCAST);
        intent.putExtra(UI_DATA, text);
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BluetoothManager man = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mGattServer = man.openGattServer(this, mGattCallback);
        mGattServer.addService(mU2FGattService);

        mBtLeAdvertiser = man.getAdapter().getBluetoothLeAdvertiser();
        mBtLeAdvertiser.startAdvertising(cfg, data, cb);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mBtLeAdvertiser.stopAdvertising(cb);
        mGattServer.removeService(mU2FGattService);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

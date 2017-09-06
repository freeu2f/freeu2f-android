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
import android.util.Log;

public class U2FService extends Service {
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
            Log.d(getClass().getCanonicalName(), "Advertising started...");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(getClass().getCanonicalName(), "Advertising failed!");
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
            } else if (chr.equals(U2FGattService.U2F_CONTROL_POINT_LENGTH)) {
                status = BluetoothGatt.GATT_SUCCESS;
                bytes = new byte[] { 0x02, 0x00 }; /* Length == 512, see U2F BT 6.1 */
            } else if (chr.equals(U2FGattService.U2F_SERVICE_REVISION_BITFIELD)) {
                status = BluetoothGatt.GATT_SUCCESS;
                bytes = new byte[] { 0x40 };       /* Version == 1.2, see U2F BT 6.1 */
            }

            Log.d(getClass().getCanonicalName(), Integer.valueOf(bytes.length).toString());
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

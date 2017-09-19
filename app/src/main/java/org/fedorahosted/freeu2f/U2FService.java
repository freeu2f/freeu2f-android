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

import org.fedorahosted.freeu2f.u2f.APDUReply;
import org.fedorahosted.freeu2f.u2f.APDURequest;
import org.fedorahosted.freeu2f.u2f.ErrorCode;
import org.fedorahosted.freeu2f.u2f.Frameable;
import org.fedorahosted.freeu2f.u2f.FrameableException;
import org.fedorahosted.freeu2f.u2f.Packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class U2FService extends Service {
    private static Map<APDURequest.Instruction, RequestHandler> requestHandlers = new HashMap<>();
    private static byte[] VERSION = new byte[] { 0b01000000 }; /* VERSION = 1.2, see U2F BT 6.1 */
    private static char MTU = 512;

    static {
        requestHandlers.put(APDURequest.Instruction.AUTHENTICATE, new AuthenticateRequestHandler());
        requestHandlers.put(APDURequest.Instruction.REGISTER, new RegisterRequestHandler());
        requestHandlers.put(APDURequest.Instruction.VERSION, new VersionRequestHandler());
    }

    private U2FGattService mU2FGattService = new U2FGattService();
    private BluetoothLeAdvertiser mBtLeAdvertiser = null;
    private BluetoothGattServer mGattServer = null;

    private Packet packet = null;

    private AdvertiseSettings cfg = new AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
            .build();

    private AdvertiseData data = new AdvertiseData.Builder()
            .addServiceUuid(new ParcelUuid(mU2FGattService.getUuid()))
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
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic chr) {
            int status = BluetoothGatt.GATT_FAILURE;
            byte[] bytes = null;

            if (offset != 0) {
                status = BluetoothGatt.GATT_INVALID_OFFSET;
            } else if (chr.equals(mU2FGattService.controlPointLength)) {
                status = BluetoothGatt.GATT_SUCCESS;
                ByteBuffer bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.BIG_ENDIAN);
                bb.putChar(MTU);
                bytes = bb.array();
            } else if (chr.equals(mU2FGattService.serviceRevisionBitfield)) {
                bytes = VERSION;
            }

            Log.d(getClass().getCanonicalName(), Integer.valueOf(bytes.length).toString());
            mGattServer.sendResponse(device, requestId, status, 0, bytes);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic chr,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            int status = BluetoothGatt.GATT_FAILURE;

            if (offset != 0) {
                status = BluetoothGatt.GATT_INVALID_OFFSET;
            } else if (chr.equals(mU2FGattService.controlPoint)) {
                status = BluetoothGatt.GATT_SUCCESS;
                Frameable reply = null;

                try {
                    if (packet == null)
                        packet = new Packet(value);
                    else
                        packet.put(value);

                    if (packet.isComplete()) {
                        switch (packet.getCommand()) {
                        case PING:
                            reply = packet;
                            break;
                        case MSG:
                            APDURequest req = new APDURequest(packet.getData());
                            reply = requestHandlers.get(req.ins).handle(req);
                            break;
                        default:
                            reply = ErrorCode.INVALID_CMD;
                            break;
                        }

                        packet = null;
                    }
                } catch (FrameableException e) {
                    packet = null;
                    reply = e;
                }

                for (byte[] frame : reply.toFrames(MTU)) {
                    mU2FGattService.status.setValue(frame);
                    mGattServer.notifyCharacteristicChanged(device, mU2FGattService.status, true);
                }
            } else if (chr.equals(mU2FGattService.serviceRevisionBitfield)) {
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

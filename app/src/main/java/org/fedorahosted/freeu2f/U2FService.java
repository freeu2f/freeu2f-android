package org.fedorahosted.freeu2f;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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

import org.fedorahosted.freeu2f.u2f.APDURequest;
import org.fedorahosted.freeu2f.u2f.ErrorCode;
import org.fedorahosted.freeu2f.u2f.Packet;
import org.fedorahosted.freeu2f.u2f.PacketParser;
import org.fedorahosted.freeu2f.u2f.Packetable;
import org.fedorahosted.freeu2f.u2f.PacketableException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class U2FService extends Service {
    private static Map<APDURequest.Instruction, RequestHandler> requestHandlers = new HashMap<>();
    private static byte[] VERSION = new byte[] { 0b01000000 }; /* VERSION = 1.2, see U2F BT 6.1 */
    private static char OUTPUT_MTU = 512;
    private static char INPUT_MTU = 47; /* Higher values break with Linux/bluez. TODO: Why? */

    static {
        requestHandlers.put(APDURequest.Instruction.AUTHENTICATE, new AuthenticateRequestHandler());
        requestHandlers.put(APDURequest.Instruction.REGISTER, new RegisterRequestHandler());
        requestHandlers.put(APDURequest.Instruction.VERSION, new VersionRequestHandler());
    }

    private U2FGattService mU2FGattService = new U2FGattService();
    private BluetoothLeAdvertiser mBtLeAdvertiser = null;
    private PacketParser mParser = new PacketParser();
    private BluetoothGattServer mGattServer = null;

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
            Log.d("AdvertiseCallback", "Advertising started...");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d("AdvertiseCallback", "Advertising failed!");
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
                bb.putChar(INPUT_MTU);
                bytes = bb.array();
            } else if (chr.equals(mU2FGattService.serviceRevisionBitfield)) {
                bytes = VERSION;
            }

            mGattServer.sendResponse(device, requestId, status, 0, bytes);
        }

        private void dump(String prfx, byte[] value, int off, int len) {
            StringBuilder sb = new StringBuilder();
            for (int i = off; i < len; i++)
                sb.append(String.format("%02X", value[i]));
            Log.d("===========================", String.format("%s (%d): %s", prfx, value.length, sb.toString()));
        }

        private void dump(String prfx, byte[] value) {
            dump(prfx, value, 0, value.length);
        }

        private void reply(BluetoothDevice dev, Packetable pkt) {
            for (byte[] frame : pkt.toPacket().toFrames(OUTPUT_MTU)) {
                dump("Output", frame);
                mU2FGattService.status.setValue(frame);
                mGattServer.notifyCharacteristicChanged(dev, mU2FGattService.status, true);
            }
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

                dump("Input", value);

                try {
                    Packet pkt = mParser.update(value);
                    if (pkt != null) {
                        Log.d("=================================", "Got packet!");
                        switch (pkt.getCommand()) {
                        case PING:
                            reply(device, pkt);
                            break;
                        case MSG:
                            APDURequest req = new APDURequest(pkt.getData());
                            reply(device, requestHandlers.get(req.ins).handle(req));
                            break;
                        default:
                            reply(device, ErrorCode.INVALID_CMD.toPacket());
                            break;
                        }
                    }
                } catch (PacketableException e) {
                    e.printStackTrace();
                    reply(device, e);
                }
            } else if (chr.equals(mU2FGattService.serviceRevisionBitfield)) {
                status = BluetoothGatt.GATT_SUCCESS;
            }

            mGattServer.sendResponse(device, requestId, status, 0, null);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
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

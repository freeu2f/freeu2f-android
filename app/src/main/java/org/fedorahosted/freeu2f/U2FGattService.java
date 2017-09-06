package org.fedorahosted.freeu2f;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class U2FGattService extends BluetoothGattService {
    static BluetoothGattCharacteristic U2F_CONTROL_POINT =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff1-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        );

    static BluetoothGattCharacteristic U2F_STATUS =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff2-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        );

    static BluetoothGattCharacteristic U2F_CONTROL_POINT_LENGTH =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff3-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        );

    static BluetoothGattCharacteristic U2F_SERVICE_REVISION_BITFIELD =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff4-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_READ
                | BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
                | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            );

    static UUID U2F_UUID = UUID.fromString("0000fffd-0000-1000-8000-00805f9b34fb");

    public U2FGattService() {
        super(U2F_UUID, SERVICE_TYPE_PRIMARY);
        addCharacteristic(U2F_CONTROL_POINT);
        addCharacteristic(U2F_STATUS);
        addCharacteristic(U2F_CONTROL_POINT_LENGTH);
        addCharacteristic(U2F_SERVICE_REVISION_BITFIELD);
    }
}

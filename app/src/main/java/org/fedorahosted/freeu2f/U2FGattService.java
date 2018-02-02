package org.fedorahosted.freeu2f;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class U2FGattService extends BluetoothGattService {
    public final BluetoothGattCharacteristic controlPoint =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff1-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        );

    public final BluetoothGattCharacteristic status =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff2-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        );

    public final BluetoothGattCharacteristic controlPointLength =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff3-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        );

    public final BluetoothGattCharacteristic serviceRevisionBitfield =
        new BluetoothGattCharacteristic(
            UUID.fromString("f1d0fff4-deaa-ecee-b42f-c9ba7ed623bb"),
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM |
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        );

    public U2FGattService() {
        super(UUID.fromString("0000fffd-0000-1000-8000-00805f9b34fb"), SERVICE_TYPE_PRIMARY);
        addCharacteristic(controlPoint);
        addCharacteristic(status);
        addCharacteristic(controlPointLength);
        addCharacteristic(serviceRevisionBitfield);
    }
}

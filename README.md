# FreeUTF for android

Server-side component to authorize U2F-over-bluetooth

### Notes on Running

AVD's (Android Virtual Devices) do not implement bluetooth stack, 
therefore this cannot be run/debugged using that. 

Instead you'll need to run/debug against a physical Android device.

### Related Reading

- [Android Bluetooth architecture overview](https://source.android.com/devices/bluetooth/)
- [FIDO Alliance Bluetooth Specification v.1.0](https://fidoalliance.org/specs/fido-u2f-v1.2-ps-20170411/fido-u2f-bt-protocol-v1.2-ps-20170411.pdf) (April 2017 rev)
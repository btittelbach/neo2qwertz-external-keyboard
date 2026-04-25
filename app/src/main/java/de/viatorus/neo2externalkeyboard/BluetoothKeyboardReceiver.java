package de.viatorus.neo2externalkeyboard;

import android.Manifest;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * Listens for {@link BluetoothDevice#ACTION_ACL_CONNECTED} broadcasts and posts the shared
 * IME switcher notification when a Bluetooth keyboard is connected. Tapping it opens the
 * system input method picker so the user can switch to this IME quickly, even if the IME
 * is not yet active (in which case the IME service hasn't posted the notification itself).
 */
public class BluetoothKeyboardReceiver extends BroadcastReceiver {
    private static final String TAG = "BtKeyboardReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive action=" + (intent == null ? "null" : intent.getAction()));
        if (intent == null || !BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
            return;
        }

        // On API 31+ we need BLUETOOTH_CONNECT to inspect the device. Bail out quietly if not
        // granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "BLUETOOTH_CONNECT permission not granted, ignoring.");
            return;
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            return;
        }

        if (!isKeyboard(device)) {
            return;
        }

        Log.d(TAG, "Bluetooth keyboard connected, posting IME switcher notification");
        Neo2InputMethodService.postImeSwitcherNotification(context);
    }

    private static boolean isKeyboard(BluetoothDevice device) {
        BluetoothClass btClass;
        try {
            btClass = device.getBluetoothClass();
        } catch (SecurityException se) {
            return false;
        }
        if (btClass == null) {
            return false;
        }
        int deviceClass = btClass.getDeviceClass();
        return deviceClass == BluetoothClass.Device.PERIPHERAL_KEYBOARD
                || deviceClass == BluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING;
    }
}

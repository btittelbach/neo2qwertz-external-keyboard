package de.viatorus.neo2externalkeyboard;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * Listens for {@link BluetoothDevice#ACTION_ACL_CONNECTED} broadcasts and shows a notification
 * when a Bluetooth keyboard is connected. Tapping the notification opens the system input method
 * picker so the user can switch to this IME quickly.
 */
public class BluetoothKeyboardReceiver extends BroadcastReceiver {
    private static final String TAG = "BtKeyboardReceiver";

    private static final String NOTIFICATION_CHANNEL_ID = "neo2_bt_keyboard_connected";
    private static final int NOTIFICATION_ID = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive action=" + (intent == null ? "null" : intent.getAction()));
        if (intent == null || !BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
            return;
        }

        // On API 31+ we need BLUETOOTH_CONNECT to inspect the device, and to post a notification
        // from API 33+ we need POST_NOTIFICATIONS. Bail out quietly if not granted.
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

        String deviceName;
        try {
            deviceName = device.getName();
        } catch (SecurityException se) {
            deviceName = null;
        }
        if (deviceName == null) {
            deviceName = context.getString(R.string.bt_keyboard_default_name);
        }

        showNotification(context, deviceName);
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

    private void showNotification(Context context, String deviceName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "POST_NOTIFICATIONS not granted, cannot show BT notification");
            return;
        }

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.bt_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(
                    context.getString(R.string.bt_notification_channel_description));
            nm.createNotificationChannel(channel);
        }

        Intent pickerIntent = new Intent(context, ImePickerReceiver.class);
        pickerIntent.setAction(Neo2InputMethodService.ACTION_SHOW_IME_PICKER);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, pickerIntent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_DEFAULT);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.bt_notification_title))
                .setContentText(
                        context.getString(R.string.bt_notification_text, deviceName))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        nm.notify(NOTIFICATION_ID, notification);
    }
}

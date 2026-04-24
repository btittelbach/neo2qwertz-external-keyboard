package de.viatorus.neo2externalkeyboard;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Minimal launcher activity. Exists for three reasons:
 * <ol>
 *   <li>An app without any launched component is kept in the "stopped" state by Android,
 *       which means manifest-declared broadcast receivers (like
 *       {@link BluetoothKeyboardReceiver}) do not fire. Launching this activity once takes
 *       the app out of that state so ACL_CONNECTED broadcasts are delivered.</li>
 *   <li>On Android 13+ the runtime {@code POST_NOTIFICATIONS} permission is required for any
 *       notification to be shown. We request it here.</li>
 *   <li>Provides quick shortcuts to enable/select this IME.</li>
 * </ol>
 */
public class MainActivity extends Activity {
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, pad);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText(R.string.main_info);
        info.setPadding(0, 0, 0, pad);
        root.addView(info);

        statusView = new TextView(this);
        statusView.setPadding(0, 0, 0, pad);
        root.addView(statusView);

        Button enableIme = new Button(this);
        enableIme.setText(R.string.main_open_ime_settings);
        enableIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
        root.addView(enableIme, matchWidth());

        Button pickIme = new Button(this);
        pickIme.setText(R.string.main_show_ime_picker);
        pickIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        });
        root.addView(pickIme, matchWidth());

        Button notifSettings = new Button(this);
        notifSettings.setText(R.string.main_open_notification_settings);
        notifSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                } else {
                    i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(android.net.Uri.fromParts("package", getPackageName(), null));
                }
                startActivity(i);
            }
        });
        root.addView(notifSettings, matchWidth());

        setContentView(root);

        maybeRequestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private ViewGroup.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS);
            }
        }
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();

        boolean notifOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifOk = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        sb.append(getString(notifOk
                ? R.string.status_notifications_granted
                : R.string.status_notifications_missing));
        sb.append('\n');

        boolean btOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            btOk = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        sb.append(getString(btOk
                ? R.string.status_bluetooth_granted
                : R.string.status_bluetooth_missing));

        statusView.setText(sb.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            // Also request BLUETOOTH_CONNECT so we can inspect the connecting device.
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_POST_NOTIFICATIONS + 1);
        }
        updateStatus();
    }
}

package de.viatorus.neo2externalkeyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;

/**
 * Receives the tap on the ongoing IME notification and shows the system input method picker.
 */
public class ImePickerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Neo2InputMethodService.ACTION_SHOW_IME_PICKER.equals(intent.getAction())) {
            return;
        }

        InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }
}

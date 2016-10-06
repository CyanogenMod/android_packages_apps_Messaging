/*
 * Copyright (C) 2015 The SudaMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.android.messaging.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.android.messaging.datamodel.BugleNotifications;
import com.android.messaging.R;


public class CopyCaptchasReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String captchas = intent.getStringExtra("captchas");
        String conversationId = intent.getStringExtra("conversationId");
        if (captchas != null) {
            ClipboardManager c = (ClipboardManager)
            context.getSystemService(Context.CLIPBOARD_SERVICE);
            c.setText(captchas);
            Toast.makeText(context, context.getString(R.string.code_have_copy), 1000).show();
            //MarkRead
            BugleNotifications.markMessagesAsRead(conversationId);
            BugleNotifications.cancelNotification(context, BugleNotifications.CAPTCHAS_NOTIFICATION_ID);
        }
    }
}


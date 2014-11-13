package com.cyanaogenmod.messaging.quickmessage;

import android.content.Intent;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.android.messaging.R;

public class QuickMessageHelper {

    public static Intent getQuickMessageIntent(Context context, NotificationInfo ni) {
        Intent qmIntent = new Intent();
        qmIntent.setClass(context, QuickMessagePopup.class);
        qmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        qmIntent.putExtra(QuickMessagePopup.SMS_NOTIFICATION_OBJECT_EXTRA, ni);
        qmIntent.putExtra(QuickMessagePopup.QR_SHOW_KEYBOARD_EXTRA, true);

        return qmIntent;
    }

    public static void addQuickMessageAction(Context context, NotificationCompat.Builder builder, NotificationInfo ni) {
        Intent qmIntent = getQuickMessageIntent(context, ni);
        CharSequence qmText = "reply";
        PendingIntent qmPendingIntent = PendingIntent.getActivity(context, 0, qmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_reply, qmText, qmPendingIntent);
    }
}

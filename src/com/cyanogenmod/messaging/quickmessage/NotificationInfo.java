package com.cyanaogenmod.messaging.quickmessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.net.Uri;

public class NotificationInfo implements Parcelable {
    public String mSenderName;
    public String mSenderNumber;
    public String mMessage;
    public long mTimeMillis;
    public String mConversationId;
    public Uri mSenderContactUri;

    public NotificationInfo(String senderName, String senderNumber, Uri senderContactUri,
            String message, long timeMillis, String conversationId) {
        mSenderName = senderName;
        mSenderNumber = senderNumber;
        mSenderContactUri = senderContactUri;
        mMessage = message;
        mTimeMillis = timeMillis;
        mConversationId = conversationId;
    }

    public NotificationInfo(String senderName, String senderNumber, Uri senderContactUri,
            CharSequence message, long timeMillis, String conversationId) {
        this(senderName, senderNumber, senderContactUri,
                message.toString(), timeMillis, conversationId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg0, int arg1) {
        arg0.writeString(mSenderName);
        arg0.writeString(mSenderNumber);
        if (mSenderContactUri == null) {
            arg0.writeInt(0);
        } else {
            arg0.writeInt(1);
            mSenderContactUri.writeToParcel(arg0, arg1);
        }
        arg0.writeString(mMessage);
        arg0.writeLong(mTimeMillis);
        arg0.writeString(mConversationId);
    }

    public NotificationInfo(Parcel in) {
        mSenderName = in.readString();
        mSenderNumber = in.readString();
        if (in.readInt() != 0) {
            mSenderContactUri = Uri.CREATOR.createFromParcel(in);
        }
        mMessage = in.readString();
        mTimeMillis = in.readLong();
        mConversationId = in.readString();
    }

    public static final Parcelable.Creator<NotificationInfo> CREATOR =
            new Parcelable.Creator<NotificationInfo>() {
        public NotificationInfo createFromParcel(Parcel in) {
            return new NotificationInfo(in);
        }

        public NotificationInfo[] newArray(int size) {
            return new NotificationInfo[size];
        }
    };
}

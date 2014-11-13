/*
 * Copyright (C) 2012 The CyanogenMod Project (DvTonder)
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

package com.cyanaogenmod.messaging.quickmessage;

import android.content.Intent;
import android.widget.EditText;
import android.content.Context;
import com.android.messaging.ui.UIIntents;

import android.net.Uri;

public class QuickMessage {
    private String mFromName;
    private String[] mFromNumber;
    private NotificationInfo mContent;
    private String mReplyText;
    private long mTimestamp;
    private EditText mEditText = null;
    private Uri mFromContactUri;

    public QuickMessage(NotificationInfo nInfo) {
        mFromName = nInfo.mSenderName;
        mFromNumber = new String[1];
        mFromNumber[0] = nInfo.mSenderNumber;
        mContent = nInfo;
        mReplyText = "";
        mTimestamp = nInfo.mTimeMillis;
        mFromContactUri = nInfo.mSenderContactUri;
    }

    public void setEditText(EditText object) {
        mEditText = object;
    }

    public EditText getEditText() {
        return mEditText;
    }

    public String getFromName() {
        return mFromName;
    }

    public String[] getFromNumber() {
        return mFromNumber;
    }

    public String getMessageBody() {
        return mContent.mMessage;
    }

    public String getReplyText() {
        return mReplyText;
    }

    public Uri getFromContactUri() {
        return mFromContactUri;
    }

    public void setReplyText(String reply) {
        mReplyText = reply;
    }

    public void saveReplyText() {
        if (mEditText != null) {
            mReplyText = mEditText.getText().toString();
        }
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public Intent getViewIntent(Context context) {
        return UIIntents.get().getIntentForConversationActivity(
                context, getConversationId(), null);
    }

    public String getConversationId() {
        return mContent.mConversationId;
    }

    public Uri getRecipientsUri() {
        if (mFromNumber == null || mFromNumber.length == 0) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for(String number : mFromNumber) {
            if (buf.length() > 0) {
                buf.append(";");
            }
            buf.append(number);
        }
        return Uri.parse("smsto:" + buf.toString());
    }
}

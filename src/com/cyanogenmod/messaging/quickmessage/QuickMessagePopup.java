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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Profile;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.messaging.datamodel.NoConfirmationSmsSendService;
import com.android.messaging.datamodel.action.MarkAsReadAction;

import com.android.internal.telephony.PhoneConstants;
import com.android.messaging.R;
import com.google.android.mms.MmsException;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class QuickMessagePopup extends Activity {
    private static final String LOG_TAG = "QuickMessagePopup";

    private boolean DEBUG = false;

    // Intent bungle fields
    public static final String SMS_NOTIFICATION_OBJECT_EXTRA =
            "com.cyanaogenmod.messaging.NOTIFICATION_OBJECT";
    public static final String QR_SHOW_KEYBOARD_EXTRA =
            "com.cyanaogenmod.messaging.QR_SHOW_KEYBOARD";

    // Templates support
    private static final int DIALOG_TEMPLATE_SELECT        = 1;
    private static final int DIALOG_TEMPLATE_NOT_AVAILABLE = 2;
    private SimpleCursorAdapter mTemplatesCursorAdapter;

    // View items
    private ImageView mQmPagerArrow;
    private TextView mQmMessageCounter;
    private Button mCloseButton;
    private Button mViewButton;

    // General items
    private Drawable mDefaultContactImage;
    private Context mContext;
    private boolean mScreenUnlocked = false;
    private KeyguardManager mKeyguardManager = null;
    private PowerManager mPowerManager;

    // Message list items
    private ArrayList<QuickMessage> mMessageList;
    private QuickMessage mCurrentQm = null;
    private int mCurrentPage = -1; // Set to an invalid index

    // Configuration
    private boolean mCloseClosesAll = false;
    private boolean mWakeAndUnlock = false;

    // Message pager
    private ViewPager mMessagePager;
    private MessagePagerAdapter mPagerAdapter;

    // Options menu items
    private static final int MENU_INSERT_SMILEY = 1;
    private static final int MENU_INSERT_EMOJI = 3;
    private static final int MENU_ADD_TEMPLATE = 2;

    // Smiley and Emoji support
    private AlertDialog mSmileyDialog;
    private AlertDialog mEmojiDialog;
    private View mEmojiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialise the message list and other variables
        mContext = this;
        mMessageList = new ArrayList<QuickMessage>();
        mDefaultContactImage = getResources().getDrawable(R.drawable.ic_contact_picture);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        // Get the preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        // TODO: re-add preferences
        //mCloseClosesAll = prefs.getBoolean(MessagingPreferenceActivity.QM_CLOSE_ALL_ENABLED, false);
        //mWakeAndUnlock = prefs.getBoolean(MessagingPreferenceActivity.QM_LOCKSCREEN_ENABLED, false);

        // Set the window features and layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_quickmessage);

        // Turn on the Options Menu
        invalidateOptionsMenu();

        // Load the views and Parse the intent to show the QuickMessage
        setupViews();
        parseIntent(getIntent().getExtras(), false);
    }

    private void setupViews() {

        // Load the main views
        mQmPagerArrow = (ImageView) findViewById(R.id.pager_arrow);
        mQmMessageCounter = (TextView) findViewById(R.id.message_counter);
        mCloseButton = (Button) findViewById(R.id.button_close);
        mViewButton = (Button) findViewById(R.id.button_view);

        // Set the theme color on the pager arrow
        Resources res = getResources();
        mQmPagerArrow.setBackgroundColor(res.getColor(R.color.quickmessage_body_light_bg));

        // ViewPager Support
        mPagerAdapter = new MessagePagerAdapter();
        mMessagePager = (ViewPager) findViewById(R.id.message_pager);
        mMessagePager.setAdapter(mPagerAdapter);
        mMessagePager.setOnPageChangeListener(mPagerAdapter);

        // Close button
        mCloseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // If not closing all, close the current QM and move on
                int numMessages = mMessageList.size();
                if (mCloseClosesAll || numMessages == 1) {
                    clearNotification(true);
                    finish();
                } else {
                    // Dismiss the keyboard if it is shown
                    QuickMessage qm = mMessageList.get(mCurrentPage);
                    if (qm != null) {
                        dismissKeyboard(qm);

                        if (mCurrentPage < numMessages-1) {
                            showNextMessageWithRemove(qm);
                        } else {
                            showPreviousMessageWithRemove(qm);
                        }
                    }
                }
            }
        });

        // View button
        mViewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // Override the re-lock if the screen was unlocked
                if (mScreenUnlocked) {
                    // Cancel the receiver that will clear the wake locks
                    ClearAllReceiver.removeCancel(getApplicationContext());
                    ClearAllReceiver.clearAll(false);
                    mScreenUnlocked = false;
                }

                // Trigger the view intent
                mCurrentQm = mMessageList.get(mCurrentPage);
                Intent vi = mCurrentQm.getViewIntent(mContext);
                if (vi != null) {
                    mCurrentQm.saveReplyText();
                    vi.putExtra("sms_body", mCurrentQm.getReplyText());

                    startActivity(vi);
                }
                clearNotification(false);
                finish();
            }
        });
    }

    private void parseIntent(Bundle extras, boolean newMessage) {
        if (extras == null) {
            return;
        }

        // Parse the intent and ensure we have a notification object to work with
        NotificationInfo nm = (NotificationInfo) extras.getParcelable(SMS_NOTIFICATION_OBJECT_EXTRA);
        if (nm != null) {
            QuickMessage qm = new QuickMessage(nm);
            mMessageList.add(qm);
            mPagerAdapter.notifyDataSetChanged();

            // If triggered from Quick Reply the keyboard should be visible immediately
            if (extras.getBoolean(QR_SHOW_KEYBOARD_EXTRA, false)) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }

            if (newMessage && mCurrentPage != -1) {
                // There is already a message showing
                // Stay on the current message
                mMessagePager.setCurrentItem(mCurrentPage);
            } else {
                // Set the current message to the last message received
                mCurrentPage = mMessageList.size()-1;
                mMessagePager.setCurrentItem(mCurrentPage);
            }

            if (DEBUG)
                Log.d(LOG_TAG, "parseIntent(): New message from " + qm.getFromName().toString()
                        + " added. Number of messages = " + mMessageList.size()
                        + ". Displaying page #" + (mCurrentPage+1));

            // Make sure the counter is accurate
            updateMessageCounter();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DEBUG)
            Log.d(LOG_TAG, "onNewIntent() called");

        // Set new intent
        setIntent(intent);

        // Load and display the new message
        parseIntent(intent.getExtras(), true);
        unlockScreen();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Unlock the screen if needed
        unlockScreen();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mScreenUnlocked) {
            // Cancel the receiver that will clear the wake locks
            ClearAllReceiver.removeCancel(getApplicationContext());
            ClearAllReceiver.clearAll(true);
        }
    }

    //==========================================================
    // Utility methods
    //==========================================================

    /**
     * This method dismisses the on screen keyboard if it is visible for the supplied qm
     *
     * @param qm - qm to check against
     */
    private void dismissKeyboard(QuickMessage qm) {
        if (qm != null) {
            EditText editView = qm.getEditText();
            if (editView != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editView.getApplicationWindowToken(), 0);
            }
        }
    }

    /**
     * If 'Wake and unlock' is enabled, this method will unlock the screen
     */
    private void unlockScreen() {
        // See if the lock screen should be disabled
        if (!mWakeAndUnlock) {
            return;
        }

        // See if the screen is locked or if no lock set and the screen is off
        // and get the wake lock to turn on the screen.
        boolean isScreenOn = mPowerManager.isScreenOn();
        boolean inKeyguardRestrictedInputMode = mKeyguardManager.inKeyguardRestrictedInputMode();
        if (inKeyguardRestrictedInputMode || ((!inKeyguardRestrictedInputMode) && !isScreenOn)) {
            ManageWakeLock.acquireFull(mContext);
            mScreenUnlocked = true;
        }
    }

    /**
     * Update the page indicator counter to show the currently selected visible page number
     */
    public void updateMessageCounter() {
        String separator = mContext.getString(R.string.message_counter_separator);
        mQmMessageCounter.setText((mCurrentPage + 1) + " " + separator + " " + mMessageList.size());

        if (DEBUG)
            Log.d(LOG_TAG, "updateMessageCounter() called, counter text set to " + (mCurrentPage + 1)
                    + " of " + mMessageList.size());
    }

    /**
     * Remove the supplied qm from the ViewPager and show the previous/older message
     *
     * @param qm
     */
    public void showPreviousMessageWithRemove(QuickMessage qm) {
        if (qm != null) {
            if (DEBUG)
                Log.d(LOG_TAG, "showPreviousMessageWithRemove()");

            markCurrentMessageRead(qm);
            if (mCurrentPage > 0) {
                updatePages(mCurrentPage-1, qm);
            }
        }
    }

    /**
     * Remove the supplied qm from the ViewPager and show the next/newer message
     *
     * @param qm
     */
    public void showNextMessageWithRemove(QuickMessage qm) {
        if (qm != null) {
            if (DEBUG)
                Log.d(LOG_TAG, "showNextMessageWithRemove()");

            markCurrentMessageRead(qm);
            if (mCurrentPage < (mMessageList.size() - 1)) {
                updatePages(mCurrentPage, qm);
            }
        }
    }

    /**
     * Handle qm removal and the move to and display of the appropriate page
     *
     * @param gotoPage - page number to display after the removal
     * @param removeMsg - qm to remove from ViewPager
     */
    private void updatePages(int gotoPage, QuickMessage removeMsg) {
        mMessageList.remove(removeMsg);
        mPagerAdapter.notifyDataSetChanged();
        mMessagePager.setCurrentItem(gotoPage);
        updateMessageCounter();

        if (DEBUG)
            Log.d(LOG_TAG, "updatePages(): Removed message " + removeMsg.getConversationId()
                    + " and changed to page #" + (gotoPage+1) + ". Remaining messages = "
                    + mMessageList.size());
    }

    /**
     * Marks the supplied qm as read
     *
     * @param qm
     */
    private void markCurrentMessageRead(QuickMessage qm) {
        MarkAsReadAction.markAsRead(qm.getConversationId());
    }

    /**
     * Marks all qm's in the message list as read
     */
    private void markAllMessagesRead() {
        // This iterates through our MessageList and marks the contained threads as read
        for (QuickMessage qm : mMessageList) {
            markCurrentMessageRead(qm);
        }
    }

    /**
     * Show the appropriate image for the QuickContact badge
     *
     * @param badge
     * @param addr
     * @param isSelf
     */
    private void updateContactBadge(QuickContactBadge badge, Uri contactUri) {
        // TODO: setImageDrawable.  might not use QuickContactBadge after visual
        // update though
        if (contactUri != null) {
            badge.assignContactUri(contactUri);
        }
    }

    /**
     * Use standard api to send the supplied message
     *
     * @param message - message to send
     * @param qm - qm to reply to (for sender details)
     */
    private void sendQuickMessage(String message, QuickMessage qm) {
        if (message != null && qm != null) {
            Intent sendIntent = new Intent(this, NoConfirmationSmsSendService.class);
            sendIntent.setAction(TelephonyManager.ACTION_RESPOND_VIA_MESSAGE);
            sendIntent.putExtra(Intent.EXTRA_TEXT, message);
            sendIntent.setData(qm.getRecipientsUri());
            startService(sendIntent);
            Toast.makeText(mContext, R.string.toast_sending_message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clears the status bar notification and, optionally, mark all messages as read
     * This is used to clean up when we are done with all qm's
     *
     * @param markAsRead - should remaining qm's be maked as read?
     */
    private void clearNotification(boolean markAsRead) {
        // Dismiss the notification that brought us here.
        NotificationManager notificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        // TODO: cancel all notifications.  this is only relevant when we allow multiple
        // quick messages at once.
        //notificationManager.cancel(MessagingNotification.FULL_NOTIFICATION_ID);

        // Mark all contained conversations as seen
        if (markAsRead) {
            markAllMessagesRead();
        }

        // Clear the messages list
        mMessageList.clear();

        if (DEBUG)
            Log.d(LOG_TAG, "clearNotification(): Message list cleared. Size = " + mMessageList.size());
    }

    //==========================================================
    // Inner classes
    //==========================================================

    /**
     * Class copied from ComposeMessageActivity.java
     * InputFilter which attempts to substitute characters that cannot be
     * encoded in the limited GSM 03.38 character set. In many cases this will
     * prevent the keyboards auto-correction feature from inserting characters
     * that would switch the message from 7-bit GSM encoding (160 char limit)
     * to 16-bit Unicode encoding (70 char limit).
     */
    private class StripUnicode implements InputFilter {

        private CharsetEncoder gsm =
            Charset.forName("gsm-03.38-2000").newEncoder();

        private Pattern diacritics =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}");

        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {

            Boolean unfiltered = true;
            StringBuilder output = new StringBuilder(end - start);

            for (int i = start; i < end; i++) {
                char c = source.charAt(i);

                // Character is encodable by GSM, skip filtering
                if (gsm.canEncode(c)) {
                    output.append(c);
                }
                // Character requires Unicode, try to replace it
                else {
                    unfiltered = false;
                    String s = String.valueOf(c);

                    // Try normalizing the character into Unicode NFKD form and
                    // stripping out diacritic mark characters.
                    s = Normalizer.normalize(s, Normalizer.Form.NFKD);
                    s = diacritics.matcher(s).replaceAll("");

                    // Special case characters that don't get stripped by the
                    // above technique.
                    s = s.replace("Œ", "OE");
                    s = s.replace("œ", "oe");
                    s = s.replace("Ł", "L");
                    s = s.replace("ł", "l");
                    s = s.replace("Đ", "DJ");
                    s = s.replace("đ", "dj");
                    s = s.replace("Α", "A");
                    s = s.replace("Β", "B");
                    s = s.replace("Ε", "E");
                    s = s.replace("Ζ", "Z");
                    s = s.replace("Η", "H");
                    s = s.replace("Ι", "I");
                    s = s.replace("Κ", "K");
                    s = s.replace("Μ", "M");
                    s = s.replace("Ν", "N");
                    s = s.replace("Ο", "O");
                    s = s.replace("Ρ", "P");
                    s = s.replace("Τ", "T");
                    s = s.replace("Υ", "Y");
                    s = s.replace("Χ", "X");
                    s = s.replace("α", "A");
                    s = s.replace("β", "B");
                    s = s.replace("γ", "Γ");
                    s = s.replace("δ", "Δ");
                    s = s.replace("ε", "E");
                    s = s.replace("ζ", "Z");
                    s = s.replace("η", "H");
                    s = s.replace("θ", "Θ");
                    s = s.replace("ι", "I");
                    s = s.replace("κ", "K");
                    s = s.replace("λ", "Λ");
                    s = s.replace("μ", "M");
                    s = s.replace("ν", "N");
                    s = s.replace("ξ", "Ξ");
                    s = s.replace("ο", "O");
                    s = s.replace("π", "Π");
                    s = s.replace("ρ", "P");
                    s = s.replace("σ", "Σ");
                    s = s.replace("τ", "T");
                    s = s.replace("υ", "Y");
                    s = s.replace("φ", "Φ");
                    s = s.replace("χ", "X");
                    s = s.replace("ψ", "Ψ");
                    s = s.replace("ω", "Ω");
                    s = s.replace("ς", "Σ");

                    output.append(s);
                }
            }

            // No changes were attempted, so don't return anything
            if (unfiltered) {
                return null;
            }
            // Source is a spanned string, so copy the spans from it
            else if (source instanceof Spanned) {
                SpannableString spannedoutput = new SpannableString(output);
                TextUtils.copySpansFrom(
                    (Spanned) source, start, end, null, spannedoutput, 0);

                return spannedoutput;
            }
            // Source is a vanilla charsequence, so return output as-is
            else {
                return output;
            }
        }
    }

    /**
     * Message Pager class, used to display and navigate through the ViewPager pages
     */
    private class MessagePagerAdapter extends PagerAdapter
                    implements ViewPager.OnPageChangeListener {

        protected LinearLayout mCurrentPrimaryLayout = null;

        @Override
        public int getCount() {
            return mMessageList.size();
        }

        @Override
        public Object instantiateItem(View collection, int position) {

            // Load the layout to be used
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout;
            layout = inflater.inflate(R.layout.quickmessage_content_light, null);

            // Load the main views
            EditText qmReplyText = (EditText) layout.findViewById(R.id.embedded_text_editor);
            TextView qmTextCounter = (TextView) layout.findViewById(R.id.text_counter);
            ImageButton qmSendButton = (ImageButton) layout.findViewById(R.id.send_button_sms);
            TextView qmMessageText = (TextView) layout.findViewById(R.id.messageTextView);
            TextView qmFromName = (TextView) layout.findViewById(R.id.fromTextView);
            TextView qmTimestamp = (TextView) layout.findViewById(R.id.timestampTextView);
            QuickContactBadge qmContactBadge = (QuickContactBadge) layout.findViewById(R.id.contactBadge);

            // Retrieve the current message
            QuickMessage qm = mMessageList.get(position);
            if (qm != null) {
                if (DEBUG)
                    Log.d(LOG_TAG, "instantiateItem(): Creating page #" + (position + 1) + " for message from "
                            + qm.getFromName() + ". Number of pages to create = " + getCount());

                // Set the general fields
                qmFromName.setText(qm.getFromName());
                qmTimestamp.setText(formatTimeStampString(QuickMessagePopup.this,
                        qm.getTimestamp(), false));
                updateContactBadge(qmContactBadge, qm.getFromContactUri());
                qmMessageText.setText(qm.getMessageBody());
                qmReplyText.setBackgroundResource(R.drawable.edit_text_holo_light);

                // Set the remaining values
                qmReplyText.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                qmReplyText.setText(qm.getReplyText());
                qmReplyText.setSelection(qm.getReplyText().length());
                qmReplyText.addTextChangedListener(new QmTextWatcher(mContext, qmTextCounter,
                        qmSendButton));
                qmReplyText.setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (event != null) {
                            // event != null means enter key pressed
                            if (!event.isShiftPressed()) {
                                // if shift is not pressed then move focus to send button
                                if (v != null) {
                                    View focusableView = v.focusSearch(View.FOCUS_RIGHT);
                                    if (focusableView != null) {
                                        focusableView.requestFocus();
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
                        if (actionId == EditorInfo.IME_ACTION_SEND) {
                            if (v != null) {
                                QuickMessage qm = mMessageList.get(mCurrentPage);
                                if (qm != null) {
                                    sendMessageAndMoveOn(v.getText().toString(), qm);
                                }
                            }
                            return true;
                        }
                        return true;
                    }
                });

                LengthFilter lengthFilter = new LengthFilter(2000);

                qmReplyText.setFilters(new InputFilter[] { lengthFilter });

                QmTextWatcher.getQuickReplyCounterText(qmReplyText.getText().toString(),
                        qmTextCounter, qmSendButton);

                // Add the context menu
                registerForContextMenu(qmReplyText);

                // Store the EditText object for future use
                qm.setEditText(qmReplyText);

                // Send button
                qmSendButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QuickMessage qm = mMessageList.get(mCurrentPage);
                        if (qm != null) {
                            EditText editView = qm.getEditText();
                            if (editView != null) {
                                sendMessageAndMoveOn(editView.getText().toString(), qm);
                            }
                        }
                    }
                });

                // Add the layout to the viewpager
                ((ViewPager) collection).addView(layout);
            }
            return layout;
        }

        /**
         * This method sends the supplied message in reply to the supplied qm and then
         * moves to the next or previous message as appropriate. If this is the last qm
         * in the MessageList, we end by clearing the notification and calling finish()
         *
         * @param message - message to send
         * @param qm - qm we are replying to (for sender details)
         */
        private void sendMessageAndMoveOn(String message, QuickMessage qm) {
            sendQuickMessage(message, qm);
            // Close the current QM and move on
            int numMessages = mMessageList.size();
            if (numMessages == 1) {
                // No more messages
                clearNotification(true);
                finish();
            } else {
                // Dismiss the keyboard if it is shown
                dismissKeyboard(qm);

                if (mCurrentPage < numMessages-1) {
                    showNextMessageWithRemove(qm);
                } else {
                    showPreviousMessageWithRemove(qm);
                }
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            LinearLayout view = ((LinearLayout)object);
            if (view != mCurrentPrimaryLayout) {
                mCurrentPrimaryLayout = view;
            }
        }

        @Override
        public void onPageSelected(int position) {
            // The user had scrolled to a new message
            if (mCurrentQm != null) {
                mCurrentQm.saveReplyText();
            }

            // Set the new 'active' QuickMessage
            mCurrentPage = position;
            mCurrentQm = mMessageList.get(position);

            if (DEBUG)
                Log.d(LOG_TAG, "onPageSelected(): Current page is #" + (position+1)
                        + " of " + getCount() + " pages. Currenty visible message is from "
                        + mCurrentQm.getFromName());

            updateMessageCounter();
        }

        @Override
        public int getItemPosition(Object object) {
            // This is needed to force notifyDatasetChanged() to rebuild the pages
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((LinearLayout) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
                return view == ((LinearLayout)object);
        }

        @Override
        public void finishUpdate(View arg0) {}

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {}

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {}

        @Override
        public void onPageScrollStateChanged(int arg0) {}

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {}
   }

   public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return DateUtils.formatDateTime(context, when, format_flags);
    }
}

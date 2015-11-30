package com.android.messaging;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.cyanogen.lookup.phonenumber.LookupHandlerThread;
import com.cyanogen.lookup.phonenumber.request.LookupRequest;
import com.cyanogen.lookup.phonenumber.response.LookupResponse;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 *      Class intended for handling communication and interaction between the lookup provider and
 *      the application
 * </pre>
 *
 * @see {@link Application.ActivityLifecycleCallbacks}
 * @see {@link LookupRequest.Callback}
 * @see {@link ILookupClient}
 */
/* package */ class LookupProviderManager implements Application.ActivityLifecycleCallbacks,
        LookupRequest.Callback, ILookupClient {

    private static final String TAG = "LookupProviderManager";
    private static final String THREAD_NAME = "PhoneLookupProviderThread";

    // Members
    private Application mApplication;
    private ConcurrentHashMap<String, LookupResponse> mPhoneNumberLookupCache;
    private ConcurrentHashMap<String, LookupProviderListener> mLookupListeners;
    private LookupHandlerThread mLookupHandlerThread;
    private boolean mIsPhoneNumberLookupInitialized;
    private short mActivityCount = 0;

    /**
     * Constructor
     *
     * @param application {@link Application}
     */
    /* package */ LookupProviderManager(Application application) {
        log("LookupProviderManager(" + application + ")");
        if (application == null) {
            throw new IllegalArgumentException("'application' must not be null!");
        }
        mPhoneNumberLookupCache = new ConcurrentHashMap<String, LookupResponse>();
        mLookupListeners = new ConcurrentHashMap<String, LookupProviderListener>();
        application.registerActivityLifecycleCallbacks(this);
        mApplication = application;
    }

    private boolean isDebug() {
        return true || Log.isLoggable(TAG, Log.DEBUG);
    }

    private void log(String str) {
        if (isDebug()) {
            Log.d(TAG, "::" + str);
        }
    }

    private boolean start() {
        log("start()");
        if (mLookupHandlerThread == null) {
            mLookupHandlerThread = new LookupHandlerThread(THREAD_NAME, mApplication);
            mLookupHandlerThread.start();
        }
        return mLookupHandlerThread.isAlive();
    }

    private void stop() {
        log("stop()");
        if (mLookupHandlerThread != null) {
            mLookupHandlerThread.tearDown();
            mLookupHandlerThread = null;
        }
        mPhoneNumberLookupCache.clear();
        mLookupListeners.clear();
        mIsPhoneNumberLookupInitialized = false;
    }

    /**
     * Registration mechanism for anyone interested in new contact info being available from an
     * external provider.  The updates aren't granular as of now - you will be notified of
     * updates to all contact info request
     *
     * @param number {@link String}
     * @param listener {@link LookupProviderListener}
     */
    public void addLookupProviderListener(String number, LookupProviderListener listener) {
        log("addLookupProviderListener(" + number + ", " + listener + ")");
        if (TextUtils.isEmpty(number) || listener == null) {
            throw new IllegalArgumentException("'number' and 'listener' must be not null and not "
                    + "empty");
        }
        mLookupListeners.put(number, listener);
    }

    /**
     * Stop getting updates about newly added contact info
     *
     * @param number {@link String}
     */
    public void removeLookupProviderListener(String number) {
        log("removeLookupProviderListener(" + number + ")");
        if (TextUtils.isEmpty(number)) {
            throw new IllegalArgumentException("'number' cannot be null or empty!");
        }
        mLookupListeners.remove(number);
    }

    /* package */ void onLowMemory() {
        log("onLowMemory()");
        mPhoneNumberLookupCache.clear();
    }

    /* ---------------------  LookupRequest callback interfaces  --------------------------------*/
    @Override
    public void onNewInfo(LookupRequest lookupRequest, LookupResponse response) {
        log("onNewInfo(" + lookupRequest + ", " + response + ")");
        mPhoneNumberLookupCache.put(lookupRequest.mPhoneNumber, response);
        if (mLookupListeners.containsKey(lookupRequest.mPhoneNumber)) {
            mLookupListeners.get(lookupRequest.mPhoneNumber).onNewInfoAvailable(response);
        }
    }

    /* ---------------------  Activity callback interfaces  -------------------------------------*/
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        log("onActivityCreated(" + activity + ", " + savedInstanceState + ")");
        ++mActivityCount;
        if (mActivityCount == 1) {
            mIsPhoneNumberLookupInitialized = start();
        }
    }

    /* ---------------------  Lookup Client interfaces  -----------------------------------------*/
    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        log("onActivityCreated(" + activity + ")");
        --mActivityCount;
        if (mActivityCount == 0) {
            if (mIsPhoneNumberLookupInitialized) {
                stop();
                mIsPhoneNumberLookupInitialized = false;
            }
        }
    }

    @Override
    public void lookupInfoForPhoneNumber(String phoneNumber) {
        log("lookupInfoForPhoneNumber(" + phoneNumber + ")");
        if (TextUtils.isEmpty(phoneNumber)) {
            throw new IllegalArgumentException("'phoneNumber' cannot be null!");
        }
        phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber,
                mApplication.getResources().getConfiguration().locale.getISO3Country());
        lookupInfoForPhoneNumberE164(phoneNumber);
    }

    @Override
    public void lookupInfoForPhoneNumberE164(String phoneNumber) {
        log("lookupInfoForPhoneNumberE164(" + phoneNumber + ")");
        lookupInfoForPhoneNumberE164(phoneNumber, false);
    }

    @Override
    public void lookupInfoForPhoneNumberE164(String phoneNumber, boolean requery) {
        log("lookupInfoForPhoneNumberE164(" + phoneNumber + ", " + requery + ")");
        if (TextUtils.isEmpty(phoneNumber)) {
            throw new IllegalArgumentException("'phoneNumber' cannot be null!");
        }

        // If we have already fetched it before
        if (mPhoneNumberLookupCache.containsKey(phoneNumber)) {
            // Short circuit and call callback
            // [TODO][MSB]: Post to ui handler?
            if (mLookupListeners.containsKey(phoneNumber)) {
                mLookupListeners.get(phoneNumber).onNewInfoAvailable(
                        mPhoneNumberLookupCache.get(phoneNumber));
            }
        }

        if (mIsPhoneNumberLookupInitialized) {
            LookupRequest request = new LookupRequest(phoneNumber, this);
            mLookupHandlerThread.fetchInfoForPhoneNumber(request);
        }

    }

    @Override
    public void markAsSpam(String phoneNumber) {
        log("markAsSpam(" + phoneNumber + ")");
        if (TextUtils.isEmpty(phoneNumber)) {
            throw new IllegalArgumentException("'phoneNumber' cannot be null!");
        }
        phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber,
                mApplication.getResources().getConfiguration().locale.getISO3Country());
        markAsSpamE164(phoneNumber);
    }

    @Override
    public void markAsSpamE164(String phoneNumber) {
        log("markAsSpamE164(" + phoneNumber + ")");
        if (TextUtils.isEmpty(phoneNumber)) {
            throw new IllegalArgumentException("'phoneNumber' cannot be null!");
        }
        if (mIsPhoneNumberLookupInitialized) {
            mLookupHandlerThread.markAsSpam(phoneNumber);
            // [TODO][MSB]: Remove from cache ?
        }
    }

    @Override
    public boolean hasSpamReporting() {
        log("hasSpamReporting()");
        return mIsPhoneNumberLookupInitialized && mLookupHandlerThread.hasSpamReporting();
    }

    @Override
    public String getProviderName() {
        log("getProviderName()");
        if (mIsPhoneNumberLookupInitialized) {
            return mLookupHandlerThread.getProviderName();
        }
        return null;
    }

    /* ---------------------  Interface  --------------------------------------------------------*/
    /**
     * Callback for clients requesting phone number lookups
     */
    public interface LookupProviderListener {
        // generic callback for when new info is available

        /**
         * Tell listener to handle update
         *
         * @param response {@link LookupResponse}
         */
        void onNewInfoAvailable(LookupResponse response);
    }

}

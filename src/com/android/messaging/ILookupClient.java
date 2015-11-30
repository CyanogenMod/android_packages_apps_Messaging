package com.android.messaging;

/**
 * <pre>
 *      Client interface for talking to lookup provider
 * </pre>
 */
public interface ILookupClient {

    /**
     * Will call for lookup
     *
     * Will automatically format to E164
     *
     * @param phoneNumber {@link String} not null or empty
     */
    void lookupInfoForPhoneNumber(String phoneNumber);

    /**
     * Will call for lookup without requery
     *
     * @param phoneNumber {@link String} not null or empty
     */
    void lookupInfoForPhoneNumberE164(String phoneNumber);

    /**
     * Will call for lookup and allow requery of possibly stale data
     *
     * @param phoneNumber {@link String} not null or empty
     * @param requery {@link Boolean}
     */
    void lookupInfoForPhoneNumberE164(String phoneNumber, boolean requery);

    /**
     * Will mark number as spam
     *
     * This will automatically format to E164
     *
     * @param phoneNumber {@link String} not null and not empty
     */
    void markAsSpam(String phoneNumber);

    /**
     * Will mark as spam and expect your arg to be formatted E164
     *
     * @param phoneNumber {@link String} not null and not empty
     */
    void markAsSpamE164(String phoneNumber);

    /**
     * Check if spam reporting is available
     *
     * @return {@link Boolean}
     */
    boolean hasSpamReporting();

    /**
     * Get the display name of the provider
     *
     * @return {@link String} or null
     */
    String getProviderName();

}

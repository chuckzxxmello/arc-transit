package com.transit.arctransit.auth.domain;

/**
 * Administrative authentication state of a staff account.
 */
public enum AccountStatus {

    /**
     * The account may authenticate when it is not archived.
     */
    ACTIVE,

    /**
     * The account has been administratively disabled.
     */
    DISABLED,

    /**
     * The account is enabled but temporarily or manually locked.
     */
    LOCKED
}
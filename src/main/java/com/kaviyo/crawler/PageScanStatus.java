package com.kaviyo.crawler;

/** Describes what Kaviyo observed while fetching one merchant-selected URL. */
public enum PageScanStatus {
    FETCHED,
    ROBOTS_BLOCKED,
    ROBOTS_UNAVAILABLE,
    DESTINATION_BLOCKED,
    NOT_FOUND,
    HTTP_ERROR,
    UNSUPPORTED_CONTENT_TYPE,
    RESPONSE_TOO_LARGE,
    REDIRECT_LIMIT,
    CROSS_ORIGIN_REDIRECT,
    TIMEOUT,
    FETCH_FAILED
}

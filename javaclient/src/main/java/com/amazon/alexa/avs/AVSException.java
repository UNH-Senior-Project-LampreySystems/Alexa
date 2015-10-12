/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

@SuppressWarnings("serial")
public class AVSException extends Exception {

    public AVSException() {
    }

    public AVSException(String message) {
        super(message);
    }

    public AVSException(Throwable cause) {
        super(cause);
    }

    public AVSException(String message, Throwable cause) {
        super(message, cause);
    }

    public AVSException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

public class AVSPlayItem {
    public String mUrl;
    public String mStreamId;
    public int mStartOffset;
    public int mProgressStartOffset;
    public int mProgressInterval;

    public AVSPlayItem(String url, String streamId, int startOffset, int progressStartOffset,
            int progressInterval) {
        mUrl = url;
        mStreamId = streamId;
        mStartOffset = (startOffset < 0) ? 0 : startOffset;
        mProgressStartOffset = progressStartOffset;
        mProgressInterval = progressInterval;
    }
}

/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.io.ByteArrayInputStream;

public class AVSSpeakItem {
    public String mCid;
    public ByteArrayInputStream mAudio;

    public AVSSpeakItem(String cid, ByteArrayInputStream audio) {
        mCid = cid;
        mAudio = audio;
    }
}

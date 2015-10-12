/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import javax.json.JsonReader;

public class AVSResponse {
    private final JsonReader mJson;
    // map of cids and the audio data returned from AVS
    private final HashMap<String, ByteArrayInputStream> mAudio;

    public AVSResponse(JsonReader inJson) {
        mJson = inJson;
        mAudio = new HashMap<String, ByteArrayInputStream>();
    }

    public void addAudio(String cid, ByteArrayInputStream data) {
        mAudio.put(cid, data);
    }

    public JsonReader getJson() {
        return mJson;
    }

    public HashMap<String, ByteArrayInputStream> getAudio() {
        return mAudio;
    }
}

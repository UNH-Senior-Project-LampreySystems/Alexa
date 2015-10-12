/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class AVSController implements RecordingStateListener {
    private final AVSListenHandler mListenHandler;
    private final AudioCapture mMicrophone;
    private final AVSClient mAVSClient;
    private String mBearerToken;
    public String mMetadata;
    private boolean mFeedbackMode = false; // give audio feedback during processing
    private boolean mEventRunning = false; // is an event currently being sent
    private static String mUrl = "https://access-alexa-na.amazon.com";
    private static final String START_SOUND = "res/start.mp3";
    private static final String END_SOUND = "res/stop.mp3";
    private static final String ERROR_SOUND = "res/error.mp3";
    private static final String PROCESSING_SOUND = "res/processing.mp3";

    private String mNavigationToken;
    private final AVSAudioPlayer mPlayer;

    public AVSController(AVSListenHandler listenHandler) {
        mListenHandler = listenHandler;
        mMicrophone = AudioCapture.getAudioHardware();
        mAVSClient = new AVSClient();
        mPlayer = new AVSAudioPlayer(this);
        mNavigationToken = "";
        // ensure we notify AVS of playbackInterrupted on app exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                mPlayer.stop();
            }
        });
    }

    public void setBearerToken(String token) {
        mBearerToken = token;
    }

    // whether to give audio feedback during processing
    public void setAudioFeedbackMode(boolean afMode) {
        mFeedbackMode = afMode;
    }

    public static void setURL(String newUrl) {
        mUrl = newUrl;
    }

    public static String getURL() {
        return mUrl;
    }

    // start the recording process and send to server
    // takes an optional RMS callback
    // after stopRecording is called, will continue to finish the request and play response
    public void startRecording(RecordingRMSListener rmsListener) throws Exception {
        try {
            // set listening to stop all speech and duck media
            mPlayer.alexaListening();
            mMicrophone.captureAudio(
                    mAVSClient.startRequest(mUrl, mBearerToken, generateSpeechMetadata()),
                    mAVSClient.mAudioFormat, this, rmsListener);
            handleResponse(mAVSClient.finishRequest());
        } catch (Exception e) {
            mPlayer.playMp3FromResource(ERROR_SOUND);
            throw e;
        } finally {
            // set done listening to allow speech and unduck media
            mPlayer.alexaDoneListening();
        }
    }

    public void requestNextItem() {
        if (mNavigationToken == null || mNavigationToken.isEmpty()) {
            mNavigationToken = "";
            return;
        }

        try {
            handleNextItemResponse(mAVSClient.sendRequest(mUrl + AVSClient.NEXTITEM, mBearerToken,
                    generateNextItemMetadata()));
        } catch (Exception e) {
            mNavigationToken = "";
            e.printStackTrace();
        }
    }

    public void sendEvent(String name, String streamId, int offset, String activity) {
        mEventRunning = true;
        try {
            handleResponse(mAVSClient.sendRequest(mUrl + AVSClient.EVENTS + name, mBearerToken,
                    generateEventMetadata(streamId, offset, activity)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mEventRunning = false;
    }

    public boolean eventRunning() {
        return mEventRunning;
    }

    public void sendError(String streamId, int offset, String activity, String errorType,
            String errorMessage) {
        try {
            handleResponse(mAVSClient.sendRequest(mUrl + AVSClient.EVENTS + "playbackError",
                    mBearerToken,
                    generateErrorMetadata(streamId, offset, activity, errorType, errorMessage)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleNextItemResponse(AVSResponse response) {
        if (response == null) {
            mNavigationToken = "";
            return;
        }

        JsonObject topObj = response.getJson().readObject();
        JsonObject body = topObj.getJsonObject("messageBody");

        try {
            mNavigationToken = body.getString("navigationToken");
        } catch (Exception e) {
            mNavigationToken = "";
        }

        handleMedia(response, body.getJsonObject("audioItem"));
    }

    private void handleMedia(AVSResponse response, JsonObject media) {
        JsonArray streams = media.getJsonArray("streams");
        for (int i = 0; i < streams.size(); i++) {
            JsonObject stream = streams.getJsonObject(i);
            String strUrl = stream.getString("streamUrl");
            String streamId = stream.getString("streamId");
            System.out.println("URL: " + strUrl);
            System.out.println("StreamId: " + streamId);
            int offset = 0;
            int progressDelay = 0;
            int progressInterval = 0;
            try {
                // no offset means start at beginning
                offset = stream.getInt("offsetInMilliseconds");
            } catch (Exception e) {
            }
            System.out.println("Offset: " + offset);
            if (stream.getBoolean("progressReportRequired")) {
                JsonObject progressReport = stream.getJsonObject("progressReport");
                progressDelay = progressReport.getInt("progressReportDelayInMilliseconds");
                progressInterval = progressReport.getInt("progressReportIntervalInMilliseconds");
                System.out.println("ProgressDelay: " + progressDelay);
                System.out.println("ProgressInterval: " + progressInterval);
            }

            // if it starts with "cid:", the audio is included in the response as a part
            if (strUrl.startsWith("cid:")) {
                try {
                    File tmp = File.createTempFile(UUID.randomUUID().toString(), ".mp3");
                    Files.copy(response.getAudio().get("<" + strUrl.substring(4) + ">"),
                            tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    mPlayer.add(new AVSPlayItem(tmp.getAbsolutePath(), streamId, offset,
                            progressDelay, progressInterval));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                mPlayer.add(new AVSPlayItem(strUrl, streamId, offset, progressDelay,
                        progressInterval));
            }
        }
    }

    public void handleResponse(AVSResponse response) throws Exception {
        if (response == null) {
            return;
        }

        JsonObject topObj = response.getJson().readObject();
        JsonObject body = topObj.getJsonObject("messageBody");

        JsonArray directives = body.getJsonArray("directives");
        for (JsonObject directive : directives.getValuesAs(JsonObject.class)) {
            String name = directive.getString("name");
            System.out.println("Directive: " + name);

            if (name.equals("speak")) {
                JsonObject payload = directive.getJsonObject("payload");
                String cid = payload.getString("audioContent");
                cid = "<" + cid.substring(4) + ">";
                System.out.println("CID: " + cid);

                mPlayer.speak(new AVSSpeakItem(cid, response.getAudio().get(cid)));

            } else if (name.equals("play")) {
                JsonObject payload = directive.getJsonObject("payload");
                try {
                    mNavigationToken = payload.getString("navigationToken");
                    System.out.println("Navigation Token: " + mNavigationToken);
                } catch (Exception e) {
                    mNavigationToken = null;
                }

                if (payload.getString("playBehavior").equals("REPLACE_PREVIOUS")) {
                    mPlayer.clearAll();
                }

                handleMedia(response, payload.getJsonObject("audioItem"));

            } else if (name.equals("stop")) {
                mPlayer.stop();
            } else if (name.equals("clearQueue")) {
                JsonObject payload = directive.getJsonObject("payload");
                mNavigationToken = null;
                if (payload.getString("clearBehavior").equals("CLEAR_ALL")) {
                    mPlayer.clearAll();
                } else {
                    mPlayer.clearQueue();
                }
            } else if (name.equals("listen")) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        mListenHandler.startListening();
                    }
                };
                thread.start();
            }

        }
    }

    // stop the recording process
    public void stopRecording() {
        mMicrophone.stopCapture();
    }

    // audio state callback for when recording has started
    @Override
    public void recordingStarted() {
        mPlayer.playMp3FromResource(START_SOUND);
    }

    // audio state callback for when recording has completed
    @Override
    public void recordingCompleted() {
        if (mFeedbackMode) {
            mPlayer.playMp3FromResource(PROCESSING_SOUND);
        } else {
            mPlayer.playMp3FromResource(END_SOUND);
        }
    }

    private String generateEventMetadata(String streamId, int offset, String activity) {
        return metadataStart() + "\"playbackState\":{\"streamId\":\"" + streamId
                + "\", \"offsetInMilliseconds\":\"" + offset + "\", \"playerActivity\":\""
                + activity + "\"}" + metadataEnd();
    }

    private String generateErrorMetadata(String streamId, int offset, String activity,
            String errorType, String errorMessage) {
        return metadataStart() + "\"errorType\":\"" + errorType + "\", \"errorMessage\":\""
                + errorMessage + "\", \"streamId\":\"" + streamId + "\", \"playbackState\":{"
                + mPlayer.getPlaybackState() + "}" + metadataEnd();
    }

    private String generateSpeechMetadata() {
        return metadataSpeechStart()
                + "\"profile\":\"alexa-close-talk\", \"locale\":\"en-us\", \"format\":\"audio/L16; rate=16000; channels=1\""
                + metadataEnd();
    }

    private String metadataStart() {
        return "{\"messageHeader\":{}, \"messageBody\":{";

    }

    private String metadataSpeechStart() {
        return "{\"messageHeader\":{\"deviceContext\":[{\"name\":\"playbackState\", \"namespace\":\"AudioPlayer\", \"payload\":{"
                + mPlayer.getPlaybackState() + "}}]}, \"messageBody\":{";
    }

    private String metadataEnd() {
        return "}}";
    }

    private String generateNextItemMetadata() {
        return metadataStart() + "\"navigationToken\":\"" + mNavigationToken + "\"" + metadataEnd();
    }

    public boolean isSpeaking() {
        return mPlayer.isSpeaking();
    }

}

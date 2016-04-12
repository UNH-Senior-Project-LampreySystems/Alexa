/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.*;

public class AudioCapture {
    private static AudioCapture sAudioCapture;
    private final TargetDataLine mInput; // microphone line to use
    private boolean mCapturing; // are we currently recording audio?

    public static AudioCapture getAudioHardware() { // get the singleton instance
        if (sAudioCapture == null) {
            sAudioCapture = new AudioCapture();
        }
        return sAudioCapture;
    }

    private AudioCapture() {
        super();
        mInput = getDefaultMicrophone();
    }

    // start capturing audio
    // takes an OutputStream to write the audio out to
    // takes an optional RecordingStateListener to callback when recording has started and stopped
    // takes an optional RecordingRMSListener to callback with the RMS value during recording
    public void captureAudio(final OutputStream audioStream, final AudioFormat audioFormat,
            final RecordingStateListener stateListener, final RecordingRMSListener rmsListener)
            throws LineUnavailableException, IOException {
        try {

            mCapturing = true; // set in-capturing to true

            mInput.open(audioFormat);
            mInput.start();

            if (stateListener != null) {
                stateListener.recordingStarted();
            }

            // 25ms of audio based on 16-bit sample size, 16K sample rate, single channel
            byte tempBuffer[] = new byte[800];
            while (true) {
                if (!mCapturing && mInput.available() <= 0) {
                    // if we are done capturing and there is no more audio to pull
                    break;
                }

                int cnt = mInput.read(tempBuffer, 0, tempBuffer.length);
                if (cnt > 0) {
                    calculateDB(tempBuffer, cnt, rmsListener);
                    audioStream.write(tempBuffer, 0, cnt);
                    audioStream.flush();
                }
            }

            if (rmsListener != null) {
                rmsListener.rmsChanged(0); // clear visualizer now that we are done recording
            }
            if (stateListener != null) {
                stateListener.recordingCompleted();
            }

        } catch (Exception e) {
            stopCapture(); // make sure to unset capturing mode if there was an exception
            throw e;
        }
    }

    public AudioInputStream sphinxCapture (final AudioFormat audioFormat)
            throws LineUnavailableException, IOException {

        try {
            mCapturing = true; // set in-capturing to true

            mInput.open(audioFormat);
            mInput.start();

            return new AudioInputStream(mInput);

        } catch( Exception e){
            stopCapture();
            throw e;
        }



    }

    // rmsListener is the AudioRMSListener callback for audio visualizer(optional - can be null)
    // assuming 16bit samples, 1 channel, little endian
    private void calculateDB(byte[] data, int cnt, RecordingRMSListener rmsListener) {
        if (rmsListener == null || cnt < 2) {
            return;
        }
        final int bytesPerSample = 2;
        int len = cnt / bytesPerSample;
        double avg = 0;

        for (int i = 0; i < cnt; i += bytesPerSample) {
            ByteBuffer bb = ByteBuffer.allocate(bytesPerSample);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put(data[i]);
            bb.put(data[i + 1]);
            // generate the signed 16 bit number from the 2 bytes
            double dVal = java.lang.Math.abs(bb.getShort(0));
            // scale it from 1 to 100. Use max/2 as values tend to be low
            dVal = (100 * dVal) / (Short.MAX_VALUE / 2.0) + 1;
            avg += dVal * dVal; // add the square to the running average
        }
        avg /= len;
        avg = java.lang.Math.sqrt(avg);
        // update the AudioRMSListener callback with the scaled root-mean-squared power value
        rmsListener.rmsChanged((int) avg);
    }

    // call to stop recording audio
    public void stopCapture() {
        mInput.stop();
        mInput.close();
        mCapturing = false; // unset capturing mode to unblock the capturing thread
    }

    // get the system default microphone
    private TargetDataLine getDefaultMicrophone() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer m = AudioSystem.getMixer(mixerInfo);
            try {
                m.open();
                m.close();
            } catch (Exception e) {
                continue;
            }

            Line.Info[] lines = m.getTargetLineInfo();
            for (Line.Info li : lines) {
                try {
                    TargetDataLine temp = (TargetDataLine) AudioSystem.getLine(li);
                    if (temp != null) {
                        return temp;
                    }
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

}

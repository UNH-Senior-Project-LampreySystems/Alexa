/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;

public class AVSAudioPlayer {
    // callback to send audio events
    private final AVSController mController;
    // vlc instance to play media
    private AudioMediaPlayerComponent mAudioPlayer;
    // queue of listen directive media
    private final Queue<AVSPlayItem> mPlayQueue;
    // queue of speak directive media
    private final Queue<AVSSpeakItem> mSpeakQueue;
    // Cache of URLs associated with the current AVSPlayItem/stream
    private Set<String> streamUrls;
    // Urls associated with the current stream that we've already tried to play
    private Set<String> attemptedUrls;
    // Did the current AVSPlayItem actually play?
    // VLC will not throw an error if it's a 'valid' stream with no content
    private boolean playedStream;

    private int mStopOffset;
    // track the last progressReport sent time
    private long mLastUpdateTime;
    private boolean mWaitForPlaybackFinished;
    // used for speak directives and earcons
    private Player mSpeaker = null;
    private final ClassLoader mResLoader; // used to load resource files

    public AVSAudioPlayer(AVSController eventListener) {
        mController = eventListener;
        mResLoader = Thread.currentThread().getContextClassLoader();
        mStopOffset = -1;
        mLastUpdateTime = 0;
        mWaitForPlaybackFinished = false;
        mPlayQueue = new LinkedList<AVSPlayItem>();
        mSpeakQueue = new LinkedList<AVSSpeakItem>();
        streamUrls = new HashSet<String>();
        attemptedUrls = new HashSet<String>();
        playedStream = false;
        setupAudioPlayer();
    }

    private void setupAudioPlayer() {
        mAudioPlayer = new AudioMediaPlayerComponent();

        mAudioPlayer.getMediaPlayer().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void stopped(MediaPlayer mediaPlayer) {
            }

            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float pos) {
                // If the position changed, we know music actually played
                playedStream = true;
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                System.out.println("Finished playing " + mediaPlayer.mrl());
                List<String> items = mediaPlayer.subItems();
                // Remember the url we just tried
                attemptedUrls.add(mediaPlayer.mrl());
                if (items.size() > 0 || streamUrls.size() > 0) {
                    // Add to the set of URLs to attempt playback
                    streamUrls.addAll(items);

                    // Play any url associated with this play item that
                    // we haven't already tried
                    for (String mrl : streamUrls) {
                        if (!attemptedUrls.contains(mrl)) {
                            System.out.println("Playing " + mrl);
                            mediaPlayer.playMedia(mrl);
                            return;
                        }
                    }
                }

                mLastUpdateTime = 0;

                // wait for any pending events to finish(playbackStarted/progressReport)
                while (mController.eventRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }

                // remove the item from the queue since it has finished playing
                AVSPlayItem media = mPlayQueue.poll();

                // check whether this was the last item
                boolean lastItem = mPlayQueue.isEmpty();
                if (lastItem) {
                    // block starting playback of media returned from getNextItem
                    // until we have sent playbackFinished for the current item
                    mWaitForPlaybackFinished = true;
                    mController.requestNextItem();
                }
                if (playedStream) {
                    mController.sendEvent("playbackFinished", media.mStreamId, 0, "PLAYING");
                }
                playedStream = false;
                // unblock playback now that playbackFinished has been sent
                mWaitForPlaybackFinished = false;
                if (!lastItem) {
                    // start playback if it wasn't the last item
                    startPlayback();
                } else if (mPlayQueue.isEmpty()) {
                    // otherwise if queue is still empty(nothing from
                    // playbackFinished/getNextItem) send Idle event
                    mController.sendEvent("playbackIdle", media.mStreamId, 0, "IDLE");
                }
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.out.println("Error playing: " + mediaPlayer.mrl());

                AVSPlayItem media = mPlayQueue.peek();
                attemptedUrls.add(mediaPlayer.mrl());
                // If there are any urls left to try, don't throw an error
                for (String mrl : streamUrls) {
                    if (!attemptedUrls.contains(mrl)) {
                        mediaPlayer.playMedia(mrl);
                        return;
                    }
                }

                mLastUpdateTime = 0;

                // wait for any pending events to finish(playbackStarted/progressReport)
                while (mController.eventRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }

                // remove the item from the queue since it failed to play
                media = mPlayQueue.poll();

                // check whether this was the last item
                boolean lastItem = mPlayQueue.isEmpty();
                mController.sendError(media.mStreamId, 0, "PLAYING", "MEDIA_ERROR_UNKNOWN",
                        "Error playing media");
                if (!lastItem) {
                    // start playback if it wasn't the last item
                    startPlayback();
                } else if (mPlayQueue.isEmpty()) {
                    // if still empty send idle
                    if (mPlayQueue.isEmpty()) {
                        mController.sendEvent("playbackIdle", media.mStreamId, 0, "IDLE");
                    }
                }
            }
        });

    }

    public void alexaListening() {
        mSpeakQueue.clear();
        if (!mPlayQueue.isEmpty() && (mStopOffset == -1)) {
            // pause any playing media during listening
            pauseMedia();
        }
    }

    private void pauseMedia() {
        if (!mPlayQueue.isEmpty() && (mStopOffset == -1)
                && mAudioPlayer.getMediaPlayer().isPlaying()) {
            mAudioPlayer.getMediaPlayer().pause();
        }
    }

    private void unpauseMedia() {
        if (!mPlayQueue.isEmpty() && (mStopOffset == -1)
                && !mAudioPlayer.getMediaPlayer().isPlaying()) {
            mAudioPlayer.getMediaPlayer().pause();
        }
    }

    public void alexaDoneListening() {
        if (mSpeakQueue.isEmpty()) {
            // unpause any playing media after finished listening if no speech
            unpauseMedia();
        }
    }

    // play directive
    public void add(AVSPlayItem media) {
        mPlayQueue.add(media);
        if (mPlayQueue.size() == 1) {
            // if we aren't already playing, start playback
            startPlayback();
        }
    }

    private void startPlayback() {
        if (mPlayQueue.isEmpty()) {
            return;
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                // wait for any speech to complete before starting playback
                // also wait for playbackFinished to be called after getNextItem
                while (!mSpeakQueue.isEmpty() || mWaitForPlaybackFinished) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                AVSPlayItem media = mPlayQueue.peek();
                if (media == null) {
                    // if a stop/clearQueue came down before we started
                    return;
                }
                if (!playItem(media.mUrl, media.mStartOffset)) {
                    // check whether this was the last item
                    boolean lastItem = mPlayQueue.isEmpty();
                    mController.sendError(media.mStreamId, 0, "PLAYING", "MEDIA_ERROR_UNKNOWN",
                            "Error playing media");
                    if (!lastItem) {
                        // start playback if it wasn't the last item
                        startPlayback();
                    } else if (mPlayQueue.isEmpty()) {
                        // otherwise if queue is still empty(nothing from playbackError)
                        // request the next item
                        mController.requestNextItem();
                        // if still empty send idle
                        if (mPlayQueue.isEmpty()) {
                            mController.sendEvent("playbackIdle", media.mStreamId, 0, "IDLE");
                        }
                    }
                    return;
                }

                mController.sendEvent("playbackStarted", media.mStreamId, 0, "PLAYING");

                // if we don't need progress reports then return
                if (media.mProgressStartOffset <= 0) {
                    return;
                }

                // sleep until the first progress report is required
                try {
                    Thread.sleep(media.mProgressStartOffset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // if the item is still playing
                if (!mPlayQueue.isEmpty() && (media == mPlayQueue.peek())) {
                    mController.sendEvent("playbackProgressReport", media.mStreamId, getProgress(),
                            "PLAYING");

                    if (media.mProgressInterval <= 0) {
                        return;
                    }

                    // update the lastupdatetime so next progress report knows when to send
                    mLastUpdateTime = System.currentTimeMillis();
                    while (mLastUpdateTime != 0 && !mPlayQueue.isEmpty()
                            && (media == mPlayQueue.peek())) {
                        try {
                            Thread.sleep(media.mProgressInterval);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        long currTime = System.currentTimeMillis();
                        if ((currTime - media.mProgressInterval) > mLastUpdateTime) {
                            mController.sendEvent("playbackProgressReport", media.mStreamId,
                                    getProgress(), "PLAYING");
                            mLastUpdateTime = currTime;
                        }
                    }
                }
            }
        };
        thread.start();
    }

    public void stop() {
        mLastUpdateTime = 0;
        if (!mPlayQueue.isEmpty() && (mStopOffset == -1)) {
            AVSPlayItem media = mPlayQueue.peek();
            mStopOffset = getProgress();
            mAudioPlayer.getMediaPlayer().stop();
            mController.sendEvent("playbackInterrupted", media.mStreamId, mStopOffset, "PAUSED");
        }
    }

    // speak directive
    public void speak(AVSSpeakItem speech) {
        mSpeakQueue.add(speech);
        // if not already speaking, start speech
        if (mSpeakQueue.size() == 1) {
            startSpeech();
        }
    }

    private void startSpeech() {
        final AVSSpeakItem speak = mSpeakQueue.peek();
        if (!mPlayQueue.isEmpty()) {
            // pause any playing media
            pauseMedia();
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                speak(speak.mAudio);
                // play the next speech item after this completes
                finishedSpeechItem();
            }
        };
        thread.start();
    }

    private void finishedSpeechItem() {
        // remove the finished item
        mSpeakQueue.poll();
        if (mSpeakQueue.isEmpty()) {
            // if done unpause media
            unpauseMedia();
        } else {
            // if not done start the next speech
            startSpeech();
        }
    }

    public void clearQueue() {
        // save the top item
        AVSPlayItem top = mPlayQueue.poll();
        // clear the queue and re-add the top item
        mPlayQueue.clear();
        if (top != null) {
            mPlayQueue.add(top);
        }
    }

    public void clearAll() {
        // stop playback and clear all
        stop();
        mPlayQueue.clear();
    }

    private boolean playItem(final String url, final int offset) {
        // we are no longer in "PAUSED" state
        mStopOffset = -1;

        // Reset url caches and state information
        streamUrls = new HashSet<String>();
        attemptedUrls = new HashSet<String>();
        playedStream = false;

        setupAudioPlayer();

        if (mAudioPlayer.getMediaPlayer().startMedia(url)) {
            if (offset > 0) {
                // progress should be the absolute time in ms since start
                // however the vlc/vlcj API fails to return this value
                // we are using relative position instead
                // unscale back to between 0 and 1
                mAudioPlayer.getMediaPlayer().setPosition(offset / 100000f);
            }
            return true;
        }
        return false;
    }

    private int getProgress() {
        if (mStopOffset == -1) {
            // progress should be the absolute time in ms since start
            // however the vlc/vlcj API fails to return this value
            // use the relative position instead
            // getPosition returns a float between 0 and 1
            // scale this by 100000 to retain precision
            return (int) (100000 * mAudioPlayer.getMediaPlayer().getPosition());
        }
        return mStopOffset;
    }

    // return playback state for deviceContext
    public String getPlaybackState() {
        String state = "IDLE";
        int offset = 0;
        String streamId = "";
        if (!mPlayQueue.isEmpty()) {
            if (mStopOffset != -1) {
                state = "PAUSED";
                offset = mStopOffset;
            } else {
                state = "PLAYING";
                offset = getProgress();
            }
            streamId = mPlayQueue.peek().mStreamId;
        }
        return "\"streamId\":\"" + streamId + "\", \"offsetInMilliseconds\":\"" + offset
                + "\", \"playerActivity\":\"" + state + "\"";
    }

    public boolean isSpeaking() {
        return !mSpeakQueue.isEmpty();
    }

    // plays MP3 data from a resource asynchronously
    // will stop any previous playback and start the new audio
    public void playMp3FromResource(String resource) {
        final InputStream inpStream = mResLoader.getResourceAsStream(resource);
        try {
            if (mSpeaker != null) {
                mSpeaker.close();
                mSpeaker = null;
            }

            mSpeaker = new Player(inpStream);

            Thread playThread = new Thread() {
                @Override
                public void run() {
                    try {
                        mSpeaker.play();
                    } catch (JavaLayerException jle) {
                        jle.printStackTrace();
                    } finally {
                        IOUtils.closeQuietly(inpStream);
                    }
                }
            };
            playThread.start();
        } catch (JavaLayerException jle) {
            jle.printStackTrace();
            IOUtils.closeQuietly(inpStream);
        }
    }

    public void speak(final InputStream inpStream) {
        if (mSpeaker != null) {
            mSpeaker.close();
            mSpeaker = null;
        }
        try {
            mSpeaker = new Player(inpStream);
            mSpeaker.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void waitForSpeechEnd() {
        while (mSpeaker != null && !mSpeaker.isComplete()) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

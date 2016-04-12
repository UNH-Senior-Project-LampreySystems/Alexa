/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class AVSApp extends JFrame implements AVSListenHandler, RecordingRMSListener {
    private static final String START_LABEL = "Start Listening";
    private static final String STOP_LABEL = "Stop Listening";
    private static final String PROCESSING_LABEL = "Processing";
    private final AVSController mController;
    private CMUWakeup mWakeUp;
    private JButton mAction;
    private JTextField mToken;
    private String mToken_string;
    private JProgressBar mVisualizer;
    private Thread mAutoEndpoint = null; // used to auto-endpoint while listening
    // minimum audio level threshold under which is considered silence
    private static final int ENDPOINT_THRESHOLD = 5;
    private static final int ENDPOINT_SECONDS = 2; // amount of silence time before endpointing

    public static void main(String[] args) {
        new AVSApp();
    }

    public AVSApp() {

        //System.out.println("The value of close microphone: " + Microphone.PROP_CLOSE_BETWEEN_UTTERANCES);

        mController = new AVSController(this);
        new AVSSetup(this);


        mWakeUp = new CMUWakeup(this);
        addTokenField();
        addVisualizerField();
        addActionField();

        getContentPane().setLayout(new GridLayout(0, 1));
        setTitle("Alexa Voice Service");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 200);
        setVisible(true);


    }

    public void wakeUpStart() {
        mAction.doClick();
        /*mController.setBearerToken(mToken_string);
        final RecordingRMSListener rmsListener = this;

        SwingWorker<Void, Void> alexaCall = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() throws Exception {
                mController.startRecording(rmsListener);
                return null;
            }

            @Override
            public void done() {
                try {
                    get(); // get any exceptions that were thrown
                } catch (Exception e) {
                    mController.stopRecording();
                    e.printStackTrace();
                }
            }
        };
        alexaCall.execute();*/
    }


    private void addTokenField() {
        getContentPane().add(new JLabel("Bearer Token:"));
        mToken = new JTextField(50);
        getContentPane().add(mToken);
    }

    private void addVisualizerField() {
        mVisualizer = new JProgressBar(0, 100);
        getContentPane().add(mVisualizer);
    }

    private void addActionField() {
        final RecordingRMSListener rmsListener = this;
        mAction = new JButton(START_LABEL);
        mAction.setEnabled(true);
        mAction.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mAction.getText().equals(START_LABEL)) { // if in idle mode
                    mAction.setText(STOP_LABEL);
                    mController.setBearerToken(mToken_string);

                    SwingWorker<Void, Void> alexaCall = new SwingWorker<Void, Void>() {
                        @Override
                        public Void doInBackground() throws Exception {
                            mController.startRecording(rmsListener);
                            return null;
                        }

                        @Override
                        public void done() {
                            try {
                                get(); // get any exceptions that were thrown
                            } catch (Exception e) {
                                mAction.doClick(); // mimic a stop click if an exception was thrown
                                e.printStackTrace();
                                JOptionPane.showMessageDialog(getContentPane(), e.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                            }
                            // update the UI after the request/response has completed
                            finishProcessing();
                        }
                    };
                    alexaCall.execute();
                } else { // else we must already be in listening
                    mAction.setText(PROCESSING_LABEL); // go into processing mode
                    mAction.setEnabled(false);
                    mVisualizer.setIndeterminate(true);
                    mController.stopRecording(); // stop the recording so the request can complete
                }
            }
        });
        getContentPane().add(mAction);
    }

    public void finishProcessing() { // update ui after stop recording completes

        mAction.setText(START_LABEL);
        mAction.setEnabled(true);
        mVisualizer.setIndeterminate(false);
        System.out.println("Listening for wakeup again");
        mWakeUp.listenForWakeup();
    }

    @Override
    public void rmsChanged(int rms) { // AudioRMSListener callback
        // if greater than threshold or not recording, kill the autoendpoint thread
        if (rms == 0 || rms > ENDPOINT_THRESHOLD) {
            if (mAutoEndpoint != null) {
                mAutoEndpoint.interrupt();
                mAutoEndpoint = null;
            }
        } else if (rms < ENDPOINT_THRESHOLD) {
            // start the autoendpoint thread if it isn't already running
            if (mAutoEndpoint == null) {
                mAutoEndpoint = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(ENDPOINT_SECONDS * 1000);
                            mController.stopRecording(); // hit stop if we get through the autoendpoint time
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                };
                mAutoEndpoint.start();
            }
        }

        mVisualizer.setValue(rms); // update the visualizer
    }

    @Override
    public void startListening() {
        while (!mAction.isEnabled() || !mAction.getText().equals(START_LABEL)
                || mController.isSpeaking()) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
        mAction.doClick();
    }

    public void showDialog(String message) {
        JTextArea textMessage = new JTextArea(message);
        textMessage.setEditable(false);
        JOptionPane.showMessageDialog(getContentPane(), textMessage, "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void setToken(String token) {
        mToken_string = token;
        mWakeUp.listenForWakeup();
    }

}

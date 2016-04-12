package com.amazon.alexa.avs;

/**
 * Created by JBarna on 3/26/2016.
 */

import edu.cmu.sphinx.api.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;


public class CMUWakeup {

    private AVSApp mApp;
    private Configuration conf;
    private AudioCapture audioCapture;

    public CMUWakeup(AVSApp app) {
        audioCapture = AudioCapture.getAudioHardware();
        mApp = app;
        conf = new Configuration();

        conf.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        conf.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        conf.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        conf.setGrammarPath("resource:/res");
        conf.setGrammarName("wakeup");
        conf.setUseGrammar(true);

        /*try {

            Context context = new Context(conf);
            context.setGlobalProperty("oogProbability", "1e-1");

        } catch( Exception e){ e.printStackTrace();}*/


    }

    public void listenForWakeup(){

        StreamSpeechRecognizer recognizer;
        try {
            recognizer = new StreamSpeechRecognizer(conf);
            AudioInputStream stream = audioCapture.sphinxCapture(new AudioFormat(16000f, 16, 1, true, false));

            recognizer.startRecognition(stream);

            System.out.println("Started recognition");
            while (true) {
                String result = recognizer.getResult().getHypothesis();
                System.out.println(result);
                if (result.equals("hello computer")) {
                    System.out.println("Going to wakeup now");
                    audioCapture.stopCapture();
                    recognizer.stopRecognition();
                    mApp.wakeUpStart();
                    break;
                }
            }

            // Pause recognition process. It can be resumed then with startRecognition(false).
            //recognizer.stopRecognition();
        } catch (Exception e) {
            System.out.println("FAILLLL");
            e.printStackTrace();
        }
    }

}


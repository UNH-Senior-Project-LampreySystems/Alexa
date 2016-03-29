package com.amazon.alexa.avs;

/**
 * Created by JBarna on 3/26/2016.
 */

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.Context;


public class WakeUpListener {


    public WakeUpListener() {

        Configuration conf = new Configuration();

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


        LiveSpeechRecognizer recognizer;
        System.out.println("Before try");
        try {
           recognizer = new LiveSpeechRecognizer(conf);

            // Start recognition process pruning previously cached data.
            recognizer.startRecognition(true);

            while (true) {
                SpeechResult result = recognizer.getResult();
                System.out.println(result.getHypothesis());
            }

// Pause recognition process. It can be resumed then with startRecognition(false).
            //recognizer.stopRecognition();
        } catch (Exception e){
            System.out.println("FAILLLL");
            e.printStackTrace();
        }


    }


}

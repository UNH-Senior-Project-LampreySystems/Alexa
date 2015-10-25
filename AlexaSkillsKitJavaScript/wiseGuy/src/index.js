/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
*/

/**
 * This sample shows how to create a Lambda function for handling Alexa Skill requests that:
 *
 * - Session State: Handles a multi-turn dialog model.
 * - LITERAL slot: demonstrates literal handling for a finite set of known values
 *
 * Examples:
 * Dialog model:
 *  User: "Alexa, ask Wise Guy to tell me a knock knock joke."
 *  Alexa: "Knock knock"
 *  User: "Who's there?"
 *  Alexa: "<phrase>"
 *  User: "<phrase> who"
 *  Alexa: "<Punchline>"
 */

/**
 * App ID for the skill
 */
var APP_ID = undefined;//replace with 'amzn1.echo-sdk-ams.app.[your-unique-value-here]';

/**
 * Array containing knock knock jokes.
 */
var JOKE_LIST = [
    {setup: "To", speechPunchline: "Correct grammar is <break time=\"0.2s\" /> to whom.",
        cardPunchline: "Correct grammar is 'to whom'."},
    {setup: "Beets!", speechPunchline: "Beats me!", cardPunchline: "Beats me!"},
    {setup: "Little Old Lady", speechPunchline: "I didn't know you could yodel!",
        cardPunchline: "I didn't know you could yodel!"},
    {setup: "A broken pencil", speechPunchline: "Never mind. It's pointless.",
        cardPunchline: "Never mind. It's pointless."},
    {setup: "Snow", speechPunchline: "Snow use. I forgot", cardPunchline: "Snow use. I forgot"},
    {setup: "Boo", speechPunchline: "Aww <break time=\"0.3s\" /> it's okay <break time=\"0.3s\" /> don't cry.",
        cardPunchline: "Aww, it's okay, don't cry."},
    {setup: "Woo", speechPunchline: "Don't get so excited, it's just a joke",
        cardPunchline: "Don't get so excited, it's just a joke"},
    {setup: "Spell", speechPunchline: "<say-as interpret-as=\"characters\">who</say-as>",
        cardPunchline: "w.h.o"},
    {setup: "Atch", speechPunchline: "I didn't know you had a cold!", cardPunchline: "I didn't know you had a cold!"},
    {setup: "Owls", speechPunchline: "Yes, they do.", cardPunchline: "Yes, they do."},
    {setup: "Berry!", speechPunchline: "Berry nice to meet you.", cardPunchline: "Berry nice to meet you."}
];

/**
 * The AlexaSkill prototype and helper functions
 */
var AlexaSkill = require('./AlexaSkill');

/**
 * WiseGuySkill is a child of AlexaSkill.
 * To read more about inheritance in JavaScript, see the link below.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript#Inheritance
 */
var WiseGuySkill = function () {
    AlexaSkill.call(this, APP_ID);
};

// Extend AlexaSkill
WiseGuySkill.prototype = Object.create(AlexaSkill.prototype);
WiseGuySkill.prototype.constructor = WiseGuySkill;

/**
 * Overriden to show that a subclass can override this function to initialize session state.
 */
WiseGuySkill.prototype.eventHandlers.onSessionStarted = function (sessionStartedRequest, session) {
    console.log("onSessionStarted requestId: " + sessionStartedRequest.requestId
        + ", sessionId: " + session.sessionId);

    // Any session init logic would go here.
};

/**
 * If the user launches without specifying an intent, route to the correct function.
 */
WiseGuySkill.prototype.eventHandlers.onLaunch = function (launchRequest, session, response) {
    console.log("WiseGuySkill onLaunch requestId: " + launchRequest.requestId + ", sessionId: " + session.sessionId);

    handleTellMeAJokeIntent(session, response);
};

/**
 * Overriden to show that a subclass can override this function to teardown session state.
 */
WiseGuySkill.prototype.eventHandlers.onSessionEnded = function (sessionEndedRequest, session) {
    console.log("onSessionEnded requestId: " + sessionEndedRequest.requestId
        + ", sessionId: " + session.sessionId);

    //Any session cleanup logic would go here.
};

WiseGuySkill.prototype.intentHandlers = {
    TellMeAJokeIntent: function (intent, session, response) {
        handleTellMeAJokeIntent(session, response);
    },

    WhosThereIntent: function (intent, session, response) {
        handleWhosThereIntent(session, response);
    },

    SetupNameWhoIntent: function (intent, session, response) {
        handleSetupNameWhoIntent(session, response);
    },

    HelpIntent: function (intent, session, response) {
        var speechOutput = "";

        switch (session.attributes.stage) {
            case 0:
                speechOutput = "Knock knock jokes are a fun call and response type of joke. " +
                    "To start the joke, just ask by saying tell me a joke, or you can say exit.";
                break;
            case 1:
                speechOutput = "You can ask, who's there, or you can say exit.";
                break;
            case 2:
                speechOutput = "You can ask, who, or you can say exit.";
                break;
            default:
                speechOutput = "Knock knock jokes are a fun call and response type of joke. " +
                    "To start the joke, just ask by saying tell me a joke, or you can say exit.";
        }

        // For the repromptText, play the speechOutput again
        response.ask({speech: speechOutput, type: AlexaSkill.speechOutput.PLAIN_TEXT},
                {speech: speechOutput, type: AlexaSkill.speechOutput.PLAIN_TEXT});
    }
}

/**
 * Selects a joke randomly and starts it off by saying "Knock knock".
 */
function handleTellMeAJokeIntent(session, response) {
    var speechOutput = "";

    //Reprompt speech will be triggered if the user doesn't respond.
    var repromptSpeech = "You can ask, who's there";

    //Check if session variables are already initialized.
    if (session.attributes.stage) {

        //Ensure the dialogue is on the correct stage.
        if (session.attributes.stage === 0) {
            //The joke is already initialized, this function has no more work.
            speechOutput = "knock knock!";
        } else {
            //The user attempted to jump to the intent of another stage.
            session.attributes.stage = 0;
            speechOutput = "That's not how knock knock jokes work! "
                + "knock knock";
        }
    } else {
        //Select a random joke and store it in the session variables.
        var jokeID = Math.floor(Math.random() * JOKE_LIST.length);

        //The stage variable tracks the phase of the dialogue. 
        //When this function completes, it will be on stage 1.
        session.attributes.stage = 1;
        session.attributes.setup = JOKE_LIST[jokeID].setup;
        session.attributes.speechPunchline = JOKE_LIST[jokeID].speechPunchline;
        session.attributes.cardPunchline = JOKE_LIST[jokeID].cardPunchline;

        speechOutput = "Knock knock!";
    }

    response.askWithCard({speech: speechOutput, type: AlexaSkill.speechOutput.PLAIN_TEXT},
        {speech: repromptSpeech, type: AlexaSkill.speechOutput.PLAIN_TEXT}, "Wise Guy", speechOutput);
}

/**
 * Responds to the user saying "Who's there".
 */
function handleWhosThereIntent(session, response) {
    var speechOutput = "";
    var repromptSpeech = "";

    if (session.attributes.stage) {
        if (session.attributes.stage === 1) {
            //Retrieve the joke's setup text.
            speechOutput = session.attributes.setup;

            //Advance the stage of the dialogue.
            session.attributes.stage = 2;

            repromptSpeech = "You can ask, " + speechOutput + " who?";
        } else {
            session.attributes.stage = 1;
            speechOutput = "That's not how knock knock jokes work! <break time=\"0.3s\" /> "
                + "knock knock";

            repromptSpeech = "You can ask, who's there."
        }
    } else {

        //If the session attributes are not found, the joke must restart. 
        speechOutput = "Sorry, I couldn't correctly retrieve the joke. "
            + "You can say, tell me a joke";

        repromptSpeech = "You can say, tell me a joke";
    }

    response.ask({speech: "<speak>" + speechOutput + "</speak>", type: AlexaSkill.speechOutput.SSML},
        {speech: "<speak>" + repromptSpeech + "</speak>", type: AlexaSkill.speechOutput.SSML});
}

/**
 * Delivers the punchline of the joke after the user responds to the setup.
 */
function handleSetupNameWhoIntent(session, response) {
    var speechOutput = "";
    var repromptSpeech = "";

    if (session.attributes.stage) {
        if (session.attributes.stage === 2) {
            speechOutput = session.attributes.speechPunchline;
            cardOutput = session.attributes.cardPunchline;
            //If the joke completes successfully, this function uses a "tell" response.
            response.tellWithCard({speech: "<speak>" + speechOutput + "</speak>", type: AlexaSkill.speechOutput.SSML},
                "Wise Guy", cardOutput);
        } else {

            session.attributes.stage = 1;
            speechOutput = "That's not how knock knock jokes work! <break time=\"0.3s\" /> "
                + "Knock knock!";
            cardOutput = "That's not how knock knock jokes work! "
                + "Knock knock!";

            repromptSpeech = "You can ask who's there."

            //If the joke has to be restarted, tis function uses an "ask" response.
            response.askWithCard({speech: "<speak>" + speechOutput + "</speak>", type: AlexaSkill.speechOutput.SSML},
                    {speech: "<speak>" + repromptSpeech + "</speak>", type: AlexaSkill.speechOutput.SSML},
                    "Wise Guy", cardOutput);
        }
    } else {
        speechOutput = "Sorry, I couldn't correctly retrieve the joke. "
            + "You can say, tell me a joke";

        repromptSpeech = "You can say, tell me a joke";

        response.askWithCard({speech: "<speak>" + speechOutput + "</speak>", type: AlexaSkill.speechOutput.SSML},
                {speech: "<speak>" + repromptSpeech + "</speak>", type: AlexaSkill.speechOutput.SSML},
                "Wise Guy", speechOutput);
    }
}

// Create the handler that responds to the Alexa Request.
exports.handler = function (event, context) {
    // Create an instance of the WiseGuy Skill.
    var skill = new WiseGuySkill();
    skill.execute(event, context);
};

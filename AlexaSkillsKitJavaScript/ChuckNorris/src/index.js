/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
*/

/**
 * This simple sample has no external dependencies or session management, and shows the most basic
 * example of how to create a Lambda function for handling Alexa Skill requests.
 *
 * Examples:
 * One-shot model:
 *  User: "Alexa, tell Greeter to say hello"
 *  Alexa: "Hello World!"
 */

/**
 * App ID for the skill
 */
var APP_ID = undefined; //replace with "amzn1.echo-sdk-ams.app.[your-unique-value-here]";

/**
 * The AlexaSkill prototype and helper functions
 */
var AlexaSkill = require('./AlexaSkill');
var Norris = require('./NorrisGen');

/**
 * ChuckNorris is a child of AlexaSkill.
 * To read more about inheritance in JavaScript, see the link below.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript#Inheritance
 */
var ChuckNorris = function () {
    AlexaSkill.call(this, APP_ID);
};

// Extend AlexaSkill
ChuckNorris.prototype = Object.create(AlexaSkill.prototype);
ChuckNorris.prototype.constructor = ChuckNorris;

ChuckNorris.prototype.eventHandlers.onSessionStarted = function (sessionStartedRequest, session) {
    console.log("ChuckNorris onSessionStarted requestId: " + sessionStartedRequest.requestId
        + ", sessionId: " + session.sessionId);
    // any initialization logic goes here
};

ChuckNorris.prototype.eventHandlers.onLaunch = function (launchRequest, session, response) {
    console.log("ChuckNorris onLaunch requestId: " + launchRequest.requestId + ", sessionId: " + session.sessionId);
    var speechOutput = "Chuck Norris is the best. Ask him for a joke";
    var repromptText = "Ask for a joke";
    response.ask(speechOutput, repromptText);
};

ChuckNorris.prototype.eventHandlers.onSessionEnded = function (sessionEndedRequest, session) {
    console.log("ChuckNorris onSessionEnded requestId: " + sessionEndedRequest.requestId
        + ", sessionId: " + session.sessionId);
    // any cleanup logic goes here
};

ChuckNorris.prototype.intentHandlers = {
    // register custom intent handlers
    ChuckNorrisIntent: function (intent, session, response) {
		Norris(function(joke){
			response.tell(joke);
		});
    },
    HelpIntent: function (intent, session, response) {
        response.ask("You can ask Chuck Norris a joke! Ask him a joke");
    }
};

// Create the handler that responds to the Alexa Request.
exports.handler = function (event, context) {
    // Create an instance of the ChuckNorris skill.
    var CN = new ChuckNorris();
    CN.execute(event, context);
};


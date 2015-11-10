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

var TimeHelper = require('./TimeHelper');

/**
 * The AlexaSkill prototype and helper functions
 */
var AlexaSkill = require('./AlexaSkill');

/**
 * Lamprey is a child of AlexaSkill.
 * To read more about inheritance in JavaScript, see the link below.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript#Inheritance
 */
var Lamprey = function () {
    AlexaSkill.call(this, APP_ID);
};

// Extend AlexaSkill
Lamprey.prototype = Object.create(AlexaSkill.prototype);
Lamprey.prototype.constructor = Lamprey;

Lamprey.prototype.eventHandlers.onLaunch = function (launchRequest, session, response) {
    console.log("Lamprey onLaunch requestId: " + launchRequest.requestId + ", sessionId: " + session.sessionId);
    var speechOutput = "Generic lamprey response.";
    response.tell(speechOutput);
};

//intents!
Lamprey.prototype.intentHandlers = {
    
    // register custom intent handlers
    GenericIntent: function (intent, session, response) {
        
        var date = TimeHelper(intent.slots.Date.value);
        
		response.tell("Generic " + (intent.slots.MeasurementType.value || " empty measurement type ") + "date: " + date);
    },
    
    HighLowAvgIntent: function(intent, session, response){
        var date = TimeHelper(intent.slots.Date.value);
        
        response.tell("HighLow " + "date: " + date);
    },
    
    TrendIntent: function(intent, session, response){
        var date = TimeHelper(intent.slots.Date.value);
        
        response.tell("Trend " + "date: " + date);
    },
    
    HelpIntent: function (intent, session, response) {
        response.tell("This is the help for lamprey");
    }
};

// Create the handler that responds to the Alexa Request.
exports.handler = function (event, context) {
    // Create an instance of the Lamprey skill.
    var lamprey = new Lamprey();
    lamprey.execute(event, context);
};


# Alexa Voice Services
This repo is just the creation of the reference implementation for the Alexa Voice Services 

## Dependencies
You have to have a lot of stuff installed, like VLC player, Maven, Node.js... you should read [the reference guide](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/docs/reference-implementation-guide).

## Usage
From the root of the directory,
start the server
```
cd services
npm install
npm start
```
And then to start the client
```
cd javaclient
mvn install
mvn exec:java
```

# Alexa Skills Kit
The skill that users can ask for is located in /AlexaSkillsKitJavaScript
There's a lot of example ones in there but unsuprisingly the folder you want is the lamprey folder. All the files need to be zipped and uploaded to an AWS lamda fucntion to run them. 


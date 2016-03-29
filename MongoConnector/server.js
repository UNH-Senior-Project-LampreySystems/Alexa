var express = require('express');
var app = express();
var bodyParser = require('body-parser');

app.use(bodyParser.json());

// fake datababse
var userHash = require('./fakeDatabase.json');

app.get('/medicalData', function (req, res) {
  res.send('medical data');
});

app.post('/userHash', function (req, res) {

	var accessToken = req.body.accessToken;
	
	//check to see if we have that item in our database
	if (userHash.hasOwnProperty(accessToken)){
		res.json({medicalUrl: userHash[accessToken]});
	}

	else res.send('hash not found');
});

var server = app.listen(28017, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Server listening at http://%s:%s', host, port);
});
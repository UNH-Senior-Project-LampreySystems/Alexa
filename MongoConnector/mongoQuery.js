var MongoClient = require('mongodb').MongoClient;

var url = 'mongodb://localhost:27017/HMS';

MongoClient.connect(url, function(err, db) {
	
	if (err) console.log("error", err);
	
    else console.log("Connected correctly to server.");
	
	if (db) db.close();
});
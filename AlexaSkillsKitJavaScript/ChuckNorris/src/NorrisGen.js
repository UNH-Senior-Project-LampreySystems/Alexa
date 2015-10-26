module.exports = Norris;

var http = require('http');

function Norris(cb){
	
	http.get('http://api.icndb.com/jokes/random', function(res){
	
		var response = "";
		
		res.setEncoding('utf8');
		res.on('data', function (chunk) {
		  response += chunk;
		});
		res.on('end', function() {
			
			//replace ' &quot;'
			response = response.replace(/&quot;/g, "");
			
		  response = JSON.parse(response);
		  cb(response.value.joke);
	  })
	});
	
}
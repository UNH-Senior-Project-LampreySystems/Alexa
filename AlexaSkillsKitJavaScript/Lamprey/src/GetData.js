module.exports = getData;

var https = require('https');

function getData(cb){
    
	https.get('https://gist.githubusercontent.com/JBarna/bdb21e9d9ad9baa62c91/raw', function(res) {
		
      var data = "";
	  
	  res.setEncoding('utf8');
	  res.on('data', function(d) {
		data += d;
	  });

	  res.on('end', function(){
		  
			cb(JSON.parse(data));
			
	  });
	}).on('error', function(e) {
	  console.error(e);
	});
}
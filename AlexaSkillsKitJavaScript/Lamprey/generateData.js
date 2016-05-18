//check the help command
if (process.argv[2].indexOf('help') > -1){
	console.log("args: timeFrame timeMultiple numDataPoints");
	process.exit(0);
}

var fs = require('fs'),
	output = {
		weight: []
	};

//user specified variables
var timeFrame = process.argv[2],
	timeMultiple = Number(process.argv[3]),
	numDataPoints = Number(process.argv[4]);

//time variables
var tm = {};
    tm.second = 1000;
    tm.minute = 60 * tm.second;
    tm.hour = 60 * tm.minute;
    tm.day = 24 * tm.hour;
    tm.week = 7 * tm.day;
    tm.month = 4 * tm.week;
    tm.year = 12 * tm.month;

var currentTime = Date.now();
var upperLimit = tm[timeFrame] * timeMultiple;

for (var i = 0; i < numDataPoints; i++){
	var timeRnd = Math.floor(Math.random() * upperLimit);
	var weightRnd = Math.floor(Math.random() * 15) + 150; //between 150 and 165
	
	output.weight.push({time: currentTime - timeRnd, value: weightRnd});
}

fs.writeFileSync('data.json', JSON.stringify(output));
var getData = require('./GetData');

//time variables
var tm = {};
    tm.second = 1000;
    tm.minute = 60 * tm.second;
    tm.hour = 60 * tm.minute;
    tm.day = 24 * tm.hour;
    tm.week = 7 * tm.day;
    tm.month = 4 * tm.week;
    tm.year = 12 * tm.month;

//private functions
function queryData(query, slots, cb){
    
    getData(function(data){
        
        //initialize the return variable
        var results = {
            numMeasurements: 0
        };
        
        //make a new variable so we only have to search it once
        var _query = {};
        query.forEach(function(singleQuery){
            _query[singleQuery] = true;
        });
            
        
        //find the minimum time
        var minTime = Date.now() - (tm[slots.TimeFrame] * slots.Multiple);

        //now go through values that are greater than minTime
        data.weight.forEach(function(dataPoint){
            
            //skip invalid data points
            if (dataPoint.time < minTime) 
                return;
            
            //increase measurements
            results.numMeasurements++;
            
            //high
            if (_query.high){
                if (typeof results.high === 'undefined')
                    results.high = dataPoint.value;
                else {
                    if ( dataPoint.value > results.high )
                        results.high = dataPoint.value;
                }
            }
            
            if (_query.low){
                if (typeof results.low === 'undefined')
                    results.low = dataPoint.value;
                else {
                    if ( dataPoint.value < results.high )
                        results.low = dataPoint.value;
                }
            }
            
            if (_query.average){
                if (typeof results.average === "undefined")
                    results.average = 0;
                
                results.average += dataPoint.value;
            }
        });
        
        
        //calculate average
        if (_query.average)
            results.average = Math.floor(results.average / results.numMeasurements);
        
        //pass on the information
        cb(results);
        
    });
    
}

exports.generic = function(slots, cb){
    
    query = ["high", "low", "average"];
    
    queryData(query, slots, cb);
    
};
        
        
    
    
module.exports = TextBuilder;

var textFunctions = {
    
    average: function(slots, results){
        return "Your average " + slots.MeasurementType + " was " + results.average + ". ";
    },
    
    high: function(slots, results){
        return "Your highest " + slots.MeasurementType + " was " + results.high + ". ";
    },
    
    low: function(slots, results){
        return "Your lowest " + slots.MeasurementType + " was " + results.low + ". ";
    },
    
    timeAndNum: function(slots, results){
        var out = "In the past ";
        
        if (slots.Multiple > 1)
            out += slots.Multiple + " " + slots.TimeFrame + "s, ";
        else
            out += slots.TimeFrame + ", ";

        out += "you had " + results.numMeasurements + " " + slots.MeasurementType + " measurements. ";
        
        return out;
    }
};



function TextBuilder(query, slots, results){
    
    var output = "";
    
    output = output + textFunctions.timeAndNum(slots, results);
    
    query.forEach(function(_query){
        
        if (textFunctions.hasOwnProperty(_query))
            output = output + textFunctions[_query](slots, results);
    });
    
    return output;
    
}
        
        
    
var dataHelper = require('./DataMiddleman'),
    SlotHelper = require('./SlotHelper'),
    TextBuilder = require('./TextBuilder');

var fakeSlots = {
    MeasurementType: { value: "weight"},
    TimeFrame: { value: "weeks"},
    Multiple: { value: "2"},
    PreTime: { value: "last"},
    HighLowAvg: { value: "high"}
};

var slotResult = SlotHelper(fakeSlots);
var query = [ "average", "high", "low"];

console.log(slotResult);


dataHelper(query, slotResult, function(result){
    console.log("result", result);
    
    console.log("textBuilder", TextBuilder(query, slotResult, result));
});


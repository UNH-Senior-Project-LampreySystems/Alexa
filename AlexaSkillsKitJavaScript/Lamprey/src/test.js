var dataHelper = require('./DataMiddleman'),
    SlotHelper = require('./SlotHelper');

var fakeSlots = {
    MeasurementType: { value: "weight"},
    TimeFrame: { value: "weeks"},
    Multiple: { value: "2"},
    PreTime: { value: "last"}
};

var slotResult = SlotHelper(fakeSlots);

console.log(slotResult);

dataHelper.generic(slotResult, function(result){
    console.log("result", result);
});


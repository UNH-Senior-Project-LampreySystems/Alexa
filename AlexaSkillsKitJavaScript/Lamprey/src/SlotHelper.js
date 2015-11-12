module.exports = SlotHelper;

function SlotHelper(slots){
    
    return {
        MeasurementType: slots.MeasurementType.value,
        PreTime: slots.PreTime.value || "this",
        Multiple: !isNaN(Number(slots.Multiple.value)) ? Number(slots.Multiple.value) : 1,
        TimeFrame: (function(){
            if (typeof slots.TimeFrame.value === 'undefined')
                return "week";
            else {
                //remove the s 
                if (slots.TimeFrame.value.charAt(slots.TimeFrame.value.length - 1) === "s")
                    return slots.TimeFrame.value.substring(0, slots.TimeFrame.value.length - 1);
                else
                    return slots.TimeFrame.value;
            }
        })()
    };
    
}
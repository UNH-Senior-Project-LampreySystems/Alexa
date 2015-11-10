module.exports = TimeHelper;

function TimeHelper(amazonDate){
    
    if (amazonDate == null){
       
        return "Date empty";
        
    } else
         console.log("The amazon date was ", amazonDate);
    
    return amazonDate;
    
}
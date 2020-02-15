
/**
 * Define your FaaS Function here.
 * Each platform handler will call and pass parameters to this function.
 *
 * @param request A JSON object provided by the platform handler.
 * @returns A JSON object to use as a response.
 */
module.exports = function(request) {
        
    //Import the module and collect data
    const inspector = new (require('./Inspector'))();
    inspector.inspectAll();
    inspector.addTimeStamp("frameworkRuntime");
    
    let calcs = request.calcs;
    let sleepTime = request.sleep;
    let loops = request.loops;

    let operand_a = new Array(calcs);
    let operand_b = new Array(calcs);
    let operand_c = new Array(calcs);

    for (let k = 0; k < loops; k++) {
        for (let i = 0; i < calcs; i++) {
            let j = Math.floor(Math.random() * calcs);
            operand_a[j] = Math.floor(Math.random() * 99999);
            operand_b[j] = Math.floor(Math.random() * 99999);
            operand_c[j] = Math.floor(Math.random() * 99999);
            let mult = operand_a[j] * operand_b[j];
            let div = mult / operand_c[j];
        }
    }
    
    inspector.inspectCPUDelta()
    return inspector.finish();
};
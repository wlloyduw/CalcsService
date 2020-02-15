import json
import logging
from Inspector import *
import time
from random import randint

#
# Define your FaaS Function here.
# Each platform handler will call and pass parameters to this function.
# 
# @param request A JSON object provided by the platform handler.
# @returns A JSON object to use as a response.
#
def yourFunction(request):
    # Import the module and collect data
    inspector = Inspector()
    inspector.inspectAll()
    inspector.addTimeStamp("frameworkRuntime")
    
    calcs = request['calcs']
    sleepTime = request['sleep']
    loops = request['loops']
    
    operand_a = [0] * calcs
    operand_b = [0] * calcs
    operand_c = [0] * calcs

    for k in range(0, loops):
        for i in range(0, calcs):
            j = randint(0, calcs)
            operand_a[j] = randint(0, 99999)
            operand_b[j] = randint(0, 99999)
            operand_c[j] = randint(0, 99999)
            mult = operand_a[j] * operand_b[j]
            div = mult / operand_c[j]
    
    inspector.inspectCPUDelta()
    return inspector.finish()
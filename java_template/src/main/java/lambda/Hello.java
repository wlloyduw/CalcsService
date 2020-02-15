package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import faasinspector.Inspector;
import java.util.HashMap;
import java.util.ArrayList;
import shared.calcThread;

/**
 * uwt.lambda_test::handleRequest
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class Hello implements RequestHandler<Request, HashMap<String, Object>> {

    /**
     * Lambda Function Handler
     *
     * @param request Request POJO with defined variables from Request.java
     * @param context
     * @return HashMap that Lambda will automatically convert into JSON.
     */
    public HashMap<String, Object> handleRequest(Request request, Context context) {

        //Collect data
        Inspector inspector = new Inspector();
        inspector.inspectAll();
        
        int threads = request.getThreads();
        int calcs = request.getCalcs();
        int sleep = request.getSleep();
        int loops = request.getLoops();
        int arraySize = request.getArraySize();

        //ArrayList<Thread> threadList = new ArrayList<>();
        //Create threads that will do math.
        //for (int i = 0; i < threads; i++) {
        //    Thread t = new Thread(new calcThread(calcs, sleep, loops, arraySize, inspector, i));
        //    threadList.add(t);
        //    t.start();
        //}

        if (threads == 2) {
            Thread t = new Thread(new calcThread((int)(((double) calcs) * 0.33), sleep, loops, arraySize, inspector, 1));
            calcThread calculator = new calcThread((int)(((double) calcs) * 0.66), sleep, loops, arraySize, inspector, 0);
            t.start();
            calculator.run();
        } else {
            calcThread calculator = new calcThread(calcs, sleep, loops, arraySize, inspector, 0);
            calculator.run();
        }

        //Using this thread, wait for threads to finish.
        //for (Thread t : threadList) {
        //    try {
        //        t.join();
        //    } catch (Exception e) {
        //        inspector.addAttribute("ERROR", e.getStackTrace());
        //    }
        //}

        inspector.addAttribute("threads", threads);
        inspector.addAttribute("calcs", calcs);
        inspector.addAttribute("loops", loops);
        inspector.addAttribute("sleep", sleep);
        inspector.addAttribute("arraySize", arraySize);

        inspector.inspectAllDeltas();
        return inspector.finish();
    }
}

package lambda;

/**
 *
 * @author Wes Lloyd
 */
public class Request {

    int threads;
    int calcs;
    int sleep;
    int loops;
    int arraySize;
    
    public Request(int calcs, int sleep, int loops, int arraySize) {
        this.calcs = calcs;
        this.sleep = sleep;
        this.loops = loops;
        this.arraySize = arraySize;
    }

    public Request() {

    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
    
    public int getCalcs() {
        return calcs;
    }

    public void setCalcs(int calcs) {
        this.calcs = calcs;
    }

    public int getArraySize() {
        return arraySize;
    }

    public void setArraySize(int arraySize) {
        this.arraySize = arraySize;
    }

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public int getLoops() {
        return loops;
    }

    public void setLoops(int loops) {
        this.loops = loops;
    }
}

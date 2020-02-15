package shared;

import java.util.Random;

import faasinspector.Inspector;

/**
 * A thread that does all of the math.
 */
public class calcThread implements Runnable {

    private final int calcs;
    private final int sleep;
    private final int loops;
    private final int arraySize;
    private final Inspector inspector;
    private final int threadID;
    
    long[] operand_a;
    long[] operand_b;
    long[] operand_c;
    
    private long lastCalc = 0;
    
    //Set seed so random always returns the same set of values.
    Random rand = new Random(42);

    /**
     * A thread to do math.
     * 
     * @param calcs Number of calcs to do ever loop.
     * @param sleep Amount of time in ms to sleep every loop.
     * @param loops The number of loops to do.
     * @param arraySize The size of arrays to create.
     * @param inspector The inspector object (used to report final calculation.)
     * @param threadID The index of the thread.
     */
    public calcThread(int calcs, int sleep, int loops, int arraySize, Inspector inspector, int threadID) {
        this.calcs = calcs;
        this.sleep = sleep;
        this.loops = loops;
        this.arraySize = arraySize;
        this.inspector = inspector;
        this.threadID = threadID;

        this.operand_a = new long[arraySize];
        this.operand_b = new long[arraySize];
        this.operand_c = new long[arraySize];
    }

    @Override
    public void run() {

        inspector.addAttribute("finalCalc" + threadID, 0);
        
        if (loops > 0) {
            for (int i = 0; i < loops; i++) {
                lastCalc = (long) randomMath();
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ie) {
                        System.out.println("Sleep was interrupted - calc mode...");
                    }
                }   
            }
        } else {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ie) {
                System.out.println("Sleep was interrupted - no calc mode...");
            }
        }

        inspector.addAttribute("finalCalc" + threadID, lastCalc);
    }

    private double randomMath() {
        // By not reusing the same variables in the calc, this should prevent
        // compiler optimization... Also each math operation should operate
        // on between operands in different memory locations.
        long mult;
        double div1 = 0;

        for (int i = 0; i < calcs; i++) {
            // By not using sequential locations in the array, we should 
            // reduce memory lookup efficiency
            int j = rand.nextInt(arraySize);
            operand_a[j] = rand.nextInt(99999);
            operand_b[j] = rand.nextInt(99999);
            operand_c[j] = rand.nextInt(99999);
            mult = operand_a[j] * operand_b[j];
            div1 = (double) mult / (double) operand_c[j];
        }
        return div1;
    }
}
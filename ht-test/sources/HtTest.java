import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.format;

class HTTest {

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.err.println("Usage: httest <opcount> <numtests> <numthreads> ");
            System.exit(1);
        }

        long opsCount = parseLong(args[0]);
        int numTests = parseInt(args[2]);
        int numThreads = parseInt(args[2]);

        for (int i = 0; i < numTests; i++) {
            System.out.println("Running test #" + i);
            runTest(opsCount, numThreads);
            System.out.println();
        }
    }

    private static void runTest(long opsCount, int numThreads) throws InterruptedException {
        var executor = Executors.newFixedThreadPool(numThreads);

        long finalResult = 0;
        final Map<Integer, Long> resultsPerThread = new ConcurrentHashMap<>();
        final Map<Integer, Long> cpuTimePerThread = new ConcurrentHashMap<>();
        final Map<Integer, Long> wallTimePerThread = new ConcurrentHashMap<>();

        var totalWallTimeBefore = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            final int threadId = i;
            executor.submit(() -> {
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                var wallTimeBefore = System.currentTimeMillis();
                var cpuTimeBefore = threadMXBean.getCurrentThreadCpuTime();
                long result = 0;
                for(long j = 0; j < opsCount; j++) {
                    result = (result + j) * j / ((j << 6) + 7);
                }
                var cpuTimeAfter = threadMXBean.getCurrentThreadCpuTime();
                var wallTimeAfter = System.currentTimeMillis();
                resultsPerThread.put(threadId, result);
                cpuTimePerThread.put(threadId, (cpuTimeAfter - cpuTimeBefore)/1000000);
                wallTimePerThread.put(threadId, wallTimeAfter - wallTimeBefore);
                latch.countDown();
            });
        }

        latch.await();

        var totalWallTimeAfter = System.currentTimeMillis();
        var totalWallTime = totalWallTimeAfter - totalWallTimeBefore;
        for (Long threadResult : resultsPerThread.values()) {
            finalResult = finalResult + threadResult;
        }
        System.out.println("finalResult = " + finalResult);
        System.out.println();
        long sumCpuTime = 0;
        long sumWallTime = 0;
        for (int i = 0; i < numThreads; i++) {
            System.out.println(format("[%s] wall=%s; cpu=%s", i, wallTimePerThread.get(i), cpuTimePerThread.get(i)));
            sumCpuTime += cpuTimePerThread.get(i);
            sumWallTime += wallTimePerThread.get(i);
        }
        System.out.println("---------------");
        System.out.println(format("totalWallTime=%s; sumWallTime=%s; sumCpuTime=%s",
                totalWallTime, sumWallTime, sumCpuTime));

        executor.shutdownNow();
    }
}
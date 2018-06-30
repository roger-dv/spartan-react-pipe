/*
 * Copyright July 2018
 * Author: Roger D. Voss
 * MIT License
 */
package spartan.react_pipe;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static spartan.react_pipe.Subscriber.makeExecutorCompletionService;

public class Main {
  private static final String progname = "genfib";
  private static final ForkJoinPool forkJoinPool = new ForkJoinPool();
  private static final Subscriber.FuturesCompletion<Boolean> executor = makeExecutorCompletionService(forkJoinPool);

  private static final class ForkJoinPool extends java.util.concurrent.ForkJoinPool {
    private static final int  MAX_CAP = 0x7fff;  // max #workers - 1
    private static final Supplier<ForkJoinWorkerThreadFactory> makeThreadFactory = () ->
    {
      final AtomicInteger workerThreadNbr = new AtomicInteger(1);
      return pool -> {
        final ForkJoinWorkerThread t = defaultForkJoinWorkerThreadFactory.newThread(pool);
        t.setDaemon(true);
        t.setName(String.format("%s-pool-thread-#%d", progname, workerThreadNbr.getAndIncrement()));
        return t;
      };
    };
    private ForkJoinPool() {
      super(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), makeThreadFactory.get(), null, true);
    }
  }

  public static void main(String[] args) {
    System.out.printf("%s: Generate Fibonacci Sequence values%n", progname);

    final double maxCeiling = args.length > 0 ? Math.floor(Double.parseDouble(args[0])) : 30d /* default */;

    final Subscriber.Iterator<Double> src = new GeneratorIterator<>(
            () -> System.out.printf("%s: [%s] source data generator done%n", progname, Thread.currentThread().getName()));

    final Subscriber.Publisher<Double> publisher = src.getPublisher();

    executor.submit(() -> {
      final Stream<Double> srcStrm = Subscriber.stream(src);
      final String currThrdName = Thread.currentThread().getName();
      srcStrm.forEach(item -> System.out.printf("%s: [%s] %.0f%n", progname, currThrdName, item));
    }, Boolean.TRUE);

    System.out.printf("%s: DEBUG: generator lambda invoked for max ceiling value of: %.0f%n", progname, maxCeiling);

    final Function<Double, Long> generateFibonacciSequence = ceiling -> {
      final double max_ceiling = ceiling;
      long publishCallCount = 0;
      try {
        long count = 0;
        double j = 0, i = 1;
        publisher.publish(j);
        count++;
        if (max_ceiling <= j) return publishCallCount;
        publisher.publish(i);
        count++;
        if (max_ceiling == i) return publishCallCount;
        for (; ; ) {
          double tmp = i;
          i += j;
          j = tmp;
          if (i > max_ceiling) break;
          publisher.publish(i);
          count++;
        }
        publishCallCount = count;
      } catch (InterruptedException e) {
        System.err.printf("%s [%s] Fibonacci Sequence generation interrupted%n",
                progname, Thread.currentThread().getName());
      } finally {
        publisher.done();
      }
      return publishCallCount;
    };

    // publishes Fibonacci Sequence generation
    final long countOfGenFibNbrs = generateFibonacciSequence.apply(maxCeiling);

    final String currThrdName = Thread.currentThread().getName();

    while(executor.count() > 0) {
      try {
        final String status = executor.take().get() ? "completed" : "incomplete";
        System.out.printf("%s: [%s] Fibonacci Sequence consumer task status: %s%n", progname, currThrdName, status);
      } catch (InterruptedException e) {
        System.err.printf("%s [%s] waiting on Fibonacci Sequence consumer task interrupted%n", progname, currThrdName);
      } catch (ExecutionException e) {
        e.printStackTrace(System.err);
      }
    }

    System.out.printf("%s: [%s] %d Fibonacci Sequence numbers generated%n", progname, currThrdName, countOfGenFibNbrs);
  }
}
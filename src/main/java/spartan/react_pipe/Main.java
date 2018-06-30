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

public class Main {
  private static final String clsName = GeneratorIterator.class.getSimpleName();
  private static final ForkJoinPool executor = new ForkJoinPool();

  private static final class ForkJoinPool extends java.util.concurrent.ForkJoinPool {
    private static final int  MAX_CAP = 0x7fff;  // max #workers - 1
    private static final Supplier<ForkJoinWorkerThreadFactory> makeThreadFactory = () ->
    {
      final AtomicInteger workerThreadNbr = new AtomicInteger(1);
      return pool -> {
        final ForkJoinWorkerThread t = defaultForkJoinWorkerThreadFactory.newThread(pool);
        t.setDaemon(true);
        t.setName(String.format("%s-pool-thread-#%d", clsName, workerThreadNbr.getAndIncrement()));
        return t;
      };
    };
    private ForkJoinPool() {
      super(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), makeThreadFactory.get(), null, true);
    }
  }

  public static void main(String[] args) {
    System.out.printf("%s: Generate Fibonacci Sequence values%n", clsName);

    final double maxCeiling = args.length > 0 ? Double.parseDouble(args[0]) : 30 /* default */;

    final Subscriber.Iterator<Double> src = new GeneratorIterator<>(
            () -> System.out.printf("%s: [%s] source data generator done%n", clsName, Thread.currentThread().getName()));

    final Subscriber.Publisher<Double> publisher = src.getPublisher();

    final ForkJoinTask<?> fut = executor.submit(() -> {
      final Stream<?> srcStrm = Subscriber.stream(src);
      final String currThrdName = Thread.currentThread().getName();
      srcStrm.forEach(item -> System.out.printf("%s: [%s] %s%n", clsName, currThrdName, item));
    });

    System.out.printf("%s: DEBUG: generator lambda invoked for max ceiling value of: %s%n", clsName, maxCeiling);

    final Function<Double, Long> generateFibonacciSequence = ceiling -> {
      long publishCallCount = 0;
      try {
        long count = 0;
        double j = 0, i = 1;
        publisher.publish(j);
        count++;
        if (ceiling <= j) return publishCallCount;
        publisher.publish(i);
        count++;
        if (ceiling == i) return publishCallCount;
        for (; ; ) {
          double tmp = i;
          i += j;
          j = tmp;
          if (i > ceiling) break;
          publisher.publish(i);
          count++;
        }
        publishCallCount = count;
      } catch (InterruptedException e) {
        System.err.printf("%s [%s] Fibonacci Sequence generation interrupted%n",
                clsName, Thread.currentThread().getName());
      } finally {
        publisher.done();
      }
      return publishCallCount;
    };

    final long countOfGeneratedNumbers = generateFibonacciSequence.apply(maxCeiling);

    System.out.printf("%s: [%s] %d Fibonacci Sequence numbers generated%n",
            clsName, Thread.currentThread().getName(), countOfGeneratedNumbers);

    fut.join();                              // insure that forkJoinTask is in fully completed state
    final Throwable e = fut.getException();  // get any exception that may have been thrown on forkJoinTask
    if (e != null) {
      e.printStackTrace(System.err);
    }
  }
}
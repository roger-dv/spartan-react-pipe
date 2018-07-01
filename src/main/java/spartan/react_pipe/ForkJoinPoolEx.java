/*
 * Copyright July 2018
 * Author: Roger D. Voss
 * MIT License
 */
package spartan.react_pipe;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ForkJoinPoolEx extends ForkJoinPool {
  private static final String progname = Main.progname;
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
  public ForkJoinPoolEx() {
    super(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), makeThreadFactory.get(), null, true);
  }
}

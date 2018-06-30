/*
 * Copyright July 2018
 * Author: Roger D. Voss
 * MIT License
 */
package spartan.react_pipe;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Subscriber {
  interface Iterator<U> extends java.util.Iterator<U> {
    void cancel();
    Publisher<U> getPublisher();
  }

  interface Publisher<U> {
    void publish(U item) throws InterruptedException;
    void done();
  }

  @FunctionalInterface
  interface OnCompletion {
    void complete();
  }

  interface FuturesCompletion<U> extends CompletionService<U> {
    int count();
  }

  static <U> Stream<U> stream(Iterator<U> iterator) {
    return StreamSupport
         .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
  }

  static <U> FuturesCompletion<U> makeExecutorCompletionService(Executor executor) {
    final ExecutorCompletionService<U> exec = new ExecutorCompletionService<>(executor);
    return new FuturesCompletion<U>() {
      private int taskCount = 0;

      @Override
      public synchronized Future<U> submit(Callable<U> task) {
        Future<U> rslt = exec.submit(task);
        taskCount++;
        return rslt;
      }

      @Override
      public synchronized Future<U> submit(Runnable task, U result) {
        Future<U> rslt = exec.submit(task, result);
        taskCount++;
        return rslt;
      }

      @Override
      public synchronized Future<U> take() throws InterruptedException {
        Future<U> rslt = exec.take();
        taskCount--;
        return rslt;
      }

      @Override
      public synchronized Future<U> poll() {
        Future<U> rslt = exec.poll();
        taskCount--;
        return rslt;
      }

      @Override
      public synchronized Future<U> poll(long timeout, TimeUnit unit) throws InterruptedException {
        Future<U> rslt = exec.poll(timeout, unit);
        taskCount--;
        return rslt;
      }

      @Override
      public synchronized int count() {
        return taskCount;
      }
    };
  }
}
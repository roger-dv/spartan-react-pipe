/*
 * Copyright July 2018
 * Author: Roger D. Voss
 * MIT License
 */
package spartan.react_pipe;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import spartan.react_pipe.Subscriber.OnCompletion;
import spartan.react_pipe.Subscriber.Publisher;
import spartan.react_pipe.Subscriber.Iterator;

public final class GeneratorIterator<U> implements Iterator<U> {
  private static final int defaultMaxWorkQueueDepth = 50;

  private final int maxWorkQueueDepth;
  private final OnCompletion onCompletion;
  private final ArrayBlockingQueue<U> workQueue;
  private final ArrayList<U> drainedItems;
  private final AtomicBoolean isCompleted;
  private int drainedItemsCount;
  private int position;
  private U nextValue = null;

  public GeneratorIterator(int maxWorkQueueDepth, OnCompletion onCompletion)
  {
    this.maxWorkQueueDepth = maxWorkQueueDepth;
    this.onCompletion = onCompletion;
    this.workQueue = new ArrayBlockingQueue<>(maxWorkQueueDepth);
    this.drainedItems = new ArrayList<>(maxWorkQueueDepth + 1);
    this.isCompleted = new AtomicBoolean(false);
    this.drainedItemsCount = this.position = maxWorkQueueDepth;
  }

  public GeneratorIterator(OnCompletion onCompletion) {
    // default for maximum work queue depth
    this(defaultMaxWorkQueueDepth, onCompletion);
  }

  public Publisher<U> getPublisher() {
    final ArrayBlockingQueue<U> theWorkQueue = this.workQueue;
    return new Publisher<U>() {
      @Override
      public void publish(U item) throws InterruptedException {
        theWorkQueue.put(item);
      }
      @Override
      public void done() {
        theWorkQueue.done();
      }
    };
  }

  private boolean drainQueue() {
    if (position >= drainedItemsCount) {
      position = 0;
      drainedItems.clear();
      drainedItemsCount = 0;
      boolean isDone = false;
      U pollItem = null;
      for(;;) {
        if (pollItem != null) {
          drainedItems.add(pollItem);
          drainedItemsCount++;
        }
        drainedItemsCount += workQueue.drainTo(drainedItems, maxWorkQueueDepth);
        if (drainedItemsCount > 0) break;
        if (isDone) return false;
        try {
          pollItem = workQueue.poll(5, TimeUnit.SECONDS);
          isDone = workQueue.isDone();
        } catch (InterruptedException e) {
          return false;
        }
        if (pollItem == null && isDone) return false;
      }
    }
    assert(drainedItemsCount > 0 && position < drainedItemsCount);
    nextValue = drainedItems.get(position++);
    return true;
  }

  private void onDone() {
    if (isCompleted.compareAndSet(false, true)) { // insure this code block is executed only once
      onCompletion.complete(); // invoke the completion lambda (can do cleanup, etc)
    }
  }

  @Override
  public boolean hasNext() {
    if (drainQueue()) {
      return true;
    } else {
      nextValue = null;
      onDone();
      return false;
    }
  }

  @Override
  public U next() { return nextValue; } // will return same value if called multiple times between calling hasNext()

  @Override
  public void cancel() {
    onDone();
  }
}
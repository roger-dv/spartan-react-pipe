/*
 * Copyright July 2018
 * Author: Roger D. Voss
 * MIT License
 */
package spartan.react_pipe;

import java.util.Spliterator;
import java.util.Spliterators;
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

  public static <U> Stream<U> stream(Iterator<U> iterator) {
    return StreamSupport
         .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
  }
}
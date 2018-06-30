/*
 * Copyright July 2018
 * Author: Roger D. Voss
 * MIT License
 */
package spartan.react_pipe;

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
}
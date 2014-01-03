import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongTest {
  private static void runTest(final boolean increment, 
                              final int threadCount, 
                              final int iterationsPerThread) {
    // we assume a 1ms delay per thread to try to get them all to start at the same time
    final long startTime = System.currentTimeMillis() + threadCount + 10;
    final AtomicLong result = new AtomicLong();
    final AtomicLong threadDoneCount = new AtomicLong();
    
    for (int i = 0; i < threadCount; i++) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            waitTillReady();
            doOperation();
          } finally {
            synchronized (threadDoneCount) {
              threadDoneCount.incrementAndGet();
              
              threadDoneCount.notifyAll();
            }
          }
        }
        
        private void waitTillReady() {
          long sleepTime = System.currentTimeMillis() - startTime;
          if (sleepTime > 0) {
            try {
              Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
              // let thread exit
              return;
            }
          }
        }
        
        private void doOperation() {
          boolean flip = true;
          for (int i = 0; i < iterationsPerThread; i++) {
            if (flip) {
              if (increment) {
                result.incrementAndGet();
              } else {
                result.decrementAndGet();
              }
              flip = false;
            } else {
              if (increment) {
                result.getAndIncrement();
              } else {
                result.getAndDecrement();
              }
              flip = true;
            }
          }
        }
      }).start();
    }
    
    synchronized (threadDoneCount) {
      while (threadDoneCount.get() < threadCount) {
        try {
          threadDoneCount.wait();
        } catch (InterruptedException e) {
          // let thread exit
          return;
        }
      }
    }
    
    long expectedResult = threadCount * iterationsPerThread;
    if (! increment) {
      expectedResult *= -1;
    }
    long resultValue = result.get();
    if (resultValue != expectedResult) {
      throw new IllegalStateException(resultValue + " != " + expectedResult);
    }
  }
  
  public static void main(String[] args) {
    runTest(true, 10, 100);
    runTest(false, 10, 100);
  }
}

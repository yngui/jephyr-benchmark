/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Igor Konev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jvnet.zephyr.benchmark;

import org.jvnet.zephyr.thread.continuation.Jsr166ForkJoinPoolExecutor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

import java.util.concurrent.locks.LockSupport;

public class ZephyrThreadRingBenchmark extends AbstractRingBenchmark {

    private Worker[] workers;
    private Worker first;

    static {
        System.setProperty("org.jvnet.zephyr.thread.continuation.ContinuationThreadImplProvider.executor",
                Jsr166ForkJoinPoolExecutor.class.getName());
        System.setProperty(Jsr166ForkJoinPoolExecutor.class.getName() + ".parallelism", Integer.toString(PARALLELISM));
    }

    @Setup(Level.Invocation)
    public void setup() {
        workers = new Worker[workerCount];
        first = new Worker();
        Worker next = first;

        for (int i = workerCount - 1; i > 0; i--) {
            Worker worker = new Worker();
            workers[i] = worker;
            worker.next = next;
            worker.waiting = true;
            worker.start();
            next = worker;
        }

        workers[0] = first;
        first.next = next;
        first.waiting = true;
        first.start();
    }

    @Benchmark
    @Override
    public final void benchmark() throws InterruptedException {
        first.message = ringSize;
        first.waiting = false;
        LockSupport.unpark(first);

        for (Worker worker : workers) {
            worker.join();
        }
    }

    private static final class Worker extends Thread {

        Worker next;
        int message;
        volatile boolean waiting;

        Worker() {
        }

        @Override
        public void run() {
            int m;
            do {
                while (waiting) {
                    LockSupport.park();
                }
                m = message;
                waiting = true;
                next.message = m - 1;
                next.waiting = false;
                LockSupport.unpark(next);
            } while (m > 0);
        }
    }
}

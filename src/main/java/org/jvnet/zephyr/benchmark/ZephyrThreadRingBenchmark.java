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

import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.locks.LockSupport;

public class ZephyrThreadRingBenchmark extends AbstractRingBenchmark {

    @Benchmark
    @Override
    public final void benchmark() throws InterruptedException {
        Worker[] workers = new Worker[workerCount];
        Worker first = new Worker();
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
        first.message = ringSize;
        first.waiting = false;
        first.start();

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

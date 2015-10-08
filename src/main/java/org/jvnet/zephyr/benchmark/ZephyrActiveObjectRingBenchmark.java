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

import org.jvnet.zephyr.activeobject.annotation.ActiveMethod;
import org.jvnet.zephyr.activeobject.annotation.ActiveObject;
import org.jvnet.zephyr.activeobject.annotation.Oneway;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

import java.util.concurrent.CountDownLatch;

public class ZephyrActiveObjectRingBenchmark extends AbstractRingBenchmark {

    private CountDownLatch latch;
    private Worker first;

    static {
        System.setProperty("org.jvnet.zephyr.thread.continuation.DefaultForkJoinPoolProvider.parallelism",
                Integer.toString(PARALLELISM));
    }

    @Setup(Level.Invocation)
    public void setup() {
        latch = new CountDownLatch(workerCount);
        first = new Worker(latch);
        Worker next = first;

        for (int i = workerCount - 1; i > 0; i--) {
            Worker worker = new Worker(latch);
            worker.link(next);
            next = worker;
        }

        first.link(next);
    }

    @Benchmark
    @Override
    public final void benchmark() throws InterruptedException {
        first.receive(ringSize);
        latch.await();
    }

    @ActiveObject
    private static final class Worker {

        private final CountDownLatch latch;
        private Worker next;

        Worker(CountDownLatch latch) {
            this.latch = latch;
        }

        void link(Worker next) {
            this.next = next;
        }

        @ActiveMethod
        @Oneway
        void receive(int message) {
            if (next != null) {
                next.receive(message - 1);
                if (message <= 0) {
                    next = null;
                    latch.countDown();
                }
            }
        }
    }
}

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

package org.jephyr.benchmark;

import java.util.concurrent.CountDownLatch;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

public class QuasarActorRingBenchmark extends AbstractRingBenchmark {

    private CountDownLatch latch;
    private ActorRef<Object> first;

    static {
        System.setProperty("co.paralleluniverse.fibers.DefaultFiberPool.parallelism", Integer.toString(PARALLELISM));
    }

    @Setup(Level.Invocation)
    public void setup() throws SuspendExecution {
        latch = new CountDownLatch(workerCount);
        first = new Worker(latch).spawn();
        ActorRef<Object> next = first;

        for (int i = workerCount - 1; i > 0; i--) {
            ActorRef<Object> worker = new Worker(latch).spawn();
            worker.send(next);
            next = worker;
        }

        first.send(next);
    }

    @Benchmark
    @Override
    public final void benchmark() throws SuspendExecution, InterruptedException {
        first.send(ringSize);
        latch.await();
    }

    private static final class Worker extends Actor<Object, Void> {

        private final CountDownLatch latch;
        private ActorRef<Object> next;

        Worker(CountDownLatch latch) {
            super(null, null);
            this.latch = latch;
        }

        @Override
        protected Void doRun() throws SuspendExecution, InterruptedException {
            next = (ActorRef<Object>) receive();
            int m;
            do {
                m = (int) receive();
                next.send(m - 1);
            } while (m > 0);
            latch.countDown();
            return null;
        }
    }
}

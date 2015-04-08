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

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.concurrent.CountDownLatch;

public class QuasarActorRingBenchmark extends AbstractRingBenchmark {

    @Benchmark
    @Override
    public final void benchmark() throws SuspendExecution, InterruptedException {
        CountDownLatch latch = new CountDownLatch(workerCount);
        ActorRef<Object>[] workers = new ActorRef[workerCount];
        ActorRef<Object> first = new Worker(latch).spawn();
        ActorRef<Object> next = first;

        for (int i = workerCount - 1; i > 0; i--) {
            ActorRef<Object> worker = new Worker(latch).spawn();
            workers[i] = worker;
            worker.send(next);
            next = worker;
        }

        workers[0] = first;
        first.send(next);
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

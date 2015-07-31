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

import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.CountDownLatch;

public class AkkaTypedActorRingBenchmark extends AbstractRingBenchmark {

    private ActorSystem system;
    private CountDownLatch latch;
    private Worker first;

    static {
        System.setProperty("akka.actor.default-dispatcher.fork-join-executor.parallelism-max",
                Integer.toString(PARALLELISM));
        System.setProperty("akka.actor.default-dispatcher.fork-join-executor.parallelism-max",
                Integer.toString(PARALLELISM));
    }

    @Setup(Level.Invocation)
    public void setup() {
        system = ActorSystem.create();
        latch = new CountDownLatch(workerCount);
        first = TypedActor.get(system).typedActorOf(new TypedProps<>(Worker.class, new WorkerImplCreator(latch)));
        Worker next = first;

        for (int i = workerCount - 1; i > 0; i--) {
            Worker worker =
                    TypedActor.get(system).typedActorOf(new TypedProps<>(Worker.class, new WorkerImplCreator(latch)));
            worker.setNext(next);
            next = worker;
        }

        first.setNext(next);
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        system.shutdown();
    }

    @Benchmark
    @Override
    public final void benchmark() throws InterruptedException {
        first.send(ringSize);
        latch.await();
    }

    public interface Worker {

        void setNext(Worker next);

        void send(int message);
    }

    public static final class WorkerImpl implements Worker {

        private final CountDownLatch latch;
        private Worker next;

        WorkerImpl(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void setNext(Worker next) {
            this.next = next;
        }

        @Override
        public void send(int message) {
            next.send(message - 1);
            if (message <= 0) {
                TypedActor.get(TypedActor.context().system()).stop(TypedActor.self());
                latch.countDown();
            }
        }
    }

    private static final class WorkerImplCreator implements Creator<WorkerImpl> {

        private static final long serialVersionUID = 1L;

        private final CountDownLatch latch;

        WorkerImplCreator(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public WorkerImpl create() {
            return new WorkerImpl(latch);
        }
    }
}

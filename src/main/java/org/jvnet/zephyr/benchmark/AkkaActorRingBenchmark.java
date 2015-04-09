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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.CountDownLatch;

public class AkkaActorRingBenchmark extends AbstractRingBenchmark {

    private ActorSystem system;
    private CountDownLatch latch;
    private ActorRef first;

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
        first = system.actorOf(Props.create(Worker.class, latch));
        ActorRef next = first;

        for (int i = workerCount - 1; i > 0; i--) {
            ActorRef worker = system.actorOf(Props.create(Worker.class, latch));
            worker.tell(next, ActorRef.noSender());
            next = worker;
        }

        first.tell(next, ActorRef.noSender());
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        system.shutdown();
    }

    @Benchmark
    @Override
    public final void benchmark() throws InterruptedException {
        first.tell(ringSize, ActorRef.noSender());
        latch.await();
    }

    private static final class Worker extends UntypedActor {

        private final CountDownLatch latch;
        private ActorRef next;

        Worker(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof Integer) {
                int m = (int) message;
                next.tell(m - 1, getSelf());
                if (m <= 0) {
                    getContext().stop(getSelf());
                    latch.countDown();
                }
            } else if (message instanceof ActorRef) {
                next = (ActorRef) message;
            } else {
                unhandled(message);
            }
        }
    }
}

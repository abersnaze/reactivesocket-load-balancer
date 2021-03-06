package io.reactivesocket.loadbalancer.servo;

import io.reactivesocket.Payload;
import io.reactivesocket.internal.rx.EmptySubscription;
import io.reactivesocket.loadbalancer.client.ReactiveSocketClient;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import rx.RxReactiveStreams;
import rx.observers.TestSubscriber;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by rroeser on 3/7/16.
 */
public class ServoMetricsReactiveSocketClientTest {
    @Test
    public void testCountSuccess() {
        ServoMetricsReactiveSocketClient client = new ServoMetricsReactiveSocketClient(new ReactiveSocketClient() {
            @Override
            public Publisher<Payload> requestResponse(Payload payload) {
                return s -> {
                    s.onNext(new Payload() {
                        @Override
                        public ByteBuffer getData() {
                            return null;
                        }

                        @Override
                        public ByteBuffer getMetadata() {
                            return null;
                        }
                    });

                    s.onComplete();
                };
            }

            @Override
            public void close() throws Exception {

            }
        }, "test");

        Publisher<Payload> payloadPublisher = client.requestResponse(new Payload() {
            @Override
            public ByteBuffer getData() {
                return null;
            }

            @Override
            public ByteBuffer getMetadata() {
                return null;
            }
        });

        TestSubscriber subscriber = new TestSubscriber();
        RxReactiveStreams.toObservable(payloadPublisher).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();

        Assert.assertEquals(1, client.success.get());
    }

    @Test
    public void testCountFailure() {
        ServoMetricsReactiveSocketClient client = new ServoMetricsReactiveSocketClient(new ReactiveSocketClient() {
            @Override
            public Publisher<Payload> requestResponse(Payload payload) {
                return new Publisher<Payload>() {
                    @Override
                    public void subscribe(Subscriber<? super Payload> s) {
                        s.onSubscribe(EmptySubscription.INSTANCE);
                        s.onError(new RuntimeException());
                    }
                };
            }

            @Override
            public void close() throws Exception {

            }
        }, "test");

        Publisher<Payload> payloadPublisher = client.requestResponse(new Payload() {
            @Override
            public ByteBuffer getData() {
                return null;
            }

            @Override
            public ByteBuffer getMetadata() {
                return null;
            }
        });

        TestSubscriber subscriber = new TestSubscriber();
        RxReactiveStreams.toObservable(payloadPublisher).subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.assertError(RuntimeException.class);

        Assert.assertEquals(1, client.failure.get());

    }

    @Test
    public void testHistogram() {
        ServoMetricsReactiveSocketClient client = new ServoMetricsReactiveSocketClient(new ReactiveSocketClient() {
            @Override
            public Publisher<Payload> requestResponse(Payload payload) {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return s -> {
                    s.onSubscribe(EmptySubscription.INSTANCE);
                    s.onNext(new Payload() {
                        @Override
                        public ByteBuffer getData() {
                            return null;
                        }

                        @Override
                        public ByteBuffer getMetadata() {
                            return null;
                        }
                    });

                    s.onComplete();
                };
            }

            @Override
            public void close() throws Exception {

            }
        }, "test");

        for (int i = 0; i < 10; i ++) {
            Publisher<Payload> payloadPublisher = client.requestResponse(new Payload() {
                @Override
                public ByteBuffer getData() {
                    return null;
                }

                @Override
                public ByteBuffer getMetadata() {
                    return null;
                }
            });

            TestSubscriber subscriber = new TestSubscriber();
            RxReactiveStreams.toObservable(payloadPublisher).subscribe(subscriber);
            subscriber.awaitTerminalEvent();
            subscriber.assertNoErrors();
        }

        Assert.assertEquals(10, client.success.get());
        Assert.assertEquals(0, client.failure.get());

        System.out.println(client.timer.histrogramToString());

        Assert.assertNotNull(client.timer.histrogramToString());
        Assert.assertNotEquals(client.timer.getMax(), client.timer.getMin());
    }
}
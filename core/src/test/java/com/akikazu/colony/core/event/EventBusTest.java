package com.akikazu.colony.core.event;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest
{
    private record SampleEvent(int payload) implements Event
    {
    }

    private record OtherEvent(String tag) implements Event
    {
    }

    @Test
    void post_delivers_event_to_consumer_subscriber()
    {
        EventBus bus = EventBuses.create("test");
        AtomicInteger received = new AtomicInteger();

        bus.subscribe(SampleEvent.class, e -> received.set(e.payload()));
        bus.post(new SampleEvent(42));

        assertEquals(42, received.get());
    }

    @Test
    void post_delivers_event_to_each_of_multiple_subscribers()
    {
        EventBus bus = EventBuses.create("test");
        AtomicInteger countA = new AtomicInteger();
        AtomicInteger countB = new AtomicInteger();

        bus.subscribe(SampleEvent.class, e -> countA.incrementAndGet());
        bus.subscribe(SampleEvent.class, e -> countB.incrementAndGet());
        bus.post(new SampleEvent(1));

        assertEquals(1, countA.get());
        assertEquals(1, countB.get());
    }

    @Test
    void subscriber_only_receives_events_of_its_own_type()
    {
        EventBus bus = EventBuses.create("test");
        AtomicInteger sampleCount = new AtomicInteger();

        bus.subscribe(SampleEvent.class, e -> sampleCount.incrementAndGet());
        bus.post(new OtherEvent("ignored"));
        bus.post(new SampleEvent(1));

        assertEquals(1, sampleCount.get());
    }

    @Test
    void unsubscribe_stops_further_delivery()
    {
        EventBus bus = EventBuses.create("test");
        AtomicInteger received = new AtomicInteger();
        Consumer<SampleEvent> handler = e -> received.incrementAndGet();

        bus.subscribe(SampleEvent.class, handler);
        bus.post(new SampleEvent(1));
        bus.unsubscribe(SampleEvent.class, handler);
        bus.post(new SampleEvent(2));

        assertEquals(1, received.get());
    }

    public static final class ReflectiveListener
    {
        final AtomicInteger samples = new AtomicInteger();
        final AtomicInteger others = new AtomicInteger();

        @Subscribe
        public void onSample(SampleEvent event)
        {
            samples.addAndGet(event.payload());
        }

        @Subscribe
        public void onOther(OtherEvent event)
        {
            others.incrementAndGet();
        }
    }

    @Test
    void subscribeReflective_dispatches_to_annotated_methods()
    {
        EventBus bus = EventBuses.create("test");
        ReflectiveListener listener = new ReflectiveListener();

        bus.subscribeReflective(listener);
        bus.post(new SampleEvent(7));
        bus.post(new OtherEvent("hi"));

        assertEquals(7, listener.samples.get());
        assertEquals(1, listener.others.get());
    }

    @Test
    void unsubscribeReflective_stops_further_delivery()
    {
        EventBus bus = EventBuses.create("test");
        ReflectiveListener listener = new ReflectiveListener();

        bus.subscribeReflective(listener);
        bus.post(new SampleEvent(3));
        bus.unsubscribeReflective(listener);
        bus.post(new SampleEvent(4));

        assertEquals(3, listener.samples.get());
    }
}

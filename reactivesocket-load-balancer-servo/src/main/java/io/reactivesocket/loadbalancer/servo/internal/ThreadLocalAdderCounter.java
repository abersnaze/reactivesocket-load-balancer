package io.reactivesocket.loadbalancer.servo.internal;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;

import java.util.List;

/**
 * A {@link Counter} implementation that uses {@link ThreadLocalAdderCounter}
 */
public class ThreadLocalAdderCounter extends AbstractMonitor<Number> implements Counter {
    private ThreadLocalAdder adder = new ThreadLocalAdder();

    public static ThreadLocalAdderCounter newThreadLocalAdderCounter(String name) {
        MonitorConfig.Builder builder = MonitorConfig.builder(name);
        MonitorConfig config = builder.build();

        ThreadLocalAdderCounter threadLocalAdderCounter = new ThreadLocalAdderCounter(config);
        DefaultMonitorRegistry.getInstance().register(threadLocalAdderCounter);

        return threadLocalAdderCounter;
    }

    public static ThreadLocalAdderCounter newThreadLocalAdderCounter(String name, List<Tag> tags) {
        MonitorConfig.Builder builder = MonitorConfig.builder(name);
        builder.withTags(tags);
        MonitorConfig config = builder.build();

        ThreadLocalAdderCounter threadLocalAdderCounter = new ThreadLocalAdderCounter(config);
        DefaultMonitorRegistry.getInstance().register(threadLocalAdderCounter);

        return threadLocalAdderCounter;
    }


    /**
     * Creates a new instance of the counter.
     */
    public ThreadLocalAdderCounter(MonitorConfig config) {
        super(config.withAdditionalTag(DataSourceType.COUNTER));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        adder.increment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(long amount) {
        adder.increment(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getValue(int pollerIdx) {
        return adder.get();
    }

    public long get() {
        return adder.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ThreadLocalAdderCounter)) {
            return false;
        }
        ThreadLocalAdderCounter m = (ThreadLocalAdderCounter) obj;
        return config.equals(m.getConfig()) && adder.get() == m.adder.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = config.hashCode();
        long n = adder.get();
        result = 31 * result + (int) (n ^ (n >>> 32));
        return result;
    }

}
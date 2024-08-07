package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static java.util.Objects.nonNull;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import org.apache.cxf.service.invoker.Invoker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides factory methods for creating an instrumented CXF invoker chain.
 *
 * @see InstrumentedInvokers
 * @see com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener
 */
public class InstrumentedInvokerFactory {

    private final MetricRegistry metricRegistry;

    /**
     * Factory method for TimedInvoker.
     */
    private Invoker timed(Invoker invoker, List<Method> timedMethods) {

        var timers = new ImmutableMap.Builder<String, Timer>();

        for (var method : timedMethods) {
            var timed = method.getAnnotation(Timed.class);
            var name = chooseName(timed.name(), timed.absolute(), method);
            var timer = metricRegistry.timer(name);
            timers.put(method.getName(), timer);
        }

        return new InstrumentedInvokers.TimedInvoker(invoker, timers.build());
    }

    /**
     * Factory method for MeteredInvoker.
     */
    private Invoker metered(Invoker invoker, List<Method> meteredMethods) {

        var meters = new ImmutableMap.Builder<String, Meter>();

        for (var method : meteredMethods) {
            var metered = method.getAnnotation(Metered.class);
            var name = chooseName(metered.name(), metered.absolute(), method);
            var meter = metricRegistry.meter(name);
            meters.put(method.getName(), meter);
        }

        return new InstrumentedInvokers.MeteredInvoker(invoker, meters.build());
    }

    /**
     * Factory method for ExceptionMeteredInvoker.
     */
    private Invoker exceptionMetered(Invoker invoker, List<Method> meteredMethods) {

        var meters = new ImmutableMap.Builder<String, InstrumentedInvokers.ExceptionMeter>();

        for (var method : meteredMethods) {

            var exceptionMetered = method.getAnnotation(ExceptionMetered.class);
            var name = chooseName(
                    exceptionMetered.name(),
                    exceptionMetered.absolute(),
                    method,
                    ExceptionMetered.DEFAULT_NAME_SUFFIX);
            var meter = metricRegistry.meter(name);
            meters.put(method.getName(), new InstrumentedInvokers.ExceptionMeter(meter, exceptionMetered.cause()));
        }

        return new InstrumentedInvokers.ExceptionMeteredInvoker(invoker, meters.build());
    }

    /**
     * Based on the private chooseName method in
     * com.codahale.metrics.jerseyX.InstrumentedResourceMethodApplicationListener,
     * where X is a Jersey version such as 2, 3, 31 (for 3.1), etc.
     *
     * @see com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener
     */
    private String chooseName(String explicitName, boolean absolute, Method method, String... suffixes) {
        if (nonNull(explicitName) && !explicitName.isEmpty()) {
            if (absolute) {
                return explicitName;
            }
            return MetricRegistry.name(method.getDeclaringClass(), explicitName);
        }
        return MetricRegistry.name(
                MetricRegistry.name(method.getDeclaringClass(),
                        method.getName()),
                suffixes
        );
    }

    /**
     * @param metricRegistry Metric registry.
     */
    public InstrumentedInvokerFactory(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }


    /**
     * Factory method for creating an instrumented invoker chain.
     */
    public Invoker create(Object service, Invoker rootInvoker) {

        List<Method> timedMethods = new ArrayList<>();
        List<Method> meteredMethods = new ArrayList<>();
        List<Method> exceptionMeteredMethods = new ArrayList<>();

        for (var method : service.getClass().getMethods()) {

            if (method.isAnnotationPresent(Timed.class)) {
                timedMethods.add(method);
            }

            if (method.isAnnotationPresent(Metered.class)) {
                meteredMethods.add(method);
            }

            if (method.isAnnotationPresent(ExceptionMetered.class)) {
                exceptionMeteredMethods.add(method);
            }
        }

        var invoker = rootInvoker;

        if (!timedMethods.isEmpty()) {
            invoker = this.timed(invoker, timedMethods);
        }

        if (!meteredMethods.isEmpty()) {
            invoker = this.metered(invoker, meteredMethods);
        }

        if (!exceptionMeteredMethods.isEmpty()) {
            invoker = this.exceptionMetered(invoker, exceptionMeteredMethods);
        }

        return invoker;
    }

}

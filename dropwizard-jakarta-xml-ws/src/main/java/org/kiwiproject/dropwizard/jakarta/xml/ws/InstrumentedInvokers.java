package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

/**
 * Provides instrumented CXF invoker implementations.
 *
 * @see com.codahale.metrics.jersey3.InstrumentedResourceMethodApplicationListener
 */
public class InstrumentedInvokers {

    private InstrumentedInvokers() {
        // utility class
    }

    /**
     * Wraps underlying invoker and manages timers for methods annotated with @Timed.
     */
    public static class TimedInvoker extends AbstractInvoker {

        private final ImmutableMap<String, Timer> timers;

        public TimedInvoker(Invoker underlying, ImmutableMap<String, Timer> timers) {
            super(underlying);
            this.timers = timers;
        }

        @Override
        public Object invoke(Exchange exchange, Object o) {

            Object result;
            var methodName = this.getTargetMethod(exchange).getName();

            if (timers.containsKey(methodName)) {
                var timer = requireNonNull(timers.get(methodName));

                // Timer.Context is AutoCloseable, so this starts and closes (stops) the Timer.Context
                try (var ignored = timer.time()) {
                    result = this.underlying.invoke(exchange, o);
                }
            } else {
                result = this.underlying.invoke(exchange, o);
            }
            return result;
        }
    }

    /**
     * Wraps underlying invoker and manages meters for methods annotated with @Metered.
     */
    public static class MeteredInvoker extends AbstractInvoker {

        private final ImmutableMap<String, Meter> meters;

        public MeteredInvoker(Invoker underlying, ImmutableMap<String, Meter> meters) {
            super(underlying);
            this.meters = meters;
        }

        @Override
        public Object invoke(Exchange exchange, Object o) {

            Object result;
            var methodName = this.getTargetMethod(exchange).getName();

            if (meters.containsKey(methodName)) {
                var meter = requireNonNull(meters.get(methodName));
                meter.mark();
            }
            result = this.underlying.invoke(exchange, o);
            return result;
        }
    }

    public static class ExceptionMeter {
        private final Meter meter;
        private final Class<? extends Throwable> exceptionClass;

        public ExceptionMeter(Meter meter, Class<? extends Throwable> exceptionClass) {
            this.meter = meter;
            this.exceptionClass = exceptionClass;
        }

        public Meter getMeter() {
            return meter;
        }

        public Class<? extends Throwable> getExceptionClass() {
            return exceptionClass;
        }
    }

    /**
     * Wraps underlying invoker and manages meters for methods annotated with @ExceptionMetered.
     */
    public static class ExceptionMeteredInvoker extends AbstractInvoker {

        private final ImmutableMap<String, ExceptionMeter> meters;

        public ExceptionMeteredInvoker(Invoker underlying, ImmutableMap<String, ExceptionMeter> meters) {
            super(underlying);
            this.meters = meters;
        }

        @Override
        public Object invoke(Exchange exchange, Object o) {

            Object result;
            var methodName = this.getTargetMethod(exchange).getName();

            try {
                result = this.underlying.invoke(exchange, o);
                return result;
            } catch (Exception e) {

                if (meters.containsKey(methodName)) {
                    var exceptionMeter = requireNonNull(meters.get(methodName));
                    var exceptionClass = exceptionMeter.getExceptionClass();
                    if (exceptionClass.isAssignableFrom(e.getClass()) ||
                            (nonNull(e.getCause()) &&
                                    exceptionClass.isAssignableFrom(e.getCause().getClass()))) {
                        exceptionMeter.getMeter().mark();
                    }
                }
                this.rethrow(e); // unchecked rethrow
                return null; // avoid compiler warning
            }
        }

    }

}

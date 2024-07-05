package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;


class InstrumentedInvokerFactoryTest {

    // Test service implementation
    static class InstrumentedService {

        public String foo() {
            return "fooReturn";
        }

        @Metered
        public String metered() {
            return "meteredReturn";
        }

        @Timed
        public String timed() {
            return "timedReturn";
        }

        @ExceptionMetered
        public String exceptionMetered(boolean doThrow) {
            if (doThrow) {
                throw new RuntimeException("Runtime exception occurred");
            } else {
                return "exceptionMeteredReturn";
            }
        }
    }

    MetricRegistry testMetricRegistry;
    MetricRegistry mockMetricRegistry;
    InstrumentedInvokerFactory invokerBuilder;
    InstrumentedService instrumentedService;
    // CXF Exchange contains message exchange and is used by Invoker to obtain invoked method name
    Exchange exchange;

    /* Invokers that invoke test service implementation */

    class FooInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.foo();
        }
    }

    class MeteredInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.metered();
        }
    }

    class TimedInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.timed();
        }
    }

    public class ExceptionMeteredInvoker implements Invoker {
        private final boolean doThrow;

        public ExceptionMeteredInvoker(boolean doThrow) {
            this.doThrow = doThrow;
        }

        @Override
        public Object invoke(Exchange exchange, Object o) {
            return instrumentedService.exceptionMetered(doThrow);
        }
    }

    /**
     * Utility method that mimics runtime CXF behaviour. Enables AbstractInvoker.getTargetMethod to work properly
     * during the test.
     */
    private void setTargetMethod(Exchange exchange, String methodName, Class<?>... parameterTypes) {
        try {
            var operationInfo = exchange.getBindingOperationInfo().getOperationInfo();
            when(operationInfo.getProperty(Method.class.getName()))
                    .thenReturn(InstrumentedService.class.getMethod(methodName, parameterTypes));
        } catch (Exception e) {
            throw new RuntimeException("setTargetMethod failed", e);
        }
    }

    @BeforeEach
    void setUp() {
        exchange = mock(Exchange.class);

        var bindingOperationInfo = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOperationInfo);

        var operationInfo = mock(OperationInfo.class);
        when(bindingOperationInfo.getOperationInfo()).thenReturn(operationInfo);

        testMetricRegistry = new MetricRegistry();
        mockMetricRegistry = mock(MetricRegistry.class);

        invokerBuilder = new InstrumentedInvokerFactory(mockMetricRegistry);
        instrumentedService = new InstrumentedService();
    }

    @Test
    void noAnnotation() {
        var timer = testMetricRegistry.timer("timed");
        var meter = testMetricRegistry.meter("metered");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(anyString())).thenReturn(meter);

        var oldTimerValue = timer.getCount();
        var oldMeterValue = meter.getCount();

        var invoker = invokerBuilder.create(instrumentedService, new FooInvoker());
        this.setTargetMethod(exchange, "foo"); // simulate CXF behavior

        var result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("fooReturn");

        assertThat(timer.getCount()).isEqualTo(oldTimerValue);
        assertThat(meter.getCount()).isEqualTo(oldMeterValue);
    }

    @Test
    void meteredAnnotation() {
        var timer = testMetricRegistry.timer("timed");
        var meter = testMetricRegistry.meter("metered");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(anyString())).thenReturn(meter);

        var oldTimerValue = timer.getCount();
        var oldMeterValue = meter.getCount();

        var invoker = invokerBuilder.create(instrumentedService, new MeteredInvoker());
        this.setTargetMethod(exchange, "metered"); // simulate CXF behavior

        var result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("meteredReturn");

        assertThat(timer.getCount()).isEqualTo(oldTimerValue);
        assertThat(meter.getCount()).isEqualTo(1 + oldMeterValue);
    }

    @Test
    void timedAnnotation() {
        var timer = testMetricRegistry.timer("timed");
        var meter = testMetricRegistry.meter("metered");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(anyString())).thenReturn(meter);

        var oldTimerValue = timer.getCount();
        var oldMeterValue = meter.getCount();

        var invoker = invokerBuilder.create(instrumentedService, new TimedInvoker());
        this.setTargetMethod(exchange, "timed"); // simulate CXF behavior

        var result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("timedReturn");

        assertThat(timer.getCount()).isEqualTo(1 + oldTimerValue);
        assertThat(meter.getCount()).isEqualTo(oldMeterValue);
    }

    @Test
    void exceptionMeteredAnnotation() {
        var timer = testMetricRegistry.timer("timed");
        var meter = testMetricRegistry.meter("metered");
        var exceptionmeter = testMetricRegistry.meter("exceptionMeteredExceptions");
        when(mockMetricRegistry.timer(anyString())).thenReturn(timer);
        when(mockMetricRegistry.meter(contains("metered"))).thenReturn(meter);
        when(mockMetricRegistry.meter(contains("exceptionMetered"))).thenReturn(exceptionmeter);

        var oldTimerValue = timer.getCount();
        var oldMeterValue = meter.getCount();
        var oldExceptionMeterValue = exceptionmeter.getCount();

        // Invoke InstrumentedResource.exceptionMetered without exception being thrown

        var invoker = invokerBuilder.create(instrumentedService, new ExceptionMeteredInvoker(false));
        this.setTargetMethod(exchange, "exceptionMetered", boolean.class); // simulate CXF behavior

        var result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("exceptionMeteredReturn");

        assertThat(timer.getCount()).isEqualTo(oldTimerValue);
        assertThat(meter.getCount()).isEqualTo(oldMeterValue);
        assertThat(exceptionmeter.getCount()).isEqualTo(oldExceptionMeterValue);

        // Invoke InstrumentedResource.exceptionMetered with exception being thrown

        var throwingInvoker = invokerBuilder.create(instrumentedService, new ExceptionMeteredInvoker(true));

        assertThatRuntimeException().isThrownBy(() -> throwingInvoker.invoke(exchange, null));

        assertThat(timer.getCount()).isEqualTo(oldTimerValue);
        assertThat(meter.getCount()).isEqualTo(oldMeterValue);
        assertThat(exceptionmeter.getCount()).isEqualTo(1 + oldExceptionMeterValue);
    }

}

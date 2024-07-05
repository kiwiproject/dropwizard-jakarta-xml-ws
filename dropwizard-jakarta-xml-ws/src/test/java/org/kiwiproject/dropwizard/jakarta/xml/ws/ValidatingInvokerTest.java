package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.dropwizard.validation.Validated;
import io.dropwizard.validation.ValidationMethod;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.xml.ws.AsyncHandler;
import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

class ValidatingInvokerTest {

    ValidatingInvoker invoker;
    Invoker underlying;
    Exchange exchange;

    static class ChildParam {
        @NotEmpty
        private final String foo;

        public ChildParam(String foo) {
            this.foo = foo;
        }

        @ValidationMethod(message = "foo may not be 'John'")
        public boolean isNotJohn() {
            return !("John".equals(foo));
        }
    }

    @SuppressWarnings("all")
    static class RootParam1 {
        @Valid
        private final ChildParam child;

        public RootParam1(ChildParam childParam) {
            this.child = childParam;
        }
    }

    @SuppressWarnings("all")
    static class RootParam2 {
        @NotEmpty
        private final String foo;

        public RootParam2(String foo) {
            this.foo = foo;
        }
    }

    @SuppressWarnings("all")
    static class DummyService {
        public void noParams() {
        }

        public void noValidation(RootParam1 rootParam1, RootParam2 rootParam2) {
        }

        public void withValidation(@Valid RootParam1 rootParam1, @Valid RootParam2 rootParam2) {
        }

        public void withDropwizardValidation(@Validated() String foo) {
        }

        @UseAsyncMethod
        public void asyncMethod(String foo) {
        }

        public void asyncMethodAsync(String foo, AsyncHandler<String> asyncHandler) {
        }
    }

    @BeforeEach
    void setup() {
        underlying = mock(Invoker.class);
        invoker = new ValidatingInvoker(underlying, Validation.buildDefaultValidatorFactory().getValidator());
        exchange = mock(Exchange.class);
        when(exchange.getInMessage()).thenReturn(mock(Message.class));
        var bindingOperationInfo = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOperationInfo);
        var operationInfo = mock(OperationInfo.class);
        when(bindingOperationInfo.getOperationInfo()).thenReturn(operationInfo);
    }

    /**
     * Utility method that mimics runtime CXF behavior. Enables AbstractInvoker.getTargetMethod to work properly
     * during the test.
     */
    private void setTargetMethod(Exchange exchange, String methodName, Class<?>... parameterTypes) {
        try {
            var operationInfo = exchange.getBindingOperationInfo().getOperationInfo();
            when(operationInfo.getProperty(Method.class.getName()))
                    .thenReturn(DummyService.class.getMethod(methodName, parameterTypes));
        } catch (Exception e) {
            throw new RuntimeException("setTargetMethod failed", e);
        }
    }

    @Test
    void invokeWithoutParams() {
        setTargetMethod(exchange, "noParams");
        invoker.invoke(exchange, null);
        verify(underlying).invoke(exchange, null);
    }

    @Test
    void invokeWithoutValidation() {
        setTargetMethod(exchange, "noValidation", RootParam1.class, RootParam2.class);

        var params = Arrays.asList(null, null);
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);

        params = Arrays.asList(new RootParam1(null), new RootParam2(null));
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);
    }

    @Test
    void invokeWithAsyncHandler() {
        setTargetMethod(exchange, "asyncMethod", String.class);

        List<Object> params = Arrays.asList(null, (AsyncHandler<String>) res -> {
            // no-op
        });
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);

        params = Arrays.asList("foo", (AsyncHandler<String>) res -> {
            // no-op
        });
        invoker.invoke(exchange, params);
        verify(underlying).invoke(exchange, params);
    }

    @Test
    void invokeWithValidation() {
        setTargetMethod(exchange, "withValidation", RootParam1.class, RootParam2.class);

        var params1 = List.of(new RootParam1(new ChildParam("")), new RootParam2("ok"));
        assertThatThrownBy(() -> invoker.invoke(exchange, params1))
                .isExactlyInstanceOf(ValidationException.class);

        var params2 = List.of(new RootParam1(new ChildParam("ok")), new RootParam2(""));
        assertThatThrownBy(() -> invoker.invoke(exchange, params2))
                .isExactlyInstanceOf(ValidationException.class);

        var params3 = List.of(new RootParam1(new ChildParam("John")), new RootParam2("ok"));
        assertThatThrownBy(() -> invoker.invoke(exchange, params3))
                .isExactlyInstanceOf(ValidationException.class)
                .hasMessageContaining("foo may not be 'John'");

        verifyNoMoreInteractions(underlying);

        var params = List.of(new RootParam1(new ChildParam("ok")), new RootParam2("ok"));
        invoker.invoke(exchange, params);

        verify(underlying).invoke(exchange, params);
    }
}

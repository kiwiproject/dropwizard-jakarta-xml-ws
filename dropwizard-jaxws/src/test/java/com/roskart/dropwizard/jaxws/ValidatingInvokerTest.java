package com.roskart.dropwizard.jaxws;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
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
import jakarta.xml.ws.Response;
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

    static class RootParam1 {
        @Valid
        private final ChildParam child;

        public RootParam1(ChildParam childParam) {
            this.child = childParam;
        }
    }

    static class RootParam2 {
        @NotEmpty
        private final String foo;

        public RootParam2(String foo) {
            this.foo = foo;
        }
    }

    class DummyService {
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
        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(boi);
        OperationInfo oi = mock(OperationInfo.class);
        when(boi.getOperationInfo()).thenReturn(oi);
    }

    /**
     * Utility method that mimics runtime CXF behaviour. Enables AbstractInvoker.getTargetMethod to work properly
     * during the test.
     */
    private void setTargetMethod(Exchange exchange, String methodName, Class<?>... parameterTypes) {
        try {
            OperationInfo oi = exchange.getBindingOperationInfo().getOperationInfo();
            when(oi.getProperty(Method.class.getName()))
                    .thenReturn(DummyService.class.getMethod(methodName, parameterTypes));
        } catch (Exception e) {
            fail("setTargetMethod failed", e);
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

        List<Object> params = Arrays.asList(null, null);
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

        params = Arrays.asList("foo", new AsyncHandler<String>() {
            @Override
            public void handleResponse(Response<String> res) {

            }
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

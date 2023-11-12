package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.dropwizard.hibernate.UnitOfWork;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class UnitOfWorkInvokerFactoryTest {

    static class FooService {
        public String foo() {
            return "foo return";
        }

        @UnitOfWork
        public String unitOfWork(boolean throwException) {
            if (throwException)
                throw new RuntimeException("Uh oh");
            else
                return "unitOfWork return";
        }

    }

    public class FooInvoker implements Invoker {
        @Override
        public Object invoke(Exchange exchange, Object o) {
            return fooService.foo();
        }
    }

    static class BarService {
        public String bar() {
            return "bar";
        }
    }

    public class BarInvoker implements Invoker {

        @Override
        public Object invoke(Exchange exchange, Object o) {
            return barService.bar();
        }
    }

    public class UnitOfWorkInvoker implements Invoker {
        private final boolean doThrow;

        public UnitOfWorkInvoker(boolean doThrow) {
            this.doThrow = doThrow;
        }

        @Override
        public Object invoke(Exchange exchange, Object o) {
            return fooService.unitOfWork(doThrow);
        }
    }

    UnitOfWorkInvokerFactory invokerBuilder;
    FooService fooService;
    BarService barService;
    SessionFactory sessionFactory;
    Session session;
    Transaction transaction;

    // CXF Exchange contains message exchange and is used by Invoker to obtain invoked method name
    Exchange exchange;

    @BeforeEach
    void setUp() {
        exchange = mock(Exchange.class);
        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(boi);
        OperationInfo oi = mock(OperationInfo.class);
        when(boi.getOperationInfo()).thenReturn(oi);
        invokerBuilder = new UnitOfWorkInvokerFactory();
        fooService = new FooService();
        barService = new BarService();
        sessionFactory = mock(SessionFactory.class);
        session = mock(Session.class);
        when(sessionFactory.openSession()).thenReturn(session);
        transaction = mock(Transaction.class);
        when(session.getTransaction()).thenReturn(transaction);
        when(transaction.getStatus()).thenReturn(TransactionStatus.ACTIVE);
    }

    /**
     * Utility method that mimics runtime CXF behaviour. Enables AbstractInvoker.getTargetMethod to work properly
     * during the test.
     */
    private void setTargetMethod(Exchange exchange, Class<?> serviceClass, String methodName, Class<?>... parameterTypes) {

        try {
            OperationInfo oi = exchange.getBindingOperationInfo().getOperationInfo();
            when(oi.getProperty(Method.class.getName()))
                    .thenReturn(serviceClass.getMethod(methodName, parameterTypes));
        } catch (Exception e) {
            throw new RuntimeException("setTargetMethod failed", e);
        }
    }

    @Test
    void noAnnotation() {
        Invoker invoker = invokerBuilder.create(fooService, new FooInvoker(), sessionFactory);
        this.setTargetMethod(exchange, FooService.class, "foo"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("foo return");

        verifyNoInteractions(sessionFactory);
        verifyNoInteractions(session);
        verifyNoInteractions(transaction);
    }

    @Test
    void shouldCallService_WhenNoMethodsAreAnnotatedWith_UnitOfWork() {
        var invoker = invokerBuilder.create(barService, new BarInvoker(), sessionFactory);
        this.setTargetMethod(exchange, BarService.class, "bar");

        var result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("bar");

        verifyNoInteractions(sessionFactory);
        verifyNoInteractions(session);
        verifyNoInteractions(transaction);
    }

    @Test
    void unitOfWorkAnnotation() {
        // use underlying invoker which invokes fooService.unitOfWork(false)
        Invoker invoker = invokerBuilder.create(fooService, new UnitOfWorkInvoker(false), sessionFactory);
        this.setTargetMethod(exchange, FooService.class, "unitOfWork", boolean.class); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertThat(result).isEqualTo("unitOfWork return");

        verify(session, times(1)).beginTransaction();
        verify(transaction, times(1)).commit();
        verify(transaction, times(0)).rollback();
        verify(session, times(1)).close();
    }

    @Test
    void unitOfWorkWithException() {
        // use underlying invoker which invokes fooService.unitOfWork(true) - exception is thrown
        Invoker invoker = invokerBuilder.create(fooService, new UnitOfWorkInvoker(true), sessionFactory);
        this.setTargetMethod(exchange, FooService.class, "unitOfWork", boolean.class);  // simulate CXF behavior

        assertThatRuntimeException()
                .isThrownBy(() -> invoker.invoke(exchange, null))
                .withMessage("Uh oh");

        verify(session, times(1)).beginTransaction();
        verify(transaction, times(0)).commit();
        verify(transaction, times(1)).rollback();
        verify(session, times(1)).close();
    }
}

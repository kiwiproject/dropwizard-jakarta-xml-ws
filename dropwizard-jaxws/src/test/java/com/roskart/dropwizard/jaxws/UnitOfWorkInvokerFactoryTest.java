package com.roskart.dropwizard.jaxws;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UnitOfWorkInvokerFactoryTest {

    class FooService {
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

    public class UnitOfWorkInvoker implements Invoker {
        private boolean doThrow = false;
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
    SessionFactory sessionFactory;
    Session session;
    Transaction transaction;

    // CXF Exchange contains message exchange and is used by Invoker to obtain invoked method name
    Exchange exchange;

    @BeforeEach
    void setup() {
        exchange = mock(Exchange.class);
        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        when(exchange.getBindingOperationInfo()).thenReturn(boi);
        OperationInfo oi = mock(OperationInfo.class);
        when(boi.getOperationInfo()).thenReturn(oi);
        invokerBuilder = new UnitOfWorkInvokerFactory();
        fooService = new FooService();
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
    private void setTargetMethod(Exchange exchange, String methodName, Class<?>... parameterTypes) {

        try {
            OperationInfo oi = exchange.getBindingOperationInfo().getOperationInfo();
            when(oi.getProperty(Method.class.getName()))
                    .thenReturn(FooService.class.getMethod(methodName, parameterTypes));
        }
        catch (Exception e) {
            fail("setTargetMethod failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Test
    void noAnnotation() {
        Invoker invoker = invokerBuilder.create(fooService, new FooInvoker(), null);
        this.setTargetMethod(exchange, "foo"); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("foo return", result);

        verifyNoInteractions(sessionFactory);
        verifyNoInteractions(session);
        verifyNoInteractions(transaction);
    }

    @Test
    void unitOfWorkAnnotation() {
        // use underlying invoker which invokes fooService.unitOfWork(false)
        Invoker invoker = invokerBuilder.create(fooService, new UnitOfWorkInvoker(false), sessionFactory);
        this.setTargetMethod(exchange, "unitOfWork", boolean.class); // simulate CXF behavior

        Object result = invoker.invoke(exchange, null);
        assertEquals("unitOfWork return", result);

        verify(session, times(1)).beginTransaction();
        verify(transaction, times(1)).commit();
        verify(transaction, times(0)).rollback();
        verify(session, times(1)).close();
    }

    @Test
    void unitOfWorkWithException() {
        // use underlying invoker which invokes fooService.unitOfWork(true) - exception is thrown
        Invoker invoker = invokerBuilder.create(fooService, new UnitOfWorkInvoker(true), sessionFactory);
        this.setTargetMethod(exchange, "unitOfWork", boolean.class); // simulate CXF behavior

        try {
            invoker.invoke(exchange, null);
        }
        catch (Exception e) {
            assertEquals("Uh oh", e.getMessage());
        }

        verify(session, times(1)).beginTransaction();
        verify(transaction, times(0)).commit();
        verify(transaction, times(1)).rollback();
        verify(session, times(1)).close();
    }
}

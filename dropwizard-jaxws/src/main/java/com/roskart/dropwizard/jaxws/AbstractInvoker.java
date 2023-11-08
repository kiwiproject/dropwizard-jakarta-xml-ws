package com.roskart.dropwizard.jaxws;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

import java.lang.reflect.Method;

/**
 * Abstract CXF invoker for wrapping underlying CXF invoker.
 */
public abstract class AbstractInvoker implements Invoker {

    protected final Invoker underlying;

    protected AbstractInvoker(Invoker underlying) {
        this.underlying = underlying;
    }

    /**
     * Utility method for getting the method which is going to be invoked on the service by underlying invoker.
     */
    public Method getTargetMethod(Exchange exchange) {

        Object o = exchange.getBindingOperationInfo().getOperationInfo().getProperty(Method.class.getName());

        if (o instanceof Method method) {
            return method;
        } else {
            throw new IllegalStateException("Target method not found on OperationInfo");
        }

    }

    /**
     * Rethrows exception, without requiring to handle checked exception.
     * Type-erasure happens at compile time, therefore if E is RuntimeException,
     * checked exception can be re-thrown without declaring them.
     */
    @SuppressWarnings("unchecked")
    protected <E extends Exception> void rethrow(Exception e) throws E {
        throw (E) e;
    }


}

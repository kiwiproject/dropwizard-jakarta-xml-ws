package org.kiwiproject.dropwizard.jakarta.xml.ws;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.cxf.service.invoker.Invoker;
import org.hibernate.SessionFactory;

import java.lang.reflect.Method;

public class UnitOfWorkInvokerFactory {

    /**
     * Factory method for creating UnitOfWorkInvoker.
     */
    public Invoker create(Object service, Invoker rootInvoker, SessionFactory sessionFactory) {

        ImmutableMap.Builder<String, UnitOfWork> unitOfWorkMethodsBuilder =
                new ImmutableMap.Builder<>();

        for (Method m : service.getClass().getMethods()) {
            if (m.isAnnotationPresent(UnitOfWork.class)) {
                unitOfWorkMethodsBuilder.put(m.getName(), m.getAnnotation(UnitOfWork.class));
            }
        }
        ImmutableMap<String, UnitOfWork> unitOfWorkMethods = unitOfWorkMethodsBuilder.build();

        Invoker invoker = rootInvoker;

        if (!unitOfWorkMethods.isEmpty()) {
            invoker = new UnitOfWorkInvoker(invoker, unitOfWorkMethods, sessionFactory);
        }

        return invoker;
    }

}

package org.kiwiproject.dropwizard.jakarta.xml.ws;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.cxf.service.invoker.Invoker;
import org.hibernate.SessionFactory;

public class UnitOfWorkInvokerFactory {

    /**
     * Factory method for creating UnitOfWorkInvoker.
     */
    public Invoker create(Object service, Invoker rootInvoker, SessionFactory sessionFactory) {

        var unitOfWorkMethodsMapBuilder = new ImmutableMap.Builder<String, UnitOfWork>();

        for (var method : service.getClass().getMethods()) {
            if (method.isAnnotationPresent(UnitOfWork.class)) {
                unitOfWorkMethodsMapBuilder.put(method.getName(), method.getAnnotation(UnitOfWork.class));
            }
        }
        var unitOfWorkMethodsMap = unitOfWorkMethodsMapBuilder.build();

        if (unitOfWorkMethodsMap.isEmpty()) {
            return rootInvoker;
        }

        return new UnitOfWorkInvoker(rootInvoker, unitOfWorkMethodsMap, sessionFactory);
    }

}

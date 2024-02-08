package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Wraps underlying invoker in a Hibernate session. Code in this class is based on Dropwizard's UnitOfWorkApplication
 * listener and UnitOfWorkAspect.
 * <p>
 * <strong>Background information:</strong>
 * The javadocs used to state "We don't use UnitOfWorkAspect here because it is declared package private." This
 * was true up until Dropwizard 1.1.0, which made UnitOfWorkAspect public in
 * <a href="https://github.com/dropwizard/dropwizard/pull/1661">this PR</a>. See
 * <a href="https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/discussions/91">this discussion</a> which
 * proposes to change this class to use UnitOfWorkAspect directly.
 * 
 *
 * @see io.dropwizard.hibernate.UnitOfWorkAspect
 * @see io.dropwizard.hibernate.UnitOfWorkApplicationListener
 * @see io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory
 */
public class UnitOfWorkInvoker extends AbstractInvoker {

    private final SessionFactory sessionFactory;
    final ImmutableMap<String, UnitOfWork> unitOfWorkMethods;

    public UnitOfWorkInvoker(Invoker underlying, ImmutableMap<String, UnitOfWork> unitOfWorkMethods,
                             SessionFactory sessionFactory) {
        super(underlying);
        this.unitOfWorkMethods = unitOfWorkMethods;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Object invoke(Exchange exchange, Object o) {

        Object result;
        String methodName = this.getTargetMethod(exchange).getName();

        if (unitOfWorkMethods.containsKey(methodName)) {

            try (var session = sessionFactory.openSession()) {
                UnitOfWork unitOfWork = requireNonNull(unitOfWorkMethods.get(methodName));
                configureSession(session, unitOfWork);
                ManagedSessionContext.bind(session);
                beginTransaction(session, unitOfWork);
                try {
                    result = underlying.invoke(exchange, o);
                    commitTransaction(session, unitOfWork);
                    return result;
                } catch (Exception e) {
                    rollbackTransaction(session, unitOfWork);
                    this.rethrow(e); // unchecked rethrow
                    return null; // avoid compiler warning
                }
            } finally {
                ManagedSessionContext.unbind(sessionFactory);
            }
        } else {
            return underlying.invoke(exchange, o);
        }
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#beginTransaction(UnitOfWork, Session)
     */
    @SuppressWarnings("JavadocReference")
    private void beginTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            session.beginTransaction();
        }
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#configureSession()
     */
    @SuppressWarnings("JavadocReference")
    private void configureSession(Session session, UnitOfWork unitOfWork) {
        session.setDefaultReadOnly(unitOfWork.readOnly());
        session.setCacheMode(unitOfWork.cacheMode());
        session.setHibernateFlushMode(unitOfWork.flushMode());
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#rollbackTransaction(UnitOfWork, Session)
     */
    @SuppressWarnings("JavadocReference")
    private void rollbackTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            final Transaction txn = session.getTransaction();
            if (txn != null && txn.getStatus().equals(TransactionStatus.ACTIVE)) {
                txn.rollback();
            }
        }
    }

    /**
     * @see io.dropwizard.hibernate.UnitOfWorkAspect#commitTransaction(UnitOfWork, Session)
     */
    @SuppressWarnings("JavadocReference")
    private void commitTransaction(Session session, UnitOfWork unitOfWork) {
        if (unitOfWork.transactional()) {
            final Transaction txn = session.getTransaction();
            if (txn != null && txn.getStatus().equals(TransactionStatus.ACTIVE)) {
                txn.commit();
            }
        }
    }

}

package org.kiwiproject.dropwizard.jakarta.xml.ws;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import com.google.common.collect.ImmutableList;
import jakarta.xml.ws.handler.Handler;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

/**
 * Jakarta XML Web Services/CXF client builder.
 */
public class ClientBuilder<T> extends AbstractBuilder {

    final Class<T> serviceClass;
    final String address;
    private int connectTimeout = 500;
    private int receiveTimeout = 2000;
    @SuppressWarnings("rawtypes")
    ImmutableList<Handler> handlers;
    String bindingId;

    public Class<T> getServiceClass() {
        return serviceClass;
    }

    public String getAddress() {
        return address;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReceiveTimeout() {
        return receiveTimeout;
    }

    @SuppressWarnings("rawtypes")
    public ImmutableList<Handler> getHandlers() {
        return handlers;
    }

    public String getBindingId() {
        return bindingId;
    }

    /**
     * Create a new ClientBuilder. Endpoint will be published relative to the CXF servlet path.
     *
     * @param serviceClass Service interface class.
     * @param address      Endpoint URL address..
     */
    public ClientBuilder(Class<T> serviceClass, String address) {
        checkArgument(nonNull(serviceClass), "ServiceClass is null");
        checkArgument(nonNull(address), "Address is null");
        checkArgument(!address.isBlank(), "Address is empty");
        this.serviceClass = serviceClass;
        this.address = address;
    }

    /**
     * Change default HTTP client connect timeout.
     *
     * @param value Timeout value in milliseconds.
     * @return ClientBuilder instance.
     */
    public ClientBuilder<T> connectTimeout(int value) {
        this.connectTimeout = value;
        return this;
    }

    /**
     * Change default HTTP client receive timeout.
     *
     * @param value Timeout value in milliseconds.
     * @return ClientBuilder instance.
     */
    public ClientBuilder<T> receiveTimeout(int value) {
        this.receiveTimeout = value;
        return this;
    }

    /**
     * Add client side Jakarta XML Web Services handlers.
     *
     * @param handlers Jakarta XML Web Services handlers.
     * @return ClientBuilder instance.
     */
    @SuppressWarnings("rawtypes")
    public ClientBuilder<T> handlers(Handler... handlers) {
        this.handlers = ImmutableList.<Handler>builder().add(handlers).build();
        return this;
    }

    /**
     * Set ClientProxyFactoryBean bindingId.
     *
     * @param bindingId bindingId.
     * @return ClientBuilder instance.
     */
    public ClientBuilder<T> bindingId(String bindingId) {
        this.bindingId = bindingId;
        return this;
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final ClientBuilder<T> cxfInInterceptors(Interceptor<? extends Message>... interceptors) {
        return (ClientBuilder<T>) super.cxfInInterceptors(interceptors);
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final ClientBuilder<T> cxfInFaultInterceptors(Interceptor<? extends Message>... interceptors) {
        return (ClientBuilder<T>) super.cxfInFaultInterceptors(interceptors);
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final ClientBuilder<T> cxfOutInterceptors(Interceptor<? extends Message>... interceptors) {
        return (ClientBuilder<T>) super.cxfOutInterceptors(interceptors);
    }

    @Override
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final ClientBuilder<T> cxfOutFaultInterceptors(Interceptor<? extends Message>... interceptors) {
        return (ClientBuilder<T>) super.cxfOutFaultInterceptors(interceptors);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClientBuilder<T> enableMtom() {
        return (ClientBuilder<T>) super.enableMtom();
    }
}

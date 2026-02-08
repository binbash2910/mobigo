package com.binbash.mobigo.config;

import com.binbash.mobigo.repository.search.*;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Provides no-op (empty) implementations of all Elasticsearch search repositories
 * when Elasticsearch is disabled (application.elasticsearch.enabled=false).
 * This allows the application to start without an Elasticsearch server.
 * Search operations will return empty results and indexing operations will be silently ignored.
 */
@Configuration
@ConditionalOnProperty(name = "application.elasticsearch.enabled", havingValue = "false")
public class NoOpSearchRepositoryConfig {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpSearchRepositoryConfig.class);

    @SuppressWarnings("unchecked")
    private <T> T createNoOpProxy(Class<T> repositoryInterface) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { repositoryInterface }, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == Stream.class) return Stream.empty();
            if (returnType == Page.class) return new PageImpl<>(Collections.emptyList());
            if (returnType == Optional.class) return Optional.empty();
            if (returnType == Iterable.class || returnType == java.util.List.class) return Collections.emptyList();
            if (returnType == boolean.class || returnType == Boolean.class) return false;
            if (returnType == long.class || returnType == Long.class) return 0L;
            if (returnType == int.class || returnType == Integer.class) return 0;
            if (returnType == void.class) return null;
            if (method.getName().equals("toString")) return "NoOp[" + repositoryInterface.getSimpleName() + "]";
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            return null;
        });
    }

    @Bean
    public BookingSearchRepository bookingSearchRepository() {
        LOG.info("Elasticsearch disabled: using no-op BookingSearchRepository");
        return createNoOpProxy(BookingSearchRepository.class);
    }

    @Bean
    public GroupAuthoritySearchRepository groupAuthoritySearchRepository() {
        return createNoOpProxy(GroupAuthoritySearchRepository.class);
    }

    @Bean
    public GroupMemberSearchRepository groupMemberSearchRepository() {
        return createNoOpProxy(GroupMemberSearchRepository.class);
    }

    @Bean
    public GroupSearchRepository groupSearchRepository() {
        return createNoOpProxy(GroupSearchRepository.class);
    }

    @Bean
    public MessageSearchRepository messageSearchRepository() {
        return createNoOpProxy(MessageSearchRepository.class);
    }

    @Bean
    public PaymentSearchRepository paymentSearchRepository() {
        return createNoOpProxy(PaymentSearchRepository.class);
    }

    @Bean
    public PeopleSearchRepository peopleSearchRepository() {
        return createNoOpProxy(PeopleSearchRepository.class);
    }

    @Bean
    public RatingSearchRepository ratingSearchRepository() {
        return createNoOpProxy(RatingSearchRepository.class);
    }

    @Bean
    public RideSearchRepository rideSearchRepository() {
        return createNoOpProxy(RideSearchRepository.class);
    }

    @Bean
    public SavedPaymentMethodSearchRepository savedPaymentMethodSearchRepository() {
        return createNoOpProxy(SavedPaymentMethodSearchRepository.class);
    }

    @Bean
    public StepSearchRepository stepSearchRepository() {
        return createNoOpProxy(StepSearchRepository.class);
    }

    @Bean
    public UserSearchRepository userSearchRepository() {
        return createNoOpProxy(UserSearchRepository.class);
    }

    @Bean
    public VehicleSearchRepository vehicleSearchRepository() {
        return createNoOpProxy(VehicleSearchRepository.class);
    }
}

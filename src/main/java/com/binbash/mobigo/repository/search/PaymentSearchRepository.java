package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.repository.PaymentRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Payment} entity.
 */
public interface PaymentSearchRepository extends ElasticsearchRepository<Payment, Long>, PaymentSearchRepositoryInternal {}

interface PaymentSearchRepositoryInternal {
    Stream<Payment> search(String query);

    Stream<Payment> search(Query query);

    @Async
    void index(Payment entity);

    @Async
    void deleteFromIndexById(Long id);
}

class PaymentSearchRepositoryInternalImpl implements PaymentSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final PaymentRepository repository;

    PaymentSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, PaymentRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Payment> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Payment> search(Query query) {
        return elasticsearchTemplate.search(query, Payment.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Payment entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Payment.class);
    }
}

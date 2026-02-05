package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.SavedPaymentMethod;
import com.binbash.mobigo.repository.SavedPaymentMethodRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link SavedPaymentMethod} entity.
 */
public interface SavedPaymentMethodSearchRepository
    extends ElasticsearchRepository<SavedPaymentMethod, Long>, SavedPaymentMethodSearchRepositoryInternal {}

interface SavedPaymentMethodSearchRepositoryInternal {
    Stream<SavedPaymentMethod> search(String query);

    Stream<SavedPaymentMethod> search(Query query);

    @Async
    void index(SavedPaymentMethod entity);

    @Async
    void deleteFromIndexById(Long id);
}

class SavedPaymentMethodSearchRepositoryInternalImpl implements SavedPaymentMethodSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final SavedPaymentMethodRepository repository;

    SavedPaymentMethodSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, SavedPaymentMethodRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<SavedPaymentMethod> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<SavedPaymentMethod> search(Query query) {
        return elasticsearchTemplate.search(query, SavedPaymentMethod.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(SavedPaymentMethod entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), SavedPaymentMethod.class);
    }
}

package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Step;
import com.binbash.mobigo.repository.StepRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Step} entity.
 */
public interface StepSearchRepository extends ElasticsearchRepository<Step, Long>, StepSearchRepositoryInternal {}

interface StepSearchRepositoryInternal {
    Stream<Step> search(String query);

    Stream<Step> search(Query query);

    @Async
    void index(Step entity);

    @Async
    void deleteFromIndexById(Long id);
}

class StepSearchRepositoryInternalImpl implements StepSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final StepRepository repository;

    StepSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, StepRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Step> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Step> search(Query query) {
        return elasticsearchTemplate.search(query, Step.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Step entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Step.class);
    }
}

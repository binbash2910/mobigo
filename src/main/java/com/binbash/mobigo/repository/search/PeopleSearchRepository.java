package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link People} entity.
 */
public interface PeopleSearchRepository extends ElasticsearchRepository<People, Long>, PeopleSearchRepositoryInternal {}

interface PeopleSearchRepositoryInternal {
    Stream<People> search(String query);

    Stream<People> search(Query query);

    @Async
    void index(People entity);

    @Async
    void deleteFromIndexById(Long id);
}

class PeopleSearchRepositoryInternalImpl implements PeopleSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final PeopleRepository repository;

    PeopleSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, PeopleRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<People> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<People> search(Query query) {
        return elasticsearchTemplate.search(query, People.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(People entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), People.class);
    }
}

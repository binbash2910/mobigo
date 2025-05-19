package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.repository.RideRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Ride} entity.
 */
public interface RideSearchRepository extends ElasticsearchRepository<Ride, Long>, RideSearchRepositoryInternal {}

interface RideSearchRepositoryInternal {
    Stream<Ride> search(String query);

    Stream<Ride> search(Query query);

    @Async
    void index(Ride entity);

    @Async
    void deleteFromIndexById(Long id);
}

class RideSearchRepositoryInternalImpl implements RideSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RideRepository repository;

    RideSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, RideRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Ride> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Ride> search(Query query) {
        return elasticsearchTemplate.search(query, Ride.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Ride entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Ride.class);
    }
}

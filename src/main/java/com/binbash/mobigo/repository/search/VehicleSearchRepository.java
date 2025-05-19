package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Vehicle;
import com.binbash.mobigo.repository.VehicleRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Vehicle} entity.
 */
public interface VehicleSearchRepository extends ElasticsearchRepository<Vehicle, Long>, VehicleSearchRepositoryInternal {}

interface VehicleSearchRepositoryInternal {
    Stream<Vehicle> search(String query);

    Stream<Vehicle> search(Query query);

    @Async
    void index(Vehicle entity);

    @Async
    void deleteFromIndexById(Long id);
}

class VehicleSearchRepositoryInternalImpl implements VehicleSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final VehicleRepository repository;

    VehicleSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, VehicleRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Vehicle> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Vehicle> search(Query query) {
        return elasticsearchTemplate.search(query, Vehicle.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Vehicle entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Vehicle.class);
    }
}

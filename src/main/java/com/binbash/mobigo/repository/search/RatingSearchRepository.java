package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Rating;
import com.binbash.mobigo.repository.RatingRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Rating} entity.
 */
public interface RatingSearchRepository extends ElasticsearchRepository<Rating, Long>, RatingSearchRepositoryInternal {}

interface RatingSearchRepositoryInternal {
    Stream<Rating> search(String query);

    Stream<Rating> search(Query query);

    @Async
    void index(Rating entity);

    @Async
    void deleteFromIndexById(Long id);
}

class RatingSearchRepositoryInternalImpl implements RatingSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RatingRepository repository;

    RatingSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, RatingRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Rating> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Rating> search(Query query) {
        return elasticsearchTemplate.search(query, Rating.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Rating entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Rating.class);
    }
}

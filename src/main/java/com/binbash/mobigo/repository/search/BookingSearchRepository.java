package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.repository.BookingRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Booking} entity.
 */
public interface BookingSearchRepository extends ElasticsearchRepository<Booking, Long>, BookingSearchRepositoryInternal {}

interface BookingSearchRepositoryInternal {
    Stream<Booking> search(String query);

    Stream<Booking> search(Query query);

    @Async
    void index(Booking entity);

    @Async
    void deleteFromIndexById(Long id);
}

class BookingSearchRepositoryInternalImpl implements BookingSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final BookingRepository repository;

    BookingSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, BookingRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Booking> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Booking> search(Query query) {
        return elasticsearchTemplate.search(query, Booking.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Booking entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Booking.class);
    }
}

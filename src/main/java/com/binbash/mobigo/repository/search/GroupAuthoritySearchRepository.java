package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.GroupAuthority;
import com.binbash.mobigo.repository.GroupAuthorityRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link GroupAuthority} entity.
 */
public interface GroupAuthoritySearchRepository
    extends ElasticsearchRepository<GroupAuthority, Long>, GroupAuthoritySearchRepositoryInternal {}

interface GroupAuthoritySearchRepositoryInternal {
    Stream<GroupAuthority> search(String query);

    Stream<GroupAuthority> search(Query query);

    @Async
    void index(GroupAuthority entity);

    @Async
    void deleteFromIndexById(Long id);
}

class GroupAuthoritySearchRepositoryInternalImpl implements GroupAuthoritySearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final GroupAuthorityRepository repository;

    GroupAuthoritySearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, GroupAuthorityRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<GroupAuthority> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<GroupAuthority> search(Query query) {
        return elasticsearchTemplate.search(query, GroupAuthority.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(GroupAuthority entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), GroupAuthority.class);
    }
}

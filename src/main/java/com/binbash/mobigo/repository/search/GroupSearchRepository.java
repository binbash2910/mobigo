package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.Group;
import com.binbash.mobigo.repository.GroupRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Group} entity.
 */
public interface GroupSearchRepository extends ElasticsearchRepository<Group, Long>, GroupSearchRepositoryInternal {}

interface GroupSearchRepositoryInternal {
    Stream<Group> search(String query);

    Stream<Group> search(Query query);

    @Async
    void index(Group entity);

    @Async
    void deleteFromIndexById(Long id);
}

class GroupSearchRepositoryInternalImpl implements GroupSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final GroupRepository repository;

    GroupSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, GroupRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<Group> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<Group> search(Query query) {
        return elasticsearchTemplate.search(query, Group.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(Group entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Group.class);
    }
}

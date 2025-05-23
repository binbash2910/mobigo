package com.binbash.mobigo.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.binbash.mobigo.domain.GroupMember;
import com.binbash.mobigo.repository.GroupMemberRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link GroupMember} entity.
 */
public interface GroupMemberSearchRepository extends ElasticsearchRepository<GroupMember, Long>, GroupMemberSearchRepositoryInternal {}

interface GroupMemberSearchRepositoryInternal {
    Stream<GroupMember> search(String query);

    Stream<GroupMember> search(Query query);

    @Async
    void index(GroupMember entity);

    @Async
    void deleteFromIndexById(Long id);
}

class GroupMemberSearchRepositoryInternalImpl implements GroupMemberSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final GroupMemberRepository repository;

    GroupMemberSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, GroupMemberRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<GroupMember> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<GroupMember> search(Query query) {
        return elasticsearchTemplate.search(query, GroupMember.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(GroupMember entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), GroupMember.class);
    }
}

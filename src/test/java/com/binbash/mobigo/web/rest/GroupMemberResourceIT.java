package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.domain.GroupMemberAsserts.*;
import static com.binbash.mobigo.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.GroupMember;
import com.binbash.mobigo.repository.GroupMemberRepository;
import com.binbash.mobigo.repository.UserRepository;
import com.binbash.mobigo.repository.search.GroupMemberSearchRepository;
import com.binbash.mobigo.service.GroupMemberService;
import com.binbash.mobigo.service.dto.GroupMemberDTO;
import com.binbash.mobigo.service.mapper.GroupMemberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link GroupMemberResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class GroupMemberResourceIT {

    private static final String ENTITY_API_URL = "/api/group-members";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/group-members/_search";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepositoryMock;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Mock
    private GroupMemberService groupMemberServiceMock;

    @Autowired
    private GroupMemberSearchRepository groupMemberSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGroupMemberMockMvc;

    private GroupMember groupMember;

    private GroupMember insertedGroupMember;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GroupMember createEntity() {
        return new GroupMember();
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GroupMember createUpdatedEntity() {
        return new GroupMember();
    }

    @BeforeEach
    void initTest() {
        groupMember = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedGroupMember != null) {
            groupMemberRepository.delete(insertedGroupMember);
            groupMemberSearchRepository.delete(insertedGroupMember);
            insertedGroupMember = null;
        }
    }

    @Test
    @Transactional
    void createGroupMember() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);
        var returnedGroupMemberDTO = om.readValue(
            restGroupMemberMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupMemberDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GroupMemberDTO.class
        );

        // Validate the GroupMember in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGroupMember = groupMemberMapper.toEntity(returnedGroupMemberDTO);
        assertGroupMemberUpdatableFieldsEquals(returnedGroupMember, getPersistedGroupMember(returnedGroupMember));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedGroupMember = returnedGroupMember;
    }

    @Test
    @Transactional
    void createGroupMemberWithExistingId() throws Exception {
        // Create the GroupMember with an existing ID
        groupMember.setId(1L);
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restGroupMemberMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupMemberDTO)))
            .andExpect(status().isBadRequest());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllGroupMembers() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);

        // Get all the groupMemberList
        restGroupMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(groupMember.getId().intValue())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGroupMembersWithEagerRelationshipsIsEnabled() throws Exception {
        when(groupMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGroupMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(groupMemberServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGroupMembersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(groupMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGroupMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(groupMemberRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getGroupMember() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);

        // Get the groupMember
        restGroupMemberMockMvc
            .perform(get(ENTITY_API_URL_ID, groupMember.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(groupMember.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingGroupMember() throws Exception {
        // Get the groupMember
        restGroupMemberMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGroupMember() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupMemberSearchRepository.save(groupMember);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());

        // Update the groupMember
        GroupMember updatedGroupMember = groupMemberRepository.findById(groupMember.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGroupMember are not directly saved in db
        em.detach(updatedGroupMember);
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(updatedGroupMember);

        restGroupMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, groupMemberDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupMemberDTO))
            )
            .andExpect(status().isOk());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGroupMemberToMatchAllProperties(updatedGroupMember);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<GroupMember> groupMemberSearchList = Streamable.of(groupMemberSearchRepository.findAll()).toList();
                GroupMember testGroupMemberSearch = groupMemberSearchList.get(searchDatabaseSizeAfter - 1);

                assertGroupMemberAllPropertiesEquals(testGroupMemberSearch, updatedGroupMember);
            });
    }

    @Test
    @Transactional
    void putNonExistingGroupMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        groupMember.setId(longCount.incrementAndGet());

        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, groupMemberDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchGroupMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        groupMember.setId(longCount.incrementAndGet());

        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGroupMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        groupMember.setId(longCount.incrementAndGet());

        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupMemberMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupMemberDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateGroupMemberWithPatch() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupMember using partial update
        GroupMember partialUpdatedGroupMember = new GroupMember();
        partialUpdatedGroupMember.setId(groupMember.getId());

        restGroupMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupMember.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupMember))
            )
            .andExpect(status().isOk());

        // Validate the GroupMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupMemberUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGroupMember, groupMember),
            getPersistedGroupMember(groupMember)
        );
    }

    @Test
    @Transactional
    void fullUpdateGroupMemberWithPatch() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupMember using partial update
        GroupMember partialUpdatedGroupMember = new GroupMember();
        partialUpdatedGroupMember.setId(groupMember.getId());

        restGroupMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupMember.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupMember))
            )
            .andExpect(status().isOk());

        // Validate the GroupMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupMemberUpdatableFieldsEquals(partialUpdatedGroupMember, getPersistedGroupMember(partialUpdatedGroupMember));
    }

    @Test
    @Transactional
    void patchNonExistingGroupMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        groupMember.setId(longCount.incrementAndGet());

        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, groupMemberDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGroupMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        groupMember.setId(longCount.incrementAndGet());

        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGroupMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        groupMember.setId(longCount.incrementAndGet());

        // Create the GroupMember
        GroupMemberDTO groupMemberDTO = groupMemberMapper.toDto(groupMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupMemberMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(groupMemberDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GroupMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteGroupMember() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);
        groupMemberRepository.save(groupMember);
        groupMemberSearchRepository.save(groupMember);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the groupMember
        restGroupMemberMockMvc
            .perform(delete(ENTITY_API_URL_ID, groupMember.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupMemberSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchGroupMember() throws Exception {
        // Initialize the database
        insertedGroupMember = groupMemberRepository.saveAndFlush(groupMember);
        groupMemberSearchRepository.save(groupMember);

        // Search the groupMember
        restGroupMemberMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + groupMember.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(groupMember.getId().intValue())));
    }

    protected long getRepositoryCount() {
        return groupMemberRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected GroupMember getPersistedGroupMember(GroupMember groupMember) {
        return groupMemberRepository.findById(groupMember.getId()).orElseThrow();
    }

    protected void assertPersistedGroupMemberToMatchAllProperties(GroupMember expectedGroupMember) {
        assertGroupMemberAllPropertiesEquals(expectedGroupMember, getPersistedGroupMember(expectedGroupMember));
    }

    protected void assertPersistedGroupMemberToMatchUpdatableProperties(GroupMember expectedGroupMember) {
        assertGroupMemberAllUpdatablePropertiesEquals(expectedGroupMember, getPersistedGroupMember(expectedGroupMember));
    }
}

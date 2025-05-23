package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.domain.GroupAuthorityAsserts.*;
import static com.binbash.mobigo.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.GroupAuthority;
import com.binbash.mobigo.repository.GroupAuthorityRepository;
import com.binbash.mobigo.repository.search.GroupAuthoritySearchRepository;
import com.binbash.mobigo.service.dto.GroupAuthorityDTO;
import com.binbash.mobigo.service.mapper.GroupAuthorityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link GroupAuthorityResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GroupAuthorityResourceIT {

    private static final String ENTITY_API_URL = "/api/group-authorities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/group-authorities/_search";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GroupAuthorityRepository groupAuthorityRepository;

    @Autowired
    private GroupAuthorityMapper groupAuthorityMapper;

    @Autowired
    private GroupAuthoritySearchRepository groupAuthoritySearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGroupAuthorityMockMvc;

    private GroupAuthority groupAuthority;

    private GroupAuthority insertedGroupAuthority;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GroupAuthority createEntity() {
        return new GroupAuthority();
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GroupAuthority createUpdatedEntity() {
        return new GroupAuthority();
    }

    @BeforeEach
    void initTest() {
        groupAuthority = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedGroupAuthority != null) {
            groupAuthorityRepository.delete(insertedGroupAuthority);
            groupAuthoritySearchRepository.delete(insertedGroupAuthority);
            insertedGroupAuthority = null;
        }
    }

    @Test
    @Transactional
    void createGroupAuthority() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);
        var returnedGroupAuthorityDTO = om.readValue(
            restGroupAuthorityMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupAuthorityDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GroupAuthorityDTO.class
        );

        // Validate the GroupAuthority in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGroupAuthority = groupAuthorityMapper.toEntity(returnedGroupAuthorityDTO);
        assertGroupAuthorityUpdatableFieldsEquals(returnedGroupAuthority, getPersistedGroupAuthority(returnedGroupAuthority));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedGroupAuthority = returnedGroupAuthority;
    }

    @Test
    @Transactional
    void createGroupAuthorityWithExistingId() throws Exception {
        // Create the GroupAuthority with an existing ID
        groupAuthority.setId(1L);
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restGroupAuthorityMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupAuthorityDTO)))
            .andExpect(status().isBadRequest());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllGroupAuthorities() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);

        // Get all the groupAuthorityList
        restGroupAuthorityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(groupAuthority.getId().intValue())));
    }

    @Test
    @Transactional
    void getGroupAuthority() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);

        // Get the groupAuthority
        restGroupAuthorityMockMvc
            .perform(get(ENTITY_API_URL_ID, groupAuthority.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(groupAuthority.getId().intValue()));
    }

    @Test
    @Transactional
    void getNonExistingGroupAuthority() throws Exception {
        // Get the groupAuthority
        restGroupAuthorityMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGroupAuthority() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        groupAuthoritySearchRepository.save(groupAuthority);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());

        // Update the groupAuthority
        GroupAuthority updatedGroupAuthority = groupAuthorityRepository.findById(groupAuthority.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGroupAuthority are not directly saved in db
        em.detach(updatedGroupAuthority);
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(updatedGroupAuthority);

        restGroupAuthorityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, groupAuthorityDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupAuthorityDTO))
            )
            .andExpect(status().isOk());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGroupAuthorityToMatchAllProperties(updatedGroupAuthority);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<GroupAuthority> groupAuthoritySearchList = Streamable.of(groupAuthoritySearchRepository.findAll()).toList();
                GroupAuthority testGroupAuthoritySearch = groupAuthoritySearchList.get(searchDatabaseSizeAfter - 1);

                assertGroupAuthorityAllPropertiesEquals(testGroupAuthoritySearch, updatedGroupAuthority);
            });
    }

    @Test
    @Transactional
    void putNonExistingGroupAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        groupAuthority.setId(longCount.incrementAndGet());

        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupAuthorityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, groupAuthorityDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupAuthorityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchGroupAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        groupAuthority.setId(longCount.incrementAndGet());

        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupAuthorityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(groupAuthorityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGroupAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        groupAuthority.setId(longCount.incrementAndGet());

        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupAuthorityMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(groupAuthorityDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateGroupAuthorityWithPatch() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupAuthority using partial update
        GroupAuthority partialUpdatedGroupAuthority = new GroupAuthority();
        partialUpdatedGroupAuthority.setId(groupAuthority.getId());

        restGroupAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupAuthority.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupAuthority))
            )
            .andExpect(status().isOk());

        // Validate the GroupAuthority in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupAuthorityUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGroupAuthority, groupAuthority),
            getPersistedGroupAuthority(groupAuthority)
        );
    }

    @Test
    @Transactional
    void fullUpdateGroupAuthorityWithPatch() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the groupAuthority using partial update
        GroupAuthority partialUpdatedGroupAuthority = new GroupAuthority();
        partialUpdatedGroupAuthority.setId(groupAuthority.getId());

        restGroupAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGroupAuthority.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGroupAuthority))
            )
            .andExpect(status().isOk());

        // Validate the GroupAuthority in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGroupAuthorityUpdatableFieldsEquals(partialUpdatedGroupAuthority, getPersistedGroupAuthority(partialUpdatedGroupAuthority));
    }

    @Test
    @Transactional
    void patchNonExistingGroupAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        groupAuthority.setId(longCount.incrementAndGet());

        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGroupAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, groupAuthorityDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupAuthorityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGroupAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        groupAuthority.setId(longCount.incrementAndGet());

        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(groupAuthorityDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGroupAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        groupAuthority.setId(longCount.incrementAndGet());

        // Create the GroupAuthority
        GroupAuthorityDTO groupAuthorityDTO = groupAuthorityMapper.toDto(groupAuthority);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGroupAuthorityMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(groupAuthorityDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GroupAuthority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteGroupAuthority() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);
        groupAuthorityRepository.save(groupAuthority);
        groupAuthoritySearchRepository.save(groupAuthority);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the groupAuthority
        restGroupAuthorityMockMvc
            .perform(delete(ENTITY_API_URL_ID, groupAuthority.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(groupAuthoritySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchGroupAuthority() throws Exception {
        // Initialize the database
        insertedGroupAuthority = groupAuthorityRepository.saveAndFlush(groupAuthority);
        groupAuthoritySearchRepository.save(groupAuthority);

        // Search the groupAuthority
        restGroupAuthorityMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + groupAuthority.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(groupAuthority.getId().intValue())));
    }

    protected long getRepositoryCount() {
        return groupAuthorityRepository.count();
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

    protected GroupAuthority getPersistedGroupAuthority(GroupAuthority groupAuthority) {
        return groupAuthorityRepository.findById(groupAuthority.getId()).orElseThrow();
    }

    protected void assertPersistedGroupAuthorityToMatchAllProperties(GroupAuthority expectedGroupAuthority) {
        assertGroupAuthorityAllPropertiesEquals(expectedGroupAuthority, getPersistedGroupAuthority(expectedGroupAuthority));
    }

    protected void assertPersistedGroupAuthorityToMatchUpdatableProperties(GroupAuthority expectedGroupAuthority) {
        assertGroupAuthorityAllUpdatablePropertiesEquals(expectedGroupAuthority, getPersistedGroupAuthority(expectedGroupAuthority));
    }
}

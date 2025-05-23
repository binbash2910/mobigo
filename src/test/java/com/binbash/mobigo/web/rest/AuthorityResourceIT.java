package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.domain.AuthorityAsserts.*;
import static com.binbash.mobigo.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.Authority;
import com.binbash.mobigo.repository.AuthorityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AuthorityResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = { "ROLE_ADMIN" })
class AuthorityResourceIT {

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_ORDRE = "AAAAAAAAAA";
    private static final String UPDATED_ORDRE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/authorities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{name}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAuthorityMockMvc;

    private Authority authority;

    private Authority insertedAuthority;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Authority createEntity() {
        return new Authority().name(UUID.randomUUID().toString()).description(DEFAULT_DESCRIPTION).ordre(DEFAULT_ORDRE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Authority createUpdatedEntity() {
        return new Authority().name(UUID.randomUUID().toString()).description(UPDATED_DESCRIPTION).ordre(UPDATED_ORDRE);
    }

    @BeforeEach
    void initTest() {
        authority = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAuthority != null) {
            authorityRepository.delete(insertedAuthority);
            insertedAuthority = null;
        }
    }

    @Test
    @Transactional
    void createAuthority() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Authority
        var returnedAuthority = om.readValue(
            restAuthorityMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(authority)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Authority.class
        );

        // Validate the Authority in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertAuthorityUpdatableFieldsEquals(returnedAuthority, getPersistedAuthority(returnedAuthority));

        insertedAuthority = returnedAuthority;
    }

    @Test
    @Transactional
    void createAuthorityWithExistingId() throws Exception {
        // Create the Authority with an existing ID
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAuthorityMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(authority)))
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAuthorities() throws Exception {
        // Initialize the database
        authority.setName(UUID.randomUUID().toString());
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        // Get all the authorityList
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=name,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].name").value(hasItem(authority.getName())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].ordre").value(hasItem(DEFAULT_ORDRE)));
    }

    @Test
    @Transactional
    void getAuthority() throws Exception {
        // Initialize the database
        authority.setName(UUID.randomUUID().toString());
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        // Get the authority
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL_ID, authority.getName()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value(authority.getName()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.ordre").value(DEFAULT_ORDRE));
    }

    @Test
    @Transactional
    void getNonExistingAuthority() throws Exception {
        // Get the authority
        restAuthorityMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAuthority() throws Exception {
        // Initialize the database
        authority.setName(UUID.randomUUID().toString());
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the authority
        Authority updatedAuthority = authorityRepository.findById(authority.getName()).orElseThrow();
        // Disconnect from session so that the updates on updatedAuthority are not directly saved in db
        em.detach(updatedAuthority);
        updatedAuthority.description(UPDATED_DESCRIPTION).ordre(UPDATED_ORDRE);

        restAuthorityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedAuthority.getName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedAuthority))
            )
            .andExpect(status().isOk());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAuthorityToMatchAllProperties(updatedAuthority);
    }

    @Test
    @Transactional
    void putNonExistingAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        authority.setName(UUID.randomUUID().toString());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAuthorityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, authority.getName()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(authority))
            )
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        authority.setName(UUID.randomUUID().toString());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuthorityMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(authority))
            )
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        authority.setName(UUID.randomUUID().toString());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuthorityMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(authority)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAuthorityWithPatch() throws Exception {
        // Initialize the database
        authority.setName(UUID.randomUUID().toString());
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the authority using partial update
        Authority partialUpdatedAuthority = new Authority();
        partialUpdatedAuthority.setName(authority.getName());

        partialUpdatedAuthority.description(UPDATED_DESCRIPTION).ordre(UPDATED_ORDRE);

        restAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAuthority.getName())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAuthority))
            )
            .andExpect(status().isOk());

        // Validate the Authority in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAuthorityUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAuthority, authority),
            getPersistedAuthority(authority)
        );
    }

    @Test
    @Transactional
    void fullUpdateAuthorityWithPatch() throws Exception {
        // Initialize the database
        authority.setName(UUID.randomUUID().toString());
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the authority using partial update
        Authority partialUpdatedAuthority = new Authority();
        partialUpdatedAuthority.setName(authority.getName());

        partialUpdatedAuthority.description(UPDATED_DESCRIPTION).ordre(UPDATED_ORDRE);

        restAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAuthority.getName())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAuthority))
            )
            .andExpect(status().isOk());

        // Validate the Authority in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAuthorityUpdatableFieldsEquals(partialUpdatedAuthority, getPersistedAuthority(partialUpdatedAuthority));
    }

    @Test
    @Transactional
    void patchNonExistingAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        authority.setName(UUID.randomUUID().toString());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, authority.getName())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(authority))
            )
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        authority.setName(UUID.randomUUID().toString());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuthorityMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(authority))
            )
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAuthority() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        authority.setName(UUID.randomUUID().toString());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAuthorityMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(authority)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAuthority() throws Exception {
        // Initialize the database
        authority.setName(UUID.randomUUID().toString());
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the authority
        restAuthorityMockMvc
            .perform(delete(ENTITY_API_URL_ID, authority.getName()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return authorityRepository.count();
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

    protected Authority getPersistedAuthority(Authority authority) {
        return authorityRepository.findById(authority.getName()).orElseThrow();
    }

    protected void assertPersistedAuthorityToMatchAllProperties(Authority expectedAuthority) {
        assertAuthorityAllPropertiesEquals(expectedAuthority, getPersistedAuthority(expectedAuthority));
    }

    protected void assertPersistedAuthorityToMatchUpdatableProperties(Authority expectedAuthority) {
        assertAuthorityAllUpdatablePropertiesEquals(expectedAuthority, getPersistedAuthority(expectedAuthority));
    }
}

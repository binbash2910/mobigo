package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.domain.PeopleAsserts.*;
import static com.binbash.mobigo.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.search.PeopleSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link PeopleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class PeopleResourceIT {

    private static final String DEFAULT_NOM = "AAAAAAAAAA";
    private static final String UPDATED_NOM = "BBBBBBBBBB";

    private static final String DEFAULT_PRENOM = "AAAAAAAAAA";
    private static final String UPDATED_PRENOM = "BBBBBBBBBB";

    private static final String DEFAULT_TELEPHONE = "AAAAAAAAAA";
    private static final String UPDATED_TELEPHONE = "BBBBBBBBBB";

    private static final String DEFAULT_CNI = "AAAAAAAAAA";
    private static final String UPDATED_CNI = "BBBBBBBBBB";

    private static final String DEFAULT_PHOTO = "AAAAAAAAAA";
    private static final String UPDATED_PHOTO = "BBBBBBBBBB";

    private static final String DEFAULT_ACTIF = "AAAAAAAAAA";
    private static final String UPDATED_ACTIF = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DATE_NAISSANCE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_NAISSANCE = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_MUSIQUE = "AAAAAAAAAA";
    private static final String UPDATED_MUSIQUE = "BBBBBBBBBB";

    private static final String DEFAULT_DISCUSSION = "AAAAAAAAAA";
    private static final String UPDATED_DISCUSSION = "BBBBBBBBBB";

    private static final String DEFAULT_CIGARETTE = "AAAAAAAAAA";
    private static final String UPDATED_CIGARETTE = "BBBBBBBBBB";

    private static final String DEFAULT_ALCOOL = "AAAAAAAAAA";
    private static final String UPDATED_ALCOOL = "BBBBBBBBBB";

    private static final String DEFAULT_ANIMAUX = "AAAAAAAAAA";
    private static final String UPDATED_ANIMAUX = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/people";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/people/_search";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PeopleRepository peopleRepository;

    @Autowired
    private PeopleSearchRepository peopleSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPeopleMockMvc;

    private People people;

    private People insertedPeople;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static People createEntity() {
        return new People()
            .nom(DEFAULT_NOM)
            .prenom(DEFAULT_PRENOM)
            .telephone(DEFAULT_TELEPHONE)
            .cni(DEFAULT_CNI)
            .photo(DEFAULT_PHOTO)
            .actif(DEFAULT_ACTIF)
            .dateNaissance(DEFAULT_DATE_NAISSANCE)
            .musique(DEFAULT_MUSIQUE)
            .discussion(DEFAULT_DISCUSSION)
            .cigarette(DEFAULT_CIGARETTE)
            .alcool(DEFAULT_ALCOOL)
            .animaux(DEFAULT_ANIMAUX);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static People createUpdatedEntity() {
        return new People()
            .nom(UPDATED_NOM)
            .prenom(UPDATED_PRENOM)
            .telephone(UPDATED_TELEPHONE)
            .cni(UPDATED_CNI)
            .photo(UPDATED_PHOTO)
            .actif(UPDATED_ACTIF)
            .dateNaissance(UPDATED_DATE_NAISSANCE)
            .musique(UPDATED_MUSIQUE)
            .discussion(UPDATED_DISCUSSION)
            .cigarette(UPDATED_CIGARETTE)
            .alcool(UPDATED_ALCOOL)
            .animaux(UPDATED_ANIMAUX);
    }

    @BeforeEach
    void initTest() {
        people = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPeople != null) {
            peopleRepository.delete(insertedPeople);
            peopleSearchRepository.delete(insertedPeople);
            insertedPeople = null;
        }
    }

    @Test
    @Transactional
    void createPeople() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        // Create the People
        var returnedPeople = om.readValue(
            restPeopleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            People.class
        );

        // Validate the People in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertPeopleUpdatableFieldsEquals(returnedPeople, getPersistedPeople(returnedPeople));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedPeople = returnedPeople;
    }

    @Test
    @Transactional
    void createPeopleWithExistingId() throws Exception {
        // Create the People with an existing ID
        people.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restPeopleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNomIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        // set the field null
        people.setNom(null);

        // Create the People, which fails.

        restPeopleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkTelephoneIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        // set the field null
        people.setTelephone(null);

        // Create the People, which fails.

        restPeopleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkCniIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        // set the field null
        people.setCni(null);

        // Create the People, which fails.

        restPeopleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkActifIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        // set the field null
        people.setActif(null);

        // Create the People, which fails.

        restPeopleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkDateNaissanceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        // set the field null
        people.setDateNaissance(null);

        // Create the People, which fails.

        restPeopleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllPeople() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);

        // Get all the peopleList
        restPeopleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(people.getId().intValue())))
            .andExpect(jsonPath("$.[*].nom").value(hasItem(DEFAULT_NOM)))
            .andExpect(jsonPath("$.[*].prenom").value(hasItem(DEFAULT_PRENOM)))
            .andExpect(jsonPath("$.[*].telephone").value(hasItem(DEFAULT_TELEPHONE)))
            .andExpect(jsonPath("$.[*].cni").value(hasItem(DEFAULT_CNI)))
            .andExpect(jsonPath("$.[*].photo").value(hasItem(DEFAULT_PHOTO)))
            .andExpect(jsonPath("$.[*].actif").value(hasItem(DEFAULT_ACTIF)))
            .andExpect(jsonPath("$.[*].dateNaissance").value(hasItem(DEFAULT_DATE_NAISSANCE.toString())))
            .andExpect(jsonPath("$.[*].musique").value(hasItem(DEFAULT_MUSIQUE)))
            .andExpect(jsonPath("$.[*].discussion").value(hasItem(DEFAULT_DISCUSSION)))
            .andExpect(jsonPath("$.[*].cigarette").value(hasItem(DEFAULT_CIGARETTE)))
            .andExpect(jsonPath("$.[*].alcool").value(hasItem(DEFAULT_ALCOOL)))
            .andExpect(jsonPath("$.[*].animaux").value(hasItem(DEFAULT_ANIMAUX)));
    }

    @Test
    @Transactional
    void getPeople() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);

        // Get the people
        restPeopleMockMvc
            .perform(get(ENTITY_API_URL_ID, people.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(people.getId().intValue()))
            .andExpect(jsonPath("$.nom").value(DEFAULT_NOM))
            .andExpect(jsonPath("$.prenom").value(DEFAULT_PRENOM))
            .andExpect(jsonPath("$.telephone").value(DEFAULT_TELEPHONE))
            .andExpect(jsonPath("$.cni").value(DEFAULT_CNI))
            .andExpect(jsonPath("$.photo").value(DEFAULT_PHOTO))
            .andExpect(jsonPath("$.actif").value(DEFAULT_ACTIF))
            .andExpect(jsonPath("$.dateNaissance").value(DEFAULT_DATE_NAISSANCE.toString()))
            .andExpect(jsonPath("$.musique").value(DEFAULT_MUSIQUE))
            .andExpect(jsonPath("$.discussion").value(DEFAULT_DISCUSSION))
            .andExpect(jsonPath("$.cigarette").value(DEFAULT_CIGARETTE))
            .andExpect(jsonPath("$.alcool").value(DEFAULT_ALCOOL))
            .andExpect(jsonPath("$.animaux").value(DEFAULT_ANIMAUX));
    }

    @Test
    @Transactional
    void getNonExistingPeople() throws Exception {
        // Get the people
        restPeopleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPeople() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        peopleSearchRepository.save(people);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());

        // Update the people
        People updatedPeople = peopleRepository.findById(people.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPeople are not directly saved in db
        em.detach(updatedPeople);
        updatedPeople
            .nom(UPDATED_NOM)
            .prenom(UPDATED_PRENOM)
            .telephone(UPDATED_TELEPHONE)
            .cni(UPDATED_CNI)
            .photo(UPDATED_PHOTO)
            .actif(UPDATED_ACTIF)
            .dateNaissance(UPDATED_DATE_NAISSANCE)
            .musique(UPDATED_MUSIQUE)
            .discussion(UPDATED_DISCUSSION)
            .cigarette(UPDATED_CIGARETTE)
            .alcool(UPDATED_ALCOOL)
            .animaux(UPDATED_ANIMAUX);

        restPeopleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedPeople.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedPeople))
            )
            .andExpect(status().isOk());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPeopleToMatchAllProperties(updatedPeople);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<People> peopleSearchList = Streamable.of(peopleSearchRepository.findAll()).toList();
                People testPeopleSearch = peopleSearchList.get(searchDatabaseSizeAfter - 1);

                assertPeopleAllPropertiesEquals(testPeopleSearch, updatedPeople);
            });
    }

    @Test
    @Transactional
    void putNonExistingPeople() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        people.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPeopleMockMvc
            .perform(put(ENTITY_API_URL_ID, people.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isBadRequest());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchPeople() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        people.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPeopleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(people))
            )
            .andExpect(status().isBadRequest());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPeople() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        people.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPeopleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(people)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdatePeopleWithPatch() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the people using partial update
        People partialUpdatedPeople = new People();
        partialUpdatedPeople.setId(people.getId());

        partialUpdatedPeople
            .nom(UPDATED_NOM)
            .cni(UPDATED_CNI)
            .photo(UPDATED_PHOTO)
            .actif(UPDATED_ACTIF)
            .musique(UPDATED_MUSIQUE)
            .discussion(UPDATED_DISCUSSION)
            .cigarette(UPDATED_CIGARETTE)
            .alcool(UPDATED_ALCOOL);

        restPeopleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPeople.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPeople))
            )
            .andExpect(status().isOk());

        // Validate the People in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPeopleUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedPeople, people), getPersistedPeople(people));
    }

    @Test
    @Transactional
    void fullUpdatePeopleWithPatch() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the people using partial update
        People partialUpdatedPeople = new People();
        partialUpdatedPeople.setId(people.getId());

        partialUpdatedPeople
            .nom(UPDATED_NOM)
            .prenom(UPDATED_PRENOM)
            .telephone(UPDATED_TELEPHONE)
            .cni(UPDATED_CNI)
            .photo(UPDATED_PHOTO)
            .actif(UPDATED_ACTIF)
            .dateNaissance(UPDATED_DATE_NAISSANCE)
            .musique(UPDATED_MUSIQUE)
            .discussion(UPDATED_DISCUSSION)
            .cigarette(UPDATED_CIGARETTE)
            .alcool(UPDATED_ALCOOL)
            .animaux(UPDATED_ANIMAUX);

        restPeopleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPeople.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPeople))
            )
            .andExpect(status().isOk());

        // Validate the People in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPeopleUpdatableFieldsEquals(partialUpdatedPeople, getPersistedPeople(partialUpdatedPeople));
    }

    @Test
    @Transactional
    void patchNonExistingPeople() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        people.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPeopleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, people.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(people))
            )
            .andExpect(status().isBadRequest());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPeople() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        people.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPeopleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(people))
            )
            .andExpect(status().isBadRequest());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPeople() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        people.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPeopleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(people)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the People in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deletePeople() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);
        peopleRepository.save(people);
        peopleSearchRepository.save(people);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the people
        restPeopleMockMvc
            .perform(delete(ENTITY_API_URL_ID, people.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(peopleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchPeople() throws Exception {
        // Initialize the database
        insertedPeople = peopleRepository.saveAndFlush(people);
        peopleSearchRepository.save(people);

        // Search the people
        restPeopleMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + people.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(people.getId().intValue())))
            .andExpect(jsonPath("$.[*].nom").value(hasItem(DEFAULT_NOM)))
            .andExpect(jsonPath("$.[*].prenom").value(hasItem(DEFAULT_PRENOM)))
            .andExpect(jsonPath("$.[*].telephone").value(hasItem(DEFAULT_TELEPHONE)))
            .andExpect(jsonPath("$.[*].cni").value(hasItem(DEFAULT_CNI)))
            .andExpect(jsonPath("$.[*].photo").value(hasItem(DEFAULT_PHOTO)))
            .andExpect(jsonPath("$.[*].actif").value(hasItem(DEFAULT_ACTIF)))
            .andExpect(jsonPath("$.[*].dateNaissance").value(hasItem(DEFAULT_DATE_NAISSANCE.toString())))
            .andExpect(jsonPath("$.[*].musique").value(hasItem(DEFAULT_MUSIQUE)))
            .andExpect(jsonPath("$.[*].discussion").value(hasItem(DEFAULT_DISCUSSION)))
            .andExpect(jsonPath("$.[*].cigarette").value(hasItem(DEFAULT_CIGARETTE)))
            .andExpect(jsonPath("$.[*].alcool").value(hasItem(DEFAULT_ALCOOL)))
            .andExpect(jsonPath("$.[*].animaux").value(hasItem(DEFAULT_ANIMAUX)));
    }

    protected long getRepositoryCount() {
        return peopleRepository.count();
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

    protected People getPersistedPeople(People people) {
        return peopleRepository.findById(people.getId()).orElseThrow();
    }

    protected void assertPersistedPeopleToMatchAllProperties(People expectedPeople) {
        assertPeopleAllPropertiesEquals(expectedPeople, getPersistedPeople(expectedPeople));
    }

    protected void assertPersistedPeopleToMatchUpdatableProperties(People expectedPeople) {
        assertPeopleAllUpdatablePropertiesEquals(expectedPeople, getPersistedPeople(expectedPeople));
    }
}

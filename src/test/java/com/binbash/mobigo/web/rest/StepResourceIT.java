package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.domain.StepAsserts.*;
import static com.binbash.mobigo.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.Step;
import com.binbash.mobigo.repository.StepRepository;
import com.binbash.mobigo.repository.search.StepSearchRepository;
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
 * Integration tests for the {@link StepResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class StepResourceIT {

    private static final String DEFAULT_VILLE = "AAAAAAAAAA";
    private static final String UPDATED_VILLE = "BBBBBBBBBB";

    private static final String DEFAULT_HEURE_DEPART = "AAAAAAAAAA";
    private static final String UPDATED_HEURE_DEPART = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/steps";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/steps/_search";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private StepRepository stepRepository;

    @Autowired
    private StepSearchRepository stepSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStepMockMvc;

    private Step step;

    private Step insertedStep;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Step createEntity() {
        return new Step().ville(DEFAULT_VILLE).heureDepart(DEFAULT_HEURE_DEPART);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Step createUpdatedEntity() {
        return new Step().ville(UPDATED_VILLE).heureDepart(UPDATED_HEURE_DEPART);
    }

    @BeforeEach
    void initTest() {
        step = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedStep != null) {
            stepRepository.delete(insertedStep);
            stepSearchRepository.delete(insertedStep);
            insertedStep = null;
        }
    }

    @Test
    @Transactional
    void createStep() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        // Create the Step
        var returnedStep = om.readValue(
            restStepMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(step)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Step.class
        );

        // Validate the Step in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertStepUpdatableFieldsEquals(returnedStep, getPersistedStep(returnedStep));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedStep = returnedStep;
    }

    @Test
    @Transactional
    void createStepWithExistingId() throws Exception {
        // Create the Step with an existing ID
        step.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restStepMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(step)))
            .andExpect(status().isBadRequest());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkVilleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        // set the field null
        step.setVille(null);

        // Create the Step, which fails.

        restStepMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(step)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkHeureDepartIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        // set the field null
        step.setHeureDepart(null);

        // Create the Step, which fails.

        restStepMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(step)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllSteps() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);

        // Get all the stepList
        restStepMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(step.getId().intValue())))
            .andExpect(jsonPath("$.[*].ville").value(hasItem(DEFAULT_VILLE)))
            .andExpect(jsonPath("$.[*].heureDepart").value(hasItem(DEFAULT_HEURE_DEPART)));
    }

    @Test
    @Transactional
    void getStep() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);

        // Get the step
        restStepMockMvc
            .perform(get(ENTITY_API_URL_ID, step.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(step.getId().intValue()))
            .andExpect(jsonPath("$.ville").value(DEFAULT_VILLE))
            .andExpect(jsonPath("$.heureDepart").value(DEFAULT_HEURE_DEPART));
    }

    @Test
    @Transactional
    void getNonExistingStep() throws Exception {
        // Get the step
        restStepMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingStep() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        stepSearchRepository.save(step);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());

        // Update the step
        Step updatedStep = stepRepository.findById(step.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedStep are not directly saved in db
        em.detach(updatedStep);
        updatedStep.ville(UPDATED_VILLE).heureDepart(UPDATED_HEURE_DEPART);

        restStepMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedStep.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedStep))
            )
            .andExpect(status().isOk());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedStepToMatchAllProperties(updatedStep);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Step> stepSearchList = Streamable.of(stepSearchRepository.findAll()).toList();
                Step testStepSearch = stepSearchList.get(searchDatabaseSizeAfter - 1);

                assertStepAllPropertiesEquals(testStepSearch, updatedStep);
            });
    }

    @Test
    @Transactional
    void putNonExistingStep() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        step.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStepMockMvc
            .perform(put(ENTITY_API_URL_ID, step.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(step)))
            .andExpect(status().isBadRequest());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchStep() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        step.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStepMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(step))
            )
            .andExpect(status().isBadRequest());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamStep() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        step.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStepMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(step)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateStepWithPatch() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the step using partial update
        Step partialUpdatedStep = new Step();
        partialUpdatedStep.setId(step.getId());

        partialUpdatedStep.heureDepart(UPDATED_HEURE_DEPART);

        restStepMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedStep.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedStep))
            )
            .andExpect(status().isOk());

        // Validate the Step in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertStepUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedStep, step), getPersistedStep(step));
    }

    @Test
    @Transactional
    void fullUpdateStepWithPatch() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the step using partial update
        Step partialUpdatedStep = new Step();
        partialUpdatedStep.setId(step.getId());

        partialUpdatedStep.ville(UPDATED_VILLE).heureDepart(UPDATED_HEURE_DEPART);

        restStepMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedStep.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedStep))
            )
            .andExpect(status().isOk());

        // Validate the Step in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertStepUpdatableFieldsEquals(partialUpdatedStep, getPersistedStep(partialUpdatedStep));
    }

    @Test
    @Transactional
    void patchNonExistingStep() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        step.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStepMockMvc
            .perform(patch(ENTITY_API_URL_ID, step.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(step)))
            .andExpect(status().isBadRequest());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchStep() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        step.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStepMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(step))
            )
            .andExpect(status().isBadRequest());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamStep() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        step.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStepMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(step)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Step in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteStep() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);
        stepRepository.save(step);
        stepSearchRepository.save(step);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the step
        restStepMockMvc
            .perform(delete(ENTITY_API_URL_ID, step.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(stepSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchStep() throws Exception {
        // Initialize the database
        insertedStep = stepRepository.saveAndFlush(step);
        stepSearchRepository.save(step);

        // Search the step
        restStepMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + step.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(step.getId().intValue())))
            .andExpect(jsonPath("$.[*].ville").value(hasItem(DEFAULT_VILLE)))
            .andExpect(jsonPath("$.[*].heureDepart").value(hasItem(DEFAULT_HEURE_DEPART)));
    }

    protected long getRepositoryCount() {
        return stepRepository.count();
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

    protected Step getPersistedStep(Step step) {
        return stepRepository.findById(step.getId()).orElseThrow();
    }

    protected void assertPersistedStepToMatchAllProperties(Step expectedStep) {
        assertStepAllPropertiesEquals(expectedStep, getPersistedStep(expectedStep));
    }

    protected void assertPersistedStepToMatchUpdatableProperties(Step expectedStep) {
        assertStepAllUpdatablePropertiesEquals(expectedStep, getPersistedStep(expectedStep));
    }
}

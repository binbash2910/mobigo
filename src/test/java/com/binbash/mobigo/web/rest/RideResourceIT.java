package com.binbash.mobigo.web.rest;

import static com.binbash.mobigo.domain.RideAsserts.*;
import static com.binbash.mobigo.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.search.RideSearchRepository;
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
 * Integration tests for the {@link RideResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RideResourceIT {

    private static final String DEFAULT_VILLE_DEPART = "AAAAAAAAAA";
    private static final String UPDATED_VILLE_DEPART = "BBBBBBBBBB";

    private static final String DEFAULT_VILLE_ARRIVEE = "AAAAAAAAAA";
    private static final String UPDATED_VILLE_ARRIVEE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DATE_DEPART = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_DEPART = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_DATE_ARRIVEE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_ARRIVEE = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_HEURE_DEPART = "AAAAAAAAAA";
    private static final String UPDATED_HEURE_DEPART = "BBBBBBBBBB";

    private static final String DEFAULT_HEURE_ARRIVEE = "AAAAAAAAAA";
    private static final String UPDATED_HEURE_ARRIVEE = "BBBBBBBBBB";

    private static final String DEFAULT_MINUTE_DEPART = "AAAAAAAAAA";
    private static final String UPDATED_MINUTE_DEPART = "BBBBBBBBBB";

    private static final String DEFAULT_MINUTE_ARRIVEE = "AAAAAAAAAA";
    private static final String UPDATED_MINUTE_ARRIVEE = "BBBBBBBBBB";

    private static final Float DEFAULT_PRIX_PAR_PLACE = 1F;
    private static final Float UPDATED_PRIX_PAR_PLACE = 2F;

    private static final Integer DEFAULT_NBRE_PLACE_DISPONIBLE = 1;
    private static final Integer UPDATED_NBRE_PLACE_DISPONIBLE = 2;

    private static final RideStatusEnum DEFAULT_STATUT = RideStatusEnum.OUVERT;
    private static final RideStatusEnum UPDATED_STATUT = RideStatusEnum.COMPLET;

    private static final String DEFAULT_LIEU_DIT_DEPART = "AAAAAAAAAA";
    private static final String UPDATED_LIEU_DIT_DEPART = "BBBBBBBBBB";

    private static final String DEFAULT_LIEU_DIT_ARRIVEE = "AAAAAAAAAA";
    private static final String UPDATED_LIEU_DIT_ARRIVEE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/rides";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/rides/_search";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideSearchRepository rideSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRideMockMvc;

    private Ride ride;

    private Ride insertedRide;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Ride createEntity() {
        return new Ride()
            .villeDepart(DEFAULT_VILLE_DEPART)
            .villeArrivee(DEFAULT_VILLE_ARRIVEE)
            .dateDepart(DEFAULT_DATE_DEPART)
            .dateArrivee(DEFAULT_DATE_ARRIVEE)
            .heureDepart(DEFAULT_HEURE_DEPART)
            .heureArrivee(DEFAULT_HEURE_ARRIVEE)
            .minuteDepart(DEFAULT_MINUTE_DEPART)
            .minuteArrivee(DEFAULT_MINUTE_ARRIVEE)
            .prixParPlace(DEFAULT_PRIX_PAR_PLACE)
            .nbrePlaceDisponible(DEFAULT_NBRE_PLACE_DISPONIBLE)
            .statut(DEFAULT_STATUT)
            .lieuDitDepart(DEFAULT_LIEU_DIT_DEPART)
            .lieuDitArrivee(DEFAULT_LIEU_DIT_ARRIVEE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Ride createUpdatedEntity() {
        return new Ride()
            .villeDepart(UPDATED_VILLE_DEPART)
            .villeArrivee(UPDATED_VILLE_ARRIVEE)
            .dateDepart(UPDATED_DATE_DEPART)
            .dateArrivee(UPDATED_DATE_ARRIVEE)
            .heureDepart(UPDATED_HEURE_DEPART)
            .heureArrivee(UPDATED_HEURE_ARRIVEE)
            .minuteDepart(UPDATED_MINUTE_DEPART)
            .minuteArrivee(UPDATED_MINUTE_ARRIVEE)
            .prixParPlace(UPDATED_PRIX_PAR_PLACE)
            .nbrePlaceDisponible(UPDATED_NBRE_PLACE_DISPONIBLE)
            .statut(UPDATED_STATUT)
            .lieuDitDepart(UPDATED_LIEU_DIT_DEPART)
            .lieuDitArrivee(UPDATED_LIEU_DIT_ARRIVEE);
    }

    @BeforeEach
    void initTest() {
        ride = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRide != null) {
            rideRepository.delete(insertedRide);
            rideSearchRepository.delete(insertedRide);
            insertedRide = null;
        }
    }

    @Test
    @Transactional
    void createRide() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // Create the Ride
        var returnedRide = om.readValue(
            restRideMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Ride.class
        );

        // Validate the Ride in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertRideUpdatableFieldsEquals(returnedRide, getPersistedRide(returnedRide));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedRide = returnedRide;
    }

    @Test
    @Transactional
    void createRideWithExistingId() throws Exception {
        // Create the Ride with an existing ID
        ride.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkVilleDepartIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setVilleDepart(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkVilleArriveeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setVilleArrivee(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkDateDepartIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setDateDepart(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkDateArriveeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setDateArrivee(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkHeureDepartIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setHeureDepart(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkHeureArriveeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setHeureArrivee(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkMinuteDepartIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setMinuteDepart(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkMinuteArriveeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setMinuteArrivee(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkPrixParPlaceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setPrixParPlace(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNbrePlaceDisponibleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setNbrePlaceDisponible(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkStatutIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        // set the field null
        ride.setStatut(null);

        // Create the Ride, which fails.

        restRideMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllRides() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);

        // Get all the rideList
        restRideMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(ride.getId().intValue())))
            .andExpect(jsonPath("$.[*].villeDepart").value(hasItem(DEFAULT_VILLE_DEPART)))
            .andExpect(jsonPath("$.[*].villeArrivee").value(hasItem(DEFAULT_VILLE_ARRIVEE)))
            .andExpect(jsonPath("$.[*].dateDepart").value(hasItem(DEFAULT_DATE_DEPART.toString())))
            .andExpect(jsonPath("$.[*].dateArrivee").value(hasItem(DEFAULT_DATE_ARRIVEE.toString())))
            .andExpect(jsonPath("$.[*].heureDepart").value(hasItem(DEFAULT_HEURE_DEPART)))
            .andExpect(jsonPath("$.[*].heureArrivee").value(hasItem(DEFAULT_HEURE_ARRIVEE)))
            .andExpect(jsonPath("$.[*].minuteDepart").value(hasItem(DEFAULT_MINUTE_DEPART)))
            .andExpect(jsonPath("$.[*].minuteArrivee").value(hasItem(DEFAULT_MINUTE_ARRIVEE)))
            .andExpect(jsonPath("$.[*].prixParPlace").value(hasItem(DEFAULT_PRIX_PAR_PLACE.doubleValue())))
            .andExpect(jsonPath("$.[*].nbrePlaceDisponible").value(hasItem(DEFAULT_NBRE_PLACE_DISPONIBLE)))
            .andExpect(jsonPath("$.[*].statut").value(hasItem(DEFAULT_STATUT.toString())))
            .andExpect(jsonPath("$.[*].lieuDitDepart").value(hasItem(DEFAULT_LIEU_DIT_DEPART)))
            .andExpect(jsonPath("$.[*].lieuDitArrivee").value(hasItem(DEFAULT_LIEU_DIT_ARRIVEE)));
    }

    @Test
    @Transactional
    void getRide() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);

        // Get the ride
        restRideMockMvc
            .perform(get(ENTITY_API_URL_ID, ride.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(ride.getId().intValue()))
            .andExpect(jsonPath("$.villeDepart").value(DEFAULT_VILLE_DEPART))
            .andExpect(jsonPath("$.villeArrivee").value(DEFAULT_VILLE_ARRIVEE))
            .andExpect(jsonPath("$.dateDepart").value(DEFAULT_DATE_DEPART.toString()))
            .andExpect(jsonPath("$.dateArrivee").value(DEFAULT_DATE_ARRIVEE.toString()))
            .andExpect(jsonPath("$.heureDepart").value(DEFAULT_HEURE_DEPART))
            .andExpect(jsonPath("$.heureArrivee").value(DEFAULT_HEURE_ARRIVEE))
            .andExpect(jsonPath("$.minuteDepart").value(DEFAULT_MINUTE_DEPART))
            .andExpect(jsonPath("$.minuteArrivee").value(DEFAULT_MINUTE_ARRIVEE))
            .andExpect(jsonPath("$.prixParPlace").value(DEFAULT_PRIX_PAR_PLACE.doubleValue()))
            .andExpect(jsonPath("$.nbrePlaceDisponible").value(DEFAULT_NBRE_PLACE_DISPONIBLE))
            .andExpect(jsonPath("$.statut").value(DEFAULT_STATUT.toString()))
            .andExpect(jsonPath("$.lieuDitDepart").value(DEFAULT_LIEU_DIT_DEPART))
            .andExpect(jsonPath("$.lieuDitArrivee").value(DEFAULT_LIEU_DIT_ARRIVEE));
    }

    @Test
    @Transactional
    void getNonExistingRide() throws Exception {
        // Get the ride
        restRideMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRide() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        rideSearchRepository.save(ride);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());

        // Update the ride
        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRide are not directly saved in db
        em.detach(updatedRide);
        updatedRide
            .villeDepart(UPDATED_VILLE_DEPART)
            .villeArrivee(UPDATED_VILLE_ARRIVEE)
            .dateDepart(UPDATED_DATE_DEPART)
            .dateArrivee(UPDATED_DATE_ARRIVEE)
            .heureDepart(UPDATED_HEURE_DEPART)
            .heureArrivee(UPDATED_HEURE_ARRIVEE)
            .minuteDepart(UPDATED_MINUTE_DEPART)
            .minuteArrivee(UPDATED_MINUTE_ARRIVEE)
            .prixParPlace(UPDATED_PRIX_PAR_PLACE)
            .nbrePlaceDisponible(UPDATED_NBRE_PLACE_DISPONIBLE)
            .statut(UPDATED_STATUT)
            .lieuDitDepart(UPDATED_LIEU_DIT_DEPART)
            .lieuDitArrivee(UPDATED_LIEU_DIT_ARRIVEE);

        restRideMockMvc
            .perform(
                put(ENTITY_API_URL_ID, updatedRide.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(updatedRide))
            )
            .andExpect(status().isOk());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRideToMatchAllProperties(updatedRide);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Ride> rideSearchList = Streamable.of(rideSearchRepository.findAll()).toList();
                Ride testRideSearch = rideSearchList.get(searchDatabaseSizeAfter - 1);

                assertRideAllPropertiesEquals(testRideSearch, updatedRide);
            });
    }

    @Test
    @Transactional
    void putNonExistingRide() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        ride.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRideMockMvc
            .perform(put(ENTITY_API_URL_ID, ride.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchRide() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        ride.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRideMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(ride))
            )
            .andExpect(status().isBadRequest());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRide() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        ride.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRideMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(ride)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateRideWithPatch() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ride using partial update
        Ride partialUpdatedRide = new Ride();
        partialUpdatedRide.setId(ride.getId());

        partialUpdatedRide
            .dateDepart(UPDATED_DATE_DEPART)
            .heureArrivee(UPDATED_HEURE_ARRIVEE)
            .minuteArrivee(UPDATED_MINUTE_ARRIVEE)
            .nbrePlaceDisponible(UPDATED_NBRE_PLACE_DISPONIBLE);

        restRideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRide.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRide))
            )
            .andExpect(status().isOk());

        // Validate the Ride in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRideUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedRide, ride), getPersistedRide(ride));
    }

    @Test
    @Transactional
    void fullUpdateRideWithPatch() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ride using partial update
        Ride partialUpdatedRide = new Ride();
        partialUpdatedRide.setId(ride.getId());

        partialUpdatedRide
            .villeDepart(UPDATED_VILLE_DEPART)
            .villeArrivee(UPDATED_VILLE_ARRIVEE)
            .dateDepart(UPDATED_DATE_DEPART)
            .dateArrivee(UPDATED_DATE_ARRIVEE)
            .heureDepart(UPDATED_HEURE_DEPART)
            .heureArrivee(UPDATED_HEURE_ARRIVEE)
            .minuteDepart(UPDATED_MINUTE_DEPART)
            .minuteArrivee(UPDATED_MINUTE_ARRIVEE)
            .prixParPlace(UPDATED_PRIX_PAR_PLACE)
            .nbrePlaceDisponible(UPDATED_NBRE_PLACE_DISPONIBLE)
            .statut(UPDATED_STATUT)
            .lieuDitDepart(UPDATED_LIEU_DIT_DEPART)
            .lieuDitArrivee(UPDATED_LIEU_DIT_ARRIVEE);

        restRideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRide.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRide))
            )
            .andExpect(status().isOk());

        // Validate the Ride in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRideUpdatableFieldsEquals(partialUpdatedRide, getPersistedRide(partialUpdatedRide));
    }

    @Test
    @Transactional
    void patchNonExistingRide() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        ride.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRideMockMvc
            .perform(patch(ENTITY_API_URL_ID, ride.getId()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(ride)))
            .andExpect(status().isBadRequest());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRide() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        ride.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(ride))
            )
            .andExpect(status().isBadRequest());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRide() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        ride.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRideMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(ride)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Ride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteRide() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);
        rideRepository.save(ride);
        rideSearchRepository.save(ride);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the ride
        restRideMockMvc
            .perform(delete(ENTITY_API_URL_ID, ride.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(rideSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchRide() throws Exception {
        // Initialize the database
        insertedRide = rideRepository.saveAndFlush(ride);
        rideSearchRepository.save(ride);

        // Search the ride
        restRideMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + ride.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(ride.getId().intValue())))
            .andExpect(jsonPath("$.[*].villeDepart").value(hasItem(DEFAULT_VILLE_DEPART)))
            .andExpect(jsonPath("$.[*].villeArrivee").value(hasItem(DEFAULT_VILLE_ARRIVEE)))
            .andExpect(jsonPath("$.[*].dateDepart").value(hasItem(DEFAULT_DATE_DEPART.toString())))
            .andExpect(jsonPath("$.[*].dateArrivee").value(hasItem(DEFAULT_DATE_ARRIVEE.toString())))
            .andExpect(jsonPath("$.[*].heureDepart").value(hasItem(DEFAULT_HEURE_DEPART)))
            .andExpect(jsonPath("$.[*].heureArrivee").value(hasItem(DEFAULT_HEURE_ARRIVEE)))
            .andExpect(jsonPath("$.[*].minuteDepart").value(hasItem(DEFAULT_MINUTE_DEPART)))
            .andExpect(jsonPath("$.[*].minuteArrivee").value(hasItem(DEFAULT_MINUTE_ARRIVEE)))
            .andExpect(jsonPath("$.[*].prixParPlace").value(hasItem(DEFAULT_PRIX_PAR_PLACE.doubleValue())))
            .andExpect(jsonPath("$.[*].nbrePlaceDisponible").value(hasItem(DEFAULT_NBRE_PLACE_DISPONIBLE)))
            .andExpect(jsonPath("$.[*].statut").value(hasItem(DEFAULT_STATUT.toString())))
            .andExpect(jsonPath("$.[*].lieuDitDepart").value(hasItem(DEFAULT_LIEU_DIT_DEPART)))
            .andExpect(jsonPath("$.[*].lieuDitArrivee").value(hasItem(DEFAULT_LIEU_DIT_ARRIVEE)));
    }

    protected long getRepositoryCount() {
        return rideRepository.count();
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

    protected Ride getPersistedRide(Ride ride) {
        return rideRepository.findById(ride.getId()).orElseThrow();
    }

    protected void assertPersistedRideToMatchAllProperties(Ride expectedRide) {
        assertRideAllPropertiesEquals(expectedRide, getPersistedRide(expectedRide));
    }

    protected void assertPersistedRideToMatchUpdatableProperties(Ride expectedRide) {
        assertRideAllUpdatablePropertiesEquals(expectedRide, getPersistedRide(expectedRide));
    }
}

package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.UserRepository;
import com.binbash.mobigo.service.AdminStatisticsService;
import com.binbash.mobigo.service.UserService;
import com.binbash.mobigo.service.dto.AdminStatisticsDTO;
import com.binbash.mobigo.service.dto.AdminUserDetailDTO;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * REST controller for admin operations.
 * All endpoints require ROLE_ADMIN authority.
 * The /api/admin/** path is already secured in SecurityConfiguration.
 */
@RestController
@RequestMapping("/api/admin")
@Transactional
public class AdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminResource.class);

    private final AdminStatisticsService adminStatisticsService;
    private final BookingRepository bookingRepository;
    private final PeopleRepository peopleRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public AdminResource(
        AdminStatisticsService adminStatisticsService,
        BookingRepository bookingRepository,
        PeopleRepository peopleRepository,
        RideRepository rideRepository,
        UserRepository userRepository,
        UserService userService
    ) {
        this.adminStatisticsService = adminStatisticsService;
        this.bookingRepository = bookingRepository;
        this.peopleRepository = peopleRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * GET /api/admin/statistics : get global admin statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminStatisticsDTO> getAdminStatistics() {
        LOG.debug("REST request to get admin statistics");
        return ResponseEntity.ok(adminStatisticsService.getAdminStatistics());
    }

    /**
     * GET /api/admin/users-detail : get paginated list of users enriched with People data.
     */
    @GetMapping("/users-detail")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AdminUserDetailDTO>> getAllUsersWithDetail(
        Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Boolean activated
    ) {
        LOG.debug("REST request to get admin users detail - search: {}, activated: {}", search, activated);

        // Normalize search: pass null if blank so the @Query treats it as "no filter"
        String searchParam = (search != null && !search.isBlank()) ? search : null;

        // Paginated query (no collection fetch join — authorities lazy-loaded within @Transactional)
        Page<User> userPage = userRepository.findAllWithFilters(searchParam, activated, pageable);

        // Enrich each user with People data
        List<AdminUserDetailDTO> result = userPage
            .getContent()
            .stream()
            .map(user -> {
                People people = peopleRepository.findByUser(user).orElse(null);
                return new AdminUserDetailDTO(user, people);
            })
            .collect(Collectors.toList());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), userPage);

        return ResponseEntity.ok().headers(headers).body(result);
    }

    /**
     * PUT /api/admin/users/{login}/toggle-activated : toggle user activation status.
     */
    @PutMapping("/users/{login}/toggle-activated")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> toggleUserActivation(@PathVariable String login) {
        LOG.debug("REST request to toggle activation for user: {}", login);

        return userRepository
            .findOneByLogin(login)
            .map(user -> {
                user.setActivated(!user.isActivated());
                userRepository.save(user);
                LOG.debug("Toggled activation for user {} to {}", login, user.isActivated());
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/admin/people/{id}/cni-override : override CNI verification status.
     */
    @PutMapping("/people/{id}/cni-override")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> overrideCniStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        LOG.debug("REST request to override CNI status for people {}: {}", id, request);
        String status = request.get("status");
        if (status == null || (!status.equals("VERIFIED") && !status.equals("REJECTED"))) {
            return ResponseEntity.badRequest().build();
        }

        return peopleRepository
            .findById(id)
            .map(people -> {
                people.setCniStatut(status);
                if ("VERIFIED".equals(status)) {
                    people.setCniVerifieAt(Instant.now());
                }
                peopleRepository.save(people);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/admin/rides : get paginated list of rides with search and status filter.
     */
    @GetMapping("/rides")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllRides(
        Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String statut
    ) {
        LOG.debug("REST request to get admin rides - search: {}, statut: {}", search, statut);

        String searchParam = (search != null && !search.isBlank()) ? search : null;
        RideStatusEnum statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try {
                statutEnum = RideStatusEnum.valueOf(statut);
            } catch (IllegalArgumentException e) {
                // ignore invalid statut
            }
        }

        Page<Ride> ridePage = rideRepository.findAllForAdmin(searchParam, statutEnum, pageable);

        List<Map<String, Object>> result = ridePage
            .getContent()
            .stream()
            .map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("villeDepart", r.getVilleDepart());
                map.put("villeArrivee", r.getVilleArrivee());
                map.put("dateDepart", r.getDateDepart());
                map.put("heureDepart", r.getHeureDepart());
                map.put("minuteDepart", r.getMinuteDepart());
                map.put("prixParPlace", r.getPrixParPlace());
                map.put("nbrePlaceDisponible", r.getNbrePlaceDisponible());
                map.put("statut", r.getStatut());
                map.put("createdDate", r.getCreatedDate());
                String driverName = "";
                if (r.getVehicule() != null && r.getVehicule().getProprietaire() != null) {
                    People p = r.getVehicule().getProprietaire();
                    String prenom = p.getPrenom() != null ? p.getPrenom() : "";
                    String nom = p.getNom() != null && !p.getNom().isEmpty() ? p.getNom().charAt(0) + "." : "";
                    driverName = (prenom + " " + nom).trim();
                }
                map.put("driverName", driverName);
                return map;
            })
            .collect(Collectors.toList());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), ridePage);

        return ResponseEntity.ok().headers(headers).body(result);
    }

    /**
     * GET /api/admin/bookings : get paginated list of bookings with status filter.
     */
    @GetMapping("/bookings")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllBookings(Pageable pageable, @RequestParam(required = false) String statut) {
        LOG.debug("REST request to get admin bookings - statut: {}", statut);

        BookingStatusEnum statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try {
                statutEnum = BookingStatusEnum.valueOf(statut);
            } catch (IllegalArgumentException e) {
                // ignore invalid statut
            }
        }

        Page<Booking> bookingPage = bookingRepository.findAllForAdmin(statutEnum, pageable);

        List<Map<String, Object>> result = bookingPage
            .getContent()
            .stream()
            .map(b -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", b.getId());
                map.put("nbPlacesReservees", b.getNbPlacesReservees());
                map.put("montantTotal", b.getMontantTotal());
                map.put("commission", b.getCommission());
                map.put("dateReservation", b.getDateReservation());
                map.put("statut", b.getStatut());
                map.put("createdDate", b.getCreatedDate());
                String passengerName = "";
                if (b.getPassager() != null) {
                    People p = b.getPassager();
                    String prenom = p.getPrenom() != null ? p.getPrenom() : "";
                    String nom = p.getNom() != null && !p.getNom().isEmpty() ? p.getNom().charAt(0) + "." : "";
                    passengerName = (prenom + " " + nom).trim();
                }
                map.put("passengerName", passengerName);
                Ride trajet = b.getTrajet();
                if (trajet != null) {
                    map.put("trajetId", trajet.getId());
                    map.put("villeDepart", trajet.getVilleDepart());
                    map.put("villeArrivee", trajet.getVilleArrivee());
                    map.put("dateDepart", trajet.getDateDepart());
                }
                return map;
            })
            .collect(Collectors.toList());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), bookingPage);

        return ResponseEntity.ok().headers(headers).body(result);
    }

    /**
     * GET /api/admin/verifications : get paginated list of CNI verifications with status filter.
     */
    @GetMapping("/verifications")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllVerifications(
        Pageable pageable,
        @RequestParam(required = false) String cniStatut
    ) {
        LOG.debug("REST request to get admin verifications - cniStatut: {}", cniStatut);

        String cniStatutParam = (cniStatut != null && !cniStatut.isBlank()) ? cniStatut : null;

        Page<People> peoplePage = peopleRepository.findAllWithCniSubmitted(cniStatutParam, pageable);

        List<Map<String, Object>> result = peoplePage
            .getContent()
            .stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("nom", p.getNom());
                map.put("prenom", p.getPrenom());
                map.put("photo", p.getPhoto());
                map.put("email", p.getUser() != null ? p.getUser().getEmail() : null);
                map.put("documentType", p.getDocumentType());
                map.put("cniStatut", p.getCniStatut());
                map.put("cniVerifieAt", p.getCniVerifieAt());
                map.put("cniPhotoRecto", p.getCniPhotoRecto());
                map.put("cniPhotoVerso", p.getCniPhotoVerso());
                map.put("cniNumero", p.getCniNumero());
                map.put("cniDateExpiration", p.getCniDateExpiration());
                map.put("cniNomMrz", p.getCniNomMrz());
                map.put("cniPrenomMrz", p.getCniPrenomMrz());
                return map;
            })
            .collect(Collectors.toList());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), peoplePage);

        return ResponseEntity.ok().headers(headers).body(result);
    }
}

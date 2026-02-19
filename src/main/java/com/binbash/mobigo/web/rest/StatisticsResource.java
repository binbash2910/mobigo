package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.service.StatisticsService;
import com.binbash.mobigo.service.dto.StatisticsDTO;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user statistics dashboard.
 */
@RestController
@RequestMapping("/api")
public class StatisticsResource {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsResource.class);

    private final StatisticsService statisticsService;

    public StatisticsResource(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * {@code GET /api/statistics/dashboard} : Get current user's statistics dashboard.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the statistics in body.
     */
    @GetMapping("/statistics/dashboard")
    public ResponseEntity<StatisticsDTO> getDashboard() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "user", "notauthenticated"));
        LOG.debug("REST request to get statistics dashboard for user '{}'", login);
        StatisticsDTO stats = statisticsService.getDashboard(login);
        return ResponseEntity.ok(stats);
    }
}

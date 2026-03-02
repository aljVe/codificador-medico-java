package com.hospital.cdi.controller;

import com.hospital.cdi.model.MedicalItem;
import com.hospital.cdi.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST API Controller for the Search Engine
 * 
 * Provides HTTP endpoints for the browser client to fetch entire datasets
 * or request server-side fuzzy filtering depending on the architectural
 * approach.
 */
@RestController
@RequestMapping("/api")
public class SearchController {

    private static final Logger LOGGER = Logger.getLogger(SearchController.class.getName());

    private final DataService dataService;

    @Autowired
    public SearchController(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * GET /api/search?q={query}
     * Server-side filtering fallback. Delegates string matching algorithms
     * to the DataService layer.
     * 
     * @param query Target search query parameters
     * @return JSON Array of matching MedicalItem objects
     */
    @GetMapping("/search")
    public List<MedicalItem> search(@RequestParam("q") String query) {
        try {
            return dataService.search(query);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception occurred during /api/search endpoint processing: ", e);
            throw e;
        }
    }

    /**
     * GET /api/all
     * Principal data hydration endpoint. Dumps the entire in-memory medical
     * dataset to the client so the frontend can execute zero-latency local
     * filtering.
     * 
     * @return JSON Array of all loaded MedicalItem objects
     */
    @GetMapping("/all")
    public List<MedicalItem> getAllData() {
        try {
            return dataService.getAll();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception occurred during /api/all payload delivery: ", e);
            throw e;
        }
    }
}

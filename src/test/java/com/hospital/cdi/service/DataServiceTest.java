package com.hospital.cdi.service;

import com.hospital.cdi.model.MedicalItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataServiceTest {

    private DataService dataService;

    @BeforeEach
    public void setup() {
        dataService = new DataService();
        dataService.init(); // loads from base_datos_recalmin.csv in root dir during test
    }

    @Test
    public void testSondajeVesicalIsNotDuplicated() {
        List<MedicalItem> results = dataService.search("sondaje vesical");
        assertEquals(1, results.size(), "Debería haber exactamente 1 resultado para 'sondaje vesical'");
        assertEquals("excluded", results.get(0).getType());
    }

    @Test
    public void testFallaHepaticaAndIsquemiaAreSplit() {
        List<MedicalItem> isquemiaResults = dataService.search("isquemia mesenterica");
        // We know we just added "Isquemia mesentérica" as its own diagnosis
        boolean foundIsquemia = isquemiaResults.stream()
                .flatMap(item -> item.getTerms().stream())
                .anyMatch(term -> term.contains("isquemia mesenterica"));
        assertTrue(foundIsquemia, "Debe existir un diagnóstico independiente para isquemia mesentérica");

        List<MedicalItem> fallaResults = dataService.search("falla hepatica aguda");
        boolean foundFalla = fallaResults.stream()
                .flatMap(item -> item.getTerms().stream())
                .anyMatch(term -> term.contains("falla hepatica aguda"));
        assertTrue(foundFalla, "Debe existir un diagnóstico de falla hepática aguda");

        // Ensure Isquemia is not bundled with Falla Hepatica anymore
        boolean fallaHasIsquemia = fallaResults.stream()
                .filter(item -> item.getTerms().contains("falla hepatica aguda"))
                .flatMap(item -> item.getTerms().stream())
                .anyMatch(term -> term.contains("isquemia"));

        assertTrue(!fallaHasIsquemia, "Falla hepática ya no debe contener términos de isquemia");
    }
}

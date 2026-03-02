package com.hospital.cdi.service;

import com.hospital.cdi.model.MedicalItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Ensures the core search logic behaves word-order agnostically and ensures
 * all CSV trees have been correctly mapped to their UI equivalents.
 */
public class ComprehensiveTest {

        private DataService dataService;

        @BeforeEach
        public void setup() {
                dataService = new DataService();
                dataService.init();
        }

        @Test
        public void testWordOrderAgnosticSearch() {
                // "falla hepatica aguda" should match "aguda hepatica" (reverse order)
                List<MedicalItem> results = dataService.search("aguda hepatica");
                boolean foundFalla = results.stream()
                                .flatMap(item -> item.getTerms().stream())
                                .anyMatch(term -> term.contains("falla hepatica aguda"));
                assertTrue(foundFalla, "El buscador debe ser agnóstico al orden de las palabras");

                // "sindrome confusional agudo" should match "agudo confusional"
                List<MedicalItem> res2 = dataService.search("agudo confusional");
                assertTrue(res2.size() > 0,
                                "Debe tolerar desorden ('agudo confusional' -> 'sindrome confusional agudo')");
        }

        @Test
        public void testExclusionsArePresentButMarkedAsExcluded() {
                List<MedicalItem> results = dataService.search("sondaje vesical");
                assertEquals(1, results.size(), "Debería haber exactamente 1 resultado");
                assertEquals("excluded", results.get(0).getType(), "Debe estar marcado como excluido");
        }

        @Test
        public void testTreeLogicFixes() {
                // SDRA (was broken, expecting tree, now should be 'simple' placeholder)
                List<MedicalItem> sdra = dataService.search("sdra");
                assertTrue(sdra.size() > 0);
                assertEquals("simple", sdra.get(0).getType(), "SDRA ya no debe requerir un árbol de botones.");

                // Crisis renal (was broken, expecting tree, now 'simple')
                List<MedicalItem> crisis = dataService.search("crisis renal esclerodermica");
                assertTrue(crisis.size() > 0);
                assertEquals("simple", crisis.get(0).getType(), "Crisis renal no debe ser árbol");

                // Neoplasia origen desconocido (was broken, expecting tree, now 'simple')
                List<MedicalItem> cpo = dataService.search("cancer de origen desconocido");
                assertTrue(cpo.size() > 0);
                assertEquals("simple", cpo.get(0).getType(), "Cancer de origen desconocido no tiene opciones");

                // Neumonia (was broken, expecting simple, but HAS bracket options, so it MUST
                // be a tree)
                List<MedicalItem> neumonia = dataService.search("neumonia");
                // Warning: This depends on the specific query matching the *correct* pneumonia
                // row from the CSV
                // In this CSV, it's just "neumonia". It might match multiple if there's
                // "neumonia por vrs"
                MedicalItem mainPneumonia = neumonia.stream()
                                .filter(item -> item.getTerms().contains("neumonia"))
                                .findFirst().orElse(null);

                assertTrue(mainPneumonia != null, "Debe existir neumonia");
                assertEquals("tree", mainPneumonia.getType(), "Neumonia TIENE corchetes con barras, debe ser 'tree'");
        }

        @Test
        public void testNewCachexiaAdditions() {
                List<MedicalItem> results = dataService.search("caquexia");
                // Ensure that at least 'caquexia general', 'caquexia tumoral', 'caquexia
                // pituitaria' come up.
                // It shouldn't match 'desnutricion' anymore since we explicitly removed the
                // term.
                long specificMatches = results.stream()
                                .filter(item -> item.getTerms().contains("caquexia general") ||
                                                item.getTerms().contains("caquexia tumoral") ||
                                                item.getTerms().contains("caquexia pituitaria") ||
                                                item.getTerms().contains("caquexia por vih"))
                                .count();

                assertTrue(specificMatches >= 4, "Las nuevas categorías específicas de caquexia deben aparecer");

                // Ensure "desnutricion" (plain) does not appear directly when searching
                // "caquexia"
                boolean hasPlainMalnutrition = results.stream()
                                .anyMatch(item -> item.getTerms().contains("desnutricion")
                                                && item.getTerms().size() == 4);
                // the terms list for pure malnut: desnutricion|malnutricion|malnutricion
                // calorico-proteica|desnutricion severa

                assertFalse(hasPlainMalnutrition,
                                "La búsqueda de 'caquexia' no debe devolver la entrada principal de 'desnutrición'");
        }

        @Test
        public void testProcedureSplitParacentesis() {
                // Test that 'paracentesis' is isolated and has a tree.
                List<MedicalItem> results = dataService.search("paracentesis");

                // Should find exactly the paracentesis entry, not toracocentesis
                MedicalItem paracentesis = results.stream()
                                .filter(item -> item.getTerms().contains("paracentesis"))
                                .findFirst().orElse(null);

                assertTrue(paracentesis != null, "Debe existir la entrada específica de paracentesis");
                assertFalse(paracentesis.getTerms().contains("toracocentesis"),
                                "No debe tener toracocentesis en los términos");
                assertEquals("tree", paracentesis.getType(),
                                "Paracentesis requiere árbol para [Diagnóstico/Terapéutico]");

                // Verify tree structure deeply
                // Paracentesis has: [Diagnósticos / Terapéuticos] and [Ninguno / Tubo de
                // drenaje]
                assertEquals("Seleccione opción:", paracentesis.getRoot().getQuestion(),
                                "La primera pregunta del árbol debe pedir elegir opción");
                assertEquals(2, paracentesis.getRoot().getOptions().size(), "Debe haber 2 opciones en el primer nivel");
        }

        @Test
        public void testProcedureSplitPuncionLumbar() {
                List<MedicalItem> results = dataService.search("puncion lumbar");
                MedicalItem puncion = results.stream()
                                .filter(item -> item.getTerms().contains("puncion lumbar"))
                                .findFirst().orElse(null);

                assertTrue(puncion != null, "Debe existir la entrada de puncion lumbar");
                assertEquals("tree", puncion.getType(), "Punción lumbar requiere árbol para [Diagnóstico/Terapéutico]");
                // It has 1 bracket set for Fin, and the Dispositivo is hardcoded "Ninguno"
                // without brackets
                assertEquals(2, puncion.getRoot().getOptions().size(),
                                "Debe haber 2 opciones (Diagnóstico/Terapéutico)");
                List<com.hospital.cdi.model.TreeOption> opts = puncion.getRoot().getOptions();
                assertTrue(opts.get(0).getValue() != null,
                                "Como solo hay 1 corchete de opciones, esta opción debe tener un valor final");
                assertTrue(opts.get(0).getValue().contains("Dispositivo: Ninguno"),
                                "El resultado final debe incluir el dispositivo hardcodeado");
        }
}

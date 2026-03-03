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
                assertEquals("simple", sdra.get(0).getType(),
                                "SDRA ya no debe requerir un árbol de botones.");

                // Crisis renal (was broken, expecting tree, now 'simple')
                List<MedicalItem> crisis = dataService.search("crisis renal esclerodermica");
                assertTrue(crisis.size() > 0);
                assertEquals("simple", crisis.get(0).getType(), "Crisis renal no debe ser árbol");

                // Neoplasia origen desconocido (was broken, expecting tree, now 'simple')
                List<MedicalItem> cpo = dataService.search("cancer de origen desconocido");
                assertTrue(cpo.size() > 0);
                assertEquals("simple", cpo.get(0).getType(),
                                "Cancer de origen desconocido no tiene opciones");

                List<MedicalItem> neumonia = dataService.search("neumonia");
                MedicalItem mainPneumonia = neumonia.stream()
                                .filter(item -> item.getTerms().contains("neumonia"))
                                .findFirst().orElse(null);

                assertTrue(mainPneumonia != null, "Debe existir neumonia");
                assertEquals("tree", mainPneumonia.getType(),
                                "Neumonia TIENE corchetes con barras, debe ser 'tree'");
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

        @Test
        public void testCie10CodesAreLoaded() {
                List<MedicalItem> results = dataService.search("insuficiencia cardiaca");
                MedicalItem ic = results.stream()
                                .filter(item -> item.getTerms().contains("insuficiencia cardiaca"))
                                .findFirst().orElse(null);

                assertTrue(ic != null, "Debe existir la insuficiencia cardiaca");
                assertEquals("I50.[Sistólica:2|Diastólica:3|Combinada:4][Aguda:1|Crónica:2|Crónica agudizada:3]",
                                ic.getCode(), "Debe tener el código CIE-10 dinámico correcto");

                List<MedicalItem> results2 = dataService.search("pancreatitis");
                MedicalItem panc = results2.stream()
                                .filter(item -> item.getTerms().contains("pancreatitis"))
                                .findFirst().orElse(null);
                assertTrue(panc != null);
                assertEquals("K85.[Biliar:1|Alcohólica:2|Idiopática:0]0", panc.getCode(),
                                "Debe tener el código CIE-10 correcto dinámico");
        }

        @Test
        public void testCodeResolutionInTree() {
                List<MedicalItem> results = dataService.search("insuficiencia cardiaca");
                MedicalItem ic = results.stream()
                                .filter(item -> item.getTerms().contains("insuficiencia cardiaca"))
                                .findFirst().orElse(null);

                assertTrue(ic != null);
                // Level 0: Aguda / Crónica / Crónica agudizada
                com.hospital.cdi.model.TreeOption l0_opt = ic.getRoot().getOptions().get(0); // "Aguda"
                assertEquals("Aguda", l0_opt.getLabel());

                // Level 1: Sistólica / Diastólica / Combinada
                com.hospital.cdi.model.TreeOption l1_opt = l0_opt.getNext().getOptions().get(0); // "Sistólica"
                assertEquals("Sistólica", l1_opt.getLabel());

                // Final code should be resolved to I50.21
                assertEquals("I50.21", l1_opt.getCode(), "El código debe resolverse exactamente en el nodo final");
        }

        @Test
        public void testCodeResolutionMoreDiseases() {
                // TEP
                MedicalItem tep = dataService.search("tromboembolismo pulmonar").stream()
                                .filter(i -> i.getTerms().contains("tep")).findFirst().orElse(null);
                assertTrue(tep != null);
                com.hospital.cdi.model.TreeOption tepConCor = tep.getRoot().getOptions().get(0); // "con"
                assertEquals("I26.09", tepConCor.getCode(), "TEP con Cor Pulmonale resuelve a I26.09");

                // Pancreatitis
                MedicalItem panc = dataService.search("pancreatitis").stream()
                                .filter(i -> i.getTerms().contains("pancreatitis")).findFirst().orElse(null);
                assertTrue(panc != null);
                // [Biliar / Alcohólica / Idiopática] -> [sin / con] -> [estéril / infectada]
                // Let's test Idiopática -> con -> infectada -> K85.00
                com.hospital.cdi.model.TreeOption p0 = panc.getRoot().getOptions().get(2); // "Idiopática"
                assertEquals("Idiopática", p0.getLabel());
                com.hospital.cdi.model.TreeOption p1 = p0.getNext().getOptions().get(1); // "con"
                com.hospital.cdi.model.TreeOption p2 = p1.getNext().getOptions().get(1); // "infectada"
                assertEquals("K85.00", p2.getCode(), "Pancreatitis Idiopática con necrosis infectada = K85.00");

                // Neumonia (Hipoxémica)
                MedicalItem neu = dataService.search("neumonia").stream()
                                .filter(i -> i.getTerms().contains("neumonia")).findFirst().orElse(null);
                assertTrue(neu != null);
                com.hospital.cdi.model.TreeOption hipoxemica = neu.getRoot().getOptions().get(0); // "Hipoxémica"
                assertEquals("J15.9", hipoxemica.getCode(),
                                "Neumonia base resolved to J15.9 since options don't have code mappings");
        }
}

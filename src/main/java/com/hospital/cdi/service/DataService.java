package com.hospital.cdi.service;

import com.hospital.cdi.model.MedicalItem;
import com.hospital.cdi.model.TreeNode;
import com.hospital.cdi.model.TreeOption;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DataService - Core Data Management Component
 * 
 * Handles the loading, parsing, and in-memory caching of the primary
 * "base_datos_recalmin.csv" dataset. Provides search capabilities.
 */
@Service
public class DataService {

    private static final Logger LOGGER = Logger.getLogger(DataService.class.getName());

    // In-memory cache holding all validated diagnostic and procedural items
    private final List<MedicalItem> medicalData = new ArrayList<>();

    /**
     * Bootstraps the service repository on application startup.
     * Manages hardcoded exclusions and parses the CSV file into memory.
     */
    @PostConstruct
    public void init() {
        LOGGER.info("DataService init(): Bootstrapping CMBD database engine...");

        try {
            File extFile = new File("base_datos_recalmin.csv");
            if (!extFile.exists()) {
                LOGGER.log(Level.SEVERE,
                        "CRITICAL INIT FAILURE: 'base_datos_recalmin.csv' not found at expected path: {0}",
                        extFile.getAbsolutePath());
                return;
            }

            LOGGER.info("Parsing dataset from: " + extFile.getAbsolutePath());

            // UTF-8 Scanner utilized to maintain character encoding integrity (BOM
            // protection)
            try (java.util.Scanner scanner = new java.util.Scanner(extFile, "UTF-8")) {
                int index = 0;
                boolean firstLine = true;

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();

                    if (firstLine) {
                        firstLine = false;
                        continue; // Skip header row
                    }
                    if (line.isEmpty())
                        continue;

                    // Parse semicolon-delimited strategy, retaining empty columns
                    String[] parts = line.split(";", -1);
                    if (parts.length < 6)
                        continue;

                    String tipo = parts[0].trim();
                    String terminos = parts[1].trim();
                    String alerta = parts[2].trim();
                    String texto = parts[3].trim();
                    String codigo = parts[4].trim();
                    String requiereArbolStr = parts[5].trim();

                    boolean isTree = "TRUE".equalsIgnoreCase(requiereArbolStr);

                    MedicalItem item = new MedicalItem();
                    item.setId("csv_" + index++);
                    item.setTerms(Arrays.asList(terminos.split("\\|")));
                    item.setAlert(alerta);
                    item.setValue(texto);
                    item.setCode(codigo);
                    item.setFromCsv(true);

                    // Type mapping for dynamic frontend rendering
                    if ("Procedimiento Omitido".equalsIgnoreCase(tipo)) {
                        item.setType("excluded");
                    } else if (isTree) {
                        try {
                            injectDynamicTree(item);
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Failed to build dynamic tree for " + terminos, ex);
                            item.setType("tree_placeholder");
                        }
                    } else {
                        item.setType("simple");
                    }

                    medicalData.add(item);
                }
                LOGGER.info("Database loaded successfully. Total cached entities: " + index);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "I/O or Parse exception encountered during dataset hydration: ", e);
        }
    }

    /**
     * Programmatically constructs and binds multi-level decision trees
     * by parsing slash-separated options inside brackets in the normative text.
     * Example: "Text [Option 1 / Option 2]" -> Generates a decision node.
     * 
     * @param item Target MedicalItem being evaluated during parse phase.
     */
    private void injectDynamicTree(MedicalItem item) {
        String template = item.getValue();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(template);

        List<String> brackets = new ArrayList<>();
        List<List<String>> allOptions = new ArrayList<>();

        while (matcher.find()) {
            String fullBracket = matcher.group(0);
            String content = matcher.group(1);

            // Validate it's a multiple choice bracket (contains "/")
            // Exclude single placeholders like "[Microorganismo]" that shouldn't be buttons
            if (content.contains("/")) {
                brackets.add(fullBracket);
                List<String> opts = new ArrayList<>();
                for (String opt : content.split("/")) {
                    opts.add(opt.trim());
                }
                allOptions.add(opts);
            }
        }

        if (allOptions.isEmpty()) {
            item.setType("simple");
            return;
        }

        item.setType("tree");
        item.setRoot(createNode(template, item.getCode(), brackets, allOptions, 0));
    }

    private TreeNode createNode(String currentTemplate, String currentCodeTemplate, List<String> brackets,
            List<List<String>> allOptions,
            int level) {
        if (level >= allOptions.size())
            return null;

        List<String> currentOptions = allOptions.get(level);
        boolean isLastLevel = (level == allOptions.size() - 1);

        List<TreeOption> treeOptions = new ArrayList<>();
        for (String opt : currentOptions) {
            String newTemplate = currentTemplate.replace(brackets.get(level), opt);
            String newCodeTemplate = resolveCodeForOption(currentCodeTemplate, opt);

            if (isLastLevel) {
                treeOptions.add(new TreeOption(opt, newTemplate, newCodeTemplate));
            } else {
                TreeNode nextNode = createNode(newTemplate, newCodeTemplate, brackets, allOptions, level + 1);
                treeOptions.add(new TreeOption(opt, nextNode));
            }
        }

        return new TreeNode("Seleccione opción:", treeOptions);
    }

    private String resolveCodeForOption(String currentCodeTemplate, String selectedOption) {
        if (currentCodeTemplate == null || currentCodeTemplate.trim().isEmpty()) {
            return currentCodeTemplate;
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(currentCodeTemplate);

        while (matcher.find()) {
            String fullBracket = matcher.group(0);
            String content = matcher.group(1);

            String[] mappings = content.split("\\|");
            for (String mapping : mappings) {
                String[] parts = mapping.split(":", 2);
                if (parts.length == 2 && parts[0].trim().equals(selectedOption)) {
                    return currentCodeTemplate.replace(fullBracket, parts[1].trim());
                }
            }
        }
        return currentCodeTemplate;
    }

    /**
     * Standard Server-Side Filtering (Fallback capability).
     * Validates that all parsed query words are substring-matched within
     * at least one valid medical alias term.
     * 
     * @param query Unsanitized user query
     * @return Filtered collection of MedicalItem entities
     */
    public List<MedicalItem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchLower = query.toLowerCase().trim();
        String[] words = searchLower.split("\\s+");

        return medicalData.stream().filter(item -> {
            return item.getTerms().stream().anyMatch(term -> {
                String termLower = term.toLowerCase();
                boolean allWordsFound = true;

                for (String word : words) {
                    if (!termLower.contains(word)) {
                        allWordsFound = false;
                        break;
                    }
                }
                return allWordsFound;
            });
        }).collect(Collectors.toList());
    }

    /**
     * Retrieves the complete dataset payload.
     * 
     * @return Entire in-memory dataset cache.
     */
    public List<MedicalItem> getAll() {
        return medicalData;
    }
}

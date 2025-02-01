/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.caspar.ai.utility.tw;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.client.bot.caspar.ai.utility.tw.considerations.TWConsideration;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecision;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionScoreEvaluator;
import megamek.client.bot.caspar.ai.utility.tw.profile.TWProfile;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TWUtilityAIRepository {
    private static final MMLogger logger = MMLogger.create(TWUtilityAIRepository.class);

    private static TWUtilityAIRepository instance;

    public static final String EVALUATORS = "evaluators";
    public static final String CONSIDERATIONS = "considerations";
    public static final String DECISIONS = "decisions";
    public static final String PROFILES = "profiles";
    private final ObjectMapper mapper;

    private final Map<String, TWProfile> profiles = new HashMap<>();
    private final Map<String, TWDecision> decisions = new HashMap<>();
    private final Map<String, TWConsideration> considerations = new HashMap<>();
    private final Map<String, TWDecisionScoreEvaluator> decisionScoreEvaluators = new HashMap<>();

    public static TWUtilityAIRepository getInstance() {
        if (instance == null) {
            instance = new TWUtilityAIRepository();
        }
        return instance;
    }

    private TWUtilityAIRepository() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        initialize();
    }

    private void initialize() {
        decisionScoreEvaluators.clear();
        considerations.clear();
        profiles.clear();
        decisions.clear();
        loadRepository();
        loadUserDataRepository();
    }

    public TWUtilityAIRepository reloadRepository() {
        initialize();
        return this;
    }

    public void persistData() {
        var twAiDir = Configuration.twAiDir();
        createDirectoryStructureIfMissing(twAiDir);
        persistToFile(new File(twAiDir, EVALUATORS + File.separator + "decision_score_evaluators.yaml"), decisionScoreEvaluators.values());
        persistToFile(new File(twAiDir, CONSIDERATIONS + File.separator + "considerations.yaml"), considerations.values());
        persistToFile(new File(twAiDir, DECISIONS + File.separator + "decisions.yaml"), decisions.values());
        persistToFile(new File(twAiDir, PROFILES + File.separator + "profiles.yaml"), profiles.values());
    }

    public void persistDataToUserData() {
        var userDataAiTwDir = Configuration.userDataAiTwDir();
        createDirectoryStructureIfMissing(userDataAiTwDir);
        persistToFile(new File(userDataAiTwDir, EVALUATORS + File.separator + "custom_decision_score_evaluators.yaml"), decisionScoreEvaluators.values());
        persistToFile(new File(userDataAiTwDir, CONSIDERATIONS + File.separator + "custom_considerations.yaml"), considerations.values());
        persistToFile(new File(userDataAiTwDir, DECISIONS + File.separator + "custom_decisions.yaml"), decisions.values());
        persistToFile(new File(userDataAiTwDir, PROFILES + File.separator + "custom_profiles.yaml"), profiles.values());
    }

    public void exportAiData(File zipOutput) throws IOException {
        var tempFolder = Files.createTempDirectory("my-ai-export");
        var tempFile = tempFolder.toFile();

        createDirectoryStructureIfMissing(tempFile);
        persistToFile(new File(tempFile, EVALUATORS + File.separator + "custom_decision_score_evaluators.yaml"), decisionScoreEvaluators.values());
        persistToFile(new File(tempFile, CONSIDERATIONS + File.separator + "custom_considerations.yaml"), considerations.values());
        persistToFile(new File(tempFile, DECISIONS + File.separator + "custom_decisions.yaml"), decisions.values());
        persistToFile(new File(tempFile, PROFILES + File.separator + "custom_profiles.yaml"), profiles.values());

        zipDirectory(tempFolder, zipOutput.toPath());
        deleteRecursively(tempFolder);
    }

    public void importAiData(File zipInput) {
        try {
            unzipDirectory(zipInput.toPath());
            loadUserDataRepository();
            persistDataToUserData();
            deleteUserTempFiles();
        } catch (IOException e) {
            logger.error(e, "Could not load data from file");
        }
    }

    private void deleteUserTempFiles() throws IOException {
        var userFolder = Configuration.userDataAiTwDir();

        Files.walk(userFolder.toPath())
            .sorted(Comparator.reverseOrder())
            .filter(p -> p.toFile().getName().startsWith("temp_"))
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    private void deleteRecursively(Path path) throws IOException {
        // Walk the directory in reverse, so we delete children before parent
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    /**
     * Create a new file, ensuring that it is within the destination directory.
     * This covers against the vulnerability for zip slip attacks
     * @param destinationDir
     * @param zipEntry
     * @return the new file
     * @throws IOException
     */
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private void unzipDirectory(Path zipFile) throws IOException {
        var destDir = Configuration.userDataAiTwDir();

        byte[] buffer = new byte[1024];
        var zis = new ZipInputStream(new FileInputStream(zipFile.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }
                if (newFile.exists()) {
                    // rewrite the filename with current timestamp at the end before the extension
                    var newName = newFile.getName();
                    newName = "temp_" + newName;
                    newFile = new File(parent, newName);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    private void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        // Try-with-resources to ensure ZipOutputStream is closed
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // Walk the directory tree
            Files.walk(sourceDir)
                // Only zip up files, not directories themselves
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    // Create a zip entry with a relative path
                    ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                    try {
                        zs.putNextEntry(zipEntry);
                        Files.copy(path, zs);
                        zs.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
    }


    public List<TWDecision> getDecisions() {
        var orderedList = new ArrayList<>(decisions.values());
        orderedList.sort(Comparator.comparing(TWDecision::getName));

        return List.copyOf(orderedList);
    }

    public List<TWConsideration> getConsiderations() {
        var orderedList = new ArrayList<>(considerations.values());
        orderedList.sort(Comparator.comparing(TWConsideration::getName));

        return List.copyOf(orderedList);
    }

    public List<TWDecisionScoreEvaluator> getDecisionScoreEvaluators() {
        var orderedList = new ArrayList<>(decisionScoreEvaluators.values());
        orderedList.sort(Comparator.comparing(TWDecisionScoreEvaluator::getName));

        return List.copyOf(orderedList);
    }

    public List<TWProfile> getProfiles() {
        var orderedList = new ArrayList<>(profiles.values());
        orderedList.sort(Comparator.comparing(TWProfile::getName));
        return List.copyOf(orderedList);
    }

    public boolean hasDecision(String name) {
        return decisions.containsKey(name);
    }

    public void removeDecision(TWDecision decision) {
        decisions.remove(decision.getName());
    }

    public void addDecision(TWDecision decision) {
        if (decisions.containsKey(decision.getName())) {
            logger.info("Decision with name {} already exists, overwriting", decision.getName());
        }
        decisions.put(decision.getName(), decision);
    }

    public boolean hasConsideration(String name) {
        return considerations.containsKey(name);
    }

    public void removeConsideration(TWConsideration consideration) {
        considerations.remove(consideration.getName());
    }

    public void addConsideration(TWConsideration consideration) {
        if (considerations.containsKey(consideration.getName())) {
            logger.info("Consideration with name {} already exists, overwriting", consideration.getName());
        }
        considerations.put(consideration.getName(), consideration);
    }

    public boolean hasDecisionScoreEvaluator(String name) {
        return decisionScoreEvaluators.containsKey(name);
    }

    public void removeDecisionScoreEvaluator(TWDecisionScoreEvaluator decisionScoreEvaluator) {
        decisionScoreEvaluators.remove(decisionScoreEvaluator.getName());
    }

    public void addDecisionScoreEvaluator(TWDecisionScoreEvaluator decisionScoreEvaluator) {
        if (decisionScoreEvaluators.containsKey(decisionScoreEvaluator.getName())) {
            logger.info("DecisionScoreEvaluator with name {} already exists, overwriting", decisionScoreEvaluator.getName());
        }
        decisionScoreEvaluators.put(decisionScoreEvaluator.getName(), decisionScoreEvaluator);
    }

    public boolean hasProfile(String name) {
        return profiles.containsKey(name);
    }

    public void removeProfile(TWProfile profile) {
        profiles.remove(profile.getName());
    }

    public void addProfile(TWProfile profile) {
        if (profiles.containsKey(profile.getName())) {
            logger.info("Profile with name {} already exists, overwriting", profile.getName());
        }
        profiles.put(profile.getName(), profile);
    }

    private void loadRepository() {
        loadData(Configuration.twAiDir());
    }

    private void loadUserDataRepository() {
        loadData(Configuration.userDataAiTwDir());
    }

    private void loadData(File directory) {
        loadConsiderations(new MegaMekFile(new File(directory, CONSIDERATIONS).toString()).getFile())
            .forEach(twConsideration -> considerations.put(twConsideration.getName(), twConsideration));
        loadDecisionScoreEvaluators(new MegaMekFile(new File(directory, EVALUATORS).toString()).getFile()).forEach(
            twDecisionScoreEvaluator -> decisionScoreEvaluators.put(twDecisionScoreEvaluator.getName(), twDecisionScoreEvaluator));
        loadDecisions(new MegaMekFile(new File(directory, DECISIONS).toString()).getFile()).forEach(
            twDecision -> decisions.put(twDecision.getName(), twDecision));
        loadProfiles(new MegaMekFile(new File(directory, PROFILES).toString()).getFile()).forEach(
            twProfile -> profiles.put(twProfile.getName(), twProfile));
    }

    private List<TWDecisionScoreEvaluator> loadDecisionScoreEvaluators(File inputFile) {
        return loadObjects(inputFile, TWDecisionScoreEvaluator.class);
    }

    private List<TWDecision> loadDecisions(File inputFile) {
        return loadObjects(inputFile, TWDecision.class);
    }

    private List<TWConsideration> loadConsiderations(File inputFile) {
        return loadObjects(inputFile, TWConsideration.class);
    }
    private List<TWProfile> loadProfiles(File inputFile) {
        return loadObjects(inputFile, TWProfile.class);
    }

    private <T> List<T> loadObjects(File inputFile, Class<T> clazz) {
        if (inputFile.isDirectory()) {
            var objects = objectsFromDirectory(inputFile, clazz);
            logger.info("Loaded {} objects of type {} from directory {}", objects.size(), clazz.getSimpleName(), inputFile);
            return objects;
        }
        if (inputFile.isFile()) {
            var objects = objectsFromFile(clazz, inputFile);
            if (objects != null) {
                logger.info("Loaded {} objects of type {} from file {}", objects.size(), clazz.getSimpleName(), inputFile);
                return objects;
            }
        }
        logger.info("No objects of type {} loaded from {}", clazz.getSimpleName(), inputFile);
        return Collections.emptyList();
    }

    private <T> List<T> objectsFromDirectory(File inputFile, Class<T> clazz) {
        List<T> objects = new ArrayList<>();
        var files = inputFile.listFiles();
        if (files != null) {
            for (var file : files) {
                objects.addAll(objectsFromFile(clazz, file));
            }
        }
        return objects.stream().filter(Objects::nonNull).toList();
    }

    private <T> List<T> objectsFromFile(Class<T> clazz, File file) {
        if (file.isFile()) {
            try (MappingIterator<T> it = mapper.readerFor(clazz).readValues(file)) {
                return it.readAll();
            } catch (IOException e) {
                logger.error(e, "Could not load file");
            }
        } else if (file.isDirectory()) {
            return objectsFromDirectory(file, clazz);
        }
        return Collections.emptyList();
    }

    private <T> void persistToFile(File outputFile, Collection<T> objects) {
        if (objects.isEmpty()) {
            return;
        }
        try (SequenceWriter seqWriter = mapper.writer().writeValues(outputFile)) {
            for (var object : objects) {
                seqWriter.write(object);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDirectoryStructureIfMissing(File directory) {
        createDirIfNecessary(directory);

        var dirs = List.of(new File(directory, EVALUATORS),
            new File(directory, CONSIDERATIONS),
            new File(directory, DECISIONS),
            new File(directory, PROFILES));

        for (var dir : dirs) {
            createDirIfNecessary(dir);
        }
    }

    private static void createDirIfNecessary(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.error("Could not create directory: {}", dir);
            }
        }
    }

}

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

package megamek.client.bot.duchess.ai.utility.tw;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.client.bot.duchess.ai.utility.tw.considerations.TWConsideration;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecision;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionScoreEvaluator;
import megamek.client.bot.duchess.ai.utility.tw.profile.TWProfile;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
//        persistData();
        loadConsiderations(new MegaMekFile(Configuration.twAiDir(), CONSIDERATIONS).getFile())
            .forEach(twConsideration -> considerations.put(twConsideration.getClass().getSimpleName(), twConsideration));
        loadDecisionScoreEvaluators(new MegaMekFile(Configuration.twAiDir(), EVALUATORS).getFile()).forEach(
            twDecisionScoreEvaluator -> decisionScoreEvaluators.put(twDecisionScoreEvaluator.getName(), twDecisionScoreEvaluator));
        loadDecisions(new MegaMekFile(Configuration.twAiDir(), DECISIONS).getFile()).forEach(
            twDecision -> decisions.put(twDecision.getName(), twDecision));
        loadProfiles(new MegaMekFile(Configuration.twAiDir(), PROFILES).getFile()).forEach(
            twProfile -> profiles.put(twProfile.getName(), twProfile));
        persistDataToUserData();
    }

    public void reloadRepository() {
        decisionScoreEvaluators.clear();
        considerations.clear();
        profiles.clear();
        decisions.clear();
        initialize();
    }

    public List<TWDecision> getDecisions() {
        return List.copyOf(decisions.values());
    }

    public List<TWConsideration> getConsiderations() {
        return List.copyOf(considerations.values());
    }

    public List<TWDecisionScoreEvaluator> getDecisionScoreEvaluators() {
        return List.copyOf(decisionScoreEvaluators.values());
    }

    public List<TWProfile> getProfiles() {
        return List.copyOf(profiles.values());
    }

    public void addDecision(TWDecision decision) {
        if (decisions.containsKey(decision.getName())) {
            logger.info("Decision with name {} already exists, overwriting", decision.getName());
        }
        decisions.put(decision.getName(), decision);
    }

    public void addConsideration(TWConsideration consideration) {
        if (considerations.containsKey(consideration.getName())) {
            logger.info("Consideration with name {} already exists, overwriting", consideration.getName());
        }
        considerations.put(consideration.getName(), consideration);
    }

    public void addDecisionScoreEvaluator(TWDecisionScoreEvaluator decisionScoreEvaluator) {
        if (decisionScoreEvaluators.containsKey(decisionScoreEvaluator.getName())) {
            logger.info("DecisionScoreEvaluator with name {} already exists, overwriting", decisionScoreEvaluator.getName());
        }
        decisionScoreEvaluators.put(decisionScoreEvaluator.getName(), decisionScoreEvaluator);
    }

    public void addProfile(TWProfile profile) {
        if (profiles.containsKey(profile.getName())) {
            logger.info("Profile with name {} already exists, overwriting", profile.getName());
        }
        profiles.put(profile.getName(), profile);
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
        List<T> objects = new ArrayList<>();

        if (inputFile.isDirectory()) {
            var files = inputFile.listFiles();
            if (files != null) {
                for (var file : files) {
                    if (file.isFile()) {
                        try (MappingIterator<T> it = mapper.readerFor(clazz).readValues(file)) {
                            objects.addAll(it.readAll());
                        } catch (IOException e) {
                            logger.error(e, "Could not load file: {}", file);
                        }
                    }
                }
            }
        } else {
            logger.formattedErrorDialog("Invalid directory", "Input file {} is not a directory", inputFile);
        }
        logger.info("Loaded {} objects of type {} from directory {}", objects.size(), clazz.getSimpleName(), inputFile);
        return objects;
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

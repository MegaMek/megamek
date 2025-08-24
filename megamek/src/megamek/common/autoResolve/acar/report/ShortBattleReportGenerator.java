/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.autoResolve.acar.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.board.Board;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Entity;
import megamek.utilities.BoardsTagger;


/**
 * Generates narrative summary text for battle reports based on the context and outcome. Uses localized text templates
 * from properties files.
 *
 * @author Luana Coppio
 * @since 0.50.07
 */
public class ShortBattleReportGenerator {

    // First paragraph templates - force composition and terrain
    private static final String[] FORCE_COMPOSITION_KEYS = {
          "acar.report.forces.standard",
          "acar.report.forces.engaged",
          "acar.report.forces.deployed"
    };

    // Terrain feature descriptors
    private static final String[] TERRAIN_FEATURE_KEYS = {
          "acar.report.terrain.urban",
          "acar.report.terrain.forest",
          "acar.report.terrain.hills",
          "acar.report.terrain.water",
          "acar.report.terrain.rough",
          "acar.report.terrain.clear",
          "acar.report.terrain.otherworldly",
          "acar.report.terrain.mixed"
    };

    // Terrain categorization map
    private static final Map<String, Set<String>> TERRAIN_CATEGORIES = new HashMap<>();

    static {
        // Urban terrain
        TERRAIN_CATEGORIES.put("urban", Set.of(
              "LightUrban", "MediumUrban", "HeavyUrban",
              "Rural", "HeavyBuilding", "HardenedBuilding",
              "ArmoredBuilding", "Fortress", "Hangar"
        ));

        // Forest terrain
        TERRAIN_CATEGORIES.put("forest", Set.of(
              "LightForest", "MediumForest", "DenseForest",
              "Woods", "Jungle", "Foliage"
        ));

        // Hills terrain
        TERRAIN_CATEGORIES.put("hills", Set.of(
              "LowHills", "HighHills", "Cliffs", "Elevator"
        ));

        // Water terrain
        TERRAIN_CATEGORIES.put("water", Set.of(
              "Ocean", "Water", "Swamp", "UnderWaterBridge",
              "HazardousLiquid"
        ));

        // Rough terrain
        TERRAIN_CATEGORIES.put("rough", Set.of(
              "Rough", "Lava", "IceTerrain", "SnowTerrain",
              "Impassable", "FuelTank", "GunEmplacement"
        ));

        // Clear terrain
        TERRAIN_CATEGORIES.put("clear", Set.of(
              "Flat", "Fields", "Roads", "GrassTheme"
        ));

        // Otherworldly terrain
        TERRAIN_CATEGORIES.put("otherworldly", Set.of(
              "LunarTheme", "MarsTheme", "VolcanicTheme",
              "UltraSublevel", "MetalContent"
        ));
    }


    // Atmospheric condition keys
    private static final String[] ATMOSPHERE_KEYS = {
          "acar.report.atmosphere.clear",
          "acar.report.atmosphere.light_rain",
          "acar.report.atmosphere.heavy_rain",
          "acar.report.atmosphere.light_snow",
          "acar.report.atmosphere.heavy_snow",
          "acar.report.atmosphere.light_fog",
          "acar.report.atmosphere.heavy_fog",
          "acar.report.atmosphere.night",
          "acar.report.atmosphere.dusk",
          "acar.report.atmosphere.vacuum"
    };

    // Second paragraph templates - combat duration
    private static final String[] DURATION_KEYS = {
          "acar.report.duration.brief",      // <= 6 rounds
          "acar.report.duration.short",       // 7-12 rounds
          "acar.report.duration.moderate",    // 13-20 rounds
          "acar.report.duration.extended",    // 21-30 rounds
          "acar.report.duration.protracted"   // > 30 rounds
    };

    // Casualty report templates
    private static final String[] CASUALTY_BALANCED_KEYS = {
          "acar.report.casualties.balanced",
          "acar.report.casualties.mutual"
    };

    private static final String[] CASUALTY_ONE_SIDED_KEYS = {
          "acar.report.casualties.onesided",
          "acar.report.casualties.minimal"
    };

    private static final String[] CASUALTY_DEVASTATING_KEYS = {
          "acar.report.casualties.devastating",
          "acar.report.casualties.heavy"
    };

    // Victory status templates
    private static final String[] VICTORY_DECISIVE_KEYS = {
          "acar.report.victory.decisive",
          "acar.report.victory.complete"
    };

    private static final String[] VICTORY_MARGINAL_KEYS = {
          "acar.report.victory.marginal",
          "acar.report.victory.narrow"
    };

    private static final String[] VICTORY_PYRRHIC_KEYS = {
          "acar.report.victory.pyrrhic",
          "acar.report.victory.costly"
    };

    private static final String DRAW_KEY = "acar.report.outcome.draw";

    // Force status templates
    private static final String[] FORCE_STATUS_INTACT_KEYS = {
          "acar.report.status.intact",
          "acar.report.status.operational"
    };

    private static final String[] FORCE_STATUS_REDUCED_KEYS = {
          "acar.report.status.reduced",
          "acar.report.status.depleted"
    };

    private static final String[] FORCE_STATUS_CRITICAL_KEYS = {
          "acar.report.status.critical",
          "acar.report.status.combat_ineffective"
    };

    private static final String[] FORCE_STATUS_VANQUISHED_KEYS = {
          "acar.report.status.vanquished",
          "acar.report.status.annihilated"
    };

    // Report format templates
    private static final String REPORT_FORMAT = "acar.report.format.standard";

    private final SimulationContext context;
    private final SimulationManager simulationManager;
    private final Board board;
    private final PlanetaryConditions planetaryConditions;
    private final Random rand;

    /**
     * Constructor for BattleReportGenerator
     *
     * @param simulationManager The simulation manager with access to victory results
     */
    public ShortBattleReportGenerator(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
        this.context = simulationManager.getGame();
        this.planetaryConditions = simulationManager.getGame().getPlanetaryConditions();
        this.rand = new Random(context.getSeed());
        this.board = simulationManager.getGame().getBoard();
    }

    /**
     * Generates a factual battle report based on simulation results.
     *
     * @return Formatted battle report with two paragraphs
     */
    public PublicReportEntry generateReport() {
        // Gather battle data
        BattleData battleData = gatherBattleData();

        // Generate first paragraph - forces and terrain
        String firstParagraph = generateFirstParagraph(battleData);

        // Generate second paragraph - duration and outcome
        String secondParagraph = generateSecondParagraph(battleData);

        // Combine into final report
        return new PublicReportEntry(REPORT_FORMAT)
              .add(firstParagraph)
              .add(secondParagraph);
    }

    /**
     * Gathers all relevant battle data for report generation
     */
    private BattleData gatherBattleData() {
        BattleData data = new BattleData();

        // Round count
        data.rounds = context.getCurrentRound();

        // Team data
        var victoryResult = simulationManager.getCurrentVictoryResult();
        data.winningTeamId = victoryResult.getWinningTeam();

        // Calculate forces for each team
        for (var team : context.getTeams()) {
            TeamData teamData = new TeamData();
            teamData.teamId = team.getId();
            teamData.teamName = team.toString();

            // Count units for all players in the team
            int remainingUnits = 0;
            int destroyedUnits = 0;
            int retreatingUnits = 0;

            for (var player : team.players()) {
                // Get remaining entities (active units)
                var playerEntities = context.getInGameObjects().stream()
                      .filter(e -> e.getOwnerId() == player.getId())
                      .filter(Entity.class::isInstance)
                      .count();

                // Get destroyed entities
                var deadEntities = context.getGraveyard().stream()
                      .filter(e -> e.getOwnerId() == player.getId())
                      .filter(Entity.class::isInstance)
                      .count();

                // Get retreating entities
                var retreatingEntities = context.getRetreatingUnits().stream()
                      .filter(e -> e.getOwnerId() == player.getId())
                      .count();

                remainingUnits += (int) playerEntities;
                destroyedUnits += (int) deadEntities;
                retreatingUnits += (int) retreatingEntities;
            }

            // Calculate totals
            teamData.remainingUnits = remainingUnits; // already counts retreated units
            teamData.destroyedUnits = destroyedUnits;
            teamData.retreatingUnits = retreatingUnits;
            teamData.initialUnits = remainingUnits + destroyedUnits;
            teamData.losses = destroyedUnits; // Only count destroyed as losses

            data.teams.put(team.getId(), teamData);
        }

        // Calculate total units
        data.totalInitialUnits = data.teams.values().stream()
              .mapToInt(t -> t.initialUnits)
              .sum();
        data.totalRemainingUnits = data.teams.values().stream()
              .mapToInt(t -> t.remainingUnits)
              .sum();
        data.totalDestroyedUnits = data.teams.values().stream()
              .mapToInt(t -> t.destroyedUnits)
              .sum();
        data.totalRetreatingUnits = data.teams.values().stream()
              .mapToInt(t -> t.retreatingUnits)
              .sum();

        // Get terrain tags
        data.terrainTags = getTerrainTags();

        // Get atmospheric conditions
        data.atmosphericConditions = getAtmosphericConditions();

        return data;
    }


    /**
     * Generates the first paragraph describing forces and terrain
     */
    private String generateFirstParagraph(BattleData data) {
        String forceCompositionKey = selectRandom(FORCE_COMPOSITION_KEYS);

        // Build team force descriptions
        List<String> teamDescriptions = new ArrayList<>();
        for (TeamData team : data.teams.values()) {
            teamDescriptions.add(new PublicReportEntry("acar.report.team_forces")
                  .add(team.teamName)
                  .add(team.initialUnits)
                  .noNL()
                  .text());
        }

        // Select terrain description
        String terrainKey = selectTerrainKey(data.terrainTags);
        List<String> terrainFeatures = getUpToThreeTerrainFeatures(data.terrainTags);
        String terrainDescription = new PublicReportEntry(terrainKey)
              .add(String.join(", ", terrainFeatures))
              .noNL()
              .text();

        // Get atmospheric description
        String atmosphereDescription = new PublicReportEntry(data.atmosphericConditions)
              .noNL()
              .text();

        // Combine into first paragraph
        return new PublicReportEntry(forceCompositionKey)
              .add(data.totalInitialUnits)
              .add(String.join(new PublicReportEntry("acar.report.and").noNL().text(), teamDescriptions))
              .add(terrainDescription)
              .add(atmosphereDescription)
              .noNL()
              .text();
    }

    /**
     * Generates the second paragraph describing combat duration and outcome
     */
    private String generateSecondParagraph(BattleData data) {
        // Select duration descriptor
        String durationKey = selectDurationKey(data.rounds);
        String durationText = new PublicReportEntry(durationKey)
              .add(data.rounds)
              .noNL()
              .text();

        // Generate casualty report
        String casualtyText = generateCasualtyReport(data);

        // Generate victory status
        String victoryText = generateVictoryStatus(data);

        // Generate force status
        String forceStatusText = generateForceStatus(data);

        // Combine into second paragraph
        return new PublicReportEntry("acar.report.paragraph2_format")
              .add(durationText)
              .add(casualtyText)
              .add(victoryText)
              .add(forceStatusText)
              .noNL()
              .text();
    }

    /**
     * Generates casualty report based on losses
     */
    private String generateCasualtyReport(BattleData data) {
        // Calculate casualty patterns
        int totalLosses = data.totalInitialUnits - data.totalRemainingUnits;
        double casualtyRate = (double) totalLosses / data.totalInitialUnits;

        // Check if one-sided
        boolean oneSided = false;
        for (TeamData team : data.teams.values()) {
            if (team.losses == 0 && totalLosses > 0) {
                oneSided = true;
                break;
            }
        }

        String casualtyKey;
        if (oneSided) {
            casualtyKey = selectRandom(CASUALTY_ONE_SIDED_KEYS);
        } else if (casualtyRate > 0.5) {
            casualtyKey = selectRandom(CASUALTY_DEVASTATING_KEYS);
        } else {
            casualtyKey = selectRandom(CASUALTY_BALANCED_KEYS);
        }

        // Build casualty details for each team
        List<String> casualtyDetails = new ArrayList<>();
        for (TeamData team : data.teams.values()) {
            casualtyDetails.add(new PublicReportEntry("acar.report.team_losses")
                  .add(team.teamName)
                  .add(team.losses)
                  .noNL()
                  .text());
        }

        return new PublicReportEntry(casualtyKey)
              .add(totalLosses)
              .add(String.join(", ", casualtyDetails))
              .noNL()
              .text();
    }

    /**
     * Generates victory status text
     */
    private String generateVictoryStatus(BattleData data) {
        if (data.winningTeamId == -1) {
            return new PublicReportEntry(DRAW_KEY).noNL().text();
        }

        TeamData winner = data.teams.get(data.winningTeamId);
        if (winner == null) {
            return new PublicReportEntry(DRAW_KEY).noNL().text();
        }

        // Calculate victory type
        double winnerCasualtyRate = (double) winner.losses / winner.initialUnits;
        double winnerStrengthRatio = (double) winner.remainingUnits / winner.initialUnits;

        String victoryKey;
        if (winnerCasualtyRate > 0.5) {
            victoryKey = selectRandom(VICTORY_PYRRHIC_KEYS);
        } else if (winnerStrengthRatio > 0.8) {
            victoryKey = selectRandom(VICTORY_DECISIVE_KEYS);
        } else {
            victoryKey = selectRandom(VICTORY_MARGINAL_KEYS);
        }

        return new PublicReportEntry(victoryKey)
              .add(winner.teamName)
              .noNL()
              .text();
    }

    /**
     * Generates force status summary
     */
    private String generateForceStatus(BattleData data) {
        List<String> statusReports = new ArrayList<>();

        for (TeamData team : data.teams.values()) {
            double strengthRatio = (double) team.remainingUnits / team.initialUnits;

            String statusKey;
            if (strengthRatio > 0.8) {
                statusKey = selectRandom(FORCE_STATUS_INTACT_KEYS);
            } else if (strengthRatio > 0.4) {
                statusKey = selectRandom(FORCE_STATUS_REDUCED_KEYS);
            } else if (strengthRatio > 0.0) {
                statusKey = selectRandom(FORCE_STATUS_CRITICAL_KEYS);
            } else {
                statusKey = selectRandom(FORCE_STATUS_VANQUISHED_KEYS);
            }

            statusReports.add(new PublicReportEntry(statusKey)
                  .add(team.teamName)
                  .add(team.remainingUnits)
                  .add(team.initialUnits)
                  .noNL()
                  .text());
        }

        return String.join(" ", statusReports);
    }

    /**
     * Selects appropriate terrain key based on tags
     */
    private String selectTerrainKey(List<String> tags) {
        // Count how many tags belong to each category
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (String tag : tags) {
            // Check which categories this tag belongs to
            for (Map.Entry<String, Set<String>> entry : TERRAIN_CATEGORIES.entrySet()) {
                if (entry.getValue().contains(tag)) {
                    categoryCounts.merge(entry.getKey(), 1, Integer::sum);
                }
            }
        }

        // Find the dominant category
        if (categoryCounts.isEmpty()) {
            return "acar.report.terrain.unremarkable"; // Default to unremarkable
        }

        String dominantCategory = (categoryCounts.size() >= 3) ? "mixed" :
              categoryCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("mixed");

        // Map category to terrain key
        return switch (dominantCategory) {
            case "urban" -> TERRAIN_FEATURE_KEYS[0];
            case "forest" -> TERRAIN_FEATURE_KEYS[1];
            case "hills" -> TERRAIN_FEATURE_KEYS[2];
            case "water" -> TERRAIN_FEATURE_KEYS[3];
            case "rough" -> TERRAIN_FEATURE_KEYS[4];
            case "clear" -> TERRAIN_FEATURE_KEYS[5];
            case "otherworldly" -> TERRAIN_FEATURE_KEYS[6];
            default -> TERRAIN_FEATURE_KEYS[7]; // mixed
        };
    }

    /**
     * Selects appropriate duration key based on round count
     */
    private String selectDurationKey(int rounds) {
        if (rounds <= 6) {
            return DURATION_KEYS[0];
        } else if (rounds <= 12) {
            return DURATION_KEYS[1];
        } else if (rounds <= 20) {
            return DURATION_KEYS[2];
        } else if (rounds <= 30) {
            return DURATION_KEYS[3];
        } else {
            return DURATION_KEYS[4];
        }
    }

    /**
     * Gets terrain tags from the board
     */
    private List<String> getTerrainTags() {
        return BoardsTagger.tagsFor(board).stream().map(t -> t.replace(" (Auto)", ""))
              .toList();
    }

    /**
     * Gets atmospheric conditions, this is a very simplistic atmospheric condition, but should suffice for now
     */
    private String getAtmosphericConditions() {
        // This requires planetary conditions to be present in the simulation context
        // right now it is a placeholder
        if (planetaryConditions.getWeather().isLightRain()) {
            return ATMOSPHERE_KEYS[1];
        }
        if (planetaryConditions.getWeather().isHeavyRainOrGustingRainOrDownpour()) {
            return ATMOSPHERE_KEYS[2];
        }
        if (planetaryConditions.getWeather().isLightSnow()) {
            return ATMOSPHERE_KEYS[3];
        }
        if (planetaryConditions.getWeather().isHeavySnow()) {
            return ATMOSPHERE_KEYS[4];
        }
        if (planetaryConditions.getFog().isFogLight()) {
            return ATMOSPHERE_KEYS[5];
        }
        if (planetaryConditions.getFog().isFogHeavy()) {
            return ATMOSPHERE_KEYS[6];
        }
        if (planetaryConditions.getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            return ATMOSPHERE_KEYS[7];
        }
        if (planetaryConditions.getLight().isDusk()) {
            return ATMOSPHERE_KEYS[8];
        }
        if (planetaryConditions.getAtmosphere().isVacuum()) {
            return ATMOSPHERE_KEYS[9];
        }

        return ATMOSPHERE_KEYS[0]; // Default to clear
    }

    private List<String> getUpToThreeTerrainFeatures(List<String> terrainTags) {
        return terrainTags.stream().limit(3)
              .map(t -> "acar.report.terrain." + t)
              .map(t -> new PublicReportEntry(t).noNL().text()).toList();
    }

    /**
     * Selects a random element from array
     */
    private String selectRandom(String[] keys) {
        return keys[rand.nextInt(keys.length)];
    }

    /**
     * Internal class for battle data
     */
    private static class BattleData {
        int rounds;
        int totalInitialUnits;
        int totalRemainingUnits;
        int totalDestroyedUnits;
        int totalRetreatingUnits;
        int winningTeamId = -1;
        Map<Integer, TeamData> teams = new HashMap<>();
        List<String> terrainTags;
        String atmosphericConditions;
    }

    /**
     * Internal class for team data
     */
    private static class TeamData {
        int teamId;
        String teamName;
        int initialUnits;
        int remainingUnits;
        int destroyedUnits;
        int retreatingUnits;
        int losses; // destroyed units only
    }
}

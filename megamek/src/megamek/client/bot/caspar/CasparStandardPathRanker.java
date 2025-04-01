/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */

package megamek.client.bot.caspar;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import megamek.ai.axis.InputAxisCalculator;
import megamek.ai.neuralnetwork.NeuralNetwork;
import megamek.client.bot.common.GameState;
import megamek.client.bot.common.behavior.MovementPreference;
import megamek.client.bot.common.formation.Formation;
import megamek.client.bot.common.formation.FormationManager;
import megamek.client.bot.princess.BasicPathRanker;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.RankedPath;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * The path ranker for CASPAR (Combat Algorithmic System for Predictive Analysis and Response).
 * @author Luana Coppio
 */
public class CasparStandardPathRanker extends BasicPathRanker {
    private final static MMLogger logger = MMLogger.create(CasparStandardPathRanker.class);

    private CasparAI casparAI;
    private final CasparGameState gameState;

    public CasparStandardPathRanker(Caspar caspar) {
        super(caspar);
        gameState = new CasparGameState(caspar);
    }

    public void setCasparAI(CasparAI casparAI) {
        this.casparAI = casparAI;
    }

    private FormationManager getFormationManager() {
        return casparAI.getFormationManager();
    }

    private NeuralNetwork getNeuralNetwork() {
        return Objects.requireNonNull(casparAI).getNeuralNetwork();
    }

    private InputAxisCalculator getInputAxisCalculator() {
        return Objects.requireNonNull(casparAI).getInputCalculator();
    }

    private DifficultyManager getDifficultyManager() {
        return Objects.requireNonNull(casparAI).getDifficultyManager();
    }

    private MovementPreference getMovementPreference() {
        return Objects.requireNonNull(casparAI).getBehaviorSettings().getMovementPreference();
    }

    /**
     * Evaluates a movement path using the neural network.
     *
     * @param path The movement path to evaluate
     * @param gameState The current game state
     * @return A score between 0 and 1
     */
    public ClassificationScore evaluateMovePath(MovePath path, GameState gameState) {
        float[] inputVector = getInputAxisCalculator().calculateInputVector(path, gameState);
        return getNeuralNetwork().predict(inputVector);
    }

    /**
     * Returns the best path of a list of ranked paths.
     *
     * @param possiblePaths The list of ranked paths to process
     * @return "Best" out of those paths
     */
    @Override
    public @Nullable RankedPath getBestPath(TreeSet<RankedPath> possiblePaths) {
        if (possiblePaths.isEmpty()) {
            return null;
        }
        return getDifficultyManager().selectFromTopPaths(possiblePaths);
    }

    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance, List<Entity> enemies, Coords friendsCoords) {
        ClassificationScore classificationScore = evaluateMovePath(path, gameState).modifyBy(getMovementPreference());
        return new RankedPath(classificationScore.getRank(), path,
              "Move Path Evaluation [" + classificationScore.getClassification() + "] - " + classificationScore);
    }

    @Override
    public @Nullable Coords calculateAlliesCenter(Entity unit, @Nullable List<Entity> friends, Game game) {
        return getFormationManager().getUnitFormation(unit)
              .map(Formation::getFormationCenter)
              .orElse(calcAllyCenter(unit.getId(), friends, game));
    }
}

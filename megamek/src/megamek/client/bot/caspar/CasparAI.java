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

import megamek.ai.axis.InputAxisCalculator;
import megamek.ai.neuralnetwork.NeuralNetwork;
import megamek.client.bot.common.AdvancedAgent;
import megamek.client.bot.common.DifficultyLevel;
import megamek.client.bot.common.GameState;
import megamek.client.bot.common.formation.FormationManager;
import megamek.client.bot.princess.BehaviorSettings;

/**
 * The main AI controller for CASPAR (Combat Algorithmic System for Predictive Analysis and Response). This class
 * coordinates the decision-making process using neural networks for both movement and firing solutions.
 *
 * @author Luana Coppio
 */
public class CasparAI {
    private final InputAxisCalculator inputCalculator;
    private final FireControlHandler fireControlHandler;
    private final FormationManager formationManager;
    private final TacticalPlanner tacticalPlanner;
    private final AdvancedAgent agent;
    private final BehaviorSettings behaviorSettings;
    private final NeuralNetwork neuralNetwork;
    private DifficultyManager difficultyManager;

    private CasparAI(Builder builder) {
        this.neuralNetwork = builder.neuralNetwork;
        this.inputCalculator = builder.inputCalculator;
        this.fireControlHandler = builder.fireControlHandler;
        this.formationManager = builder.formationManager;
        this.tacticalPlanner = builder.tacticalPlanner;
        this.difficultyManager = builder.difficultyManager;
        this.agent = builder.agent;
        this.behaviorSettings = agent.getBehaviorSettings();
    }

    public AdvancedAgent getAgent() {
        return agent;
    }

    /**
     * Returns the current game state.
     *
     * @return The current game state
     */
    public GameState getGameState() {
        return new CasparGameState(getAgent());
    }

    public NeuralNetwork getNeuralNetwork() {
        return neuralNetwork;
    }

    public InputAxisCalculator getInputCalculator() {
        return inputCalculator;
    }

    public FireControlHandler getFireControlHandler() {
        return fireControlHandler;
    }

    public FormationManager getFormationManager() {
        return formationManager;
    }

    public TacticalPlanner getTacticalPlanner() {
        return tacticalPlanner;
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public DifficultyManager getDifficultyManager() {
        return difficultyManager;
    }

    public void changeDifficulty(DifficultyLevel level) {
        difficultyManager = new DifficultyManager(level);
    }

    /**
     * Creates a neural network appropriate for the current difficulty level.
     */
    public void changeNeuralNetworkModel(String modelName) {
        this.neuralNetwork.loadModel(modelName);
    }

    /**
     * Builder pattern for creating CasparAI instances.
     */
    public static class Builder {
        private final AdvancedAgent agent;
        private final String modelName;
        private NeuralNetwork neuralNetwork;
        private InputAxisCalculator inputCalculator;
        private FireControlHandler fireControlHandler;
        private FormationManager formationManager;
        private TacticalPlanner tacticalPlanner;
        private DifficultyManager difficultyManager;

        public Builder(AdvancedAgent agent, String modelName) {
            this.agent = agent;
            this.modelName = modelName;
        }

        public Builder withInputCalculator(InputAxisCalculator inputCalculator) {
            this.inputCalculator = inputCalculator;
            return this;
        }

        public Builder withFireControlHandler(FireControlHandler fireControlHandler) {
            this.fireControlHandler = fireControlHandler;
            return this;
        }

        public Builder withFormationManager(FormationManager formationManager) {
            this.formationManager = formationManager;
            return this;
        }

        public Builder withTacticalPlanner(TacticalPlanner tacticalPlanner) {
            this.tacticalPlanner = tacticalPlanner;
            return this;
        }

        public Builder withDifficultyManager(DifficultyManager difficultyManager) {
            this.difficultyManager = difficultyManager;
            return this;
        }

        public CasparAI build() {
            // Validate and provide defaults if needed

            if (agent == null) {
                throw new IllegalStateException("Agent must be provided");
            }

            if (formationManager == null) {
                this.formationManager = new FormationManager();
            }

            if (inputCalculator == null) {
                this.inputCalculator = new DefaultInputAxisCalculator();
            }

            if (difficultyManager == null) {
                this.difficultyManager = new DifficultyManager(DifficultyLevel.HARDCORE);
            }

            if (fireControlHandler == null) {
                this.fireControlHandler = new FireControlHandler(difficultyManager);
            }

            if (tacticalPlanner == null) {
                this.tacticalPlanner = new TacticalPlanner(difficultyManager);
            }

            this.neuralNetwork = new NeuralNetwork().loadModel(modelName);

            return new CasparAI(this);
        }
    }
}

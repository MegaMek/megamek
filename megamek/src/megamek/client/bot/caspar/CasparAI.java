///*
// * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
// *
// * This file is part of MegaMek.
// *
// * MegaMek is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License (GPL),
// * version 2 or (at your option) any later version,
// * as published by the Free Software Foundation.
// *
// * MegaMek is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty
// * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// * See the GNU General Public License for more details.
// *
// * A copy of the GPL should have been included with this project;
// * if not, see <https://www.gnu.org/licenses/>.
// *
// * NOTICE: The MegaMek organization is a non-profit group of volunteers
// * creating free software for the BattleTech community.
// *
// * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
// * of The Topps Company, Inc. All Rights Reserved.
// *
// * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
// * InMediaRes Productions, LLC.
// */
//package megamek.client.bot.caspar;
//
//import megamek.client.bot.princess.RankedPath;
//import megamek.common.Entity;
//import megamek.common.MovePath;
//
//import java.util.List;
//import java.util.TreeSet;
//
///**
// * The main AI controller for CASPAR (Combat Algorithmic System for Predictive Analysis and Response).
// * This class coordinates the decision-making process using neural networks for both movement and firing solutions.
// * @author Luana Coppio
// */
//public class CasparAI {
//    private final NeuralNetwork neuralNetwork;
//    private final InputAxisCalculator inputCalculator;
//    private final MovementHandler movementHandler;
//    private final FireControlHandler fireControlHandler;
//    private final FormationManager formationManager;
//    private final TacticalPlanner tacticalPlanner;
//    private final DifficultyManager difficultyManager;
//
//    private CasparAI(Builder builder) {
//        this.neuralNetwork = builder.neuralNetwork;
//        this.inputCalculator = builder.inputCalculator;
//        this.movementHandler = builder.movementHandler;
//        this.fireControlHandler = builder.fireControlHandler;
//        this.formationManager = builder.formationManager;
//        this.tacticalPlanner = builder.tacticalPlanner;
//        this.difficultyManager = builder.difficultyManager;
//    }
//
//    /**
//     * Evaluates a movement path and returns a score based on the neural network output.
//     *
//     * @param movePath The movement path to evaluate
//     * @return A score between 0 and 1
//     */
//    public double evaluateMovePath(MovePath movePath) {
//        double[] inputVector = inputCalculator.calculateInputVector(movePath);
//        return neuralNetwork.predict(inputVector);
//    }
//
//    /**
//     * Selects the best movement path for a unit from a set of possible paths.
//     *
//     * @param unit The unit to move
//     * @param possiblePaths List of possible movement paths
//     * @return The selected movement path
//     */
//    public MovePath selectMovePath(Entity unit, List<MovePath> possiblePaths) {
//        TreeSet<RankedPath> scoredPaths = new TreeSet<>();
//
//        for (MovePath path : possiblePaths) {
//            double score = evaluateMovePath(path);
//            scoredPaths.add(new RankedPath(score, path, "Move Path Evaluation"));
//        }
//
//        // Choose one of the top paths based on difficulty settings
//        return difficultyManager.selectFromTopPaths(scoredPaths);
//    }
//
//    /**
//     * Builder pattern for creating CasparAI instances.
//     */
//    public static class Builder {
//        private NeuralNetwork neuralNetwork;
//        private InputAxisCalculator inputCalculator;
//        private MovementHandler movementHandler;
//        private FireControlHandler fireControlHandler;
//        private FormationManager formationManager;
//        private TacticalPlanner tacticalPlanner;
//        private DifficultyManager difficultyManager;
//
//        public Builder withNeuralNetwork(NeuralNetwork neuralNetwork) {
//            this.neuralNetwork = neuralNetwork;
//            return this;
//        }
//
//        public Builder withInputCalculator(InputAxisCalculator inputCalculator) {
//            this.inputCalculator = inputCalculator;
//            return this;
//        }
//
//        public Builder withMovementHandler(MovementHandler movementHandler) {
//            this.movementHandler = movementHandler;
//            return this;
//        }
//
//        public Builder withFireControlHandler(FireControlHandler fireControlHandler) {
//            this.fireControlHandler = fireControlHandler;
//            return this;
//        }
//
//        public Builder withFormationManager(FormationManager formationManager) {
//            this.formationManager = formationManager;
//            return this;
//        }
//
//        public Builder withTacticalPlanner(TacticalPlanner tacticalPlanner) {
//            this.tacticalPlanner = tacticalPlanner;
//            return this;
//        }
//
//        public Builder withDifficultyManager(DifficultyManager difficultyManager) {
//            this.difficultyManager = difficultyManager;
//            return this;
//        }
//
//        public CasparAI build() {
//            // Validate and provide defaults if needed
//            if (neuralNetwork == null) {
//                throw new IllegalStateException("Neural network must be provided");
//            }
//
//            if (inputCalculator == null) {
//                this.inputCalculator = new DefaultInputAxisCalculator();
//            }
//
//            if (movementHandler == null) {
//                this.movementHandler = new DefaultMovementHandler();
//            }
//
//            if (fireControlHandler == null) {
//                this.fireControlHandler = new DefaultFireControlHandler();
//            }
//
//            if (formationManager == null) {
//                this.formationManager = new DefaultFormationManager();
//            }
//
//            if (tacticalPlanner == null) {
//                this.tacticalPlanner = new DefaultTacticalPlanner();
//            }
//
//            if (difficultyManager == null) {
//                this.difficultyManager = new DefaultDifficultyManager();
//            }
//
//            return new CasparAI(this);
//        }
//    }
//}

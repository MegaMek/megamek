/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
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
package megamek.ai.optimizer;

import megamek.ai.dataset.ActionAndState;
import megamek.ai.dataset.TrainingDataset;
import megamek.ai.dataset.UnitState;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is responsible for optimizing the parameters of a cost function using a training dataset.
 * @author Luana Coppio
 */
public class ParameterOptimizer {
    private final TrainingDataset actionAndStates;
    private final CostFunction costFunction;
    private final Map<Integer, UnitState> unitStateMap = new HashMap<>();
    private final Map<Integer, UnitState> nextUnitStateMap = new HashMap<>();

    private final int maxIterations; // = 100_000;
    private final double tolerance; // = 1e-6;
    private final double baseLearningRate; // = 1e-3;
    private final double maxLearningRate; // = 1e-1;
    private final int cycleLength; // = 2000;
    private final int numberOfParameters;

    private double learningRate;
    private final int patience; // = 50;
    private Parameters velocity;
    private final double momentum; // = 0.9;
    double[] finalParams;
    private Checkpoint checkpoint;

    private final Random random = new Random();

    private ParameterOptimizer(Builder builder) {
        actionAndStates = builder.actionAndStates;
        costFunction = builder.costFunction;
        maxIterations = builder.maxIterations;
        tolerance = builder.tolerance;
        baseLearningRate = builder.baseLearningRate;
        maxLearningRate = builder.maxLearningRate;
        cycleLength = builder.cycleLength;
        learningRate = builder.learningRate;
        patience = builder.patience;
        momentum = builder.momentum;
        checkpoint = builder.checkpoint;
        numberOfParameters = builder.numberOfParameters;
        velocity = Parameters.zeroes(numberOfParameters);
    }

    public static Builder newBuilder(ParameterOptimizer copy) {
        return new Builder(copy.actionAndStates, copy.costFunction, copy.learningRate, copy.maxIterations,
            copy.tolerance, copy.baseLearningRate, copy.maxLearningRate, copy.cycleLength,
            copy.patience, copy.momentum, copy.checkpoint);
    }

    /**
     * Optimize the parameters of the cost function using the training dataset.
     * @return The optimized parameters
     */
    public Parameters optimize() {
        Parameters params = Parameters.random(numberOfParameters, random, 0, 1);

        if (checkpoint != null) {
            params = Parameters.fromArray(checkpoint.getValues());
            params = params.addNoise(0.01, random);
        }

        double bestLoss = Double.MAX_VALUE;
        int noImprovementCount = 0;
        for (int i = 0; i < maxIterations; i++) {
            learningRate = baseLearningRate + (maxLearningRate-baseLearningRate)*(1 + Math.cos(2*Math.PI*i/cycleLength))/2;
            // Add exploration noise
            if(i % 100 == 0) {
                params = params.addNoise(0.01, random);
            }

            Parameters gradient = computeGradient(params);
            velocity = velocity.multiply(momentum).add(gradient.multiply(learningRate));
            params = params.subtract(velocity);

            // Clamp parameters
            params = params.clamp(0.0, 1.0);
            Iterator<ActionAndState> iterator = actionAndStates.sampleTrainingDataset(actionAndStates.size() / 2).iterator();
            // Adaptive restart
            double currentLoss = computeLoss(params, iterator);
            if(currentLoss < bestLoss) {
                bestLoss = currentLoss;
            } else {
                if(++noImprovementCount > patience) {
                    learningRate *= 0.5;
                    params = params.addNoise(0.05, random);
                    noImprovementCount = 0;
                }
            }

            // Print progress
            if(i % (maxIterations / 10) == 0) {
                System.out.printf("Iter %6d | Loss: %.2e | LR: %.2e >> ", i, bestLoss, learningRate);
            }
            if (bestLoss < tolerance) {
                break;
            }
        }

        System.out.printf("\n\tFinal loss: %.8f%n\t%s\n", bestLoss, params);
        finalParams = params.toArray();
        return params;
    }

    private Parameters clipGradient(Parameters grad) {
        double maxGrad = 1.0;
        return grad.multiply(maxGrad/grad.maxAbs());
    }

    private double computeLoss(Parameters params, Iterator<ActionAndState> batch) {
        List<Double> results = new ArrayList<>();


        while (batch.hasNext()) {
            ActionAndState actionAndState = batch.next();
            if (!batch.hasNext()) break;
            ActionAndState futureActionAndState = batch.next();

            unitStateMap.clear();
            unitStateMap.putAll(actionAndState.boardUnitState().stream()
                .collect(Collectors.toMap(UnitState::id, Function.identity())));
            nextUnitStateMap.clear();
            nextUnitStateMap.putAll(futureActionAndState.boardUnitState().stream()
                .collect(Collectors.toMap(UnitState::id, Function.identity())));
            results.add(costFunction.resolve(
                actionAndState.unitAction(), unitStateMap, nextUnitStateMap, params
            ));
        }

        double mse = results.stream().mapToDouble(v -> v * v).average().orElse(0.0);
        double reg = params.stream().mapToDouble(v -> v * v).sum() * 1e-4;
        // Mean Squared Error loss
        return mse + reg;
    }

    private Parameters computeGradient(Parameters params) {
        Parameters gradient = Parameters.zeroes(numberOfParameters);
        Iterator<ActionAndState> batch = actionAndStates.sampleTrainingDataset(actionAndStates.size() / 5).iterator();
        double baseLoss = computeLoss(params, batch);

        for (int i = 0; i < params.size(); i++) {
            batch = actionAndStates.sampleTrainingDataset(actionAndStates.size() / 5).iterator();
            double epsilon = adaptiveEpsilon(params.get(i));
            Parameters perturbed = params.perturb(i, epsilon);
            double perturbedLoss = computeLoss(perturbed, batch);
            double derivative = (perturbedLoss - baseLoss) / epsilon;
            gradient.set(i, derivative);
        }
        return clipGradient(gradient);
    }

    private double adaptiveEpsilon(double paramValue) {
        return Math.max(1e-8, 1e-5*Math.abs(paramValue));
    }

    /**
     * Return the current state of the optimizer in a checkpoint.
     * @return The checkpoint
     */
    public Checkpoint saveCheckpoint() {
        this.checkpoint = new Checkpoint(this);
        return checkpoint;
    }

    /**
     * {@code ParameterOptimizer} builder static inner class.
     */
    public static final class Builder {
        private final TrainingDataset actionAndStates;
        private final CostFunction costFunction;
        private int maxIterations = 100_000;
        private double tolerance = 1e-6;
        private double baseLearningRate = 1e-3;
        private double maxLearningRate = 1e-1;
        private int cycleLength = 2000;
        private final double learningRate;
        private int patience = 50;
        private double momentum = 0.9;
        private final int numberOfParameters;
        private Checkpoint checkpoint;

        public Builder(TrainingDataset actionAndStates, CostFunction costFunction, double learningRate) {
            this.actionAndStates = actionAndStates;
            this.costFunction = costFunction;
            this.learningRate = learningRate;
            this.numberOfParameters = costFunction.numberOfParameters();
        }

        public Builder(TrainingDataset actionAndStates, CostFunction costFunction, double learningRate, int maxIterations,
                       double tolerance, double baseLearningRate, double maxLearningRate, int cycleLength, int patience, double momentum,
                       Checkpoint checkpoint) {
            this(actionAndStates, costFunction, learningRate);
            this.maxIterations = maxIterations;
            this.tolerance = tolerance;
            this.baseLearningRate = baseLearningRate;
            this.maxLearningRate = maxLearningRate;
            this.cycleLength = cycleLength;
            this.patience = patience;
            this.momentum = momentum;
            this.checkpoint = checkpoint;
        }

        public static Builder newBuilder(TrainingDataset actionAndStates, CostFunction costFunction, double learningRate) {
            return new Builder(actionAndStates, costFunction, learningRate);
        }

        /**
         * Sets the {@code maxIterations} and returns a reference to this Builder enabling method chaining.
         * The {@code maxIterations} is the maximum number of iterations for the optimizer.
         * {@code maxIterations} default value is 100_000.
         * @param val the {@code maxIterations} to set
         * @return a reference to this Builder
         */
        public Builder withMaxIterations(int val) {
            maxIterations = val;
            return this;
        }

        /**
         * Sets the {@code checkpoint} and returns a reference to this Builder enabling method chaining.
         * The {@code checkpoint} is the maximum number of iterations to run the optimizer.
         * @param val the {@code checkpoint} to set
         * @return a reference to this Builder
         */
        public Builder withCheckpoint(Checkpoint val) {
            checkpoint = val;
            return this;
        }

        /**
         * Sets the {@code tolerance} and returns a reference to this Builder enabling method chaining.
         * The {@code tolerance} is a threshold for the calculated loss, if it is lower than tolerance then it considers the training done.
         * {@code tolerance} default value is 1e-6.
         * @param val the {@code tolerance} to set
         * @return a reference to this Builder
         */
        public Builder withTolerance(int val) {
            tolerance = val;
            return this;
        }

        /**
         * Sets the {@code baseLearningRate} and returns a reference to this Builder enabling method chaining.
         * The {@code baseLearningRate} is the minimum learning rate the system will set itself during cycles.
         * {@code baseLearningRate} default value is 1e-3.
         * @param val the {@code baseLearningRate} to set
         * @return a reference to this Builder
         */
        public Builder withBaseLearningRate(double val) {
            baseLearningRate = val;
            return this;
        }

        /**
         * Sets the {@code maxLearningRage} and returns a reference to this Builder enabling method chaining.
         * The {@code maxLearningRage} is the minimum learning rate the system will set itself during cycles.
         * {@code maxLearningRage} default value is 1e-1.
         * @param val the {@code maxLearningRage} to set
         * @return a reference to this Builder
         */
        public Builder withMaxLearningRate(double val) {
            maxLearningRate = val;
            return this;
        }


        /**
         * Sets the {@code cycleLength} and returns a reference to this Builder enabling method chaining.
         * The {@code cycleLength} is number of cycles on a circle for the learning rate chage.
         * {@code cycleLength} default value is 2000.
         * @param val the {@code cycleLength} to set
         * @return a reference to this Builder
         */
        public Builder withCycleLength(int val) {
            cycleLength = val;
            return this;
        }

        /**
         * Sets the {@code patience} and returns a reference to this Builder enabling method chaining.
         * The {@code patience} is tolerable number of cycles without improvement in the error cost before forcing a learning rate change
         * and a parameter noise addition.
         * {@code patience} default value is 50.
         * @param val the {@code patience} to set
         * @return a reference to this Builder
         */
        public Builder withPatience(int val) {
            patience = val;
            return this;
        }

        /**
         * Sets the {@code momentum} and returns a reference to this Builder enabling method chaining.
         * The {@code momentum} is the momentum factor for the optimizer.
         * {@code momentum} default value is 50.
         * @param val the {@code momentum} to set
         * @return a reference to this Builder
         */
        public Builder withMomentum(double val) {
            momentum = val;
            return this;
        }

        /**
         * Returns a {@code ParameterOptimizer} built from the parameters previously set.
         *
         * @return a {@code ParameterOptimizer} built with parameters of this {@code ParameterOptimizer.Builder}
         */
        public ParameterOptimizer build() {
            return new ParameterOptimizer(this);
        }

    }
}

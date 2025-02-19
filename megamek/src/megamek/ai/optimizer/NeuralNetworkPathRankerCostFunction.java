/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.ai.optimizer;

import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitState;
import megamek.ai.utility.*;
import megamek.client.bot.caspar.ai.utility.tw.ClusteringService;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeuralNetworkPathRankerCostFunction implements CostFunction {

    private final NeuralNetworkScoreEvaluator scoreEvaluator;
    private final NeuralNetwork neuralNetwork;
    private final Board board;
    private final StrategicGoalsManager strategicGoalsManager;
    private final BehaviorSettings behaviorSettings;
    private final Game game;

    public NeuralNetworkPathRankerCostFunction(
        NeuralNetwork neuralNetwork,
        List<Consideration> considerations,
        StrategicGoalsManager strategicGoalsManager,
        BehaviorSettings behaviorSettings,
        Board board,
        Game game)
    {
        this.scoreEvaluator = new NeuralNetworkScoreEvaluator(new ConcreteDecisionScoreEvaluator(considerations), neuralNetwork);
        this.neuralNetwork = neuralNetwork;
        this.strategicGoalsManager = strategicGoalsManager;
        this.behaviorSettings = behaviorSettings;
        this.board = board;
        this.game = game;
        this.game.setBoard(board);
    }

    @Override
    public int numberOfParameters() {
        return 50;
    }

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Parameters parameters) {
        MovePath movePath = createMovePath(game, unitAction, currentUnitStates.get(unitAction.id()).entity());
        World world = new ConcreteWorld(
            unitAction,
            currentUnitStates,
            new QuickBoardRepresentation(board),
            new ClusteringService(15d, 6)
        );
        ConcreteDecisionContext context = new ConcreteDecisionContext(
            world,
            movePath,
            null,
            new HashMap<>(),
            1.0,
            strategicGoalsManager,
            new ConcreteDecisionContext.ConcreteThreatAssessment(world.getQuickBoardRepresentation(),
                world.getAlliedUnits(),
                world.getEnemyUnits(),
                world.getMyUnits(),
                world.getStructOfAllyUnitArrays(),
                world.getStructOfEnemyUnitArrays(),
                world.getStructOfOwnUnitsArrays()),
            new ConcreteDecisionContext.ConcreteUnitInformationProvider(
                currentUnitStates.get(unitAction.id()).entity(),
                unitAction,
                movePath,
                unitAction.facing()),
            behaviorSettings,
            new ConcreteDecisionContext.ConcreteDamageCalculator(0, 0, 0)
        );
        return scoreEvaluator.score(context, 1.0, IDebugReporter.noOp);
    }

    public static MovePath createMovePath(Game game, UnitAction unitAction, Entity entity) {
        entity.setPosition(unitAction.currentPosition());
        MovePath movePath = new MovePath(game, entity);
        for (var step : unitAction.steps()) {
            movePath.addStep(step);
        }
        return movePath;
    }

    @Override
    public double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Map<Integer, UnitState> nextUnitState, Parameters parameters) {
        return CostFunction.super.resolve(unitAction, currentUnitStates, nextUnitState, parameters);
    }

    public void train(double[][] X, double[][] Y, int epochs, double learningRate, int batchSize) {
        neuralNetwork.train(X, Y, epochs, learningRate, batchSize);
    }
}

/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.ai.dataset;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a training dataset.
 * This dataset is used to train a model optimize the action selection of a bot player.
 * @author Luana Coppio
 */
public class TrainingDataset {

    /**
     * An iterator for the training dataset.
     * it always returns the ActionAndState for the current round and then the next round for the same unit.
     */
    public static class TrainingDatasetIterator implements Iterator<ActionAndState> {
        private final List<ActionAndState> actionAndStates;
        private final List<ActionAndState> nextRoundActionAndState;
        private int index = 0;
        private boolean retrieveActionStateForCurrentRound = true;

        public TrainingDatasetIterator(TrainingDataset trainingDataset) {
            this.actionAndStates = trainingDataset.actionAndStates;
            this.nextRoundActionAndState = trainingDataset.nextRoundActionAndState;
        }

        @Override
        public boolean hasNext() {
            return index < actionAndStates.size() && index < nextRoundActionAndState.size();
        }

        @Override
        public ActionAndState next() {
            if (retrieveActionStateForCurrentRound) {
                retrieveActionStateForCurrentRound = false;
                return actionAndStates.get(index);
            }
            retrieveActionStateForCurrentRound = true;
            return nextRoundActionAndState.get(index++);
        }
    }

    private final List<ActionAndState> actionAndStates = new ArrayList<>();
    private final List<ActionAndState> nextRoundActionAndState = new ArrayList<>();

    /**
     * Create a new training dataset from a list of action and state pairs.
     * This will filter out any actions that do not have a corresponding state for the player.
     * This will discard all actions taken by a player different from player 0
     * @param actionAndStates The list of action and state pairs.
     */
    public TrainingDataset(List<ActionAndState> actionAndStates) {
        Set<Integer> entityIds = actionAndStates.stream().map(ActionAndState::unitAction).map(UnitAction::id).collect(Collectors.toSet());
        for (int entityId : entityIds) {
            List<ActionAndState> actionsForEntity = actionAndStates.stream()
                .filter(actionAndState -> actionAndState.unitAction().id() == entityId)
                .filter(actionAndState -> actionAndState.boardUnitState().stream().anyMatch(u -> u.id() == entityId))
                .toList();
            if (actionsForEntity.size() < 2) {
                continue;
            }
            for (int i = 0; i < actionsForEntity.size() - 1; i++) {
                this.actionAndStates.add(actionsForEntity.get(i));
                this.nextRoundActionAndState.add(actionsForEntity.get(i + 1));
            }
        }
    }

    public TrainingDataset(List<ActionAndState> actionAndStates, List<ActionAndState> nextRoundActionAndState) {
        this.actionAndStates.addAll(actionAndStates);
        this.nextRoundActionAndState.addAll(nextRoundActionAndState);
    }

    public int boardHeight() {
        int maxY = Integer.MIN_VALUE;
        for (var actionState : actionAndStates) {
            for (var state : actionState.boardUnitState()) {
                if (state.y() > 10_000 || state.y() < 0) {
                    continue;
                }
                maxY = Math.max(maxY, state.y());
            }
        }
        return maxY + 5;
    }

    public int boardWidth() {
        int maxX = Integer.MIN_VALUE;
        for (var actionState : actionAndStates) {
            for (var state : actionState.boardUnitState()) {
                if (state.x() > 10_000 || state.x() < 0) {
                    continue;
                }
                maxX = Math.max(maxX, state.x());
            }
        }
        return maxX + 5;
    }

    public int size() {
        return Math.min(actionAndStates.size(), nextRoundActionAndState.size());
    }

    public boolean isEmpty() {
        return actionAndStates.isEmpty() || nextRoundActionAndState.isEmpty();
    }

    /**
     * Get an iterator for the training dataset.
     * This iterator always returns the current state and then the next state in the dataset,
     * giving you access the state of the board before and after an action.
     * @return An iterator for the training dataset.
     */
    public Iterator<ActionAndState> iterator() {
        return new TrainingDatasetIterator(this);
    }

    /**
     * Sample a training dataset with a given batch size.
     * This will randomly sample the dataset, the same index will not be sampled twice. It is returned out of order.
     * @param batchSize The batch size.
     * @return A new training dataset with the sampled data.
     */
    public TrainingDataset sampleTrainingDataset(int batchSize) {
        List<ActionAndState> a = new ArrayList<>();
        List<ActionAndState> b = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < actionAndStates.size(); i++) {
            indexList.add(i);
        }
        assert batchSize <= indexList.size();
        Random rand = new Random();
        for (int i = 0; i < batchSize; i++) {
            int idx = indexList.remove(rand.nextInt(indexList.size()));
            a.add(actionAndStates.get(idx));
            b.add(nextRoundActionAndState.get(idx));
        }
        return new TrainingDataset(a, b);
    }

    /**
     * Copy the training dataset.
     * @return A new training dataset with the same data.
     */
    public TrainingDataset copy() {
        return new TrainingDataset(new ArrayList<>(actionAndStates), new ArrayList<>(nextRoundActionAndState));
    }
}

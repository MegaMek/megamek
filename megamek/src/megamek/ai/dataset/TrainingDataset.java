/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.dataset;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Represents a training dataset.
 * This dataset is used to train a model optimize the action selection of a bot player.</p>
 * <p>This has the particularity of having two lists internally, they represent the same
 * dataset but are offset by one round, this is a peculiarity of its internal representation so you should not worry about it
 * unless you plan to use its iterator.</p>
 *
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
     * This will discard all actions taken by non-human players
     * @param actionAndStates The list of action and state pairs.
     */
    public TrainingDataset(List<ActionAndState> actionAndStates) {
        Set<Integer> entityIds = actionAndStates.stream().map(ActionAndState::unitAction).map(UnitAction::id).collect(Collectors.toSet());
        for (int entityId : entityIds) {
            List<ActionAndState> actionsForEntity = actionAndStates.stream()
                .filter(actionAndState -> actionAndState.unitAction().isHuman())
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

    /**
     * <p>Create a new training dataset from two lists of action state pairs, the second list is the state of the game in the round after
     * that action was made.</p>
     * This expects that all action and state pairs are human actions.
     * @param actionAndStates The list of action and state pairs.
     * @param nextRoundActionAndState The list of action and state pairs for the next round.
     */
    public TrainingDataset(List<ActionAndState> actionAndStates, List<ActionAndState> nextRoundActionAndState) {
        this.actionAndStates.addAll(actionAndStates);
        this.nextRoundActionAndState.addAll(nextRoundActionAndState);
    }

    /**
     * Get the height of the board.
     * @return The height of the board.
     */
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

    /**
     * Get the width of the board.
     * @return The width of the board.
     */
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

    /**
     * <p>Get the size of the training dataset.</p>
     * Even though internally it has two lists, the one of current action and another with the state of the game in the following
     * round, this only considers the number of actions in one of the lists.
     * @return The size of the training dataset.
     */
    public int size() {
        return actionAndStates.size();
    }

    /**
     * Check if the training dataset is empty.
     * @return True if the training dataset is empty.
     */
    public boolean isEmpty() {
        return actionAndStates.isEmpty() || nextRoundActionAndState.isEmpty();
    }

    /**
     * Get an iterator for the training dataset.
     * This iterator always returns the current state and then the next state in the dataset,
     * giving you access the state of the board before and after an action.
     * <p>This iterator will return the action and state for the current round and then the next round for the same unit.</p>
     * <p>It always return in this two step way, current actionAndState followed by next round actionAndState, this is necessary
     * because during training, we need to know the direct result of the action taken to judge if it was a good decision or bad.</p>
     * @return An iterator for the training dataset.
     */
    public Iterator<ActionAndState> iterator() {
        return new TrainingDatasetIterator(this);
    }

    /**
     * Sample a training dataset with a given {@code batchSize}.
     * This will not modify the original dataset,
     * and it will randomly sample the dataset.
     * The same index will not be sampled twice.
     * It is returned out of order.
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

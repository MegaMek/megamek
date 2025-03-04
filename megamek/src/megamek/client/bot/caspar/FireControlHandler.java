package megamek.client.bot.caspar;

import megamek.client.bot.princess.FiringPlan;
import megamek.common.Entity;
import megamek.common.util.FiringSolution;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles firing decisions for CASPAR AI units.
 * Determines target selection, weapon allocation, and special attack usage.
 */
public class FireControlHandler {
    private final DifficultyManager difficultyManager;
    private final Map<Integer, Entity> targetPriorities = new HashMap<>();

    /**
     * Creates a fire control handler with the specified difficulty manager.
     *
     * @param difficultyManager The difficulty manager to use
     */
    public FireControlHandler(DifficultyManager difficultyManager) {
        this.difficultyManager = difficultyManager;
    }

    /**
     * Determines the firing solution for a unit.
     *
     * @param unit The unit to fire with
     * @param possibleTargets List of possible targets
     * @param gameState The current game state
     * @return The selected firing solution
     */
    public FiringPlan determineFiringPlan(Entity unit, List<Entity> possibleTargets, GameState gameState) {
        if (possibleTargets.isEmpty()) {
            return new FiringPlan(unit, List.of(), false);
        }

        // Update target priorities
        updateTargetPriorities(gameState);

        // Calculate target scores
        Map<Entity, Double> targetScores = possibleTargets.stream()
            .collect(Collectors.toMap(
                target -> target,
                target -> calculateTargetScore(unit, target, gameState)
            ));

        // Sort targets by score
        List<Entity> sortedTargets = possibleTargets.stream()
            .sorted(Comparator.comparingDouble(targetScores::get).reversed())
            .collect(Collectors.toList());

        // Determine whether to use special attacks
        boolean useSpecialAttack = shouldUseSpecialAttack(unit, sortedTargets.get(0), gameState);

        // Select weapons and allocate to targets
        List<WeaponAllocation> weaponAllocations = allocateWeapons(unit, sortedTargets, gameState);

        return new FiringSolution(unit, weaponAllocations, useSpecialAttack);
    }

    /**
     * Updates target priorities based on game state.
     *
     * @param gameState The current game state
     */
    private void updateTargetPriorities(GameState gameState) {
        targetPriorities.clear();

        // Prioritize targets based on threat level, role, and position
        List<Entity> enemies = gameState.getEnemyUnits();

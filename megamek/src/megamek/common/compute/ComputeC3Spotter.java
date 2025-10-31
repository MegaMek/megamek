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
package megamek.common.compute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.common.ECMInfo;
import megamek.common.LosEffects;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * Contains utility methods for finding a valid C3 spotter for an attack, i.e. a unit with better range than the actual
 * attacker.
 */
public class ComputeC3Spotter {

    /**
     * Returns the closest unit to the target that has a working C3-type connection to the attacker, i.e., the best unit
     * in a C3-type network of the attacker for range calculation. The calculation includes C3, C3i, Naval C3 and Nova.
     * If the attacker has no working C3 system or connection or no other unit is placed better or in any other case
     * where no better C3 spotter can be found, the attacker itself is returned.
     *
     * @param attacker The firing unit
     * @param target   The target of the potential attack
     * @param game     The game
     *
     * @return A C3-type spotter or the attacker itself if no spotters are found
     */
    static Entity findC3Spotter(Game game, Entity attacker, Targetable target) {
        if (!attackerCanUseC3(attacker, game)) {
            return attacker;
        }

        List<SpotterInfo> spotters = new ArrayList<>();

        for (Entity other : game.getEntitiesVector()) {
            if (isValidC3Spotter(other, attacker, game)) {
                int spotterRange = Compute.effectiveDistance(game, other, target, false);
                spotters.add(new SpotterInfo(other, spotterRange));
            }
        }

        if (!spotters.isEmpty()) {
            // ensure network connectivity
            List<ECMInfo> allECMInfo = ComputeECM.computeAllEntitiesECMInfo(game.getEntitiesVector());
            spotters.sort(Comparator.comparingInt(SpotterInfo::rangeToTarget));

            // PLAYTEST3 C3 spotters can only work if they have LOS to the target.
            LosEffects c3LOS;

            int position = 0;
            for (SpotterInfo spotterInfo : spotters) {
                Entity spotter = spotterInfo.spotter;
                for (int count = position++; count < spotters.size(); count++) {
                    if (canCompleteNodePath(spotter, attacker, spotters, count, allECMInfo)) {

                        // PLAYTEST3 check the LOS from the spotter to the target
                        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
                            c3LOS = LosEffects.calculateLOS(game, spotter, target);
                            if (!c3LOS.isBlocked()) {
                                return spotter;
                            }
                        } else {
                            return spotter;
                        }
                    }
                }
            }
        }

        return attacker;
    }

    // PLAYTEST3 return spotter even with ECM
    static Entity playtestFindC3Spotter(Game game, Entity attacker, Targetable target) {
        if (!attackerCanUseC3(attacker, game)) {
            return attacker;
        }

        List<SpotterInfo> spotters = new ArrayList<>();

        for (Entity other : game.getEntitiesVector()) {
            if (isValidC3Spotter(other, attacker, game)) {
                int spotterRange = Compute.effectiveDistance(game, other, target, false);
                spotters.add(new SpotterInfo(other, spotterRange));
            }
        }

        if (!spotters.isEmpty()) {
            // ensure network connectivity
            List<ECMInfo> allECMInfo = ComputeECM.computeAllEntitiesECMInfo(game.getEntitiesVector());
            spotters.sort(Comparator.comparingInt(SpotterInfo::rangeToTarget));

            LosEffects c3LOS;

            int position = 0;
            for (SpotterInfo spotterInfo : spotters) {
                Entity spotter = spotterInfo.spotter;
                c3LOS = LosEffects.calculateLOS(game, spotter, target);
                if (!c3LOS.isBlocked()) {
                    spotter.setC3ecmAffected(!canCompleteNodePath(spotter, attacker, spotters, position, allECMInfo));
                    return spotter;
                }
                position++;
            }
        }

        return attacker;
    }

    /**
     * Returns false if the attacker does not have a functional C3 system or is prevented from using it at this time,
     * true otherwise. When this returns true, a spotter should be searched, otherwise this can be omitted.
     *
     * @param attacker The attacking unit
     *
     * @return False if the attacker cannot make use of C3 at this time
     */
    private static boolean attackerCanUseC3(Entity attacker, Game game) {
        if (attacker.isOffBoard() || attacker.isShutDown()
              || !attacker.hasC3() && !attacker.hasC3i() && !attacker.hasActiveNovaCEWS() && !attacker.hasNavalC3()) {
            return false;
        }
        
        // PLAYTEST3 Stealth kills C3. Now that ECM halves bonuses, we need to exit early.
        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            if (attacker.isStealthActive()) {
                return false;
            }
        }

        if (attacker.isLargeCraft() && !attacker.isSpaceborne()) {
            // Assuming that Naval C3 only works in space as it is "optimized for the speeds and distances of space
            // weapons"; TO:AUE p.144
            return false;
        }

        // Assuming that C3, C3i systems only work on ground maps, TW p.131. Nova can be mounted on some aeros
        // but only works when "interacting with ground units", IO:AE p.60
        return attacker.isLargeCraft() || game.isOnGroundMap(attacker);
    }

    /**
     * Returns true if the given other unit is a valid C3 spotter for the attacker, i.e. if it is connected to the
     * attacker's C3, deployed and on the same board and not otherwise prevented from acting as a C3 spotter.
     *
     * @param other    The unit to test (all units are allowed to be passed in)
     * @param attacker The attacking unit
     *
     * @return True if the other is a valid C3 spotter and may be used for range calculation
     */
    private static boolean isValidC3Spotter(Entity other, Entity attacker, Game game) {
        return !attacker.equals(other)
              && other.isDeployed()
              && !other.isStealthActive()
              && !other.isShutDown()
              && !other.isOffBoard()
              && !other.isTransported()
              && game.hasBoardLocationOf(other)
              && attacker.onSameC3NetworkAs(other, true)
              && game.onTheSameBoard(attacker, other);
    }

    /**
     * Looks through the network list to ensure that there's no ECM blocking the path from spotter to attacker.
     *
     * @param start         The unit to start the network path on
     * @param end           The unit to end the network path on
     * @param network       The network of possibly connected units
     * @param startPosition The spotter's index in the network list
     *
     * @return True when the given Entity is connected to the network
     */
    private static boolean canCompleteNodePath(Entity start, Entity end, List<SpotterInfo> network, int startPosition,
          List<ECMInfo> allECMInfo) {

        Entity spotter = network.get(startPosition).spotter;

        // ECMInfo for line between spotter's position and start's position
        ECMInfo spotterStartECM = ComputeECM.getECMEffects(spotter, start.getPosition(), spotter.getPosition(),
              true, allECMInfo);

        // Check for ECM between spotter and start
        boolean isC3BDefeated = start.hasBoostedC3() && (spotterStartECM != null) && spotterStartECM.isAngelECM();
        boolean isNovaDefeated = start.hasNovaCEWS() && (spotterStartECM != null) && spotterStartECM.isNovaECM();
        boolean isC3Defeated = !(start.hasBoostedC3() || start.hasNovaCEWS())
              && (spotterStartECM != null)
              && spotterStartECM.isECM();
        if (isC3BDefeated || isNovaDefeated || isC3Defeated) {
            return false;
        }

        // ECMInfo for line between spotter's position and end's position
        ECMInfo spotterEndECM = ComputeECM.getECMEffects(spotter, spotter.getPosition(), end.getPosition(),
              true, allECMInfo);
        isC3BDefeated = start.hasBoostedC3() && (spotterEndECM != null) && spotterEndECM.isAngelECM();
        isNovaDefeated = start.hasNovaCEWS() && (spotterEndECM != null) && spotterEndECM.isNovaECM();
        isC3Defeated = !(start.hasBoostedC3() || start.hasNovaCEWS())
              && (spotterEndECM != null)
              && spotterEndECM.isECM();
        // If there's no ECM between spotter and end, we're done
        if (!(isC3BDefeated || isNovaDefeated || isC3Defeated)) {
            return true;
        }

        for (++startPosition; startPosition < network.size(); startPosition++) {
            if (canCompleteNodePath(spotter, end, network, startPosition, allECMInfo)) {
                return true;
            }
        }

        return false;
    }

    private record SpotterInfo(Entity spotter, int rangeToTarget) {}

    private ComputeC3Spotter() {}
}

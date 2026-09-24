/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.List;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.common.ECMInfo;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.INarcPod;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

/**
 * Game rules for the Virtual Reality Piloting Pod (VRPP) cockpit, Interstellar Operations: Alternate Eras p.63.
 *
 * <p>Undisturbed, the pod gives its MekWarrior {@value #GUNNERY_MODIFIER} to weapon attacks and
 * {@value #PILOTING_MODIFIER} to Piloting Skill Rolls. Any hostile electronic disruption (enemy ECM over the unit's
 * hex, an iNarc ECM pod, EMI planetary conditions, the EMI left by a nuclear strike, or an EMP mine, taser or TSEMP
 * hit) blinds the unit as though its sensors were destroyed: no weapon attacks, and {@value #BLINDED_PILOTING_MODIFIER}
 * to Piloting Skill Rolls. If the ECCM rules are in play and a friendly ECCM field covers the unit's hex without a
 * hostile ECM field still winning there, the blindness is replaced by {@value #DEGRADED_MODIFIER} to both Gunnery and
 * Piloting.</p>
 *
 * <p>Both the client and the server compute the state from the same shared game state, so the firing display and
 * the server always agree on whether the unit may fire.</p>
 */
public final class VirtualRealityPilotingPod {

    private static final MMLogger LOGGER = MMLogger.create(VirtualRealityPilotingPod.class);

    /** Gunnery modifier for an undisturbed pod. */
    public static final int GUNNERY_MODIFIER = -1;
    /** Piloting Skill Roll modifier for an undisturbed pod. */
    public static final int PILOTING_MODIFIER = -2;
    /** Gunnery and Piloting modifier while friendly ECCM holds off hostile interference. */
    public static final int DEGRADED_MODIFIER = 2;
    /** Piloting Skill Roll modifier while blinded by hostile interference. */
    public static final int BLINDED_PILOTING_MODIFIER = 3;

    /**
     * How much the pod's interface is currently disrupted.
     */
    public enum InterferenceState {
        /** No hostile interference: the pod's own bonuses apply. */
        NORMAL,
        /** Hostile interference held off by friendly ECCM: {@value #DEGRADED_MODIFIER} to Gunnery and Piloting. */
        DEGRADED,
        /** Hostile interference: the unit is blind and cannot fire weapons. */
        BLINDED
    }

    /**
     * The pod's current interference state and what caused it.
     *
     * @param state  the interference state
     * @param source the player-facing name of the disruption source, or {@code null} when the state is
     *               {@link InterferenceState#NORMAL}
     */
    public record Interference(InterferenceState state, @Nullable String source) {

        /** The state of an undisturbed pod. */
        public static final Interference NONE = new Interference(InterferenceState.NORMAL, null);

        /**
         * @return {@code true} if the unit is blind and cannot fire weapons
         */
        public boolean isBlinded() {
            return state == InterferenceState.BLINDED;
        }
    }

    private VirtualRealityPilotingPod() {}

    /**
     * Works out how much hostile interference the pod is suffering right now.
     *
     * @param mek the Mek carrying the pod
     *
     * @return the current interference state; {@link Interference#NONE} when the Mek has no pod, is not in a game, or
     *       is not on the board
     */
    public static Interference getInterference(Mek mek) {
        Game game = mek.getGame();
        Coords position = mek.getPosition();
        if (!mek.hasVirtualRealityPilotingPod() || (game == null) || (position == null)) {
            return Interference.NONE;
        }

        List<ECMInfo> allFields = ComputeECM.collectAllEcmInfo(game.getEntitiesVector());
        String disruptionSource = findDisruptionSource(mek, position, allFields);
        if (disruptionSource == null) {
            return Interference.NONE;
        }

        boolean eccmRulesInPlay = game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_ECCM);
        boolean friendlyEccmCoversHex = isFriendlyEccmInHex(mek, position, allFields);
        boolean hostileEcmStillWins = ComputeECM.isAffectedByECM(mek, position, position);
        if (eccmRulesInPlay && friendlyEccmCoversHex && !hostileEcmStillWins) {
            LOGGER.trace("[VRPP] {}: degraded - {} held off by friendly ECCM", mek.getShortName(), disruptionSource);
            return new Interference(InterferenceState.DEGRADED, disruptionSource);
        }
        LOGGER.trace("[VRPP] {}: blinded by {} (ECCM rules {}, friendly ECCM in hex {}, hostile ECM wins {})",
              mek.getShortName(), disruptionSource, eccmRulesInPlay, friendlyEccmCoversHex, hostileEcmStillWins);
        return new Interference(InterferenceState.BLINDED, disruptionSource);
    }

    /**
     * Adds the pod's Gunnery modifier for a weapon attack. A blinded pod cannot fire at all; that case is refused in
     * {@code ComputeToHitIsImpossible} and adds nothing here.
     *
     * @param attacker the attacking unit
     * @param toHit    the to-hit data to add the modifier to
     */
    public static void addGunneryModifier(Entity attacker, ToHitData toHit) {
        if (!(attacker instanceof Mek attackingMek) || !attackingMek.hasVirtualRealityPilotingPod()) {
            return;
        }
        Interference interference = getInterference(attackingMek);
        switch (interference.state()) {
            case NORMAL -> toHit.addModifier(GUNNERY_MODIFIER, Messages.getString("WeaponAttackAction.Vrpp"));
            case DEGRADED -> toHit.addModifier(DEGRADED_MODIFIER,
                  Messages.getString("WeaponAttackAction.VrppDegraded", interference.source()));
            case BLINDED -> {
                // Refused before the modifiers are compiled; nothing to add.
            }
        }
    }

    /**
     * @param attacker the attacking unit
     *
     * @return the reason the unit cannot fire because its pod is blinded, or {@code null} if it may fire
     */
    public static @Nullable String getBlindedReason(Entity attacker) {
        if (!(attacker instanceof Mek attackingMek) || !attackingMek.hasVirtualRealityPilotingPod()) {
            return null;
        }
        Interference interference = getInterference(attackingMek);
        if (!interference.isBlinded()) {
            return null;
        }
        return Messages.getString("WeaponAttackAction.VrppBlinded", interference.source());
    }

    /**
     * Adds the pod's Piloting Skill Roll modifier.
     *
     * @param mek  the Mek carrying the pod
     * @param roll the roll to add the modifier to
     */
    public static void addPilotingModifier(Mek mek, PilotingRollData roll) {
        if (!mek.hasVirtualRealityPilotingPod()) {
            return;
        }
        Interference interference = getInterference(mek);
        switch (interference.state()) {
            case NORMAL -> roll.addModifier(PILOTING_MODIFIER, Messages.getString("PilotingRoll.Vrpp"));
            case DEGRADED -> roll.addModifier(DEGRADED_MODIFIER,
                  Messages.getString("PilotingRoll.VrppDegraded", interference.source()));
            case BLINDED -> roll.addModifier(BLINDED_PILOTING_MODIFIER,
                  Messages.getString("PilotingRoll.VrppBlinded", interference.source()));
        }
    }

    /**
     * Finds the first hostile disruption acting on the unit. Each source is checked on its own so the reason reported
     * to the player names the actual cause.
     *
     * @return the player-facing name of the disruption, or {@code null} when nothing disrupts the pod
     */
    private static @Nullable String findDisruptionSource(Mek mek, Coords position, List<ECMInfo> allFields) {
        if (mek.isSufferingEMI()) {
            return Messages.getString("VirtualRealityPilotingPod.source.nuclearEmi");
        }
        if (mek.getGame().getPlanetaryConditions().getEMI().isEMI()) {
            return Messages.getString("VirtualRealityPilotingPod.source.planetaryEmi");
        }
        if ((mek.getEMPInterferenceRounds() > 0) || (mek.getEMPShutdownRounds() > 0)) {
            return Messages.getString("VirtualRealityPilotingPod.source.empMine");
        }
        if ((mek.getTaserInterferenceRounds() > 0) || (mek.getTaserShutdownRounds() > 0)) {
            return Messages.getString("VirtualRealityPilotingPod.source.taser");
        }
        if (mek.getTsempEffect() != MMConstants.TSEMP_EFFECT_NONE) {
            return Messages.getString("VirtualRealityPilotingPod.source.tsemp");
        }
        if (mek.isINarcedWith(INarcPod.ECM)) {
            return Messages.getString("VirtualRealityPilotingPod.source.iNarcEcm");
        }
        if (isHostileEcmInHex(mek, position, allFields)) {
            return Messages.getString("VirtualRealityPilotingPod.source.hostileEcm");
        }
        return null;
    }

    /**
     * @return {@code true} if any hostile ECM field reaches the unit's hex, before any ECCM is taken into account
     */
    private static boolean isHostileEcmInHex(Mek mek, Coords position, List<ECMInfo> allFields) {
        for (ECMInfo field : allFields) {
            boolean isHostileEcm = field.isECM() && field.isOpposed(mek.getOwner());
            if (isHostileEcm && (position.distance(field.getPos()) <= field.getRange())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if a friendly ECCM field reaches the unit's hex
     */
    private static boolean isFriendlyEccmInHex(Mek mek, Coords position, List<ECMInfo> allFields) {
        for (ECMInfo field : allFields) {
            boolean isFriendlyEccm = field.isECCM() && (field.getOwner() != null) && !field.isOpposed(mek.getOwner());
            if (isFriendlyEccm && (position.distance(field.getPos()) <= field.getRange())) {
                return true;
            }
        }
        return false;
    }
}

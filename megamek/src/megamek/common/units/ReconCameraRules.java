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
package megamek.common.units;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.compute.ComputeEnvironmentalToHitMods;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;

/**
 * Stateless rules for the Recon Camera used by a ground unit (TO:AUE p.150, as corrected by the errata of 2013): once
 * per turn the unit may point its camera at one hostile unit and "hit" it using the same rules and ranges as TAG. On a
 * hit the unit spots that target for LRM indirect fire (TW p.111) for the rest of the turn, and neither the indirect
 * shot nor the unit's own attacks take the usual +1 spotting penalty. A camera spot is never a TAG designation, so
 * TAG-guided weapons (semi-guided LRMs, laser-guided bombs, Arrow IV homing) get nothing from it.
 *
 * <p>The client (the Camera Spot button and its tooltip) and the server (accepting and rolling the spot) share these
 * checks, so the button never offers a spot the server would refuse. Airborne aerospace units follow different camera
 * rules and are not handled here.</p>
 */
public final class ReconCameraRules {

    private static final MMLogger LOGGER = MMLogger.create(ReconCameraRules.class);

    /** The TAG a unit's camera takes its range brackets from. */
    private static final String STANDARD_TAG = "ISTAG";

    /** ProtoMeks carry Light TAG, so their camera uses its shorter brackets. */
    private static final String LIGHT_TAG = "CLLightTAG";

    private ReconCameraRules() {}

    /**
     * Checks whether a unit carries a Recon Camera that can be used this turn under the ground-unit rules: a working
     * camera, an active crew, a place on the board, not an airborne aerospace unit, and no camera spot yet this turn.
     *
     * @param camera the unit to check
     *
     * @return {@code true} if the unit may try a camera spot this turn
     */
    public static boolean canUseCamera(Entity camera) {
        return usabilityRefusal(camera) == null;
    }

    /**
     * Checks whether a unit has at least one hostile unit it could spot with its camera this turn. The Off-Board phase
     * gives a camera unit a turn only when it has something to spot.
     *
     * @param game   the game
     * @param camera the unit with the camera
     *
     * @return {@code true} if a camera spot is possible against some unit
     */
    public static boolean hasAnyTarget(Game game, Entity camera) {
        Refusal usability = usabilityRefusal(camera);
        if (usability != null) {
            return false;
        }
        int candidates = 0;
        for (Entity other : game.getEntitiesVector()) {
            if (targetRefusal(game, camera, other) == null) {
                candidates++;
            }
        }
        LOGGER.debug("[ReconCamera] {}: {} unit(s) it could spot this turn", camera.getShortName(), candidates);
        return candidates > 0;
    }

    /**
     * Checks whether a unit may try a camera spot against the given target this turn. A refusal is logged with its
     * reason.
     *
     * @param game   the game
     * @param camera the unit with the camera
     * @param target the unit to spot, may be {@code null}
     *
     * @return the reason the spot is refused, ready to show to the player, or {@code null} when it is allowed
     */
    public static @Nullable String spotRefusal(Game game, Entity camera, @Nullable Targetable target) {
        Refusal refusal = usabilityRefusal(camera);
        if (refusal == null) {
            refusal = targetRefusal(game, camera, target);
        }
        if (refusal == null) {
            return null;
        }
        LOGGER.debug("[ReconCamera] {}: no camera spot on {} - {}", camera.getShortName(),
              (target == null) ? "no target" : target.getDisplayName(), refusal.reason());
        return Messages.getString(refusal.messageKey());
    }

    /**
     * Works out the roll a camera spot needs: the unit's gunnery skill with the modifiers a TAG shot from the same unit
     * at the same target would take - range bracket (with TAG or, for a ProtoMek, Light TAG ranges), both units'
     * movement, intervening terrain and cover, the target's hex, the unit's heat, and light and weather.
     *
     * @param game   the game
     * @param camera the unit with the camera
     * @param target the unit to spot
     *
     * @return the roll needed; impossible when the spot is not allowed at all
     */
    public static ToHitData spotToHit(Game game, Entity camera, Entity target) {
        String refusal = spotRefusal(game, camera, target);
        if (refusal != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, refusal);
        }
        ToHitData toHit = new ToHitData(camera.getCrew().getGunnery(),
              Messages.getString("WeaponAttackAction.GunSkill"));

        int distance = Compute.effectiveDistance(game, camera, target);
        switch (rangeBracket(game, camera, distance)) {
            case RangeType.RANGE_SHORT -> toHit.addModifier(camera.getShortRangeModifier(),
                  Messages.getString("ReconCamera.modifier.shortRange"));
            case RangeType.RANGE_MEDIUM -> toHit.addModifier(camera.getMediumRangeModifier(),
                  Messages.getString("ReconCamera.modifier.mediumRange"));
            case RangeType.RANGE_LONG -> toHit.addModifier(camera.getLongRangeModifier(),
                  Messages.getString("ReconCamera.modifier.longRange"));
            case RangeType.RANGE_EXTREME -> toHit.addModifier(camera.getExtremeRangeModifier(),
                  Messages.getString("ReconCamera.modifier.extremeRange"));
            default -> {
                // spotRefusal has already ruled out anything beyond range
            }
        }

        toHit.append(Compute.getAttackerMovementModifier(game, camera.getId()));
        toHit.append(Compute.getTargetMovementModifier(game, target.getId()));
        toHit.append(LosEffects.calculateLOS(game, camera, target).losModifiers(game));
        toHit.append(Compute.getTargetTerrainModifier(game, target));
        ToHitData immobileModifier = Compute.getImmobileMod(target);
        if (immobileModifier != null) {
            toHit.append(immobileModifier);
        }
        int heatModifier = camera.getHeatFiringModifier();
        if (heatModifier != 0) {
            toHit.addModifier(heatModifier, Messages.getString("WeaponAttackAction.Heat"));
        }
        ComputeEnvironmentalToHitMods.compileEnvironmentalToHitMods(game, camera, target, tagFor(camera), null,
              toHit, false);
        return toHit;
    }

    /**
     * Lists the players who see a unit because a camera on their side spotted it this turn. Under double-blind the
     * camera's whole side sees what the camera spotted, or the units it spots for could not target it.
     *
     * @param game   the game
     * @param target the unit that may have been spotted
     *
     * @return the players on the side of every camera that spotted the unit this turn; empty when none did
     */
    public static List<Player> playersSeeingThroughCameras(Game game, Entity target) {
        List<Player> viewers = new ArrayList<>();
        for (Entity camera : game.getEntitiesVector()) {
            if (camera.getReconCameraSpotTargetId() != target.getId()) {
                continue;
            }
            Player cameraOwner = camera.getOwner();
            if (cameraOwner == null) {
                continue;
            }
            for (Player player : game.getPlayersList()) {
                boolean isCameraSide = (player.getId() == cameraOwner.getId())
                      || ((cameraOwner.getTeam() != Player.TEAM_NONE) && (player.getTeam() == cameraOwner.getTeam()));
                if (isCameraSide && !viewers.contains(player)) {
                    viewers.add(player);
                }
            }
        }
        return viewers;
    }

    /**
     * Checks whether a unit spotted the given target with its camera this turn.
     *
     * @param spotter the possible spotter
     * @param target  the target
     *
     * @return {@code true} if the spotter's camera spot on the target succeeded this turn
     */
    public static boolean isCameraSpotting(Entity spotter, Targetable target) {
        return spotter.getReconCameraSpotTargetId() == target.getId();
    }

    private static @Nullable Refusal usabilityRefusal(Entity camera) {
        if (!camera.hasWorkingMisc(MiscType.F_RECON_CAMERA)) {
            return new Refusal("ReconCamera.refusal.noCamera", "no working recon camera");
        }
        if (camera.isAirborne() && camera.isAero()) {
            return new Refusal("ReconCamera.refusal.airborne", "airborne aerospace units use the aerospace camera rules");
        }
        if ((camera.getCrew() == null) || !camera.getCrew().isActive()) {
            return new Refusal("ReconCamera.refusal.crew", "the crew cannot act");
        }
        if ((camera.getPosition() == null) || camera.isOffBoard()) {
            return new Refusal("ReconCamera.refusal.notOnBoard", "the unit is not on the board");
        }
        if (camera.hasReconCameraSpotThisTurn()) {
            return new Refusal("ReconCamera.refusal.alreadyUsed", "the camera has already spotted this turn");
        }
        return null;
    }

    private static @Nullable Refusal targetRefusal(Game game, Entity camera, @Nullable Targetable target) {
        if (!(target instanceof Entity targetUnit)) {
            return new Refusal("ReconCamera.refusal.notUnit", "the target is not a unit");
        }
        if (!camera.isEnemyOf(targetUnit)) {
            return new Refusal("ReconCamera.refusal.notHostile", "the target is not hostile");
        }
        if ((targetUnit.getPosition() == null) || targetUnit.isOffBoard() || targetUnit.isHidden()) {
            return new Refusal("ReconCamera.refusal.targetNotOnBoard", "the target is not on the board");
        }
        if (targetUnit.getBoardId() != camera.getBoardId()) {
            return new Refusal("ReconCamera.refusal.otherBoard", "the target is on another board");
        }
        int distance = Compute.effectiveDistance(game, camera, targetUnit);
        if (rangeBracket(game, camera, distance) == RangeType.RANGE_OUT) {
            return new Refusal("ReconCamera.refusal.outOfRange", "beyond TAG range at " + distance + " hexes");
        }
        LosEffects lineOfSight = LosEffects.calculateLOS(game, camera, targetUnit);
        if (!lineOfSight.canSee()) {
            return new Refusal("ReconCamera.refusal.noLineOfSight", "no line of sight");
        }
        boolean isDoubleBlind = game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
        if (isDoubleBlind && !Compute.canSee(game, camera, targetUnit, true, lineOfSight, null)) {
            return new Refusal("ReconCamera.refusal.notSeen", "the camera unit cannot see the target");
        }
        return null;
    }

    private static int rangeBracket(Game game, Entity camera, int distance) {
        WeaponType tag = tagFor(camera);
        int[] ranges = { tag.getMinimumRange(), tag.getShortRange(), tag.getMediumRange(), tag.getLongRange(),
                         tag.getExtremeRange() };
        boolean useExtremeRange = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE);
        return RangeType.rangeBracket(distance, ranges, useExtremeRange, false);
    }

    private static WeaponType tagFor(Entity camera) {
        return (WeaponType) EquipmentType.get((camera instanceof ProtoMek) ? LIGHT_TAG : STANDARD_TAG);
    }

    /**
     * Why a camera spot is refused.
     *
     * @param messageKey the player-facing reason's key in the client messages
     * @param reason     the reason as written to the log
     */
    private record Refusal(String messageKey, String reason) {}
}

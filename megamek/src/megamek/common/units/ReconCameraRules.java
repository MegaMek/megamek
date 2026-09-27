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
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.compute.ComputeEnvironmentalToHitMods;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;

/**
 * Stateless rules for the Recon Camera (TO:AUE p.150, as corrected by the errata of 2013).
 *
 * <p>A ground unit may, once per turn, point its camera at one hostile unit and "hit" it using the same rules and
 * ranges as TAG. An airborne aerospace unit (a LAM only in fighter mode; a VTOL is a ground unit here) between
 * Altitudes 5 and 10 may instead spot a ground unit below it with a roll of Gunnery +2 or, with the camera set to
 * Reveal, try to reveal the hostile hidden units it flies over; either way it makes no other attack that turn. A
 * camera spot is never a TAG designation, so TAG-guided weapons get nothing from it.</p>
 *
 * <p>The camera is the exception to the 2024 errata that limits an airborne unit's own eyes to Altitude 8: its spot
 * and reveal work up to Altitude 10. On a ground map the errata's range cut still applies: the target must be within
 * the visible range, less twice the altitude, of the flight path.</p>
 *
 * <p>The client (the Camera Spot button and its tooltip) and the server (accepting and rolling the spot) share these
 * checks, so the button never offers a spot the server would refuse.</p>
 */
public final class ReconCameraRules {

    private static final MMLogger LOGGER = MMLogger.create(ReconCameraRules.class);

    /** The lowest altitude an aerospace unit may use its camera from. */
    public static final int MINIMUM_CAMERA_ALTITUDE = 5;

    /** The highest altitude an aerospace unit may use its camera from. */
    public static final int MAXIMUM_CAMERA_ALTITUDE = 10;

    /** The camera mode that spots a target, the default. */
    public static final String MODE_SPOT = "Spot";

    /** The camera mode that tries to reveal the hidden units an aerospace unit flies over. */
    public static final String MODE_REVEAL = "Reveal";

    /** The base number a hidden unit's owner rolls against when a camera flies over it. */
    public static final int REVEAL_BASE_TARGET_NUMBER = 9;

    /** An aerospace unit's camera spot needs its pilot's gunnery skill plus this. */
    private static final int AEROSPACE_SPOT_MODIFIER = 2;

    /** The TAG a unit's camera takes its range brackets from. */
    private static final String STANDARD_TAG = "ISTAG";

    /** ProtoMeks carry Light TAG, so their camera uses its shorter brackets. */
    private static final String LIGHT_TAG = "CLLightTAG";

    private ReconCameraRules() {}

    /**
     * Checks whether a unit follows the aerospace camera rules: an airborne aerospace unit, which includes a LAM in
     * fighter mode but not a VTOL.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if the unit uses the aerospace camera rules
     */
    public static boolean isAerospaceCamera(Entity unit) {
        return unit.isAero() && unit.isAirborne();
    }

    /**
     * Checks whether a unit carries a Recon Camera that can spot this turn: a working camera set to spot, an active
     * crew, a place on the board, no camera use yet this turn and, for an aerospace unit, an altitude between 5 and
     * 10 over the ground.
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
        if (!camera.hasWorkingMisc(MiscType.F_RECON_CAMERA)) {
            return false;
        }
        int candidates = spotTargets(game, camera).size();
        LOGGER.debug("[ReconCamera] {}: {} unit(s) it could spot this turn", camera.getShortName(), candidates);
        return candidates > 0;
    }

    /**
     * Lists the units a camera could spot this turn, without logging a refusal for each unit it cannot. Used by the bot
     * to choose a target.
     *
     * @param game   the game
     * @param camera the unit with the camera
     *
     * @return the units the camera may try to spot; empty when it cannot spot at all
     */
    public static List<Entity> spotTargets(Game game, Entity camera) {
        List<Entity> targets = new ArrayList<>();
        if (usabilityRefusal(camera) != null) {
            return targets;
        }
        for (Entity other : game.getEntitiesVector()) {
            if (targetRefusal(game, camera, other) == null) {
                targets.add(other);
            }
        }
        return targets;
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
     * Works out the roll a camera spot needs. A ground unit rolls what a TAG shot from the same unit at the same
     * target would need: gunnery, the range bracket (with TAG or, for a ProtoMek, Light TAG ranges), both units'
     * movement, intervening terrain and cover, the target's hex, heat, and light and weather. An aerospace unit rolls
     * its gunnery +2 with light and weather and no range modifier.
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
        if (isAerospaceCamera(camera)) {
            toHit.addModifier(AEROSPACE_SPOT_MODIFIER, Messages.getString("ReconCamera.modifier.fromAltitude"));
        } else {
            addGroundSpotModifiers(game, camera, target, toHit);
        }
        ComputeEnvironmentalToHitMods.compileEnvironmentalToHitMods(game, camera, target, tagFor(camera), null,
              toHit, false);
        return toHit;
    }

    /**
     * Checks whether the camera keeps a unit from making any other attack this turn: an aerospace unit that used its
     * camera, to spot or to reveal hidden units, makes no other attack (TO:AUE p.150).
     *
     * @param unit the unit about to attack
     *
     * @return {@code true} if the camera rules forbid the unit any other attack this turn
     */
    public static boolean forbidsOtherAttacks(Entity unit) {
        return isAerospaceCamera(unit) && unit.hasReconCameraSpotThisTurn();
    }

    /**
     * Checks whether a unit's camera is set to reveal hidden units rather than to spot.
     *
     * @param unit the unit to check
     *
     * @return {@code true} if a working camera on the unit is in Reveal mode
     */
    public static boolean isInRevealMode(Entity unit) {
        for (MiscMounted camera : unit.getMisc()) {
            boolean isWorkingCamera = camera.getType().hasFlag(MiscType.F_RECON_CAMERA) && !camera.isInoperable();
            if (isWorkingCamera && camera.curMode().equals(MODE_REVEAL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether an aerospace unit's flight this turn reveals hidden units: a working camera set to Reveal, an
     * active crew, flying between Altitudes 5 and 10 over the ground, and no camera use yet this turn.
     *
     * @param camera the unit with the camera
     *
     * @return {@code true} if the hidden units under the unit's flight path get a reveal roll
     */
    public static boolean canRevealHiddenUnits(Entity camera) {
        if (!isAerospaceCamera(camera) || !isInRevealMode(camera)) {
            return false;
        }
        Refusal refusal = commonUsabilityRefusal(camera);
        if (refusal != null) {
            LOGGER.debug("[ReconCamera] {}: no hidden unit reveal - {}", camera.getShortName(), refusal.reason());
            return false;
        }
        return true;
    }

    /**
     * Lists the hostile hidden units under an aerospace camera's flight this turn: on a low-altitude map, those on each
     * ground map under a hex it flew over; on a ground map, those within the visible range, less twice its altitude,
     * of its flight path.
     *
     * @param game   the game
     * @param camera the aerospace unit with the camera
     *
     * @return the hidden units that get a reveal roll; empty when there are none
     */
    public static List<Entity> hiddenUnitsBelowFlightPath(Game game, Entity camera) {
        List<Entity> hiddenUnits = new ArrayList<>();
        for (Entity other : game.getEntitiesVector()) {
            boolean isHostileHiddenUnit = other.isHidden() && other.isEnemyOf(camera);
            boolean isOnTheMap = other.getPosition() != null;
            if (isHostileHiddenUnit && isOnTheMap && isBelowFlightPath(game, camera, other)) {
                hiddenUnits.add(other);
            }
        }
        return hiddenUnits;
    }

    /**
     * Works out the roll a hidden unit's owner makes when a camera flies over it: 9, plus the terrain modifiers of its
     * hex (woods, jungle, smoke and the other target terrain modifiers), plus 1 for each level of water depth past 1
     * when the unit is submerged. Rolling this or more reveals it.
     *
     * @param game   the game
     * @param hidden the hidden unit
     *
     * @return the roll that reveals the unit
     */
    public static ToHitData revealTargetNumber(Game game, Entity hidden) {
        ToHitData targetNumber = new ToHitData(REVEAL_BASE_TARGET_NUMBER,
              Messages.getString("ReconCamera.reveal.base"));
        targetNumber.append(Compute.getTargetTerrainModifier(game, hidden));
        Hex hex = game.getHex(hidden.getPosition(), hidden.getBoardId());
        boolean isSubmerged = (hex != null) && hidden.isUnderwater();
        if (isSubmerged && (hex.depth() > 1)) {
            targetNumber.addModifier(hex.depth() - 1, Messages.getString("ReconCamera.reveal.depth"));
        }
        return targetNumber;
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
            for (Player player : game.getPlayersList()) {
                if (isOnCameraSide(camera, player) && !viewers.contains(player)) {
                    viewers.add(player);
                }
            }
        }
        return viewers;
    }

    /**
     * Checks whether a player is on a camera's side: its owner or a teammate. That side sees what the camera spotted,
     * and only that side is shown which units the camera is spotting.
     *
     * @param camera the unit with the camera
     * @param player the player to check, may be {@code null}
     *
     * @return {@code true} if the player owns the camera or is on its owner's team
     */
    public static boolean isOnCameraSide(Entity camera, @Nullable Player player) {
        Player cameraOwner = camera.getOwner();
        if ((player == null) || (cameraOwner == null)) {
            return false;
        }
        boolean isCameraOwner = player.getId() == cameraOwner.getId();
        boolean hasTeam = cameraOwner.getTeam() != Player.TEAM_NONE;
        boolean isTeammate = hasTeam && (player.getTeam() == cameraOwner.getTeam());
        return isCameraOwner || isTeammate;
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

    /**
     * Checks whether a unit's camera spot this turn marks an artillery target: the spotted unit itself, or the hex it
     * stands in.
     *
     * @param game    the game
     * @param spotter the possible spotter
     * @param target  the artillery target, a unit or a hex
     *
     * @return {@code true} if the spotter camera-spotted a unit at the target this turn
     */
    public static boolean isCameraSpottingAt(Game game, Entity spotter, Targetable target) {
        Entity spotted = game.getEntity(spotter.getReconCameraSpotTargetId());
        if ((spotted == null) || (spotted.getPosition() == null)) {
            return false;
        }
        boolean isTheSpottedUnit = (target.getTargetType() == Targetable.TYPE_ENTITY)
              && (target.getId() == spotted.getId());
        if (isTheSpottedUnit) {
            return true;
        }
        boolean isSameBoard = spotted.getBoardId() == target.getBoardId();
        return isSameBoard && spotted.getPosition().equals(target.getPosition());
    }

    private static void addGroundSpotModifiers(Game game, Entity camera, Entity target, ToHitData toHit) {
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
    }

    private static @Nullable Refusal usabilityRefusal(Entity camera) {
        Refusal refusal = commonUsabilityRefusal(camera);
        if (refusal != null) {
            return refusal;
        }
        if (isInRevealMode(camera)) {
            return new Refusal("ReconCamera.refusal.revealMode", "the camera is set to reveal hidden units");
        }
        return null;
    }

    private static @Nullable Refusal commonUsabilityRefusal(Entity camera) {
        if (!camera.hasWorkingMisc(MiscType.F_RECON_CAMERA)) {
            return new Refusal("ReconCamera.refusal.noCamera", "no working recon camera");
        }
        if ((camera.getCrew() == null) || !camera.getCrew().isActive()) {
            return new Refusal("ReconCamera.refusal.crew", "the crew cannot act");
        }
        if ((camera.getPosition() == null) || camera.isOffBoard()) {
            return new Refusal("ReconCamera.refusal.notOnBoard", "the unit is not on the board");
        }
        if (camera.hasReconCameraSpotThisTurn()) {
            return new Refusal("ReconCamera.refusal.alreadyUsed", "the camera has already been used this turn");
        }
        if (isAerospaceCamera(camera)) {
            return altitudeRefusal(camera);
        }
        return null;
    }

    private static @Nullable Refusal altitudeRefusal(Entity camera) {
        if (camera.isSpaceborne()) {
            return new Refusal("ReconCamera.refusal.space", "the camera works only over the ground");
        }
        int altitude = camera.getAltitude();
        if ((altitude < MINIMUM_CAMERA_ALTITUDE) || (altitude > MAXIMUM_CAMERA_ALTITUDE)) {
            return new Refusal("ReconCamera.refusal.altitude", "altitude " + altitude + " is outside 5 to 10");
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
        if (isAerospaceCamera(camera)) {
            return aerospaceTargetRefusal(game, camera, targetUnit);
        }
        return groundTargetRefusal(game, camera, targetUnit);
    }

    private static @Nullable Refusal aerospaceTargetRefusal(Game game, Entity camera, Entity targetUnit) {
        if (targetUnit.isAirborne()) {
            return new Refusal("ReconCamera.refusal.notGroundTarget", "an aerospace camera spots only ground units");
        }
        if (!isUnderCamera(game, camera, targetUnit, false)) {
            return new Refusal("ReconCamera.refusal.notBelow", "the target is not on the ground below the camera");
        }
        return null;
    }

    private static @Nullable Refusal groundTargetRefusal(Game game, Entity camera, Entity targetUnit) {
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

    private static boolean isBelowFlightPath(Game game, Entity camera, Entity groundUnit) {
        return isUnderCamera(game, camera, groundUnit, true);
    }

    /**
     * Checks whether a ground unit lies under an aerospace camera. On a low-altitude map it must stand on the ground
     * map under the camera's own hex or, for a reveal, under any hex the camera flew over this turn. On a ground map
     * it must be within the visible range, less twice the camera's altitude, of the flight path.
     */
    private static boolean isUnderCamera(Game game, Entity camera, Entity groundUnit, boolean isWholeFlightPath) {
        if (game.isOnAtmosphericMap(camera)) {
            Board atmosphericMap = game.getBoard(camera);
            List<Coords> cameraHexes = new ArrayList<>();
            cameraHexes.add(camera.getPosition());
            if (isWholeFlightPath) {
                cameraHexes.addAll(camera.getPassedThrough());
            }
            for (Coords atmosphericHex : cameraHexes) {
                if (atmosphericMap.getEmbeddedBoardAt(atmosphericHex) == groundUnit.getBoardId()) {
                    return true;
                }
            }
            return false;
        }
        boolean isOverTheSameGroundMap = camera.isAirborneAeroOnGroundMap()
              && (groundUnit.getBoardId() == camera.getBoardId());
        if (!isOverTheSameGroundMap) {
            return false;
        }
        Coords closestPoint = Compute.getClosestToFlightPath(camera, groundUnit.getPosition());
        Coords viewpoint = (closestPoint == null) ? camera.getPosition() : closestPoint;
        int distance = viewpoint.distance(groundUnit.getPosition()) + (2 * camera.getAltitude());
        int visibleRange = game.getPlanetaryConditions().getVisualRange(camera, groundUnit.isIlluminated());
        return distance <= visibleRange;
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

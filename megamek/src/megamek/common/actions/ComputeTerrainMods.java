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
package megamek.common.actions;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;

import java.util.EnumSet;

class ComputeTerrainMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the terrain and line of sight (LOS) Woods
     * along the LOS? Target Underwater? Partial cover? You'll find that here. Also, if the to-hit table is changed due
     * to cover/angle/elevation, look here. -4 for shooting at an immobile target? Using a weapon with a TH penalty?
     * Those are in other methods.
     *
     * @param game             The current {@link Game}
     * @param ae               The Entity making this attack
     * @param target           The Targetable object being attacked
     * @param ttype            The targetable object type
     * @param aElev            An int value representing the attacker's elevation
     * @param tElev            An int value representing the target's elevation
     * @param targEl           An int value representing the target's relative elevation
     * @param distance         The distance in hexes from attacker to target
     * @param los              The calculated LOS between attacker and target
     * @param toHit            The running total ToHitData for this WeaponAttackAction
     * @param losMods          A cached set of LOS-related modifiers
     * @param eistatus         An int value representing the ei cockpit/pilot upgrade status
     * @param wtype            The WeaponType of the weapon being used
     * @param weapon           The Mounted weapon being used
     * @param weaponId         The id number of the weapon being used - used by some external calculations
     * @param atype            The AmmoType being used for this attack
     * @param munition         Long indicating the munition type flag being used, if applicable
     * @param inSameBuilding   flag that indicates whether this attack originates from within the same building
     * @param isIndirect       flag that indicates whether this is an indirect attack (LRM, mortar...)
     * @param isPointBlankShot flag that indicates whether or not this is a PBS by a hidden unit
     * @param underWater       flag that indicates whether the weapon being used is underwater
     */
    static ToHitData compileTerrainAndLosToHitMods(Game game, Entity ae, Targetable target, int ttype,
          int aElev, int tElev, int targEl, int distance, LosEffects los, ToHitData toHit, ToHitData losMods,
          int eistatus, WeaponType wtype, WeaponMounted weapon, int weaponId, AmmoType atype, AmmoMounted ammo,
          EnumSet<AmmoType.Munitions> munition, boolean isAttackerInfantry, boolean inSameBuilding, boolean isIndirect,
          boolean isPointBlankShot, boolean underWater) {
        if (ae == null || target == null) {
            // Can't handle these attacks without a valid attacker and target
            return toHit;
        }

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Target's hex
        Hex targHex = game.getHexOf(target);

        boolean targetHexContainsWater = targHex != null && targHex.containsTerrain(Terrains.WATER);
        boolean targetHexContainsFortified = targHex != null && targHex.containsTerrain(Terrains.FORTIFIED);

        Entity te = null;
        if (ttype == Targetable.TYPE_ENTITY) {
            // Some of these weapons only target valid entities
            te = (Entity) target;
        }

        // Add range mods - If the attacker and target are in the same building
        // & hex, range mods don't apply (and will cause the shot to fail)
        // Don't apply this to bomb attacks either, which are going to be at 0 range of
        // necessity
        // Also don't apply to ADA Missiles (range computed separately)
        if (((los.getThruBldg() == null) || !los.getTargetPosition().equals(ae.getPosition())) &&
                  (wtype != null &&
                         (!(wtype.hasFlag(WeaponType.F_ALT_BOMB) ||
                                  wtype.hasFlag(WeaponType.F_DIVE_BOMB) ||
                                  (atype != null && atype.getMunitionType().contains(AmmoType.Munitions.M_ADA)))) &&
                         weaponId > WeaponType.WEAPON_NA)) {
            toHit.append(Compute.getRangeMods(game, ae, weapon, ammo, target));
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);

        // Attacker Terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, ae.getId()));

        // Target Terrain

        // BMM p. 31, semi-guided indirect missile attacks vs tagged targets ignore
        // terrain modifiers
        boolean semiGuidedIndirectVsTaggedTarget = isIndirect &&
                                                         (atype != null) &&
                                                         atype.getMunitionType()
                                                               .contains(AmmoType.Munitions.M_SEMIGUIDED) &&
                                                         Compute.isTargetTagged(target, game);

        // TW p.111
        boolean indirectMortarWithoutSpotter = (wtype != null) &&
                                                     wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT) &&
                                                     isIndirect &&
                                                     (Compute.findSpotter(game, ae, target) == null);

        // Base terrain calculations, not applicable when delivering minefields or bombs
        // also not applicable in pointblank shots from hidden units
        if ((ttype != Targetable.TYPE_MINEFIELD_DELIVER) &&
                  !isPointBlankShot &&
                  !semiGuidedIndirectVsTaggedTarget &&
                  !indirectMortarWithoutSpotter) {
            toHit.append(Compute.getTargetTerrainModifier(game, target, eistatus, inSameBuilding, underWater));
        }

        // Fortified/Dug-In Infantry
        if ((target instanceof Infantry) && wtype != null && !wtype.hasFlag(WeaponType.F_FLAMER)) {
            if (targetHexContainsFortified || (((Infantry) target).getDugIn() == Infantry.DUG_IN_COMPLETE)) {
                toHit.addModifier(2, megamek.client.ui.Messages.getString("WeaponAttackAction.DugInInf"));
            }
        }

        // target in water?
        int partialWaterLevel = 1;
        if (te != null && (te instanceof Mek) && ((Mek) te).isSuperHeavy()) {
            partialWaterLevel = 2;
        }
        if ((te != null) &&
                  targetHexContainsWater
                  // target in partial water
                  &&
                  (targHex.terrainLevel(Terrains.WATER) == partialWaterLevel) &&
                  (targEl == 0) &&
                  (te.height() > 0)) {
            los.setTargetCover(los.getTargetCover() | LosEffects.COVER_HORIZONTAL);
        }

        // Change hit table for partial cover, accommodate for partial underwater (legs)
        if (los.getTargetCover() != LosEffects.COVER_NONE) {
            if (underWater && (targetHexContainsWater && (targEl == 0) && (te != null && te.height() > 0))) {
                // weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                toHit.setCover(LosEffects.COVER_UPPER);
            } else {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PARTIAL_COVER)) {
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
                // If this is a called shot (high) the table has already been set and should be
                // used instead of partial cover.
                if (toHit.getHitTable() != ToHitData.HIT_ABOVE) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                }
                // Set damageable cover state information
                toHit.setDamagableCoverTypePrimary(los.getDamagableCoverTypePrimary());
                toHit.setCoverLocPrimary(los.getCoverLocPrimary());
                toHit.setCoverDropshipPrimary(los.getCoverDropshipPrimary());
                toHit.setCoverBuildingPrimary(los.getCoverBuildingPrimary());
                toHit.setDamagableCoverTypeSecondary(los.getDamagableCoverTypeSecondary());
                toHit.setCoverLocSecondary(los.getCoverLocSecondary());
                toHit.setCoverDropshipSecondary(los.getCoverDropshipSecondary());
                toHit.setCoverBuildingSecondary(los.getCoverBuildingSecondary());
            }
        }

        // Hull Down - Some cover states are dependent on LOS
        if ((te != null) && te.isHullDown()) {
            if ((te instanceof Mek) &&
                      !(te instanceof QuadVee && te.getConversionMode() == QuadVee.CONV_MODE_VEHICLE) &&
                      (los.getTargetCover() > LosEffects.COVER_NONE)) {
                toHit.addModifier(2, megamek.client.ui.Messages.getString("WeaponAttackAction.HullDown"));
            }
            // tanks going Hull Down is different rules then 'Meks, the
            // direction the attack comes from matters
            else if ((te instanceof Tank ||
                            (te instanceof QuadVee && te.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) &&
                           targetHexContainsFortified) {
                // TODO make this a LoS mod so that attacks will come in from
                // directions that grant Hull Down Mods
                int moveInDirection;

                if (!((Tank) te).isBackedIntoHullDown()) {
                    moveInDirection = ToHitData.SIDE_FRONT;
                } else {
                    moveInDirection = ToHitData.SIDE_REAR;
                }

                if ((te.sideTable(ae.getPosition()) == moveInDirection) ||
                          (te.sideTable(ae.getPosition()) == ToHitData.SIDE_LEFT) ||
                          (te.sideTable(ae.getPosition()) == ToHitData.SIDE_RIGHT)) {
                    toHit.addModifier(2, megamek.client.ui.Messages.getString("WeaponAttackAction.HullDown"));
                }
            }
        }

        // Special Equipment

        // BAP Targeting rule enabled - TO:AR 6th p.97
        // have line of sight and there are woods in our way
        // we have BAP in range or C3 member has BAP in range
        // we reduce the BTH by 1
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_BAP)) {
            boolean targetWoodsAffectModifier = (te != null) &&
                                                      !te.isOffBoard() &&
                                                      (te.getPosition() != null) &&
                                                      (game.getHexOf(te) != null) &&
                                                      game.getHexOf(te).hasVegetation() &&
                                                      !game.getOptions()
                                                             .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER);
            if (los.canSee() && (targetWoodsAffectModifier || los.thruWoods())) {
                boolean bapInRange = Compute.bapInRange(game, ae, te);
                boolean c3BAP = false;
                if (!bapInRange) {
                    for (Entity en : game.getC3NetworkMembers(ae)) {
                        if (ae.equals(en)) {
                            continue;
                        }
                        bapInRange = Compute.bapInRange(game, en, te);
                        if (bapInRange) {
                            c3BAP = true;
                            break;
                        }
                    }
                }
                if (bapInRange) {
                    if (c3BAP) {
                        toHit.addModifier(-1, megamek.client.ui.Messages.getString("WeaponAttackAction.BAPInWoodsC3"));
                    } else {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BAPInWoods"));
                    }
                }
            }
        }

        // To-hit table changes with no to-hit modifiers

        // Aeros in air-to-air combat can hit above and below
        if (Compute.isAirToAir(game, ae, target)) {
            if ((ae.getAltitude() - target.getAltitude()) > 2) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if ((target.getAltitude() - ae.getAltitude()) > 2) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            } else if (((ae.getAltitude() - target.getAltitude()) > 0) &&
                             te != null &&
                             (te.isAero() && ((IAero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_ABOVE);
            } else if (((ae.getAltitude() - target.getAltitude()) < 0) &&
                             te != null &&
                             (te.isAero() && ((IAero) te).isSpheroid())) {
                toHit.setHitTable(ToHitData.HIT_BELOW);
            }
        }

        // Change hit table for elevation differences inside building.
        if ((null != los.getThruBldg()) && (aElev != tElev)) {

            // Tanks get hit in a random side.
            if (target instanceof Tank) {
                toHit.setSideTable(ToHitData.SIDE_RANDOM);
            } else if (target instanceof Mek) {
                // Meks have special tables for shots from above and below.
                if (aElev > tElev) {
                    toHit.setHitTable(ToHitData.HIT_ABOVE);
                } else {
                    toHit.setHitTable(ToHitData.HIT_BELOW);
                }
            }
        }

        // Ground-to-air attacks always hit from below
        if (Compute.isGroundToAir(ae, target) && ((target.getAltitude() - ae.getAltitude()) > 2)) {
            toHit.setHitTable(ToHitData.HIT_BELOW);
        }

        // factor in target side
        if (isAttackerInfantry && (0 == distance)) {
            // Infantry attacks from the same hex are resolved against the
            // front.
            toHit.setSideTable(ToHitData.SIDE_FRONT);
        } else {
            if (weapon != null) {
                toHit.setSideTable(ComputeSideTable.sideTable(ae, target, weapon.getCalledShot().getCall()));
            }
        }

        // Change hit table for surface naval vessels hit by underwater attacks
        if (underWater && targetHexContainsWater && (null != te) && te.isSurfaceNaval()) {
            toHit.setHitTable(ToHitData.HIT_UNDERWATER);
        }

        return toHit;
    }

    private ComputeTerrainMods() { }
}

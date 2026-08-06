/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.alphaStrike;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.LG;
import static megamek.common.alphaStrike.BattleForceSUA.SLG;
import static megamek.common.alphaStrike.BattleForceSUA.VLG;

import java.util.Arrays;
import java.util.Map;

import megamek.common.interfaces.CombatRole;
import megamek.common.strategicBattleSystems.BattleForceSUAFormatter;
import megamek.common.units.BTObject;

/**
 * This interface is implemented by classes that can be displayed on an AlphaStrike card
 * {@link megamek.common.alphaStrike.cardDrawer.ASCard}. Implementing classes must be able to return the AlphaStrike
 * values printed on a card. Currently implemented by MekSummary and AlphaStrikeElement. This interface also offers some
 * utility methods for unit information that can be directly derived from the available data; those don't require any
 * overriding (e.g. {@link #isLargeAerospace()}). It also has methods that return current (= possibly damaged) values of
 * an element. These return an undamaged state by default and thus require overriding in AlphaStrikeElement (e.g.
 * {@link #getCurrentArmor()}).
 */
public interface ASCardDisplayable extends BattleForceSUAFormatter, BTObject, CombatRole {

    // TODO : Must also be able to return more "current" values for MV, Dmg, crits etc.


    @Override
    default String generalName() {
        return getChassis();
    }

    @Override
    default String specificName() {
        return getModel();
    }

    /** @return The AS element's model, such as "AS7-D". */
    String getModel();

    /** @return The AS element's chassis, such as "Atlas". */
    String getChassis();

    String getFullChassis();

    /** @return This AS element's MUL ID if it has one, -1 otherwise. */
    int getMulId();

    /** @return The AS element's Point Value (PV). This is not adjusted for damage on the element. */
    int getPointValue();

    /** @return The AS element's Pilot Skill (SKILL). Unless another skill has been set, this is 4. */
    default int getSkill() {
        return 4;
    }

    /** @return The AS element's type (TP), e.g. BM or CV. */
    ASUnitType getASUnitType();

    /** @return The AS element's size (SZ). */
    int getSize();

    /** @return The AS element's Target Movement Modifier (TMM). */
    int getTMM();

    /** @return The AS element's entire movement capability (MV). */
    Map<String, Integer> getMovement();

    /** @return The primary (= first) movement type String, such as "" for some ground units or "s" for submarines. */
    String getPrimaryMovementMode();

    /**
     * @return The standard damage (SML or SMLE depending on type). This will be empty for elements that use arcs.
     */
    ASDamageVector getStandardDamage();

    /** @return The AS element's extra Overheat Damage capability (OV) (not the current heat buildup). */
    int getOV();

    /** @return True if this AS element has an OV value other than 0. */
    default boolean hasOV() {
        return usesOV() && (getOV() > 0);
    }

    /**
     * @return True when this AS element is of a type that tracks heat levels and can have the OV and OVL abilities (BM,
     *       IM and AF).
     */
    default boolean usesOV() {
        return getASUnitType().isAnyOf(BM, IM, AF);
    }

    /** @return The AS element's front arc. Returns an empty arc for elements that don't use arcs. */
    ASArcSummary getFrontArc();

    /** @return The AS element's left arc. Returns an empty arc for elements that don't use arcs. */
    ASArcSummary getLeftArc();

    /** @return The AS element's right arc. Returns an empty arc for elements that don't use arcs. */
    ASArcSummary getRightArc();

    /** @return The AS element's rear arc. Returns an empty arc for elements that don't use arcs. */
    ASArcSummary getRearArc();

    /** @return The AS element's armor threshold (TH), if it uses threshold. */
    int getThreshold();

    /** @return True if this AS element uses the Threshold value (equivalent to {@link #isAerospace()}). */
    default boolean usesThreshold() {
        return isAerospace();
    }

    /** @return The AS element's full (=undamaged) Armor (A). */
    int getFullArmor();

    /** @return The AS element's current Armor (A). */
    default int getCurrentArmor() {
        return getFullArmor();
    }

    /** @return The AS element's full (=undamaged) Structure (S). */
    int getFullStructure();

    /** @return The AS element's current Structure (S). */
    default int getCurrentStructure() {
        return getFullStructure();
    }

    /** @return The squad size (typically 4, 5 or 6), if this AS element is a BA. */
    int getSquadSize();

    /** @return The element's central special abilities (not those in arcs). */
    ASSpecialAbilityCollection getSpecialAbilities();

    /** @return True if this AS element has the given movement mode, including LAM's a/g modes. */
    default boolean hasMovementMode(String mode) {
        return getMovement().containsKey(mode);
    }

    /** @return True if this AS element is a fighter (AF, CF) or an Aero SV (Fixed Wing Support). */
    @Override
    default boolean isFighter() {
        return getASUnitType().isAnyOf(AF, CF) || isFixedWingSupport();
    }

    /** @return True if this AS element is a BattleMek or Industrial Mek (BM, IM). */
    @Override
    default boolean isMek() {
        return getASUnitType().isMek();
    }

    /** @return True if this AS element is a BattleMek (BM). */
    @Override
    default boolean isBattleMek() {
        return getASUnitType().isBattleMek();
    }

    /** @return True if this AS element is a ProtoMek (PM). */
    @Override
    default boolean isProtoMek() {
        return getASUnitType().isProtoMek();
    }

    /** @return True if this AS element is a large Aerospace unit, i.e. SC, DS, DA, SS, JS, WS. */
    @Override
    default boolean isLargeAerospace() {
        return getASUnitType().isLargeAerospace();
    }

    /** @return True if this AS element is a BattleArmor unit, i.e. BA. */
    @Override
    default boolean isBattleArmor() {
        return getASUnitType().isBattleArmor();
    }

    /** @return True if this AS element is a Conventional Infantry unit, i.e. CI. */
    @Override
    default boolean isConventionalInfantry() {
        return getASUnitType().isConventionalInfantry();
    }

    @Override
    default boolean isAero() {
        return isAerospace() || hasMovementMode("a");
    }

    /**
     * Returns true if this AS element is an aerospace SV, i.e. an SV with a movement mode of "a", "k", "i", and "p".
     *
     * @return True if this AS element is an aerospace SV.
     */
    @Override
    default boolean isAerospaceSV() {
        return isSupportVehicle() && (hasMovementMode("a") || hasMovementMode("k")
              || hasMovementMode("i") || hasMovementMode("p"));
    }

    /** @return True if this AS element is a support vehicle of any kind (SV). */
    @Override
    default boolean isSupportVehicle() {
        return getASUnitType().isSupportVehicle();
    }

    @Override
    default boolean isConventionalFighter() {
        return getASUnitType().isAnyOf(CF);
    }

    @Override
    default boolean isAerospaceFighter() {
        return getASUnitType().isAnyOf(AF);
    }

    @Override
    default boolean isFixedWingSupport() {
        return isSupportVehicle() && hasMovementMode("a");
    }

    /** @return True if this AS element is a combat vehicle (CV, not support vehicle). */
    @Override
    default boolean isCombatVehicle() {
        return getASUnitType().isCombatVehicle();
    }

    /** @return True if this AS element uses three range bands S, M and L (equivalent to {@link #isGround()}). */
    default boolean usesSML() {
        return isGround();
    }

    /** @return True if this AS element uses four range bands S, M, L and E (equivalent to {@link #isAerospace()}). */
    default boolean usesSMLE() {
        return isAerospace();
    }

    @Override
    default boolean isTripodMek() {
        return getSpecialAbilities().hasSUA(BattleForceSUA.TRI);
    }

    @Override
    default boolean isQuadMek() {
        return getSpecialAbilities().hasSUA(BattleForceSUA.QUAD);
    }

    @Override
    default boolean isSmallCraft() {
        return isType(SC);
    }

    @Override
    default boolean isDropShip() {
        return isType(DS, DA);
    }

    @Override
    default boolean isSpheroid() {
        return isType(ASUnitType.DS) || (isType(ASUnitType.SC)
              && !getSpecialAbilities().hasSUA(BattleForceSUA.AERODYNESC));
    }

    /**
     * Returns true if this unit uses the 4 firing arcs of Warships, Dropships and other units. When this is the case,
     * {@link #getStandardDamage()} will return zero damage and the actual damage values are contained in the arcs.
     *
     * @return True if this unit uses firing arcs
     */
    default boolean usesArcs() {
        return isLargeAerospace() || (isSupportVehicle() &&
              (getSpecialAbilities().hasSUA(LG) || getSpecialAbilities().hasSUA(SLG)
                    || getSpecialAbilities().hasSUA(VLG)));
    }

    /** @return True if this AS element uses CAP weapons in its arcs, i.e. WS, SS or JS. */
    default boolean usesCapitalWeapons() {
        return isType(WS, SS, JS);
    }

    /** @return True if this AS element is any of the given types. */
    default boolean isType(ASUnitType type, ASUnitType... furtherTypes) {
        return getASUnitType() == type || Arrays.stream(furtherTypes).anyMatch(this::isType);
    }

    /**
     * @return True when this AS element is jump-capable. This is the case when its movement modes contains the "j"
     *       movement mode.
     */
    default boolean isJumpCapable() {
        return getMovement().containsKey("j");
    }

    /**
     * Returns true when this AS element is a submarine. This checks if it is a combat vehicle and has the "s" primary
     * movement type.
     *
     * @return True when this AS element is as a submarine
     */
    default boolean isSubmarine() {
        return isCombatVehicle() && getPrimaryMovementMode().equals("s");
    }

    /** @return True when this element uses TMM; equivalent to !{@link #isAerospace()}. */
    default boolean usesTMM() {
        return !isAerospace();
    }

    /**
     * Returns true when this element has any water movement of any kind.
     * <p>
     * movement type of (n, s, h, g) Mek ProtoMek
     */
    default boolean hasWaterMovement() {
        return hasMovementMode("n") || hasMovementMode("s") || hasMovementMode("h") || hasMovementMode("g")
              || isMek()
              || isProtoMek();
    }

    /**
     * Returns true when this element has ground movement of any kind.
     * <p>
     * movement type of (w, t, h, g, f, m, j) Mek ProtoMek
     */
    default boolean hasGroundMovement() {
        return hasMovementMode("w") || hasMovementMode("t") || hasMovementMode("h") || hasMovementMode("g")
              || hasMovementMode("f") || hasMovementMode("m") || hasMovementMode("j")
              || isMek()
              || isProtoMek();
    }

    /**
     * Returns true when this element has air movement of any kind.
     * <p>
     * movement type of (v, g, a, p)
     */
    default boolean hasAirMovement() {
        return hasMovementMode("v") || hasMovementMode("g") || hasMovementMode("a") || hasMovementMode("p");
    }

    @Override
    default boolean isIndustrialMek() {
        return isType(IM);
    }

    @Override
    default boolean isWarShip() {
        return isType(WS);
    }

    @Override
    default boolean isJumpShip() {
        return isType(JS);
    }

    @Override
    default boolean isSpaceStation() {
        return isType(SS);
    }
}

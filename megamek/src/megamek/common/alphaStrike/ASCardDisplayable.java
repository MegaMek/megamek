/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General  License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General  License for more details.
 *
 * You should have received a copy of the GNU General  License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.alphaStrike;

import megamek.common.BTObject;
import megamek.common.UnitRole;

import java.util.Arrays;
import java.util.Map;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

/**
 * This interface is implemented by classes that can be displayed on an AlphaStrike card
 * {@link megamek.common.alphaStrike.cardDrawer.ASCard}. Implementing classes must be able to return
 * the AlphaStrike values printed on a card. Currently implemented by MechSummary and 
 * AlphaStrikeElement.
 * This interface also offers some utility methods for unit information that can be directly derived from
 * the available data; those don't require any overriding (e.g. {@link #isLargeAerospace()}.
 * It also has methods that return current (= possibly damaged) values of an element.
 * These return an undamaged state by default and thus require overriding in AlphaStrikeElement
 * (e.g. {@link #getCurrentArmor()}.
 */
public interface ASCardDisplayable extends BTObject {

    // TODO : Must also be able to return more "current" values for MV, Dmg, crits etc.

    /** @return The AS element's model, such as "AS7-D". */
    String getModel();

    /** @return The AS element's chassis, such as "Atlas". */
    String getChassis();

    /** @return This AS element's MUL ID if it has one, -1 otherwise. */
    int getMulId();

    /** @return The AS element's Point Value (PV). This is not adjusted for damage on the element. */
    int getPointValue();

    /** @return The AS element's battlefield role (ROLE). */
    UnitRole getRole();

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
     * @return The standard damage (SML or SMLE depending on type). This will be empty for
     * elements that use arcs.
     */
    ASDamageVector getStandardDamage();

    /** @return The AS element's extra Overheat Damage capability (OV) (not the current heat buildup). */
    int getOV();

    /** @return True if this AS element has an OV value other than 0. */
    default boolean hasOV() {
        return usesOV() && (getOV() > 0);
    }

    /**
     * @return True when this AS element is of a type that tracks heat levels and can have
     * the OV and OVL abilities (BM, IM and AF).
     */
    default boolean usesOV() {
        return getASUnitType().isAnyOf(BM, IM, AF);
    }

    /** @return The AS element's front arc. Returns an empty arc for elements that don't use arcs. */
    ASSpecialAbilityCollection getFrontArc();

    /** @return The AS element's left arc. Returns an empty arc for elements that don't use arcs. */
    ASSpecialAbilityCollection getLeftArc();

    /** @return The AS element's right arc. Returns an empty arc for elements that don't use arcs. */
    ASSpecialAbilityCollection getRightArc();

    /** @return The AS element's rear arc. Returns an empty arc for elements that don't use arcs. */
    ASSpecialAbilityCollection getRearArc();

    /** @return The AS element's armor threshold (TH), if it uses threshold. */
    int getThreshold();

    /** @return True if this AS element uses the Threshold value (equivalent to {@link #isAerospace()}). */
    default boolean usesThreshold() {
        return isAerospace();
    }

    /** @return The AS element's full (=undamaged) Armor (A).*/
    int getFullArmor();

    /** @return The AS element's current Armor (A).*/
    default int getCurrentArmor() {
        return getFullArmor();
    }

    /** @return The AS element's full (=undamaged) Structure (S).*/
    int getFullStructure();

    /** @return The AS element's current Structure (S).*/
    default int getCurrentStructure() {
        return getFullStructure();
    }

    /** @return The squad size (typically 4, 5 or 6), if this AS element is a BA.*/
    int getSquadSize();

    /** @return The element's central special abilities (not those in arcs). */
    ASSpecialAbilityCollection getSpecialAbilities();

    /** @return True if this AS element has the given movement mode, including LAM's a/g modes. */
    default boolean hasMovementMode(String mode) {
        return getMovement().containsKey(mode);
    }

    /** @return True if this AS element is a fighter (AF, CF). */
    default boolean isFighter() {
        return getASUnitType().isAnyOf(AF, CF) || isAerospaceSV();
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
     * Returns true if this AS element is an aerospace SV, i.e. an SV with a movement mode of
     * "a", "k", "i", and "p".
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

    /**
     * Returns true if this unit uses the 4 firing arcs of Warships, Dropships and other units.
     * When this is the case, {@link #getStandardDamage()} will return zero damage and the actual
     * damage values are contained in the arcs.
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
     * @return True when this AS element is jump-capable. This is the case when its movement modes
     * contains the "j" movement mode.
     */
    default boolean isJumpCapable() {
        return getMovement().containsKey("j");
    }

    /**
     * Returns true when this AS element is a submarine. This checks if it is a combat vehicle
     * and has the "s" primary movement type.
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
}

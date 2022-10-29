/*
 * Copyright (c) 2021 - 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.alphaStrike;

import megamek.common.*;

import java.util.Arrays;

/** Represents the AlphaStrike Element Types (ASC, page 91) */
public enum ASUnitType {

    BM, IM, PM, CV, SV, MS, BA, CI, AF, CF, SC, DS, DA, JS, WS, SS, UNKNOWN;

    /** Returns the AlphaStrike element type for the given entity or UNKNOWN if it has no AS equivalent. */
    public static ASUnitType getUnitType(Entity en) {
        if (en instanceof Mech) {
            return ((Mech) en).isIndustrial() ? IM : BM;
        } else if (en instanceof Protomech) {
            return PM;
        } else if (en instanceof Tank) {
            return en.isSupportVehicle() ? SV : CV;
        } else if (en instanceof BattleArmor) {
            return BA;
        } else if (en instanceof Infantry) {
            return CI;
        } else if (en instanceof SpaceStation) {
            return SS;
        } else if (en instanceof Warship) {
            return WS;
        } else if (en instanceof Jumpship) {
            return JS;
        } else if (en instanceof Dropship) {
            return ((Dropship) en).isSpheroid() ? DS : DA;
        } else if (en instanceof SmallCraft) {
            return SC;
        } else if (en instanceof FixedWingSupport) {
            return SV;
        } else if (en instanceof ConvFighter) {
            return CF;
        } else if (en instanceof Aero) {
            return AF;
        }
        return UNKNOWN;
    }

    /** Returns true if this AS Element Type is equal to any of the given Types. */
    public boolean isAnyOf(ASUnitType type, ASUnitType... furtherTypes) {
        return (this == type) || Arrays.stream(furtherTypes).anyMatch(t -> this == t);
    }

    /** @return True if this ASUnitType is BattleMek (BM). */
    public boolean isBattleMek() {
        return this == BM;
    }

    /** @return True if this ASUnitType is ProtoMek (PM). */
    public boolean isProtoMek() {
        return this == PM;
    }

    /** @return True if this ASUnitType is a Large Aerospace type, i.e. SC, DS, DA, SS, JS, WS. */
    public boolean isLargeAerospace() {
        return isAnyOf(SC, DS, DA, SS, JS, WS);
    }

    /** @return True if this ASUnitType is BattleArmor (BA). */
    public boolean isBattleArmor() {
        return this == BA;
    }

    /** @return True if this ASUnitType is Conventional Infantry (CI). */
    public boolean isConventionalInfantry() {
        return this == CI;
    }

    /** @return True if this ASUnitType is a BattleMek or Industrial Mek type (BM, IM). */
    public boolean isMek() {
        return isAnyOf(BM, IM);
    }

    /** @return True if this ASUnitType is an Infantry type (BA or CI). */
    public boolean isInfantry() {
        return isAnyOf(BA, CI);
    }

    /** @return True if this ASUnitType is Combat Vehicle (CV). */
    public boolean isCombatVehicle() {
        return this == CV;
    }

    /** @return True if this ASUnitType is Support Vehicle (SV). */
    public boolean isSupportVehicle() {
        return this == SV;
    }

    /** @return True if this ASUnitType is Fighter (AF, CF). */
    public boolean isFighter() {
        return isAnyOf(AF, CF);
    }

}

/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Arrays;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.*;

/** Represents the AlphaStrike Element Types (ASC, page 91) */
public enum ASUnitType {

    BM, IM, PM, CV, SV, MS, BA, CI, AF, CF, SC, DS, DA, JS, WS, SS, UNKNOWN;

    /** Returns the AlphaStrike element type for the given entity or UNKNOWN if it has no AS equivalent. */
    public static ASUnitType getUnitType(Entity en) {
        if (en instanceof Mek) {
            return ((Mek) en).isIndustrial() ? IM : BM;
        } else if (en instanceof ProtoMek) {
            return PM;
        } else if (en instanceof MobileStructure) {
            return MS;
        } else if (en instanceof Tank || en instanceof BuildingEntity) {
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
            return en.isSpheroid() ? DS : DA;
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
}

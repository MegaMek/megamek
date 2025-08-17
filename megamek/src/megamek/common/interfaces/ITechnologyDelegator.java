/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.interfaces;

import megamek.common.SimpleTechLevel;

/**
 * Convenience interface that allows classes to implement the ITechnology interface by delegating to a member that
 * implements ITechnology.
 *
 * @author Neoancient
 */
public interface ITechnologyDelegator extends ITechnology {

    ITechnology getTechSource();

    @Override
    default boolean isClan() {
        return getTechSource().isClan();
    }

    @Override
    default boolean isMixedTech() {
        return getTechSource().isMixedTech();
    }

    @Override
    default TechBase getTechBase() {
        return getTechSource().getTechBase();
    }

    @Override
    default int getIntroductionDate() {
        return getTechSource().getIntroductionDate();
    }

    @Override
    default int getPrototypeDate() {
        return getTechSource().getPrototypeDate();
    }

    @Override
    default int getProductionDate() {
        return getTechSource().getProductionDate();
    }

    @Override
    default int getCommonDate() {
        return getTechSource().getCommonDate();
    }

    @Override
    default int getExtinctionDate() {
        return getTechSource().getExtinctionDate();
    }

    @Override
    default int getReintroductionDate() {
        return getTechSource().getReintroductionDate();
    }

    @Override
    default TechRating getTechRating() {
        return getTechSource().getTechRating();
    }

    @Override
    default AvailabilityValue getBaseAvailability(Era era) {
        return getTechSource().getBaseAvailability(era);
    }

    @Override
    default int getIntroductionDate(boolean clan, Faction faction) {
        return getTechSource().getIntroductionDate(clan, faction);
    }

    @Override
    default int getPrototypeDate(boolean clan, Faction faction) {
        return getTechSource().getPrototypeDate(clan, faction);
    }

    @Override
    default int getProductionDate(boolean clan, Faction faction) {
        return getTechSource().getProductionDate(clan, faction);
    }

    @Override
    default int getExtinctionDate(boolean clan, Faction faction) {
        return getTechSource().getExtinctionDate(clan, faction);
    }

    @Override
    default int getReintroductionDate(boolean clan, Faction faction) {
        return getTechSource().getReintroductionDate(clan, faction);
    }

    @Override
    default SimpleTechLevel getStaticTechLevel() {
        return getTechSource().getStaticTechLevel();
    }

}

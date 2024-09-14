/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

/**
 * Convenience interface that allows classes to implement the ITechnology
 * interface by delegating
 * to a member that implements ITechnology.
 *
 * @author Neoancient
 *
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
    default int getTechBase() {
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
    default int getTechRating() {
        return getTechSource().getTechRating();
    }

    @Override
    default int getBaseAvailability(int era) {
        return getTechSource().getBaseAvailability(era);
    }

    @Override
    default int getIntroductionDate(boolean clan, int faction) {
        return getTechSource().getIntroductionDate(clan, faction);
    }

    @Override
    default int getPrototypeDate(boolean clan, int faction) {
        return getTechSource().getPrototypeDate(clan, faction);
    }

    @Override
    default int getProductionDate(boolean clan, int faction) {
        return getTechSource().getProductionDate(clan, faction);
    }

    @Override
    default int getExtinctionDate(boolean clan, int faction) {
        return getTechSource().getExtinctionDate(clan, faction);
    }

    @Override
    default int getReintroductionDate(boolean clan, int faction) {
        return getTechSource().getReintroductionDate(clan, faction);
    }

    @Override
    default SimpleTechLevel getStaticTechLevel() {
        return getTechSource().getStaticTechLevel();
    }

}

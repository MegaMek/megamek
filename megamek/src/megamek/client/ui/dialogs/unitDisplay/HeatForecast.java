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
package megamek.client.ui.dialogs.unitDisplay;

import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * The heat a unit is on course to have at the end of the turn, as the unit display shows it.
 * <p>
 * This is a forecast, not the server's heat phase: last round's heat, engine crits, external heat and cooling,
 * infernos, planetary temperature, fire and magma under the unit, active stealth systems, and the heat of every weapon
 * already fired this phase. Committed heat is current heat - declaring an attack spends its heat straight away, so a
 * player deciding on the next shot sees where the unit really stands.
 */
public final class HeatForecast {
    private static final MMLogger LOGGER = MMLogger.create(HeatForecast.class);

    /** The standard cap on external heat, TW p.159, used when the game option is absent or invalid. */
    private static final int STANDARD_MAX_EXTERNAL_HEAT = 15;
    private static final int MAX_EXTERNAL_COOLING = 9;
    private static final int STEALTH_SYSTEM_HEAT = 10;
    private static final int CHAMELEON_SHIELD_HEAT = 6;
    private static final int NOVA_CEWS_HEAT = 2;
    private static final int COMBAT_COMPUTER_HEAT_REDUCTION = 4;

    /**
     * The forecast for one unit.
     *
     * @param buildup            the heat the unit is on course for, never below zero
     * @param capacity           the unit's heat capacity including water and radical heat sinks
     * @param capacityText       the capacity as the tooltip code formats it, e.g. {@code "30 [40]"}
     * @param firedThisPhase     whether any weapon has fired this phase; its heat is in {@code buildup}
     * @param combatComputer     whether the Combat Computer quirk reduced the buildup
     * @param extremeTemperature whether the planetary temperature is extreme
     */
    public record Result(int buildup, int capacity, String capacityText, boolean firedThisPhase,
          boolean combatComputer, boolean extremeTemperature) {

        /**
         * @return how far the buildup exceeds the capacity; zero or less when it does not
         */
        public int overCapacity() {
            return buildup - capacity;
        }
    }

    private HeatForecast() {
        // static use only
    }

    /**
     * Forecasts the given unit's heat.
     *
     * @param entity the unit
     * @param game   the game, or {@code null} outside a game (a unit viewer), in which case the standard external
     *               heat cap applies and no planetary or terrain heat is counted
     *
     * @return the forecast
     */
    public static Result forecast(Entity entity, @Nullable Game game) {
        int maxExternalHeat = (game != null)
              ? game.getOptions().intOption(OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT)
              : STANDARD_MAX_EXTERNAL_HEAT;
        if (maxExternalHeat < 0) {
            maxExternalHeat = STANDARD_MAX_EXTERNAL_HEAT;
        }

        int buildup = (entity.heat // heat from last round
              + entity.getEngineCritHeat() // heat engine crits will add
              + Math.min(maxExternalHeat, entity.heatFromExternal) // heat from external sources
              + entity.heatBuildup) // heat we're building up this round
              - Math.min(MAX_EXTERNAL_COOLING, entity.coolFromExternal); // cooling from external sources

        buildup += mekEnvironmentHeat(entity, game);
        buildup += terrainHeat(entity, game);
        buildup += activeSystemsHeat(entity);

        boolean firedThisPhase = false;
        boolean[] usedFrontArc = new boolean[entity.locations()];
        boolean[] usedRearArc = new boolean[entity.locations()];
        for (WeaponMounted mounted : entity.getWeaponListWithHHW()) {
            if (isHiddenLamBombWeapon(entity, mounted)) {
                continue;
            }
            if (!hasFiredThisPhase(mounted, game)) {
                continue;
            }
            firedThisPhase = true;
            buildup += firedWeaponHeat(entity, mounted, game, usedFrontArc, usedRearArc);
        }

        if (entity.hasDamagedRHS() && firedThisPhase) {
            buildup++;
        }

        boolean combatComputer = entity.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER);
        if (combatComputer) {
            buildup -= COMBAT_COMPUTER_HEAT_REDUCTION;
        }

        // extreme cold can take the buildup below zero; the unit is simply cool
        buildup = Math.max(0, buildup);

        UnitToolTip.HeatDisplayHelper capacity = UnitToolTip.getHeatCapacityForDisplay(entity);
        boolean extremeTemperature = (game != null) && game.getPlanetaryConditions().isExtremeTemperature();
        return new Result(buildup, capacity.heatCapWater, capacity.heatCapacityStr, firedThisPhase, combatComputer,
              extremeTemperature);
    }

    /** Infernos and planetary temperature, which only affect Meks. */
    private static int mekEnvironmentHeat(Entity entity, @Nullable Game game) {
        if (!(entity instanceof Mek mek)) {
            return 0;
        }
        int heat = 0;
        if (mek.infernos.isStillBurning()) {
            heat += mek.infernos.getHeat();
        }
        if (game == null) {
            return heat;
        }
        if (game.getPlanetaryConditions().getTemperature() > 0) {
            int temperatureHeat = game.getPlanetaryConditions().getTemperatureDifference(50, -30);
            if (mek.hasIntactHeatDissipatingArmor()) {
                temperatureHeat /= 2;
            }
            heat += temperatureHeat;
        } else {
            heat -= game.getPlanetaryConditions().getTemperatureDifference(50, -30);
        }
        return heat;
    }

    /** Fire and magma in the unit's own hex. */
    private static int terrainHeat(Entity entity, @Nullable Game game) {
        Coords position = entity.getPosition();
        if ((game == null) || entity.isOffBoard() || !game.hasBoardLocation(position, entity.getBoardId())) {
            return 0;
        }
        Hex hex = game.getBoard(entity.getBoardId()).getHex(position);
        if (hex == null) {
            LOGGER.warn("An entity is not offboard but has a position not on board.");
            return 0;
        }
        boolean heatDissipatingArmor = (entity instanceof Mek mek) && mek.hasIntactHeatDissipatingArmor();
        int heat = 0;
        if (hex.containsTerrain(Terrains.FIRE) && (hex.getFireTurn() > 0)) {
            heat += heatDissipatingArmor ? 2 : 5;
        }
        if (hex.terrainLevel(Terrains.MAGMA) == 1) {
            heat += heatDissipatingArmor ? 2 : 5;
        } else if (hex.terrainLevel(Terrains.MAGMA) == 2) {
            heat += heatDissipatingArmor ? 5 : 10;
        }
        return heat;
    }

    /** Stealth armor, null-signature and void-signature systems, chameleon shield and Nova CEWS. */
    private static int activeSystemsHeat(Entity entity) {
        boolean mekOrAero = (entity instanceof Mek) || (entity instanceof Aero);
        int heat = 0;
        if ((mekOrAero && entity.isStealthActive()) || entity.isNullSigActive() || entity.isVoidSigActive()) {
            heat += STEALTH_SYSTEM_HEAT;
        }
        if ((entity instanceof Mek) && entity.isChameleonShieldOn()) {
            heat += CHAMELEON_SHIELD_HEAT;
        }
        if (mekOrAero && entity.hasActiveNovaCEWS()) {
            heat += NOVA_CEWS_HEAT;
        }
        return heat;
    }

    /** A LAM in Mek mode does not show its bomb weapons, except rocket launchers and TAG; they add no heat either. */
    private static boolean isHiddenLamBombWeapon(Entity entity, WeaponMounted mounted) {
        return (entity instanceof LandAirMek)
              && (entity.getConversionMode() == LandAirMek.CONV_MODE_MEK)
              && mounted.getType().hasFlag(WeaponType.F_BOMB_WEAPON)
              && (mounted.getType().getAmmoType() != AmmoType.AmmoTypeEnum.RL_BOMB)
              && !mounted.getType().hasFlag(WeaponType.F_TAG);
    }

    private static boolean hasFiredThisPhase(WeaponMounted mounted, @Nullable Game game) {
        return mounted.isUsedThisRound()
              && (game != null)
              && (game.getPhase() == mounted.usedInPhase())
              && game.getPhase().isFiring();
    }

    /**
     * The heat of a weapon fired this phase. A large craft pays per bay, or once per firing arc when the bay-heat
     * option is off; anything else pays the weapon's own heat, unless the weapon is a bomb or belongs to a different
     * unit (a handheld weapon's own entity).
     */
    private static int firedWeaponHeat(Entity entity, WeaponMounted mounted, Game game, boolean[] usedFrontArc,
          boolean[] usedRearArc) {
        if (!entity.isLargeCraft()) {
            boolean ownWeapon = !mounted.isBombMounted() && entity.equals(mounted.getEntity());
            return ownWeapon ? mounted.getHeatByBay() : 0;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
            return mounted.getHeatByBay();
        }
        int location = mounted.getLocation();
        boolean rearMount = mounted.isRearMounted();
        boolean[] usedArc = rearMount ? usedRearArc : usedFrontArc;
        if (usedArc[location]) {
            return 0;
        }
        usedArc[location] = true;
        return entity.getHeatInArc(location, rearMount);
    }
}

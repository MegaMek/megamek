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

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.TaintedAtmosphereRules;

/**
 * The optional rule that lets a crew wear something of their own, so that what they are wearing still matters once
 * they are outside their unit.
 * <p>
 * A crew that ejects arrives on the board as conventional infantry, and MegaMek already knows what conventional
 * infantry survives, reading it off their armor kit: a space suit answers vacuum, a parka answers cold, an
 * environment suit answers several things at once. All this rule does is dress the ejected crew in what they were
 * wearing, and those existing answers apply. It does nothing unless {@link OptionsConstants#RPG_COMBAT_SUITS} is
 * switched on.
 * <p>
 * The MekWarrior Combat Suit is the one kit that needs more than its own flags. A Time of War p.294 gives it as a
 * combat suit plus a combat neurohelmet and says the helmet "may be sealed in hostile environments" with its own air
 * supply, but MegaMek's equipment entry carries only a damage divisor. Its flags cannot say what the book says, so
 * the interpretation lives here: air the crew would otherwise have to breathe, and heat, because the suit is armored
 * cooling gear. Not vacuum or water - it covers the torso, arms and legs and is not pressurized, and holding
 * pressure over a body is a different job from supplying air.
 */
public final class CrewArmorKitRules {

    /** The internal name of the kit whose protection is an interpretation rather than a flag. */
    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    public static final String COMBAT_SUIT_NAME = "MechWarrior Combat Suit";

    /**
     * What a full combat suit kit costs, in C-bills: 20,000 for the suit, 1,400 for the combat neurohelmet and 175
     * for the plasteel boots (A Time of War p.294). The crew is issued the whole kit rather than the suit alone,
     * because the sealing that makes the rule work belongs to the helmet.
     * <p>
     * MegaMek charges nobody for crew equipment, so this is recorded for MekHQ, which buys the kit and issues it to
     * a person before the battle starts.
     */
    public static final int COMBAT_SUIT_KIT_COST_C_BILLS = 21_575;

    private CrewArmorKitRules() {
    }

    /**
     * Whether this unit's crew can be issued a kit at all.
     * <p>
     * Offered to crews who might end up outside their unit on foot: BattleMeks and IndustrialMeks, vehicle crews who
     * abandon, and aerospace crews who eject. Conventional infantry and battle armor are excluded because they carry
     * their armor on the unit itself, and a ProtoMek has no ejection system to leave through.
     *
     * @param entity the unit whose crew is being configured, or {@code null}
     *
     * @return {@code true} if the crew may be issued an armor kit
     */
    public static boolean canWearArmorKit(@Nullable Entity entity) {
        if ((entity == null) || (entity.defaultCrewType() == CrewType.NONE) || (entity instanceof Infantry)) {
            return false;
        }
        return (entity instanceof Mek) || (entity instanceof Tank) || entity.isAero();
    }

    /**
     * Every armor kit MegaMek knows, which is the list a crew can be issued from.
     *
     * @return the kits, in the order the equipment tables hold them
     */
    public static List<EquipmentType> availableArmorKits() {
        List<EquipmentType> armorKits = new ArrayList<>();
        for (EquipmentType equipment : EquipmentType.allTypes()) {
            if (equipment.hasFlag(MiscType.F_ARMOR_KIT)) {
                armorKits.add(equipment);
            }
        }
        return armorKits;
    }

    /**
     * Whether the optional rule is in force.
     *
     * @param game the game to ask, or {@code null}
     *
     * @return {@code true} if crew equipment does anything in this game
     */
    public static boolean isRuleInPlay(@Nullable Game game) {
        return (game != null) && game.getOptions().booleanOption(OptionsConstants.RPG_COMBAT_SUITS);
    }

    /**
     * The kit this unit's crew would leave wearing, with the optional rule in play.
     *
     * @param entity the unit the crew is aboard, or {@code null}
     * @param game   the game whose options say whether the rule is in force, or {@code null}
     *
     * @return the kit, or {@code null} if the crew wear none or the rule is off
     */
    public static @Nullable EquipmentType crewArmorKit(@Nullable Entity entity, @Nullable Game game) {
        if ((entity == null) || (entity.getCrew() == null) || !isRuleInPlay(game)) {
            return null;
        }
        if (!canWearArmorKit(entity)) {
            return null;
        }
        String kitName = entity.getCrew().getAnyArmorKitName();
        if ((kitName == null) || kitName.isBlank()) {
            return null;
        }
        EquipmentType armorKit = EquipmentType.get(kitName);
        if ((armorKit == null) || !isAvailableIn(armorKit, entity, game)) {
            return null;
        }
        return armorKit;
    }

    /**
     * Whether this kit has been invented yet, and not gone extinct, in the year being played. The dates are the
     * equipment's own, so this defers to the tech progression already recorded rather than repeating it.
     *
     * @param armorKit the kit to check, or {@code null}
     * @param entity   the unit whose crew would wear it, which decides whether Clan dates apply, or {@code null}
     * @param game     the game whose year applies, or {@code null}
     *
     * @return {@code true} if a crew may have this kit in this year
     */
    public static boolean isAvailableIn(@Nullable EquipmentType armorKit, @Nullable Entity entity,
          @Nullable Game game) {
        if ((armorKit == null) || (game == null)) {
            return false;
        }
        int year = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        boolean isClanCrew = (entity != null) && entity.isClan();
        return armorKit.isAvailableIn(year, isClanCrew, false);
    }

    /**
     * Whether this crew, now on foot, is wearing the MekWarrior Combat Suit.
     *
     * @param ejectedCrew the crew on the board, or {@code null}
     *
     * @return {@code true} if they are wearing one and the rule is in force
     */
    public static boolean isWearingCombatSuit(@Nullable Entity ejectedCrew) {
        if (!(ejectedCrew instanceof ConvInfantry convInfantry) || !isRuleInPlay(convInfantry.getGame())) {
            return false;
        }
        return convInfantry.hasCombatSuit();
    }

    /**
     * Whether the combat suit keeps its wearer alive in air they would otherwise have to breathe.
     * <p>
     * Only the combat suit needs asking about. Every other kit carries its own tainted and toxic flags, which the
     * conventional infantry rules already read.
     *
     * @param ejectedCrew the crew on the board, or {@code null}
     *
     * @return {@code true} if the suit protects them from the air itself
     */
    public static boolean protectsAgainstAtmosphericTaint(@Nullable Entity ejectedCrew) {
        return isWearingCombatSuit(ejectedCrew);
    }

    /**
     * Whether the combat suit keeps its wearer alive in extreme heat, which armored cooling gear does. Extreme cold
     * is a different problem and the suit is not described as solving it.
     *
     * @param ejectedCrew the crew on the board, or {@code null}
     * @param temperature the temperature they are standing in
     *
     * @return {@code true} if the suit protects them from this temperature
     */
    public static boolean protectsAgainstTemperature(@Nullable Entity ejectedCrew, int temperature) {
        return (temperature > 0) && isWearingCombatSuit(ejectedCrew);
    }

    /**
     * Whether a kit is worth mentioning in the ejection report, being the difference between the crew living and
     * dying out there.
     * <p>
     * A crew in fair weather does not need telling that their kit made no difference, and a crew ejecting into
     * something the kit does not answer must not be told they are safe.
     *
     * @param armorKit   the kit the crew is wearing, or {@code null}
     * @param conditions the conditions they are ejecting into, or {@code null}
     *
     * @return {@code true} if the kit answers a danger that is actually out there
     */
    public static boolean coversSomethingIn(@Nullable EquipmentType armorKit,
          @Nullable PlanetaryConditions conditions) {
        if ((armorKit == null) || (conditions == null)) {
            return false;
        }
        boolean isAirTooThinToBreathe = conditions.getAtmosphere().isLighterThan(Atmosphere.THIN);
        boolean isCombatSuit = armorKit.hasFlag(MiscTypeFlag.S_COMBAT_SUIT);

        if (isAirTooThinToBreathe) {
            // Only a sealed kit answers vacuum; the combat suit explicitly does not.
            return armorKit.hasAnyFlag(MiscTypeFlag.S_SPACE_SUIT, MiscTypeFlag.S_XCT_VACUUM);
        }
        // A taint needs an atmosphere to be carried in. The combat suit answers it by interpretation; other kits
        // carry the flags for it themselves.
        boolean isAirPoisonous = TaintedAtmosphereRules.requiresXctInfantry(conditions.getAtmosphericTaint());
        boolean answersTheAir = isCombatSuit
              || armorKit.hasAnyFlag(MiscTypeFlag.S_TAINTED_ATMOSPHERE, MiscTypeFlag.S_TOXIC_ATMOSPHERE);
        if (isAirPoisonous && answersTheAir) {
            return true;
        }
        if (!conditions.isExtremeTemperature()) {
            return false;
        }
        boolean isExtremeHeat = conditions.getTemperature() > 0;
        return isExtremeHeat
              ? (isCombatSuit || armorKit.hasFlag(MiscTypeFlag.S_HOT_WEATHER))
              : armorKit.hasAnyFlag(MiscTypeFlag.S_COLD_WEATHER, MiscTypeFlag.S_XCT_VACUUM);
    }
}

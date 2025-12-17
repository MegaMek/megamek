/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.equipment;

import java.util.*;

import megamek.common.RangeType;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.enums.AmmoTypeFlag;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.weapons.Weapon;
import megamek.logging.MMLogger;
import org.apache.commons.lang3.ArrayUtils;

public class AmmoType extends EquipmentType {
    private static final MMLogger LOGGER = MMLogger.create(AmmoType.class);

    public enum AmmoCategory {
        Ballistic,
        Missile,
        Energy,
        Artillery,
        Bomb,
        Chemical,
        Special
    }

    public enum AmmoTypeEnum {
        NA(-1, "N/A", AmmoCategory.Special),
        AC(1, "Autocannon", AmmoCategory.Ballistic),
        VEHICLE_FLAMER(2, "Vehicle Flamer", AmmoCategory.Chemical),
        MG(3, "Machine Gun", AmmoCategory.Ballistic),
        MG_HEAVY(4, "Heavy Machine Gun", AmmoCategory.Ballistic),
        MG_LIGHT(5, "Light Machine Gun", AmmoCategory.Ballistic),
        GAUSS(6, "Gauss Rifle", AmmoCategory.Ballistic),
        LRM(7, "Long Range Missile", AmmoCategory.Missile),
        LRM_TORPEDO(8, "LRM Torpedo", AmmoCategory.Missile),
        SRM(9, "Short Range Missile", AmmoCategory.Missile),
        SRM_TORPEDO(10, "SRM Torpedo", AmmoCategory.Missile),
        SRM_STREAK(11, "Streak SRM", AmmoCategory.Missile),
        MRM(12, "Medium Range Missile", AmmoCategory.Missile),
        NARC(13, "NARC", AmmoCategory.Special),
        AMS(14, "Anti-Missile System", AmmoCategory.Special),
        ARROW_IV(15, "Arrow IV", AmmoCategory.Artillery),
        LONG_TOM(16, "Long Tom", AmmoCategory.Artillery),
        SNIPER(17, "Sniper", AmmoCategory.Artillery),
        THUMPER(18, "Thumper", AmmoCategory.Artillery),
        AC_LBX(19, "LB-X Autocannon", AmmoCategory.Ballistic),
        AC_ULTRA(20, "Ultra Autocannon", AmmoCategory.Ballistic),
        GAUSS_LIGHT(21, "Light Gauss Rifle", AmmoCategory.Ballistic),
        GAUSS_HEAVY(22, "Heavy Gauss Rifle", AmmoCategory.Ballistic),
        AC_ROTARY(23, "Rotary Autocannon", AmmoCategory.Ballistic),
        SRM_ADVANCED(24, "Advanced SRM", AmmoCategory.Missile),
        BA_MICRO_BOMB(25, "BA Micro Bomb", AmmoCategory.Bomb),
        LRM_TORPEDO_COMBO(26, "LRM Torpedo Combo", AmmoCategory.Missile),
        MINE(27, "Mine", AmmoCategory.Special),
        ATM(28, "Advanced Tactical Missile", AmmoCategory.Missile),
        ROCKET_LAUNCHER(29, "Rocket Launcher", AmmoCategory.Missile),
        INARC(30, "iNARC", AmmoCategory.Special),
        LRM_STREAK(31, "Streak LRM", AmmoCategory.Missile),
        AC_LBX_THB(32, "LB-X Autocannon THB", AmmoCategory.Ballistic),
        AC_ULTRA_THB(33, "Ultra Autocannon THB", AmmoCategory.Ballistic),
        LAC(34, "Light Autocannon", AmmoCategory.Ballistic),
        HEAVY_FLAMER(35, "Heavy Flamer", AmmoCategory.Chemical),
        COOLANT_POD(36, "Coolant Pod", AmmoCategory.Special),
        EXLRM(37, "Extended LRM", AmmoCategory.Missile),
        APGAUSS(38, "AP Gauss Rifle", AmmoCategory.Ballistic),
        MAGSHOT(39, "MagShot Gauss Rifle", AmmoCategory.Ballistic),
        // PXLRM(40, "Prototype Extended LRM", AmmoCategory.Missile),
        // HSRM(41, "Heavy SRM", AmmoCategory.Missile),
        // MRM_STREAK(42, "Streak Medium Range Missile", AmmoCategory.Missile),
        MPOD(43, "M-Pod", AmmoCategory.Special),
        HAG(44, "Hyper Assault Gauss", AmmoCategory.Ballistic),
        MML(45, "Multi-Missile Launcher", AmmoCategory.Missile),
        PLASMA(46, "Plasma Rifle", AmmoCategory.Energy),
        SBGAUSS(47, "Silver Bullet Gauss", AmmoCategory.Ballistic),
        RAIL_GUN(48, "Rail Gun", AmmoCategory.Ballistic),
        TBOLT_5(49, "Thunderbolt 5", AmmoCategory.Missile),
        TBOLT_10(50, "Thunderbolt 10", AmmoCategory.Missile),
        TBOLT_15(51, "Thunderbolt 15", AmmoCategory.Missile),
        TBOLT_20(52, "Thunderbolt 20", AmmoCategory.Missile),
        NAC(53, "Naval Autocannon", AmmoCategory.Ballistic),
        LIGHT_NGAUSS(54, "Light Naval Gauss", AmmoCategory.Ballistic),
        MED_NGAUSS(55, "Medium Naval Gauss", AmmoCategory.Ballistic),
        HEAVY_NGAUSS(56, "Heavy Naval Gauss", AmmoCategory.Ballistic),
        KILLER_WHALE(57, "Killer Whale", AmmoCategory.Missile),
        WHITE_SHARK(58, "White Shark", AmmoCategory.Missile),
        BARRACUDA(59, "Barracuda", AmmoCategory.Missile),
        KRAKEN_T(60, "Kraken-T", AmmoCategory.Missile),
        AR10(61, "AR10", AmmoCategory.Missile),
        SCREEN_LAUNCHER(62, "Screen Launcher", AmmoCategory.Special),
        ALAMO(63, "Alamo", AmmoCategory.Missile),
        IGAUSS_HEAVY(64, "Heavy iGauss", AmmoCategory.Ballistic),
        CHEMICAL_LASER(65, "Chemical Laser", AmmoCategory.Energy),
        HYPER_VELOCITY(66, "Hyper Velocity AC", AmmoCategory.Ballistic),
        MEK_MORTAR(67, "Mek Mortar", AmmoCategory.Artillery),
        CRUISE_MISSILE(68, "Cruise Missile", AmmoCategory.Missile),
        BPOD(69, "B-Pod", AmmoCategory.Special),
        SCC(70, "Sub-Capital Cannon", AmmoCategory.Ballistic),
        MANTA_RAY(71, "Manta Ray", AmmoCategory.Missile),
        SWORDFISH(72, "Swordfish", AmmoCategory.Missile),
        STINGRAY(73, "Stingray", AmmoCategory.Missile),
        PIRANHA(74, "Piranha", AmmoCategory.Missile),
        TASER(75, "Taser", AmmoCategory.Special),
        BOMB(76, "Bomb", AmmoCategory.Bomb),
        AAA_MISSILE(77, "AAA Missile", AmmoCategory.Missile),
        AS_MISSILE(78, "AS Missile", AmmoCategory.Missile),
        ASEW_MISSILE(79, "ASEW Missile", AmmoCategory.Missile),
        LAA_MISSILE(80, "LAA Missile", AmmoCategory.Missile),
        RL_BOMB(81, "RL Bomb", AmmoCategory.Bomb),
        ARROW_IV_BOMB(82, "Arrow IV Bomb", AmmoCategory.Bomb),
        FLUID_GUN(83, "Fluid Gun", AmmoCategory.Chemical),
        SNIPER_CANNON(84, "Sniper Cannon", AmmoCategory.Artillery),
        THUMPER_CANNON(85, "Thumper Cannon", AmmoCategory.Artillery),
        LONG_TOM_CANNON(86, "Long Tom Cannon", AmmoCategory.Artillery),
        NAIL_RIVET_GUN(87, "Nail/Rivet Gun", AmmoCategory.Ballistic),
        ACi(88, "ACi", AmmoCategory.Ballistic),
        KRAKENM(89, "Kraken-M", AmmoCategory.Missile),
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        PAC(90, "ProtoMech AC", AmmoCategory.Ballistic),
        NLRM(91, "NLRM", AmmoCategory.Missile),
        RIFLE(92, "Rifle", AmmoCategory.Ballistic),
        VGL(93, "Vehicle Grenade Launcher", AmmoCategory.Special),
        C3_REMOTE_SENSOR(94, "C3 Remote Sensor", AmmoCategory.Special),
        AC_PRIMITIVE(95, "Primitive Autocannon", AmmoCategory.Ballistic),
        LRM_PRIMITIVE(96, "Primitive LRM", AmmoCategory.Missile),
        SRM_PRIMITIVE(97, "Primitive SRM", AmmoCategory.Missile),
        BA_TUBE(98, "BA Tube Artillery", AmmoCategory.Artillery),
        IATM(99, "Improved ATM", AmmoCategory.Missile),
        LMASS(100, "Light Mass Driver", AmmoCategory.Ballistic),
        MMASS(101, "Medium Mass Driver", AmmoCategory.Ballistic),
        HMASS(102, "Heavy Mass Driver", AmmoCategory.Ballistic),
        APDS(103, "APDS", AmmoCategory.Ballistic),
        AC_IMP(104, "Improved Autocannon", AmmoCategory.Ballistic),
        GAUSS_IMP(105, "Improved Gauss", AmmoCategory.Ballistic),
        SRM_IMP(106, "Improved SRM", AmmoCategory.Missile),
        LRM_IMP(107, "Improved LRM", AmmoCategory.Missile),
        LONG_TOM_PRIM(108, "Primitive Long Tom", AmmoCategory.Artillery),
        ARROWIV_PROTO(109, "Prototype Arrow IV", AmmoCategory.Artillery),
        KILLER_WHALE_T(110, "Killer Whale-T", AmmoCategory.Missile),
        WHITE_SHARK_T(111, "White Shark-T", AmmoCategory.Missile),
        BARRACUDA_T(112, "Barracuda-T", AmmoCategory.Missile),
        INFANTRY(113, "Infantry", AmmoCategory.Special);

        private static final Map<Integer, AmmoTypeEnum> INDEX_LOOKUP = new HashMap<>();

        static {
            for (AmmoTypeEnum at : values()) {
                INDEX_LOOKUP.put(at.index, at);
            }
        }

        private final int index;
        private final String name;
        private final AmmoCategory category;

        AmmoTypeEnum(int index, String name, AmmoCategory category) {
            this.index = index;
            this.name = name;
            this.category = category;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public AmmoCategory getCategory() {
            return category;
        }

        /**
         * Gets the AmmoTypeEnum for the given integer value.
         *
         * @param index The integer value to convert
         *
         * @return The corresponding AmmoTypeEnum, or NA if not found
         */
        public static AmmoTypeEnum fromIndex(int index) {
            return INDEX_LOOKUP.getOrDefault(index, NA);
        }

        /**
         * @return True if this ammo type is any of the given types
         */
        public boolean isAnyOf(AmmoTypeEnum type, AmmoTypeEnum... otherTypes) {
            return (this == type) || Arrays.stream(otherTypes).anyMatch(t -> this == t);
        }
    }

    /**
     * Contains the {@code AmmoType}s that could share ammo (e.g., SRM 2 and SRM 6, both fire SRM rounds).
     */
    private static final AmmoTypeEnum[] ALLOWED_BY_TYPE_ARRAY = { AmmoTypeEnum.LRM, AmmoTypeEnum.LRM_PRIMITIVE,
                                                                  AmmoTypeEnum.LRM_STREAK, AmmoTypeEnum.LRM_TORPEDO,
                                                                  AmmoTypeEnum.LRM_TORPEDO_COMBO, AmmoTypeEnum.SRM,
                                                                  AmmoTypeEnum.SRM_ADVANCED, AmmoTypeEnum.SRM_PRIMITIVE,
                                                                  AmmoTypeEnum.SRM_STREAK, AmmoTypeEnum.SRM_TORPEDO,
                                                                  AmmoTypeEnum.MRM, AmmoTypeEnum.ROCKET_LAUNCHER,
                                                                  AmmoTypeEnum.EXLRM, AmmoTypeEnum.MML,
                                                                  AmmoTypeEnum.NLRM,
                                                                  AmmoTypeEnum.MG, AmmoTypeEnum.MG_LIGHT,
                                                                  AmmoTypeEnum.MG_HEAVY,
                                                                  AmmoTypeEnum.NAIL_RIVET_GUN, AmmoTypeEnum.ATM,
                                                                  AmmoTypeEnum.IATM };

    /**
     * Contains the set of {@code AmmoType}s which could share ammo (e.g., SRM 2 and SRM 6, both fire SRM rounds), and
     * conceptually can share ammo.
     */
    public static final Set<AmmoTypeEnum> ALLOWED_BY_TYPE = Set.of(ALLOWED_BY_TYPE_ARRAY);

    // ammo flags
    public static final AmmoTypeFlag F_MG = AmmoTypeFlag.F_MG;
    public static final AmmoTypeFlag F_BATTLEARMOR = AmmoTypeFlag.F_BATTLEARMOR; // only used by BA squads
    public static final AmmoTypeFlag F_PROTOMEK = AmmoTypeFlag.F_PROTOMEK; // only used by ProtoMeks
    public static final AmmoTypeFlag F_HOTLOAD = AmmoTypeFlag.F_HOT_LOAD; // Ammo can be hot-loaded

    // BA can't jump or make anti-mek until dumped
    public static final AmmoTypeFlag F_ENCUMBERING = AmmoTypeFlag.F_ENCUMBERING;

    public static final AmmoTypeFlag F_MML_LRM = AmmoTypeFlag.F_MML_LRM; // LRM type
    public static final AmmoTypeFlag F_AR10_WHITE_SHARK = AmmoTypeFlag.F_AR10_WHITE_SHARK; // White shark type
    public static final AmmoTypeFlag F_AR10_KILLER_WHALE = AmmoTypeFlag.F_AR10_KILLER_WHALE; // Killer Whale type
    public static final AmmoTypeFlag F_AR10_BARRACUDA = AmmoTypeFlag.F_AR10_BARRACUDA; // barracuda type
    public static final AmmoTypeFlag F_NUCLEAR = AmmoTypeFlag.F_NUCLEAR; // Nuclear missile
    public static final AmmoTypeFlag F_SANTA_ANNA = AmmoTypeFlag.F_SANTA_ANNA; // Santa Anna Missile
    public static final AmmoTypeFlag F_PEACEMAKER = AmmoTypeFlag.F_PEACEMAKER; // Peacemaker Missile
    public static final AmmoTypeFlag F_TELE_MISSILE = AmmoTypeFlag.F_TELE_MISSILE; // Tele-Missile
    public static final AmmoTypeFlag F_CAP_MISSILE = AmmoTypeFlag.F_CAP_MISSILE; // Other Capital-Missile
    public static final AmmoTypeFlag F_SPACE_BOMB = AmmoTypeFlag.F_SPACE_BOMB; // can be used to space bomb

    // can be used to ground bomb
    public static final AmmoTypeFlag F_GROUND_BOMB = AmmoTypeFlag.F_GROUND_BOMB;
    public static final AmmoTypeFlag F_MML_SRM = AmmoTypeFlag.F_MML_SRM; // SRM type

    // Numbers 14-15 out of order. See nuclear missiles, above

    // For tag, rl pods, missiles and the like
    public static final AmmoTypeFlag F_OTHER_BOMB = AmmoTypeFlag.F_OTHER_BOMB;

    // Used by MHQ for loading ammo bins
    public static final AmmoTypeFlag F_CRUISE_MISSILE = AmmoTypeFlag.F_CRUISE_MISSILE;

    // Used by MHQ for loading ammo bins
    public static final AmmoTypeFlag F_SCREEN = AmmoTypeFlag.F_SCREEN;

    // Used for Internal Bomb Bay bombs; to differentiate them from
    public static final AmmoTypeFlag F_INTERNAL_BOMB = AmmoTypeFlag.F_INTERNAL_BOMB;

    private static final MunitionMutator CLAN_MPM_MUNITION_MUTATOR = new MunitionMutator("(Clan) Multi-Purpose",
          1,
          Munitions.M_MULTI_PURPOSE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CGS)
                .setProductionFactions(Faction.CGS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "229, TW");

    private static final MunitionMutator INFERNO_MUNITION_MUTATOR = new MunitionMutator("Inferno",
          1,
          Munitions.M_INFERNO,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "231, TM");

    private static final MunitionMutator CLAN_TORPEDO_MUNITION_MUTATOR = new MunitionMutator("(Clan) Torpedo",
          1,
          Munitions.M_TORPEDO,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator TORPEDO_MUNITION_MUTATOR = new MunitionMutator("Torpedo",
          1,
          Munitions.M_TORPEDO,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator ACID_MUNITION_MUTATOR = new MunitionMutator("Acid",
          2,
          Munitions.M_AX_HEAD,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setISAdvancement(3053)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "367, TO");

    private static final MunitionMutator HEAT_SEEKING_MUNITION_MUTATOR = new MunitionMutator("Heat-Seeking",
          2,
          Munitions.M_HEAT_SEEKING,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.F)
                .setISAdvancement(2365, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "369, TO");

    private static final MunitionMutator SMOKE_MUNITION_MUTATOR = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE_WARHEAD,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "371, TO");

    // Tandem Charge Updated to alight with fluff text in TacOps.
    private static final MunitionMutator TANDEM_CHARGE_MUNITION_MUTATOR = new MunitionMutator("Tandem-Charge",
          2,
          Munitions.M_TANDEM_CHARGE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(2757, DATE_NONE, DATE_NONE, 2784, 3062)
                .setISApproximate(true, false, false, true, true)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.FS)
                .setReintroductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "372, TO");

    private static final MunitionMutator ANTI_TSM_MUNITION_MUTATOR = new MunitionMutator("Anti-TSM",
          1,
          Munitions.M_ANTI_TSM,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setISAdvancement(3026, 3027, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "104, IO");

    private static final MunitionMutator ARTEMIS_CAPABLE_MUNITION_MUTATOR = new MunitionMutator("Artemis-capable",
          1,
          Munitions.M_ARTEMIS_CAPABLE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setISAdvancement(2592, 2598, 3045, 2855, 3035)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator DEAD_FIRE_MUNITION_MUTATOR = new MunitionMutator("Dead-Fire",
          1,
          Munitions.M_DEAD_FIRE,
          new TechAdvancement(TechBase.IS).setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3052)
                .setPrototypeFactions(Faction.DC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "131, IO");

    private static final MunitionMutator FRAGMENTATION_MUNITION_MUTATOR = new MunitionMutator("Fragmentation",
          1,
          Munitions.M_FRAGMENTATION,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(2375, 2377, 3058, 2790, 3054)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator LISTEN_KILL_MUNITION_MUTATOR = new MunitionMutator("Listen-Kill",
          1,
          Munitions.M_LISTEN_KILL,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X)
                .setISAdvancement(3037, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "105, IO");

    private static final MunitionMutator MINE_CLEARANCE_MUNITION_MUTATOR = new MunitionMutator("Mine Clearance",
          1,
          Munitions.M_MINE_CLEARANCE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(3065, 3069, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "370, TO");

    private static final MunitionMutator NARC_CAPABLE_MUNITION_MUTATOR = new MunitionMutator("Narc-capable",
          1,
          Munitions.M_NARC_CAPABLE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setISAdvancement(2520, 2587, 3049, 2795, 3035)
                .setISApproximate(true, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "142, TW");

    private static final MunitionMutator CLAN_ACID_MUNITION_MUTATOR = new MunitionMutator("(Clan) Acid",
          2,
          Munitions.M_AX_HEAD,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setClanAdvancement(3053)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "367, TO");

    private static final MunitionMutator CLAN_HEAT_SEEKING_MUNITIONS_MUTATOR = new MunitionMutator("(Clan) Heat-Seeking",
          2,
          Munitions.M_HEAT_SEEKING,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.F)
                .setClanAdvancement(2365, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "369, TO");

    private static final MunitionMutator CLAN_INFERNO_MUNITION_MUTATOR = new MunitionMutator("(Clan) Inferno",
          1,
          Munitions.M_INFERNO,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "231, TM");

    private static final MunitionMutator CLAN_SMOKE_MUNITION_MUTATOR_ADVANCED_FOR_SRM = new MunitionMutator(
          "(Clan) Smoke",
          1,
          Munitions.M_SMOKE_WARHEAD,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "371, TO");

    private static final MunitionMutator CLAN_TANDEM_CHARGE_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "(Clan) Tandem-Charge",
          2,
          Munitions.M_TANDEM_CHARGE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(2757, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "372, TO");

    private static final MunitionMutator CLAN_ANTI_TSM_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "(Clan) Anti-TSM",
          1,
          Munitions.M_ANTI_TSM,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setClanAdvancement(3026, 3027, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "104, IO");

    private static final MunitionMutator CLAN_ARTEMIS_CAPABLE_MUNITION_MUTATOR_FOR_SRM = new MunitionMutator(
          "(Clan) Artemis-capable",
          1,
          Munitions.M_ARTEMIS_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 2818, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CSA)
                .setProductionFactions(Faction.CSA)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "207, TM");

    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    private static final MunitionMutator CLAN_ARTEMIS_V_CAPABLE_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Artemis V-capable",
          1,
          Munitions.M_ARTEMIS_V_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, 3061, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(Faction.CGS)
                .setProductionFactions(Faction.CSF, Faction.RD)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "283, TO");

    private static final MunitionMutator CLAN_DEAD_FIRE_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "(Clan) Dead-Fire",
          1,
          Munitions.M_DEAD_FIRE,
          new TechAdvancement(TechBase.CLAN).setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3052)
                .setPrototypeFactions(Faction.DC)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
          "131, IO");

    private static final MunitionMutator CLAN_FRAGMENTATION_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Fragmentation",
          1,
          Munitions.M_FRAGMENTATION,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setClanAdvancement(2375, 2377, 3058, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator CLAN_LISTEN_KILL_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "(Clan) Listen-Kill",
          1,
          Munitions.M_LISTEN_KILL,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
                .setClanAdvancement(3037, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
          "230, TM");

    private static final MunitionMutator CLAN_MINE_CLEARANCE_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "(Clan) Mine Clearance",
          1,
          Munitions.M_MINE_CLEARANCE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setClanAdvancement(3065, 3069, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "370, TO");

    private static final MunitionMutator CLAN_NARC_CAPABLE_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "(Clan) Narc-capable",
          1,
          Munitions.M_NARC_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "370, TO");

    private static final MunitionMutator FOLLOW_THE_LEADER_MUNITION_MUTATOR = new MunitionMutator("Follow The Leader",
          2,
          Munitions.M_FOLLOW_THE_LEADER,
          new TechAdvancement(TechBase.IS).setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
                .setISAdvancement(2750, DATE_NONE, DATE_NONE, 2770, DATE_NONE)
                .setISApproximate(true, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "368, TO");

    private static final MunitionMutator ARAD_MUNITION_MUTATOR = new MunitionMutator("Anti-Radiation",
          1,
          Munitions.M_ARAD,
          new TechAdvancement(TechBase.IS).setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.F)
                .setISAdvancement(3066, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "180, TO:AUE");

    private static final MunitionMutator SEMI_GUIDED_MUNITION_MUTATOR = new MunitionMutator("Semi-guided",
          1,
          Munitions.M_SEMIGUIDED,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(3053, 3057, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "231, TM");

    // Note of Swarms the intro dates in IntOps are off, and it allows Swarm-I to appear before Swarm during the Clan
    // Invasion. Proposed errata makes 3052 for Swarm-I a hard date, and 3053 for Swarm re-introduction a flexible
    // date.
    private static final MunitionMutator SWARM_MUNITION_MUTATOR = new MunitionMutator("Swarm",
          1,
          Munitions.M_SWARM,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2615, 2621, 3058, 2833, 3053)
                .setISApproximate(true, false, false, false, true)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "371, TO");

    private static final MunitionMutator SWARM_I_MUNITION_MUTATOR = new MunitionMutator("Swarm-I",
          1,
          Munitions.M_SWARM_I,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(3052, 3057, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "371, TO");

    private static final MunitionMutator THUNDER_MUNITION_MUTATOR = new MunitionMutator("Thunder",
          1,
          Munitions.M_THUNDER,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2618, 2620, 2650, 2840, 3052)
                .setISApproximate(true, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.LC, Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator THUNDER_ACTIVE_MUNITION_MUTATOR = new MunitionMutator("Thunder-Active",
          2,
          Munitions.M_THUNDER_ACTIVE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3054, 3058, 3064, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator THUNDER_AUGMENTED_MUNITION_MUTATOR = new MunitionMutator("Thunder-Augmented",
          2,
          Munitions.M_THUNDER_AUGMENTED,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3054, 3057, 3064, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator THUNDER_BIBRABOMB_MUNITION_MUTATOR = new MunitionMutator("Thunder-Vibrabomb",
          2,
          Munitions.M_THUNDER_VIBRABOMB,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3054, 3056, 3064, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator THUNDER_INFERNO_MUTATION_MUTATOR = new MunitionMutator("Thunder-Inferno",
          2,
          Munitions.M_THUNDER_INFERNO,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3054, 3056, 3062, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator FRAGMENTATION_MUNITION_MUTATOR_FOR_LRM = new MunitionMutator("Fragmentation",
          1,
          Munitions.M_FRAGMENTATION,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(2375, 2377, 3058, 2790, 3054)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setReintroductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator CLAN_FOLLOW_THE_LEADER_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Follow The Leader",
          2,
          Munitions.M_FOLLOW_THE_LEADER,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.X)
                .setClanAdvancement(2750, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "368, TO");

    private static final MunitionMutator CLAN_ARAD_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Anti-Radiation",
          1,
          Munitions.M_ARAD,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.F)
                .setClanAdvancement(3057, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CSJ)
                .setProductionFactions(Faction.CSJ)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "180, TO:AUE");

    private static final MunitionMutator CLAN_SEMI_GUIDED = new MunitionMutator("(Clan) Semi-guided",
          1,
          Munitions.M_SEMIGUIDED,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(3053, 3057, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "231, TM");

    private static final MunitionMutator CLAN_SMOKE_STANDARD_MUNITION_MUTATOR = new MunitionMutator("(Clan) Smoke",
          1,
          Munitions.M_SMOKE_WARHEAD,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "371, TO");

    private static final MunitionMutator CLAN_SWARM_MUNITION_MUTATOR = new MunitionMutator("(Clan) Swarm",
          1,
          Munitions.M_SWARM,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2615, 2621, 3058, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "371, TO");

    private static final MunitionMutator CLAN_SWARM_I_ADV_MUNITION_MUTATOR = new MunitionMutator("(Clan) Swarm-I",
          1,
          Munitions.M_SWARM_I,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(3052, 3057, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "371, TO");

    private static final MunitionMutator CLAN_THUNDER_ADV_MUNITION_MUTATOR = new MunitionMutator("(Clan) Thunder",
          1,
          Munitions.M_THUNDER,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2618, 2620, 2650, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.LC, Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator CLAN_THUNDER_ACTIVE_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Thunder-Active",
          2,
          Munitions.M_THUNDER_ACTIVE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3054, 3058, 3064, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator CLAN_THUNDER_AUGMENTED_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Thunder-Augmented",
          2,
          Munitions.M_THUNDER_AUGMENTED,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3054, 3057, 3064, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator CLAN_THUNDER_VIBRABOMB_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Thunder-Vibrabomb",
          2,
          Munitions.M_THUNDER_VIBRABOMB,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3054, 3056, 3064, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator CLAN_THUNDER_INFORMATION_MUNITION_MUTATOR = new MunitionMutator(
          "(Clan) Thunder-Inferno",
          2,
          Munitions.M_THUNDER_INFERNO,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3054, 3056, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator CLAN_ARTEMIS_CAPABLE_MUNITION_MUTATOR_FOR_LRM = new MunitionMutator(
          "(Clan) Artemis-capable",
          1,
          Munitions.M_ARTEMIS_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setClanAdvancement(2592, 2598, 3045, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "207, TM");

    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    private static final MunitionMutator CLAN_ARTEMIS_V_CAPABLE_MUNITION_MUTATOR_FOR_MISSILE_AND_TORPEDO = new MunitionMutator(
          "(Clan) Artemis V-capable",
          1,
          Munitions.M_ARTEMIS_V_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, 3061, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(Faction.CGS)
                .setProductionFactions(Faction.CSF, Faction.RD)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "283, TO");

    private static final MunitionMutator CLAN_DEAD_FIRE_MUNITION_MUTATOR = new MunitionMutator("(Clan) Dead-Fire",
          1,
          Munitions.M_DEAD_FIRE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3052, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.DC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "131, IO");

    private static final MunitionMutator ARMOR_PIERCING_MUNITION_MUTATOR = new MunitionMutator("Armor-Piercing",
          2,
          Munitions.M_ARMOR_PIERCING,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3055, 3059, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    // PLAYTEST3 AP ammo new weight
    private static final MunitionMutator ARMOR_PIERCING_PLAYTEST_MUNITION_MUTATOR = new MunitionMutator("Armor"
          + "-Piercing Playtest",
          (5.0/3),
          Munitions.M_ARMOR_PIERCING_PLAYTEST,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3055, 3059, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    private static final MunitionMutator CASELESS_MUNITION_MUTATOR = new MunitionMutator("Caseless",
          1,
          Munitions.M_CASELESS,
          new TechAdvancement(TechBase.ALL).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "352, TO");

    // PLAYTEST3 RAC can now use caseless.
    private static final MunitionMutator PLAYTEST_CASELESS_MUNITION_MUTATOR = new MunitionMutator("Playtest Caseless",
          1,
          Munitions.M_CASELESS,
          new TechAdvancement(TechBase.ALL).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "352, TO");

    private static final MunitionMutator FLAK_MUNITION_MUTATOR = new MunitionMutator("Flak",
          1,
          Munitions.M_FLAK,
          new TechAdvancement(TechBase.ALL).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
                .setAdvancement(DATE_ES, 2310, 3070, DATE_NONE, DATE_NONE)
                .setApproximate(false, false, true, false, false)
                .setProductionFactions(Faction.TA)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "352, TO");

    private static final MunitionMutator FLECHETTE_MUNITION_MUTATOR = new MunitionMutator("Flechette",
          1,
          Munitions.M_FLECHETTE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3053, 3055, 3058, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    private static final MunitionMutator PRECISION_MUNITION_MUTATOR = new MunitionMutator("Precision",
          2,
          Munitions.M_PRECISION,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3058, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    // PLAYTEST3 Precision ammo modifier
    private static final MunitionMutator PRECISION_PLAYTEST_MUNITION_MUTATOR = new MunitionMutator("Precision Playtest",
          (5.0/3),
          Munitions.M_PRECISION_PLAYTEST,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3058, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS)
                .setProductionFactions(Faction.FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    private static final MunitionMutator TRACER_MUNITION_MUTATOR = new MunitionMutator("Tracer",
          1,
          Munitions.M_TRACER,
          new TechAdvancement(TechBase.ALL).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E)
                .setISAdvancement(DATE_ES, 2300, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setProductionFactions(Faction.TA)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "353, TO");

    private static final MunitionMutator CLAN_IMPROVED_ARMOR_PIERCING_MUNITION_MUTATOR = new MunitionMutator(
          "Armor-Piercing",
          2,
          Munitions.M_ARMOR_PIERCING,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CLAN)
                .setProductionFactions(Faction.CLAN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    private static final MunitionMutator CLAN_IMPROVED_FLECHETTE_MUNITION_MUTATOR = new MunitionMutator("Flechette",
          1,
          Munitions.M_FLECHETTE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3105, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CLAN)
                .setProductionFactions(Faction.CLAN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    private static final MunitionMutator CLAN_IMPROVED_PRECISION_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "Precision",
          2,
          Munitions.M_PRECISION,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3053, 3055, 3058, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
          "208, TM");

    private static final MunitionMutator CLAN_IMPROVED_TRACER_MUNITION_MUTATOR = new MunitionMutator("Tracer",
          1,
          Munitions.M_TRACER,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                .setClanApproximate(false, true, false, true, false)
                .setPrototypeFactions(Faction.CLAN)
                .setProductionFactions(Faction.CLAN)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "353, TO");

    private static final MunitionMutator CLAN_ARMOR_PIERCING_MUNITION_MUTATOR_FOR_PROTO = new MunitionMutator(
          "Armor-Piercing",
          2,
          Munitions.M_ARMOR_PIERCING,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, 3095, 3105, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setProductionFactions(Faction.CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
    private static final MunitionMutator CLAN_FLECHETTE_MUNITION_MUTATOR_FOR_PROTO = new MunitionMutator("Flechette",
          1,
          Munitions.M_FLECHETTE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, 3095, 3105, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setProductionFactions(Faction.CHH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    private static final MunitionMutator CLAN_PRECISION_MUNITION_MUTATOR_FOR_PROTO = new MunitionMutator("Precision",
          2,
          Munitions.M_PRECISION,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(3070, 3073, 3145, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CBS)
                .setProductionFactions(Faction.CBS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "208, TM");

    private static final MunitionMutator CLAN_TRACER_MUNITION_MUTATOR_FOR_PROTO = new MunitionMutator("Tracer",
          1,
          Munitions.M_TRACER,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.F)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
                .setClanAdvancement(3070, 3073, 3145, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CBS)
                .setProductionFactions(Faction.CBS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "353, TO");

    private static final MunitionMutator ADA_MUNITION_MUTATOR = new MunitionMutator("Air-Defense Arrow (ADA) Missiles",
          1,
          Munitions.M_ADA,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
                .setPrototypeFactions(Faction.CC)
                .setISAdvancement(3068, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setApproximate(false, false, false, false, false)
                .setTechRating(TechRating.E)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "165, TO:AU&E");

    private static final MunitionMutator CLUSTER_MUNITION_MUTATOR = new MunitionMutator("Cluster",
          1,
          Munitions.M_CLUSTER,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(2594, 2600, DATE_NONE, 2830, 3047)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "354, TO");

    private static final MunitionMutator HOMING_MUNITION_MUTATOR = new MunitionMutator("Homing",
          1,
          Munitions.M_HOMING,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3045)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "354, TO");

    private static final MunitionMutator ILLUMINATION_MUNITION_MUTATOR = new MunitionMutator("Illumination",
          1,
          Munitions.M_FLARE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2615, 2621, DATE_NONE, 2800, 3047)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator INFERNO_IV_MUNITION_MUTATOR = new MunitionMutator("Inferno-IV",
          1,
          Munitions.M_INFERNO_IV,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator LASER_INHIBITING_MUNITION_MUTATOR = new MunitionMutator("Laser Inhibiting",
          1,
          Munitions.M_LASER_INHIB,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setISAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator SMOKE_MUNITION_MUTATOR_FOR_ARROW = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(2595, 2600, DATE_NONE, 2840, 3044)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "356, TO");

    private static final MunitionMutator THUNDER_FASCAM_MUNITION_MUTATOR = new MunitionMutator("Thunder (FASCAM)",
          1,
          Munitions.M_FASCAM,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2621, 2844, DATE_NONE, 2770, 3051)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.CHH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "356, TO");

    private static final MunitionMutator THUNDER_VIBRABOMB_IV_MUNITION_MUTATOR = new MunitionMutator(
          "Thunder Vibrabomb-IV",
          1,
          Munitions.M_VIBRABOMB_IV,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(3056, 3065, DATE_NONE, DATE_NONE, DATE_NONE)
                .setApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "357, TO");

    private static final MunitionMutator DAVY_CROCKETT_M_MUNITION_MUTATOR = new MunitionMutator("Davy Crockett-M",
          5,
          Munitions.M_DAVY_CROCKETT_M,
          new TechAdvancement(TechBase.IS).setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
                .setISAdvancement(2412, DATE_NONE, DATE_NONE, 2830, 3044)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "174, IO");

    private static final MunitionMutator FUEL_AIR_MUNITION_MUTATOR = new MunitionMutator("Fuel-Air",
          1,
          Munitions.M_FAE,
          new TechAdvancement(TechBase.ALL).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "165, IO");

    private static final MunitionMutator CLAN_ADA_MUNITION_MUTATOR = new MunitionMutator(
          "Air-Defense Arrow (ADA) Missiles",
          1,
          Munitions.M_ADA,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
                .setClanAdvancement(3068, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "165, TO:AU&E");

    private static final MunitionMutator CLAN_CLUSTER_MUNITION_MUTATOR_FOR_ARROW = new MunitionMutator("Cluster",
          1,
          Munitions.M_CLUSTER,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setClanAdvancement(2594, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "354, TO");

    private static final MunitionMutator CLAN_HOMING_MUNITION_MUTATOR = new MunitionMutator("Homing",
          1,
          Munitions.M_HOMING,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setClanAdvancement(2593, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "354, TO");

    private static final MunitionMutator CLAN_ILLUMINATION_MUNITION_MUTATOR = new MunitionMutator("Illumination",
          1,
          Munitions.M_FLARE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2615, 2621, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator CLAN_INFERNO_IV_MUNITION_MUTATOR = new MunitionMutator("Inferno-IV",
          1,
          Munitions.M_INFERNO_IV,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator CLAN_LASER_INHIBITING_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator(
          "Laser Inhibiting",
          1,
          Munitions.M_LASER_INHIB,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
                .setClanAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.FS, Faction.LC)
                .setProductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
          "355, TO");

    private static final MunitionMutator CLAN_SMOKE_MUNITION_MUTATOR_FOR_ARROW_IV = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setClanAdvancement(2595, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "356, TO");

    private static final MunitionMutator CLAN_THUNDER_FASCAM_MUNITION_MUTATOR = new MunitionMutator("Thunder (FASCAM)",
          1,
          Munitions.M_FASCAM,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
                .setClanAdvancement(2621, 2844, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.CHH)
                .setReintroductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "356, TO");

    private static final MunitionMutator CLAN_THUNDER_VIBRABOMB_IV_MUNITION_MUTATOR = new MunitionMutator(
          "Thunder Vibrabomb-IV",
          1,
          Munitions.M_VIBRABOMB_IV,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.D)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(3056, 3065, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.CC)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "357, TO");

    private static final MunitionMutator CLAN_CHAFF_VEE_MUNITION_MUTATOR = new MunitionMutator("Chaff",
          1,
          Munitions.M_CHAFF,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E)
                .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(Faction.CLAN)
                .setProductionFactions(Faction.CLAN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "363, TO");

    private static final MunitionMutator CLAN_INDENCIARY_VEE_MUNITION_MUTATOR = new MunitionMutator("Incendiary",
          1,
          Munitions.M_INCENDIARY,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(Faction.CLAN)
                .setProductionFactions(Faction.CLAN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "364, TO");

    private static final MunitionMutator CLAN_SMOKE_VEE_MUNITION_MUTATOR = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(Faction.CLAN)
                .setProductionFactions(Faction.CLAN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "364, TO");

    private static final MunitionMutator CHAFF_VEE_MUNITION_MUTATOR = new MunitionMutator("Chaff",
          1,
          Munitions.M_CHAFF,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "363, TO");

    private static final MunitionMutator INCENDIARY_VEE_MUNITION_MUTATOR = new MunitionMutator("Incendiary",
          1,
          Munitions.M_INCENDIARY,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "363, TO");

    private static final MunitionMutator SMOKE_VEE_MUNITION_MUTATOR = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "363, TO");

    private static final MunitionMutator CLUSTER_ARTY_MUNITION_MUTATOR = new MunitionMutator("Cluster",
          1,
          Munitions.M_CLUSTER,
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "354, TO");

    private static final MunitionMutator COPPERHEAD_ARTY_MUNITION_MUTATOR = new MunitionMutator("Copperhead",
          1,
          Munitions.M_HOMING,
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(2640, 2645, DATE_NONE, 2800, 3051)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "354, TO");

    private static final MunitionMutator FASCAM_ARTY_MUNITION_MUTATOR = new MunitionMutator("FASCAM",
          1,
          Munitions.M_FASCAM,
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(2621, 2844, DATE_NONE, 2770, 3051)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator FLECHETTE_ARTY_MUNITION_MUTATOR = new MunitionMutator("Flechette",
          1,
          Munitions.M_FLECHETTE,
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator ILLUMINATION_ARTY_MUNITION_MUTATOR = new MunitionMutator("Illumination",
          1,
          Munitions.M_FLARE,
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator SMOKE_ARTY_MUNITION_MUTATOR = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE,
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "355, TO");

    private static final MunitionMutator FUEL_AIR_ARTY_MUNITION_MUTATOR_UNOFFICIAL = new MunitionMutator("Fuel-Air",
          1,
          Munitions.M_FAE,
          new TechAdvancement(TechBase.ALL).setIntroLevel(false)
                .setUnofficial(false) // Should be marked true here
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          // Or marked unnoficial here
          "159, IO");

    private static final MunitionMutator SMOKE_MUNITION_MUTATOR_FOR_BA_TUBE = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
                .setISAdvancement(3070, 3075, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.CS)
                .setProductionFactions(Faction.CS),
          "375, TO");

    private static final MunitionMutator CLAN_ARTEMIS_CAPABLE_MUNTION_MUTATOR_FOR_TORPEDO = new MunitionMutator(
          "Artemis-capable",
          1,
          Munitions.M_ARTEMIS_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setClanAdvancement(2592, 2598, 3045, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "230, TM");

    private static final MunitionMutator COOLANT_MUNITION_MUTATOR = new MunitionMutator("Coolant",
          1,
          Munitions.M_COOLANT,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "360, TO");

    private static final MunitionMutator CLAN_COOLANT_MUNITION_MUTATOR = new MunitionMutator("(Clan) Coolant",
          1,
          Munitions.M_COOLANT,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "360, TO");

    private static final MunitionMutator COOLANT_MUNITION_MUTATOR_FOR_HEAVY_FLAMER = new MunitionMutator("Coolant",
          1,
          Munitions.M_COOLANT,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "360, TO");

    private static final MunitionMutator CLAN_COOLANT_MUNITION_MUTATOR_FOR_HEAVY_FLAMER = new MunitionMutator(
          "(Clan) Coolant",
          1,
          Munitions.M_COOLANT,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "360, TO");

    private static final MunitionMutator CLAN_NARC_CAPABLE_MUNITION_MUTATOR_FOR_MISSILE = new MunitionMutator(
          "(Clan) Narc-capable",
          1,
          Munitions.M_NARC_CAPABLE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.E)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
                .setClanAdvancement(2520, 2587, 3049, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD),
          "142, TW");

    private static final MunitionMutator CLAN_SMOKE_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE_WARHEAD,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
                .setClanAdvancement(2526, 2531, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "375, TO");

    private static final MunitionMutator CLAN_SEMI_GUIDED_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Semi-Guided",
          1,
          Munitions.M_SEMIGUIDED,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setClanAdvancement(3055, 3064, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "374, TO");

    private static final MunitionMutator CLAN_FLARE_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Flare",
          1,
          Munitions.M_FLARE,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
                .setClanAdvancement(2533, 2536, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "374, TO");

    private static final MunitionMutator CLAN_ANTI_PERSONNEL_MORTAR_MUNITION_MUTATOR = new MunitionMutator(
          "Anti-personnel",
          1,
          Munitions.M_ANTI_PERSONNEL,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setClanAdvancement(2540, 2544, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator CLAN_AIRBURST_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Airburst",
          1,
          Munitions.M_AIRBURST,
          new TechAdvancement(TechBase.CLAN).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.D)
                .setClanAdvancement(2540, 2544, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator SMOKE_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Smoke",
          1,
          Munitions.M_SMOKE_WARHEAD,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
                .setISAdvancement(2526, 2531, DATE_NONE, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "375, TO");

    private static final MunitionMutator SEMI_GUIDED_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Semi-Guided",
          1,
          Munitions.M_SEMIGUIDED,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(3055, 3064, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.FW)
                .setProductionFactions(Faction.FW)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "374, TO");

    private static final MunitionMutator FLARE_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Flare",
          1,
          Munitions.M_FLARE,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
                .setISAdvancement(2533, 2536, DATE_NONE, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "374, TO");

    private static final MunitionMutator ANTI_PERSONNEL_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Anti-personnel",
          1,
          Munitions.M_ANTI_PERSONNEL,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.B)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
                .setISAdvancement(2526, 2531, 3052, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setReintroductionFactions(Faction.FS, Faction.LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    private static final MunitionMutator AIRBURST_MORTAR_MUNITION_MUTATOR = new MunitionMutator("Airburst",
          1,
          Munitions.M_AIRBURST,
          new TechAdvancement(TechBase.IS).setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.D)
                .setISAdvancement(2540, 2544, DATE_NONE, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.TH)
                .setProductionFactions(Faction.TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED),
          "373, TO");

    // ammo munitions, used for custom load outs
    // N.B. We use EnumSet<Munitions> allow "incendiary"
    // to be combined to other munition types.
    public enum Munitions {
        M_STANDARD,

        // AC Munition Types
        M_CLUSTER,
        M_ARMOR_PIERCING,
        M_ARMOR_PIERCING_PLAYTEST,
        M_FLECHETTE,
        M_INCENDIARY_AC,
        M_PRECISION,
        M_PRECISION_PLAYTEST,
        M_TRACER,
        M_FLAK,
        M_CASELESS,

        // ATM Munition Types
        M_EXTENDED_RANGE,
        M_HIGH_EXPLOSIVE,
        M_IATM_IMP,
        M_IATM_IIW,

        // LRM & SRM Munition Types
        M_FRAGMENTATION,
        M_LISTEN_KILL,
        M_ANTI_TSM,
        M_NARC_CAPABLE,
        M_ARTEMIS_CAPABLE,
        M_DEAD_FIRE,
        M_HEAT_SEEKING,
        M_TANDEM_CHARGE,
        M_ARTEMIS_V_CAPABLE,
        M_SMOKE_WARHEAD,
        // Mine Clearance munition type defined later, to maintain order

        // LRM Munition Types
        // Incendiary is special, though...
        // FIXME - I'm not implemented!!!
        M_INCENDIARY_LRM,
        M_FLARE,
        M_SEMIGUIDED,
        M_SWARM,
        M_SWARM_I,
        M_THUNDER,
        M_THUNDER_AUGMENTED,
        M_THUNDER_INFERNO,
        M_THUNDER_VIBRABOMB,
        M_THUNDER_ACTIVE,
        M_FOLLOW_THE_LEADER,
        M_ARAD,
        M_MULTI_PURPOSE,

        // SRM Munition Types
        // TODO: Inferno should be available to fluid guns and vehicle flamers
        // TO page 362
        M_INFERNO,
        M_AX_HEAD,
        // HARPOON

        // SRM, MRM and LRM
        M_TORPEDO,

        // iNarc Munition Types
        M_NARC_EX,
        M_ECM,
        M_HAYWIRE,
        M_NEMESIS,

        M_EXPLOSIVE,

        // Arrow IV Munition Types
        M_HOMING,
        M_FASCAM,
        M_INFERNO_IV,
        M_VIBRABOMB_IV,
        M_ADA,

        // M_ACTIVE_IV
        M_SMOKE,
        M_LASER_INHIB,

        // Nuclear Munitions
        M_DAVY_CROCKETT_M,
        // M_SANTA_ANNA,

        // fluid gun
        // TODO: implement all of these except coolant
        // water should also be used for vehicle flamers
        // TO page 361-363
        M_WATER,
        M_PAINT_OBSCURANT,
        M_OIL_SLICK,
        M_ANTI_FLAME_FOAM,
        M_CORROSIVE,
        M_COOLANT,

        // vehicular grenade launcher
        M_CHAFF,
        M_INCENDIARY,
        // Number 56 was M_SMOKEGRENADE, but that has now been merged with M_SMOKE

        // Number 57 is used for iATMs IMP ammo in the ATM section above.
        // and 58 for IIW

        // Mek mortar munitions
        M_AIRBURST,
        M_ANTI_PERSONNEL,
        // The rest were already defined
        // Flare
        // Semi-guided
        // Smoke

        // More SRM+LRM Munitions types
        M_MINE_CLEARANCE,

        // note that 62 is in use above
        // this area is a primary target for the introduction of an enum or some other
        // kind of refactoring
        M_FAE
    }

    public static final EnumSet<AmmoType.Munitions> SMOKE_MUNITIONS = EnumSet.of(AmmoType.Munitions.M_SMOKE,
          AmmoType.Munitions.M_SMOKE_WARHEAD);
    public static final EnumSet<AmmoType.Munitions> FLARE_MUNITIONS = EnumSet.of(AmmoType.Munitions.M_FLARE);
    public static final EnumSet<AmmoType.Munitions> MINE_MUNITIONS = EnumSet.of(AmmoType.Munitions.M_THUNDER,
          AmmoType.Munitions.M_THUNDER_ACTIVE,
          AmmoType.Munitions.M_THUNDER_AUGMENTED,
          AmmoType.Munitions.M_THUNDER_INFERNO,
          AmmoType.Munitions.M_THUNDER_VIBRABOMB,
          AmmoType.Munitions.M_FASCAM);

    private static final EnumMap<AmmoTypeEnum, Vector<AmmoType>> m_vaMunitions = new EnumMap<>(AmmoTypeEnum.class);

    public static Vector<AmmoType> getMunitionsFor(AmmoTypeEnum ammoType) {
        return m_vaMunitions.get(ammoType);
    }

    protected int damagePerShot;
    protected int rackSize;
    protected AmmoTypeEnum ammoType;
    protected EnumSet<Munitions> munitionType = EnumSet.of(Munitions.M_STANDARD);
    protected int shots;
    private double kgPerShot = -1;

    // ratio for capital ammo
    private double ammoRatio;
    private String subMunitionName = "";

    // Reference to the base ammo type, if any
    protected AmmoType base = null;

    // Collate artillery / artillery cannon types for flak check
    // Add ADA here when implemented
    private final AmmoTypeEnum[] ARTILLERY_TYPES = { AmmoTypeEnum.LONG_TOM, AmmoTypeEnum.SNIPER, AmmoTypeEnum.THUMPER,
                                                     AmmoTypeEnum.ARROW_IV };

    private final AmmoTypeEnum[] ARTILLERY_CANNON_TYPES = { AmmoTypeEnum.LONG_TOM_CANNON, AmmoTypeEnum.SNIPER_CANNON,
                                                            AmmoTypeEnum.THUMPER_CANNON };

    private final EnumSet<Munitions> ARTILLERY_FLAK_MUNITIONS = EnumSet.of(Munitions.M_CLUSTER, Munitions.M_STANDARD);

    public static final Map<String, Integer> blastRadius;

    static {
        blastRadius = new HashMap<>();
    }

    public AmmoType() {
        criticalSlots = 1;
        tankSlots = 0;
        tonnage = 1.0f;
        explosive = true;
        instantModeSwitch = false;
        ammoRatio = 0;
    }

    /**
     * Returns the base ammo type, if any.
     */
    public AmmoType getBaseAmmo() {
        return base;
    }

    /**
     * When comparing <code>AmmoType</code>s, look at the ammoType only.
     *
     * @param other the <code>Object</code> to compare to this one.
     *
     * @return <code>true</code> if the other is an <code>AmmoType</code> object of
     *       the same <code>ammoType</code> as this object. N.B. different munition types are still equal.
     */
    public boolean equalsAmmoTypeOnly(Object other) {
        if (!(other instanceof AmmoType otherAmmoType)) {
            return false;
        }

        // There a couple of flags that need to be checked before we check on getAmmoType() strictly.
        if (is(AmmoTypeEnum.MML)) {
            if (hasFlag(F_MML_LRM) != otherAmmoType.hasFlag(F_MML_LRM)) {
                return false;
            }
        }

        if (is(AmmoTypeEnum.AR10)) {
            if (hasFlag(F_AR10_BARRACUDA) != otherAmmoType.hasFlag(F_AR10_BARRACUDA)) {
                return false;
            }
            if (hasFlag(F_AR10_WHITE_SHARK) != otherAmmoType.hasFlag(F_AR10_WHITE_SHARK)) {
                return false;
            }
            if (hasFlag(F_AR10_KILLER_WHALE) != otherAmmoType.hasFlag(F_AR10_KILLER_WHALE)) {
                return false;
            }
            if (hasFlag(F_NUCLEAR) != otherAmmoType.hasFlag(F_NUCLEAR)) {
                return false;
            }
        }

        return is(otherAmmoType.getAmmoType());
    }

    /**
     * Gets a value indicating whether this {@code AmmoType} is compatible with another {@code AmmoType}.
     * <p>
     * this roughly means the same ammo type and munition type, but not rack size.
     * </p>
     *
     * @param other The other {@code AmmoType} to determine compatibility with.
     */
    @SuppressWarnings("unused")
    public boolean isCompatibleWith(AmmoType other) {
        if (other == null) {
            return false;
        }

        // If it isn't an allowed type, then nope!
        if (!ALLOWED_BY_TYPE.contains(getAmmoType()) || !ALLOWED_BY_TYPE.contains(other.getAmmoType())) {
            return false;
        }

        // MML Launchers, ugh.
        if ((is(AmmoTypeEnum.MML) || other.is(AmmoTypeEnum.MML))
              && (getMunitionType().equals(other.getMunitionType()))) {
            // LRMs...
            if (is(AmmoTypeEnum.MML) && hasFlag(F_MML_LRM) && other.is(AmmoTypeEnum.LRM)) {
                return true;
            } else if (other.is(AmmoTypeEnum.MML) && other.hasFlag(AmmoType.F_MML_LRM) && is(AmmoTypeEnum.LRM)) {
                return true;
            }

            // SRMs
            if (is(AmmoTypeEnum.MML) && !hasFlag(AmmoType.F_MML_LRM) && is(AmmoTypeEnum.SRM)) {
                return true;
            } else if (other.is(AmmoTypeEnum.MML) && !other.hasFlag(AmmoType.F_MML_LRM) && is(AmmoTypeEnum.SRM)) {
                return true;
            }
        }

        // ATM Launchers
        if (((is(AmmoTypeEnum.ATM) && other.is(AmmoTypeEnum.IATM)) || (is(AmmoTypeEnum.IATM)
              && other.is(AmmoTypeEnum.ATM))) &&
              (getMunitionType() == other.getMunitionType())) {
            // Ammo exclusive to iATMs couldn't have the same munition type as standard ATMs
            return true;
        }

        // General Launchers
        return is(other.getAmmoType()) && (getMunitionType().equals(other.getMunitionType()));
    }

    public AmmoTypeEnum getAmmoType() {
        return ammoType;
    }

    public int getToHitModifier() {
        return toHitModifier;
    }

    @Override
    public boolean hasFlag(EquipmentFlag flag) {
        if (flag instanceof AmmoTypeFlag) {
            return super.hasFlag(flag);
        } else {
            LOGGER.warn("Incorrect flag check: tested {} instead of AmmoTypeFlag",
                  flag.getClass().getSimpleName(),
                  new Throwable("Incorrect flag tested " +
                        flag.getClass().getSimpleName() +
                        " instead of " +
                        "AmmoTypeFlag"));
            return false;
        }
    }

    /**
     * Analog to WeaponType.getFireTNRoll(), but based on munitions. See TO:AR pg 42
     *
     * @return TN Roll
     */
    public int getFireTN() {
        if (munitionType.contains(Munitions.M_INFERNO)) {
            return TargetRoll.AUTOMATIC_SUCCESS;
        } else if (EnumSet.of(Munitions.M_INCENDIARY, Munitions.M_INCENDIARY_AC, Munitions.M_INCENDIARY_LRM)
              .containsAll(munitionType)) {
            return 5;
        } else {
            return 9;
        }
    }

    /**
     * Gets a value indicating whether this is a certain ammo type.
     *
     * @param ammoType The ammo type to compare against.
     */
    public boolean is(AmmoTypeEnum ammoType) {
        return getAmmoType() == ammoType;
    }

    /**
     * Checks if this ammo can be intercepted by AMS (or PD).
     * TODO: rules need to be checked
     *
     * @param amsWeapon   The AMS weapon to check against, if null an AMS is assumed to be available.
     * @param gameOptions The game options, used to check for special rules. If null, standard rules are assumed.
     *
     * @return true if this ammo is affected by AMS, false otherwise
     */
    public boolean canBeInterceptedBy(@Nullable WeaponMounted amsWeapon, @Nullable GameOptions gameOptions) {
        // Arrow IV Missiles can be affected by AMS/PD BAY, but only with Advanced Point Defense rules in space combat.
        if (((this.getAmmoType() == AmmoTypeEnum.ARROW_IV_BOMB)
              || (this.getAmmoType() == AmmoTypeEnum.ARROW_IV)
              || (this.getAmmoType() == AmmoTypeEnum.ARROWIV_PROTO))
              && (gameOptions != null)
              && (amsWeapon != null)
              && (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE))
              && (amsWeapon.getType().hasFlag(WeaponType.F_AMS_BAY)
              || (amsWeapon.getType().hasFlag(WeaponType.F_PD_BAY) && amsWeapon.hasModes() && amsWeapon.curMode()
              .equals(Weapon.MODE_POINT_DEFENSE)))) {
            return true;
        }
        // Only missile category ammo can be affected by AMS
        if (this.getAmmoType().getCategory() != AmmoCategory.Missile) {
            return false;
        }
        // Capital missiles require AMS Bay to counter
        if (this.capital) {
            // Only with Advanced Point Defense rules AMS Bay can counter capital missiles. Standard rules don't (TW, p130)
            if ((gameOptions == null)
                  || !gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE)) {
                return false;
            }
            return (amsWeapon != null)
                  && (amsWeapon.getType().hasFlag(WeaponType.F_AMS_BAY)
                  || (amsWeapon.getType().hasFlag(WeaponType.F_PD_BAY) && amsWeapon.hasModes() && amsWeapon.curMode()
                  .equals(Weapon.MODE_POINT_DEFENSE)));

        }
        // Standard missiles can be countered by regular AMS or AMS Bay
        // If no specific AMS weapon provided, assume any AMS can affect it
        if (amsWeapon == null) {
            return true;
        }
        // Check if the weapon has AMS capabilities
        if (amsWeapon.getType().hasFlag(WeaponType.F_AMS)
              || amsWeapon.getType().hasFlag(WeaponType.F_AMS_BAY)
              || (amsWeapon.getType().hasFlag(WeaponType.F_PD_BAY) && amsWeapon.hasModes() && amsWeapon.curMode()
              .equals(Weapon.MODE_POINT_DEFENSE))) {
            return true;
        }
        // If the weapon is not an AMS or AMS Bay, it cannot intercept this ammo
        return false;
    }

    /**
     * We need a way to quickly determine if a given ammo type / munition counts as "Flak"
     * <p>
     * Note, not _is_ Flak (as in the case of M_FLAK) but can be considered Flak by TW/TO/IO rules.
     * <p>
     * Arrow IV missiles with M_CLUSTER, M_ADA, or M_STANDARD (not M_HOMING) count as Flak (TO:AU&amp;E pp166-167, 224)
     *
     * @return counts true if this ammo can be considered Flak in some situations
     */
    public boolean countsAsFlak() {
        boolean counts = false;

        if (ArrayUtils.contains(ARTILLERY_TYPES, this.getAmmoType())) {
            // Air-Defense Arrow IV _is_ Flak, but is _not_ Artillery
            counts = ARTILLERY_FLAK_MUNITIONS.containsAll(this.getMunitionType()) ||
                  this.getMunitionType().contains(Munitions.M_ADA);
        } else if (ArrayUtils.contains(ARTILLERY_CANNON_TYPES, this.getAmmoType())) {
            counts = this.getMunitionType().contains(Munitions.M_STANDARD);
        }
        return counts;
    }

    public EnumSet<Munitions> getMunitionType() {
        return EnumSet.copyOf(munitionType);
    }

    /**
     * @return true if this munition type is not allowed by clan rules
     *
     * @deprecated since 0.50.06, the rules that don't allow some ammo for clan units should be based on tech
     *       access/availability, not a hardcoded list of ammo types. As soon as the tech rules for munition variations
     *       are working completely then this will be removed.
     */
    @Deprecated(since = "0.50.06")
    public boolean notAllowedByClanRules() {
        return (munitionType.contains(AmmoType.Munitions.M_SEMIGUIDED) ||
              (munitionType.contains(AmmoType.Munitions.M_SWARM_I)) ||
              (munitionType.contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)) ||
              (munitionType.contains(AmmoType.Munitions.M_THUNDER_INFERNO)) ||
              (munitionType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) ||
              (munitionType.contains(AmmoType.Munitions.M_THUNDER_ACTIVE)) ||
              (munitionType.contains(AmmoType.Munitions.M_INFERNO_IV)) ||
              (munitionType.contains(AmmoType.Munitions.M_VIBRABOMB_IV)) ||
              (munitionType.contains(AmmoType.Munitions.M_LISTEN_KILL)) ||
              (munitionType.contains(AmmoType.Munitions.M_ANTI_TSM)) ||
              (munitionType.contains(AmmoType.Munitions.M_DEAD_FIRE)) ||
              (munitionType.contains(AmmoType.Munitions.M_MINE_CLEARANCE)));
    }

    protected int heat;
    protected RangeType range;
    protected int tech;
    protected boolean capital = false;

    public int getDamagePerShot() {
        return damagePerShot;
    }

    public int getRackSize() {
        return rackSize;
    }

    public int getShots() {
        return shots;
    }

    public double getAmmoRatio() {
        return ammoRatio;
    }

    public boolean isCapital() {
        return capital;
    }

    public boolean hasCustomKgPerShot() {
        return kgPerShot > 0;
    }

    /**
     * Used by units that are constructed using per-shot weights (BA and ProtoMeks). Some ammo is defined in the rules
     * rounded to a set number of decimal places.
     *
     * @return KG per Shot
     */
    public double getKgPerShot() {
        /*
         * kgPerShot is initialized to -1. Some ammo types are set by the rules to round
         * to a certain number of decimal places and can do that by setting the
         * kgPerShot field.
         *
         * For those that are not set we calculate it.
         */
        if (kgPerShot < 0) {
            return 1000.0 / shots;
        }
        return kgPerShot;
    }

    /**
     * Aerospace units cannot use specialty munitions except Artemis and LBX cluster (but not standard). ATM ER and HE
     * rounds are considered standard munitions. AR10 missiles are designed for aerospace units and all munition types
     * are available.
     *
     * @return true if the munition can be used by aerospace units
     */
    public boolean canAeroUse() {
        return switch (ammoType) {
            case AC_LBX, SBGAUSS -> munitionType.contains(Munitions.M_CLUSTER);
            case ATM, IATM -> (munitionType.contains(Munitions.M_STANDARD)) ||
                  (munitionType.contains(Munitions.M_HIGH_EXPLOSIVE)) ||
                  (munitionType.contains(Munitions.M_EXTENDED_RANGE));
            case AR10 -> true;
            default -> (munitionType.contains(Munitions.M_STANDARD)) ||
                  (munitionType.contains(Munitions.M_ARTEMIS_CAPABLE)) ||
                  (munitionType.contains(Munitions.M_ARTEMIS_V_CAPABLE));
        };
    }

    /**
     * Aerospace units cannot use specialty munitions except Artemis and LBX cluster (but not standard). ATM ER and HE
     * rounds are considered standard munitions. AR10 missiles are designed for aerospace units and all munition types
     * are available.
     *
     * @param option True if unofficial game option allowing alternate munitions for artillery bays is enabled
     *
     * @return true if the munition can be used by aerospace units
     */
    public boolean canAeroUse(boolean option) {
        if (option) {
            return switch (ammoType) {
                case AC_LBX, SBGAUSS -> munitionType.contains(Munitions.M_CLUSTER);
                case ATM, IATM -> (munitionType.contains(Munitions.M_STANDARD)) ||
                      (munitionType.contains(Munitions.M_HIGH_EXPLOSIVE)) ||
                      (munitionType.contains(Munitions.M_EXTENDED_RANGE));
                case AR10 -> true;
                case ARROW_IV -> (munitionType.contains(Munitions.M_FLARE)) ||
                      (munitionType.contains(Munitions.M_CLUSTER)) ||
                      (munitionType.contains(Munitions.M_HOMING)) ||
                      (munitionType.contains(Munitions.M_INFERNO_IV)) ||
                      (munitionType.contains(Munitions.M_LASER_INHIB)) ||
                      (munitionType.contains(Munitions.M_SMOKE)) ||
                      (munitionType.contains(Munitions.M_FASCAM)) ||
                      (munitionType.contains(Munitions.M_DAVY_CROCKETT_M)) ||
                      (munitionType.contains(Munitions.M_VIBRABOMB_IV)) ||
                      (munitionType.contains(Munitions.M_STANDARD));
                case LONG_TOM -> (munitionType.contains(Munitions.M_FLARE)) ||
                      (munitionType.contains(Munitions.M_CLUSTER)) ||
                      (munitionType.contains(Munitions.M_HOMING)) ||
                      (munitionType.contains(Munitions.M_FLECHETTE)) ||
                      (munitionType.contains(Munitions.M_SMOKE)) ||
                      (munitionType.contains(Munitions.M_FASCAM)) ||
                      (munitionType.contains(Munitions.M_DAVY_CROCKETT_M)) ||
                      (munitionType.contains(Munitions.M_STANDARD));
                case SNIPER, THUMPER -> (munitionType.contains(Munitions.M_FLARE)) ||
                      (munitionType.contains(Munitions.M_CLUSTER)) ||
                      (munitionType.contains(Munitions.M_HOMING)) ||
                      (munitionType.contains(Munitions.M_FLECHETTE)) ||
                      (munitionType.contains(Munitions.M_SMOKE)) ||
                      (munitionType.contains(Munitions.M_FASCAM)) ||
                      (munitionType.contains(Munitions.M_STANDARD));
                default -> (munitionType.contains(Munitions.M_STANDARD)) ||
                      (munitionType.contains(Munitions.M_ARTEMIS_CAPABLE)) ||
                      (munitionType.contains(Munitions.M_ARTEMIS_V_CAPABLE));
            };
        } else {
            return canAeroUse();
        }
    }

    /**
     * @param mounted {@link Mounted} Weapon/Item
     *
     * @return the first usable ammo type for the given one shot launcher
     */
    public static @Nullable AmmoType getOneshotAmmo(Mounted<?> mounted) {
        WeaponType weaponType = (WeaponType) mounted.getType();
        if (weaponType.getAmmoType() == AmmoTypeEnum.NA) {
            return null;
        }
        Vector<AmmoType> vAmmo = AmmoType.getMunitionsFor(weaponType.getAmmoType());
        int techLevelYear = mounted.getEntity().getTechLevelYear();
        int techLevel = mounted.getType().getTechLevel(techLevelYear);
        boolean mixedTech = mounted.getEntity().isMixedTech();
        AmmoType ammoType;
        for (int i = 0; i < vAmmo.size(); i++) {
            ammoType = vAmmo.elementAt(i);
            if ((ammoType.getRackSize() == weaponType.getRackSize()) &&
                  ammoType.isLegal(techLevelYear, techLevel, mixedTech)) {
                if (isNotValidBattleArmorMunition(weaponType, ammoType)) {
                    continue;
                }
                return ammoType;
            }
        }

        // found none, let's try again with tech level year 3071
        for (int i = 0; i < vAmmo.size(); i++) {
            ammoType = vAmmo.elementAt(i);
            if ((ammoType.getRackSize() == weaponType.getRackSize()) &&
                  (TechConstants.isLegal(mounted.getType().getTechLevel(3071),
                        ammoType.getTechLevel(3071),
                        false,
                        mixedTech))) {
                if (isNotValidBattleArmorMunition(weaponType, ammoType)) {
                    continue;
                }
                return ammoType;
            }
        }
        return null; // couldn't find any ammo for this weapon type
    }

    /**
     * Battle Armor Missile munition variations need a special check, otherwise it would select any SRM/LRM missile as
     * valid to load on it
     *
     * @param weaponType the weaponType being checked against
     * @param ammoType   the ammo type to validate against the weaponType
     *
     * @return true if this is a valid missile munition variation for battle armor
     */
    private static boolean isNotValidBattleArmorMunition(WeaponType weaponType, AmmoType ammoType) {
        return (weaponType.hasFlag(WeaponTypeFlag.F_MISSILE) &&
              weaponType.hasFlag(WeaponTypeFlag.F_BA_WEAPON) &&
              !ammoType.hasFlag(AmmoTypeFlag.F_BATTLEARMOR));
    }

    public static void initializeTypes() {
        // Save copies of the SRM and LRM ammo to use to create munitions.
        ArrayList<AmmoType> srmAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanSrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> baSrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanBaLrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> isBaLrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> lrmAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> clanLrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> enhancedLRMAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> acAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> racAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> arrowAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> protoArrowAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> clanArrowAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> thumperAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> thumperCannonAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> sniperAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> sniperCannonAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> longTomAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> longTomCannonAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> baTubeAmmos = new ArrayList<>(2);
        ArrayList<AmmoType> mortarAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> clanMortarAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> lrtAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> clanLrtAmmos = new ArrayList<>();
        ArrayList<AmmoType> srtAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> clanSrtAmmos = new ArrayList<>();
        ArrayList<AmmoType> vglAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanVGLAmmos = new ArrayList<>();
        ArrayList<AmmoType> vehicleFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanVehicleFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> heavyFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanHeavyFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanImprovedLRMsAmmo = new ArrayList<>();
        ArrayList<AmmoType> clanImprovedSRMsAmmo = new ArrayList<>();
        ArrayList<AmmoType> clanImprovedAcAmmo = new ArrayList<>();
        ArrayList<AmmoType> clanProtoAcAmmo = new ArrayList<>();

        // Updated, never used.
        // They need to be implemented
        //        ArrayList<AmmoType> fluidGunAmmos = new ArrayList<>();
        //        ArrayList<AmmoType> clanFluidGunAmmos = new ArrayList<>();
        //        ArrayList<AmmoType> primLongTomAmmos = new ArrayList<>();

        ArrayList<MunitionMutator> munitions = new ArrayList<>();

        AmmoType base;

        // all level 1 ammo
        base = AmmoType.createISVehicleFlamerAmmo();
        vehicleFlamerAmmos.add(base);
        clanVehicleFlamerAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISMGAmmo());
        EquipmentType.addType(AmmoType.createISMGAmmoHalf());

        base = AmmoType.createISAC2Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISAC5Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISAC10Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISAC20Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISLRM5Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM10Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM15Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM20Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISLRM5pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM10pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM15pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM20pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSRM2Ammo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSRM4Ammo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSRM6Ammo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSRM2pAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSRM4pAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSRM6pAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        // Level 3 Ammo
        // Note, some level 3 stuff is mixed into level 2.
        base = AmmoType.createISEnhancedLRM5Ammo();
        enhancedLRMAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISEnhancedLRM10Ammo();
        enhancedLRMAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISEnhancedLRM15Ammo();
        enhancedLRMAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISEnhancedLRM20Ammo();
        enhancedLRMAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC2Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC5Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC10Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC20Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISHeavyFlamerAmmo();
        heavyFlamerAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLHeavyFlamerAmmo();
        clanHeavyFlamerAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISCoolantPod());
        EquipmentType.addType(AmmoType.createISRailGunAmmo());
        EquipmentType.addType(AmmoType.createISMPodAmmo());
        EquipmentType.addType(AmmoType.createISBPodAmmo());

        // Start of Level2 Ammo
        EquipmentType.addType(AmmoType.createISLB2XAmmo());
        EquipmentType.addType(AmmoType.createISLB5XAmmo());
        EquipmentType.addType(AmmoType.createISLB10XAmmo());
        EquipmentType.addType(AmmoType.createISLB20XAmmo());
        EquipmentType.addType(AmmoType.createISLB2XClusterAmmo());
        EquipmentType.addType(AmmoType.createISLB5XClusterAmmo());
        EquipmentType.addType(AmmoType.createISLB10XClusterAmmo());
        EquipmentType.addType(AmmoType.createISLB20XClusterAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB2XAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB5XAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB20XAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB2XClusterAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB5XClusterAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB20XClusterAmmo());
        EquipmentType.addType(AmmoType.createISUltra2Ammo());
        EquipmentType.addType(AmmoType.createISUltra5Ammo());
        EquipmentType.addType(AmmoType.createISUltra10Ammo());
        EquipmentType.addType(AmmoType.createISUltra20Ammo());
        EquipmentType.addType(AmmoType.createISTHBUltra2Ammo());
        EquipmentType.addType(AmmoType.createISTHBUltra10Ammo());
        EquipmentType.addType(AmmoType.createISTHBUltra20Ammo());

        // PLAYTEST3 Caseless RAC ammo
        base = AmmoType.createISRotary2Ammo();
        racAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISRotary5Ammo();
        racAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISRotary10Ammo();
        racAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISRotary20Ammo();
        racAmmos.add(base);
        EquipmentType.addType(base);

        EquipmentType.addType(AmmoType.createISGaussAmmo());
        EquipmentType.addType(AmmoType.createISLTGaussAmmo());
        EquipmentType.addType(AmmoType.createISHVGaussAmmo());
        EquipmentType.addType(AmmoType.createISIHVGaussAmmo());
        EquipmentType.addType(AmmoType.createISStreakSRM2Ammo());
        EquipmentType.addType(AmmoType.createISStreakSRM4Ammo());
        EquipmentType.addType(AmmoType.createISStreakSRM6Ammo());
        EquipmentType.addType(AmmoType.createISMRM10Ammo());
        EquipmentType.addType(AmmoType.createISMRM20Ammo());
        EquipmentType.addType(AmmoType.createISMRM30Ammo());
        EquipmentType.addType(AmmoType.createISMRM40Ammo());
        EquipmentType.addType(AmmoType.createISRL10Ammo());
        EquipmentType.addType(AmmoType.createISRL15Ammo());
        EquipmentType.addType(AmmoType.createISRL20Ammo());
        EquipmentType.addType(AmmoType.createISAMSAmmo());
        EquipmentType.addType(AmmoType.createISNarcAmmo());
        EquipmentType.addType(AmmoType.createISNarcExplosiveAmmo());
        EquipmentType.addType(AmmoType.createISiNarcAmmo());
        EquipmentType.addType(AmmoType.createISiNarcECMAmmo());
        EquipmentType.addType(AmmoType.createISiNarcExplosiveAmmo());
        EquipmentType.addType(AmmoType.createISiNarcHaywireAmmo());
        EquipmentType.addType(AmmoType.createISiNarcNemesisAmmo());
        EquipmentType.addType(AmmoType.createISExtendedLRM5Ammo());
        EquipmentType.addType(AmmoType.createISExtendedLRM10Ammo());
        EquipmentType.addType(AmmoType.createISExtendedLRM15Ammo());
        EquipmentType.addType(AmmoType.createISExtendedLRM20Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt5Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt10Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt15Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt20Ammo());
        EquipmentType.addType(AmmoType.createISMagshotGRAmmo());

        /*
         * Removed all references to Phoenix/Hawk/Streak MRM. Was ammo only with no
         * weapon or code to support them.
         */
        EquipmentType.addType(AmmoType.createISHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createISHeavyMGAmmoHalf());
        EquipmentType.addType(AmmoType.createISLightMGAmmo());
        EquipmentType.addType(AmmoType.createISLightMGAmmoHalf());
        EquipmentType.addType(AmmoType.createISSBGaussRifleAmmo());
        EquipmentType.addType(AmmoType.createISHVAC10Ammo());
        EquipmentType.addType(AmmoType.createISHVAC5Ammo());
        EquipmentType.addType(AmmoType.createISHVAC2Ammo());
        EquipmentType.addType(AmmoType.createISMekTaserAmmo());
        EquipmentType.addType(AmmoType.createISAC2pAmmo());
        EquipmentType.addType(AmmoType.createISAC5pAmmo());
        EquipmentType.addType(AmmoType.createISAC10pAmmo());
        EquipmentType.addType(AmmoType.createISAC20pAmmo());

        // IO Equipment
        EquipmentType.addType(AmmoType.createCLImprovedGaussAmmo());

        // Clan Improved
        base = AmmoType.createCLImprovedAC2Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedAC5Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedAC10Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedAC20Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLPROAC2Ammo();
        clanProtoAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLPROAC4Ammo();
        clanProtoAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLPROAC8Ammo();
        clanProtoAcAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLImprovedLRM5Ammo();
        clanImprovedLRMsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedLRM10Ammo();
        clanImprovedLRMsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedLRM15Ammo();
        clanImprovedLRMsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedLRM20Ammo();
        clanImprovedLRMsAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLImprovedSRM2Ammo();
        clanImprovedSRMsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedSRM4Ammo();
        clanImprovedSRMsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedSRM6Ammo();
        clanImprovedSRMsAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISMML3LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML3SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML5LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML5SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML7LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML7SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML9LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML9SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createLongTomAmmo();
        longTomAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISPrimitiveLongTomAmmo();
        //        primLongTomAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISLongTomCannonAmmo();
        longTomCannonAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createSniperAmmo();
        sniperAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSniperCannonAmmo();
        sniperCannonAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createThumperAmmo();
        thumperAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISThumperCannonAmmo();
        thumperCannonAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISArrowIVAmmo();
        arrowAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createPrototypeArrowIVAmmo();
        protoArrowAmmos.add(base);
        EquipmentType.addType(base);

        EquipmentType.addType(AmmoType.createCLLB2XAmmo());
        EquipmentType.addType(AmmoType.createCLLB5XAmmo());
        EquipmentType.addType(AmmoType.createCLLB10XAmmo());
        EquipmentType.addType(AmmoType.createCLLB20XAmmo());
        EquipmentType.addType(AmmoType.createCLLB2XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLLB5XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLLB10XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLLB20XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLUltra2Ammo());
        EquipmentType.addType(AmmoType.createCLUltra5Ammo());
        EquipmentType.addType(AmmoType.createCLUltra10Ammo());
        EquipmentType.addType(AmmoType.createCLUltra20Ammo());
        EquipmentType.addType(AmmoType.createCLRotary2Ammo());
        EquipmentType.addType(AmmoType.createCLRotary5Ammo());
        EquipmentType.addType(AmmoType.createCLRotary10Ammo());
        EquipmentType.addType(AmmoType.createCLRotary20Ammo());
        EquipmentType.addType(AmmoType.createCLGaussAmmo());
        EquipmentType.addType(AmmoType.createCLStreakSRM1Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM2Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM3Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM4Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM5Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM6Ammo());
        EquipmentType.addType(AmmoType.createCLMGAmmo());
        EquipmentType.addType(AmmoType.createCLMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createCLHeavyMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLLightMGAmmo());
        EquipmentType.addType(AmmoType.createCLLightMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLAMSAmmo());
        EquipmentType.addType(AmmoType.createCLNarcExplosiveAmmo());
        EquipmentType.addType(AmmoType.createCLATM3Ammo());
        EquipmentType.addType(AmmoType.createCLATM3ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM3HEAmmo());
        EquipmentType.addType(AmmoType.createCLATM6Ammo());
        EquipmentType.addType(AmmoType.createCLATM6ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM6HEAmmo());
        EquipmentType.addType(AmmoType.createCLATM9Ammo());
        EquipmentType.addType(AmmoType.createCLATM9ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM9HEAmmo());
        EquipmentType.addType(AmmoType.createCLATM12Ammo());
        EquipmentType.addType(AmmoType.createCLATM12ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM12HEAmmo());
        EquipmentType.addType(AmmoType.createCLStreakLRM1Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM2Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM3Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM4Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM5Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM6Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM7Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM8Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM9Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM10Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM11Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM12Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM13Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM14Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM15Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM16Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM17Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM18Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM19Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM20Ammo());
        EquipmentType.addType(AmmoType.createCLSRT1Ammo());
        EquipmentType.addType(AmmoType.createCLSRT2Ammo());
        EquipmentType.addType(AmmoType.createCLSRT3Ammo());
        EquipmentType.addType(AmmoType.createCLSRT4Ammo());
        EquipmentType.addType(AmmoType.createCLSRT5Ammo());
        EquipmentType.addType(AmmoType.createCLSRT6Ammo());
        EquipmentType.addType(AmmoType.createCLLRT1Ammo());
        EquipmentType.addType(AmmoType.createCLLRT2Ammo());
        EquipmentType.addType(AmmoType.createCLLRT3Ammo());
        EquipmentType.addType(AmmoType.createCLLRT4Ammo());
        EquipmentType.addType(AmmoType.createCLLRT5Ammo());
        EquipmentType.addType(AmmoType.createCLLRT6Ammo());
        EquipmentType.addType(AmmoType.createCLLRT7Ammo());
        EquipmentType.addType(AmmoType.createCLLRT8Ammo());
        EquipmentType.addType(AmmoType.createCLLRT9Ammo());
        EquipmentType.addType(AmmoType.createCLLRT10Ammo());
        EquipmentType.addType(AmmoType.createCLLRT11Ammo());
        EquipmentType.addType(AmmoType.createCLLRT12Ammo());
        EquipmentType.addType(AmmoType.createCLLRT13Ammo());
        EquipmentType.addType(AmmoType.createCLLRT14Ammo());
        EquipmentType.addType(AmmoType.createCLLRT15Ammo());
        EquipmentType.addType(AmmoType.createCLLRT16Ammo());
        EquipmentType.addType(AmmoType.createCLLRT17Ammo());
        EquipmentType.addType(AmmoType.createCLLRT18Ammo());
        EquipmentType.addType(AmmoType.createCLLRT19Ammo());
        EquipmentType.addType(AmmoType.createCLLRT20Ammo());
        EquipmentType.addType(AmmoType.createCLMPodAmmo());

        EquipmentType.addType(AmmoType.createCLHAG20Ammo());
        EquipmentType.addType(AmmoType.createCLHAG30Ammo());
        EquipmentType.addType(AmmoType.createCLHAG40Ammo());
        EquipmentType.addType(AmmoType.createCLPlasmaCannonAmmo());
        EquipmentType.addType(AmmoType.createISPlasmaRifleAmmo());
        EquipmentType.addType(AmmoType.createCLAPGaussRifleAmmo());
        EquipmentType.addType(AmmoType.createCLMediumChemicalLaserAmmo());
        EquipmentType.addType(AmmoType.createCLSmallChemicalLaserAmmo());
        EquipmentType.addType(AmmoType.createCLLargeChemicalLaserAmmo());
        EquipmentType.addType(AmmoType.createISNailRivetGunAmmo());
        EquipmentType.addType(AmmoType.createISNailRivetGunAmmoHalf());
        EquipmentType.addType(AmmoType.createISC3RemoteSensorAmmo());

        EquipmentType.addType(AmmoType.createCLIATM3Ammo());
        EquipmentType.addType(AmmoType.createCLIATM3ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM3HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM3IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM3IMPAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6Ammo());
        EquipmentType.addType(AmmoType.createCLIATM6ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6IMPAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9Ammo());
        EquipmentType.addType(AmmoType.createCLIATM9ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9IMPAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12Ammo());
        EquipmentType.addType(AmmoType.createCLIATM12ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12IMPAmmo());

        // Unofficial Ammo
        base = AmmoType.createISAC15Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISAC10iAmmo());
        base = AmmoType.createISGAC2Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISGAC4Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISGAC6Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISGAC8Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLArrowIVAmmo();
        clanArrowAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM1Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM2Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM3Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM4Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM5Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM6Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM1Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM2Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM3Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM4Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM5Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM6Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM7Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM8Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM9Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM10Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM11Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM12Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM13Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM14Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM15Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM16Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM17Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM18Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM19Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM20Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);

        // Start of BattleArmor ammo
        EquipmentType.addType(AmmoType.createBAMicroBombAmmo());
        EquipmentType.addType(AmmoType.createCLTorpedoLRM5Ammo());
        EquipmentType.addType(AmmoType.createBACompactNarcAmmo());
        EquipmentType.addType(AmmoType.createBAMineLauncherAmmo());
        EquipmentType.addType(AmmoType.createAdvancedSRM1Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM2Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM3Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM4Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM5Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM6Ammo());
        EquipmentType.addType(AmmoType.createBARL1Ammo());
        EquipmentType.addType(AmmoType.createBARL2Ammo());
        EquipmentType.addType(AmmoType.createBARL3Ammo());
        EquipmentType.addType(AmmoType.createBARL4Ammo());
        EquipmentType.addType(AmmoType.createBARL5Ammo());
        EquipmentType.addType(AmmoType.createISMRM1Ammo());
        EquipmentType.addType(AmmoType.createISMRM2Ammo());
        EquipmentType.addType(AmmoType.createISMRM3Ammo());
        EquipmentType.addType(AmmoType.createISMRM4Ammo());
        EquipmentType.addType(AmmoType.createISMRM5Ammo());
        EquipmentType.addType(AmmoType.createISBATaserAmmo());
        base = AmmoType.createBATubeArtyAmmo();
        EquipmentType.addType(base);
        baTubeAmmos.add(base);
        base = AmmoType.createBAISLRM1Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM2Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM3Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM4Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM5Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM1Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM2Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM3Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM4Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM5Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM1Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM2Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM3Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM4Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM5Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM6Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);

        // ProtoMek-specific ammo
        EquipmentType.addType(AmmoType.createCLPROHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROLightMGAmmo());

        // naval ammo
        EquipmentType.addType(AmmoType.createNAC10Ammo());
        EquipmentType.addType(AmmoType.createNAC20Ammo());
        EquipmentType.addType(AmmoType.createNAC25Ammo());
        EquipmentType.addType(AmmoType.createNAC30Ammo());
        EquipmentType.addType(AmmoType.createNAC35Ammo());
        EquipmentType.addType(AmmoType.createNAC40Ammo());
        EquipmentType.addType(AmmoType.createLightNGaussAmmo());
        EquipmentType.addType(AmmoType.createMediumNGaussAmmo());
        EquipmentType.addType(AmmoType.createHeavyNGaussAmmo());
        EquipmentType.addType(AmmoType.createKrakenAmmo());
        EquipmentType.addType(AmmoType.createKillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createPeacemakerAmmo());
        EquipmentType.addType(AmmoType.createWhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createSantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createBarracudaAmmo());
        EquipmentType.addType(AmmoType.createKillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createWhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createBarracudaTAmmo());
        EquipmentType.addType(AmmoType.createAR10KillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createAR10PeacemakerAmmo());
        EquipmentType.addType(AmmoType.createAR10WhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createAR10SantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createAR10BarracudaAmmo());
        EquipmentType.addType(AmmoType.createAR10KillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createAR10WhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createAR10BarracudaTAmmo());
        EquipmentType.addType(AmmoType.createScreenLauncherAmmo());
        EquipmentType.addType(AmmoType.createAlamoAmmo());
        EquipmentType.addType(AmmoType.createLightSCCAmmo());
        EquipmentType.addType(AmmoType.createMediumSCCAmmo());
        EquipmentType.addType(AmmoType.createHeavySCCAmmo());
        EquipmentType.addType(AmmoType.createMantaRayAmmo());
        EquipmentType.addType(AmmoType.createSwordfishAmmo());
        EquipmentType.addType(AmmoType.createStingrayAmmo());
        EquipmentType.addType(AmmoType.createPiranhaAmmo());
        EquipmentType.addType(AmmoType.createKrakenMAmmo());
        EquipmentType.addType(AmmoType.createHeavyMassDriverAmmo());
        EquipmentType.addType(AmmoType.createMediumMassDriverAmmo());
        EquipmentType.addType(AmmoType.createLightMassDriverAmmo());
        EquipmentType.addType(AmmoType.createInfantryAmmo());
        EquipmentType.addType(AmmoType.createInfantryInfernoAmmo());

        base = AmmoType.createISAPMortar1Ammo();
        mortarAmmos.add(base);
        base = AmmoType.createISAPMortar2Ammo();
        mortarAmmos.add(base);
        base = AmmoType.createISAPMortar4Ammo();
        mortarAmmos.add(base);
        base = AmmoType.createISAPMortar8Ammo();
        mortarAmmos.add(base);

        base = AmmoType.createCLAPMortar1Ammo();
        clanMortarAmmos.add(base);
        base = AmmoType.createCLAPMortar2Ammo();
        clanMortarAmmos.add(base);
        base = AmmoType.createCLAPMortar4Ammo();
        clanMortarAmmos.add(base);
        base = AmmoType.createCLAPMortar8Ammo();
        clanMortarAmmos.add(base);

        // Create the munition types for IS Mek mortars
        munitions.add(AIRBURST_MORTAR_MUNITION_MUTATOR);
        munitions.add(ANTI_PERSONNEL_MORTAR_MUNITION_MUTATOR);
        // Armor Piercing is the base ammo type see further down.
        munitions.add(FLARE_MORTAR_MUNITION_MUTATOR);
        munitions.add(SEMI_GUIDED_MORTAR_MUNITION_MUTATOR);
        munitions.add(SMOKE_MORTAR_MUNITION_MUTATOR);
        AmmoType.createMunitions(mortarAmmos, munitions);

        // Create the munition types for Clan Mek mortars
        munitions.clear();
        munitions.add(CLAN_AIRBURST_MORTAR_MUNITION_MUTATOR);
        munitions.add(CLAN_ANTI_PERSONNEL_MORTAR_MUNITION_MUTATOR);
        // Armor Piercing is the base ammo type see further down.
        munitions.add(CLAN_FLARE_MORTAR_MUNITION_MUTATOR);
        munitions.add(CLAN_SEMI_GUIDED_MORTAR_MUNITION_MUTATOR);
        munitions.add(CLAN_SMOKE_MORTAR_MUNITION_MUTATOR);
        AmmoType.createMunitions(clanMortarAmmos, munitions);

        // Long range Torpedo
        base = AmmoType.createISLRT5Ammo();
        lrtAmmos.add(base);
        base = AmmoType.createISLRT10Ammo();
        lrtAmmos.add(base);
        base = AmmoType.createISLRT15Ammo();
        lrtAmmos.add(base);
        base = AmmoType.createISLRT20Ammo();
        lrtAmmos.add(base);

        EquipmentType.addType(AmmoType.createISLRT5Ammo());
        EquipmentType.addType(AmmoType.createISLRT10Ammo());
        EquipmentType.addType(AmmoType.createISLRT15Ammo());
        EquipmentType.addType(AmmoType.createISLRT20Ammo());

        base = AmmoType.createISSRT2Ammo();
        srtAmmos.add(base);
        base = AmmoType.createISSRT4Ammo();
        srtAmmos.add(base);
        base = AmmoType.createISSRT6Ammo();
        srtAmmos.add(base);

        EquipmentType.addType(AmmoType.createISSRT2Ammo());
        EquipmentType.addType(AmmoType.createISSRT4Ammo());
        EquipmentType.addType(AmmoType.createISSRT6Ammo());

        EquipmentType.addType(AmmoType.createISAPMortar1Ammo());
        EquipmentType.addType(AmmoType.createISAPMortar2Ammo());
        EquipmentType.addType(AmmoType.createISAPMortar4Ammo());
        EquipmentType.addType(AmmoType.createISAPMortar8Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar1Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar2Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar4Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar8Ammo());

        EquipmentType.addType(AmmoType.createISCruiseMissile50Ammo());
        EquipmentType.addType(AmmoType.createISCruiseMissile70Ammo());
        EquipmentType.addType(AmmoType.createISCruiseMissile90Ammo());
        EquipmentType.addType(AmmoType.createISCruiseMissile120Ammo());

        base = AmmoType.createISFluidGunAmmo();
        //        fluidGunAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLFluidGunAmmo();
        //        clanFluidGunAmmos.add(base);
        EquipmentType.addType(base);

        // Rifles
        EquipmentType.addType(AmmoType.createISLightRifleAmmo());
        EquipmentType.addType(AmmoType.createISMediumRifleAmmo());
        EquipmentType.addType(AmmoType.createISHeavyRifleAmmo());

        EquipmentType.addType(AmmoType.createISAPDSAmmo());

        base = AmmoType.createCLLRT5Ammo();
        clanLrtAmmos.add(base);
        base = AmmoType.createCLLRT10Ammo();
        clanLrtAmmos.add(base);
        base = AmmoType.createCLLRT15Ammo();
        clanLrtAmmos.add(base);
        base = AmmoType.createCLLRT20Ammo();
        clanLrtAmmos.add(base);

        base = AmmoType.createCLSRT2Ammo();
        clanSrtAmmos.add(base);
        base = AmmoType.createCLSRT4Ammo();
        clanSrtAmmos.add(base);
        base = AmmoType.createCLSRT6Ammo();
        clanSrtAmmos.add(base);

        base = AmmoType.createISVGLAmmo();
        EquipmentType.addType(base);
        vglAmmos.add(base);
        base = AmmoType.createCLVGLAmmo();
        clanVGLAmmos.add(base);
        EquipmentType.addType(base);

        munitions.clear();
        // Battle Armor Missiles
        // Multi-Purpose missiles are CLAN BA LRM only
        // Forum ruling - last access 2025-05-18 21:52 - https://bg.battletech.com/forums/index.php/topic,63753
        // Archived link https://web.archive.org/web/20250518225141/https://bg.battletech.com/forums/index.php/topic,63753
        // HOWEVER it is errata'd to LRM and SRM, so it is being made available for SRM too
        // BA missiles: standard SRM/LRM, torpedo SRT/LRT, multipurpose missile Clan LRMs, inferno SRMs
        // The new errata on it can be found on the forum here Multi-purpose missiles - last access 2025-05-18 22:05
        // Link: https://bg.battletech.com/forums/index.php/topic,33530.msg1950733.html#msg1950733
        // Archived Link: https://web.archive.org/web/20250518230820/https://bg.battletech.com/forums/index.php/topic,33530.msg1950733.html#msg1950733
        munitions.add(TORPEDO_MUNITION_MUTATOR);
        munitions.add(CLAN_TORPEDO_MUNITION_MUTATOR);
        munitions.add(INFERNO_MUNITION_MUTATOR);
        munitions.add(CLAN_INFERNO_MUNITION_MUTATOR);
        munitions.add(CLAN_MPM_MUNITION_MUTATOR);
        AmmoType.createMunitions(baSrmAmmos, munitions);

        munitions.clear();
        munitions.add(TORPEDO_MUNITION_MUTATOR);
        AmmoType.createMunitions(isBaLrmAmmos, munitions);

        munitions.clear();
        munitions.add(CLAN_TORPEDO_MUNITION_MUTATOR);
        munitions.add(CLAN_MPM_MUNITION_MUTATOR);
        AmmoType.createMunitions(clanBaLrmAmmos, munitions);


        // Create the munition types for IS LRM launchers.
        munitions.clear();
        /*
         * munitions.add(new MunitionMutator("Harpoon", 2, Munitions.M_HARPOON, new
         * TechAdvancement(ITechnology.TechBase.ALL).setIntroLevel(false).setUnofficial(false).
         * setTechRating(TechRating.C) .setAvailability(TechRating.C, TechRating.C, TechRating.C,
         * TechRating.C) .setISAdvancement(2395, 2400, 2415, DATE_NONE, DATE_NONE)
         * .setISApproximate(true, false, false, false,
         * false).setPrototypeFactions(Faction.LC) .setProductionFactions(Faction.LC), "369, TO"));
         */
        // TODO harpoon
        // TODO Tear Gas See IO pg 372
        // TODO Retro-Streak IO pg 132
        // TODO Mag Pulse see IO pg 62
        // TODO: Harpoon SRMs (TO 369), Tear Gas SRMs (TO 371), RETRO-STREAK (IO 193)
        munitions.add(ARAD_MUNITION_MUTATOR);
        munitions.add(INFERNO_MUNITION_MUTATOR);
        munitions.add(ACID_MUNITION_MUTATOR);
        munitions.add(HEAT_SEEKING_MUNITION_MUTATOR);
        munitions.add(SMOKE_MUNITION_MUTATOR);
        munitions.add(TANDEM_CHARGE_MUNITION_MUTATOR);
        munitions.add(ANTI_TSM_MUNITION_MUTATOR);
        munitions.add(ARTEMIS_CAPABLE_MUNITION_MUTATOR);
        munitions.add(DEAD_FIRE_MUNITION_MUTATOR);
        munitions.add(FRAGMENTATION_MUNITION_MUTATOR);
        munitions.add(LISTEN_KILL_MUNITION_MUTATOR);
        munitions.add(MINE_CLEARANCE_MUNITION_MUTATOR);
        munitions.add(NARC_CAPABLE_MUNITION_MUTATOR);
        AmmoType.createMunitions(srmAmmos, munitions);

        // Create the munition types for Clan SRM launchers.

        /*
         * munitions.add(new MunitionMutator("Harpoon", 2, Munitions.M_HARPOON, new
         * TechAdvancement(ITechnology.TechBase.ALL).setIntroLevel(false).setUnofficial(false).
         * setTechRating(TechRating.C) .setAvailability(TechRating.C, TechRating.C, TechRating.C,
         * TechRating.C) .setClanAdvancement(2395, 2400, 2415, DATE_NONE, DATE_NONE)
         * .setClanApproximate(true, false, false, false,
         * false).setPrototypeFactions(Faction.LC) .setProductionFactions(Faction.LC), "369, TO"));
         */

        munitions.clear();
        // TODO Tear Gas See IO pg 372
        // TODO Mag Pulse See IO pg 62
        // TODO: Harpoon SRMs (TO 369), Tear Gas SRMs (TO 371), RETRO-STREAK (IO 193)
        munitions.add(CLAN_ARAD_MUNITION_MUTATOR);
        munitions.add(CLAN_ACID_MUNITION_MUTATOR);
        munitions.add(CLAN_HEAT_SEEKING_MUNITIONS_MUTATOR);
        munitions.add(CLAN_INFERNO_MUNITION_MUTATOR);
        munitions.add(CLAN_SMOKE_MUNITION_MUTATOR_ADVANCED_FOR_SRM);
        munitions.add(CLAN_TANDEM_CHARGE_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_ANTI_TSM_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_ARTEMIS_CAPABLE_MUNITION_MUTATOR_FOR_SRM);
        munitions.add(CLAN_ARTEMIS_V_CAPABLE_MUNITION_MUTATOR);
        munitions.add(CLAN_DEAD_FIRE_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_FRAGMENTATION_MUNITION_MUTATOR);
        munitions.add(CLAN_LISTEN_KILL_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_MINE_CLEARANCE_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_NARC_CAPABLE_MUNITION_MUTATOR_UNOFFICIAL);
        AmmoType.createMunitions(clanSrmAmmos, munitions);
        AmmoType.createMunitions(clanImprovedSRMsAmmo, munitions);

        munitions.clear();
        /*
         * munitions.add(new MunitionMutator("Incendiary", 2,
         * Munitions.M_INCENDIARY_LRM, new
         * TechAdvancement(ITechnology.TechBase.IS) .setIntroLevel(false) .setUnofficial(false)
         * .setTechRating(TechRating.C) .setAvailability(TechRating.E, TechRating.E, TechRating.E,
         * TechRating.E) .setClanAdvancement(2341, 2342, 2352, DATE_NONE, DATE_NONE)
         * .setClanApproximate(false, false, false, false, false)
         * .setPrototypeFactions(Faction.TH) .setProductionFactions(Faction.TH),"369, TO"));
         */
        // TODO Flare LRMs IO pg 230
        // TODO Incendiary LRMs - IO pg 61, TO pg 369
        // TODO Mag Pulse see IO pg 62
        munitions.add(ARAD_MUNITION_MUTATOR);
        munitions.add(FOLLOW_THE_LEADER_MUNITION_MUTATOR);
        munitions.add(HEAT_SEEKING_MUNITION_MUTATOR);
        munitions.add(SEMI_GUIDED_MUNITION_MUTATOR);
        munitions.add(SMOKE_MUNITION_MUTATOR);
        munitions.add(SWARM_MUNITION_MUTATOR);
        munitions.add(SWARM_I_MUNITION_MUTATOR);
        munitions.add(THUNDER_MUNITION_MUTATOR);
        munitions.add(THUNDER_ACTIVE_MUNITION_MUTATOR);
        munitions.add(THUNDER_AUGMENTED_MUNITION_MUTATOR);
        munitions.add(THUNDER_BIBRABOMB_MUNITION_MUTATOR);
        munitions.add(THUNDER_INFERNO_MUTATION_MUTATOR);
        munitions.add(ANTI_TSM_MUNITION_MUTATOR);
        munitions.add(ARTEMIS_CAPABLE_MUNITION_MUTATOR);
        munitions.add(DEAD_FIRE_MUNITION_MUTATOR);
        munitions.add(FRAGMENTATION_MUNITION_MUTATOR_FOR_LRM);
        munitions.add(LISTEN_KILL_MUNITION_MUTATOR);
        munitions.add(MINE_CLEARANCE_MUNITION_MUTATOR);
        munitions.add(NARC_CAPABLE_MUNITION_MUTATOR);
        AmmoType.createMunitions(lrmAmmos, munitions);
        AmmoType.createMunitions(enhancedLRMAmmos, munitions);

        // Create the munition types for Clan LRM launchers.
        munitions.clear();
        /*
         * munitions.add(new MunitionMutator("(Clan) Incendiary", 2,
         * Munitions.M_INCENDIARY_LRM,
         * new TechAdvancement(TechBase.CLAN) .setIntroLevel(false)
         * .setUnofficial(false) .setTechRating(TechRating.C) .setAvailability(TechRating.E,
         * TechRating.E, TechRating.E, TechRating.E) .setClanAdvancement(2341, 2342, 2352,
         * DATE_NONE, DATE_NONE) .setClanApproximate(false, false, false, false, false)
         * .setPrototypeFactions(Faction.TH) .setProductionFactions(Faction.TH),"369, TO"));
         */
        // TODO Incendiary LRMs - IO pg 61, TO pg 369
        // TODO Mag Pulse see IO pg 62
        munitions.add(CLAN_ARAD_MUNITION_MUTATOR);
        munitions.add(CLAN_FOLLOW_THE_LEADER_MUNITION_MUTATOR);
        munitions.add(CLAN_HEAT_SEEKING_MUNITIONS_MUTATOR);
        munitions.add(CLAN_SEMI_GUIDED);
        munitions.add(CLAN_SMOKE_STANDARD_MUNITION_MUTATOR);
        munitions.add(CLAN_SWARM_MUNITION_MUTATOR);
        munitions.add(CLAN_SWARM_I_ADV_MUNITION_MUTATOR);
        munitions.add(CLAN_THUNDER_ADV_MUNITION_MUTATOR);
        munitions.add(CLAN_THUNDER_ACTIVE_MUNITION_MUTATOR);
        munitions.add(CLAN_THUNDER_AUGMENTED_MUNITION_MUTATOR);
        munitions.add(CLAN_THUNDER_VIBRABOMB_MUNITION_MUTATOR);
        munitions.add(CLAN_THUNDER_INFORMATION_MUNITION_MUTATOR);
        munitions.add(CLAN_ANTI_TSM_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_ARTEMIS_CAPABLE_MUNITION_MUTATOR_FOR_LRM);
        munitions.add(CLAN_ARTEMIS_V_CAPABLE_MUNITION_MUTATOR_FOR_MISSILE_AND_TORPEDO);
        munitions.add(CLAN_DEAD_FIRE_MUNITION_MUTATOR);
        munitions.add(CLAN_FRAGMENTATION_MUNITION_MUTATOR);
        munitions.add(CLAN_LISTEN_KILL_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_MINE_CLEARANCE_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_NARC_CAPABLE_MUNITION_MUTATOR_FOR_MISSILE);
        AmmoType.createMunitions(clanLrmAmmos, munitions);
        AmmoType.createMunitions(clanImprovedLRMsAmmo, munitions);

        // Create the munition types for AC rounds.
        munitions.clear();
        munitions.add(ARMOR_PIERCING_MUNITION_MUTATOR);
        // PLAYTEST3 add AP ammo
        munitions.add(ARMOR_PIERCING_PLAYTEST_MUNITION_MUTATOR);
        munitions.add(CASELESS_MUNITION_MUTATOR);
        munitions.add(FLAK_MUNITION_MUTATOR);
        munitions.add(FLECHETTE_MUNITION_MUTATOR);
        munitions.add(PRECISION_MUNITION_MUTATOR);
        // PLAYTEST3 add Precision ammo
        munitions.add(PRECISION_PLAYTEST_MUNITION_MUTATOR);
        munitions.add(TRACER_MUNITION_MUTATOR);
        AmmoType.createMunitions(acAmmos, munitions);

        // PLAYTEST create the munition types for RAC rounds.
        munitions.clear();
        munitions.add(PLAYTEST_CASELESS_MUNITION_MUTATOR);
        AmmoType.createMunitions(racAmmos, munitions);

        // Create the munition types for Clan Improved AC rounds. Since Improved AC go
        // extinct the ammo will as well.
        munitions.clear();
        munitions.add(CLAN_IMPROVED_ARMOR_PIERCING_MUNITION_MUTATOR);
        munitions.add(CASELESS_MUNITION_MUTATOR);
        munitions.add(FLAK_MUNITION_MUTATOR);
        munitions.add(CLAN_IMPROVED_FLECHETTE_MUNITION_MUTATOR);
        munitions.add(CLAN_IMPROVED_PRECISION_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_IMPROVED_TRACER_MUNITION_MUTATOR);
        AmmoType.createMunitions(clanImprovedAcAmmo, munitions);

        // Create the munition types for Clan Protomek AC rounds. Ammo Tech Ratings based off the weapon itself
        munitions.clear();
        munitions.add(CLAN_ARMOR_PIERCING_MUNITION_MUTATOR_FOR_PROTO);
        munitions.add(CASELESS_MUNITION_MUTATOR);
        munitions.add(FLAK_MUNITION_MUTATOR);
        munitions.add(CLAN_FLECHETTE_MUNITION_MUTATOR_FOR_PROTO);
        munitions.add(CLAN_PRECISION_MUNITION_MUTATOR_FOR_PROTO);
        munitions.add(CLAN_TRACER_MUNITION_MUTATOR_FOR_PROTO);
        AmmoType.createMunitions(clanProtoAcAmmo, munitions);

        // Create the munition types for IS Arrow IV launchers.
        munitions.clear();
        // TODO: Arrow IV [Thunder Active-IV] - TO (357)
        munitions.add(ADA_MUNITION_MUTATOR);
        munitions.add(CLUSTER_MUNITION_MUTATOR);
        munitions.add(HOMING_MUNITION_MUTATOR);
        munitions.add(ILLUMINATION_MUNITION_MUTATOR);
        munitions.add(INFERNO_IV_MUNITION_MUTATOR);
        munitions.add(LASER_INHIBITING_MUNITION_MUTATOR);
        munitions.add(SMOKE_MUNITION_MUTATOR_FOR_ARROW);
        munitions.add(THUNDER_FASCAM_MUNITION_MUTATOR);
        munitions.add(THUNDER_VIBRABOMB_IV_MUNITION_MUTATOR);
        munitions.add(DAVY_CROCKETT_M_MUNITION_MUTATOR);
        munitions.add(FUEL_AIR_MUNITION_MUTATOR);
        AmmoType.createMunitions(arrowAmmos, munitions);
        AmmoType.createMunitions(protoArrowAmmos, munitions);

        // Create the munition types for Clan Arrow IV launchers.
        munitions.clear();
        // TODO - Implement them.
        // TODO: Fuel-Air Mutators (See IO 165)
        // TODO: Thunder-Active-IV (See IO 165)
        munitions.add(CLAN_ADA_MUNITION_MUTATOR);
        munitions.add(CLAN_CLUSTER_MUNITION_MUTATOR_FOR_ARROW);
        munitions.add(CLAN_HOMING_MUNITION_MUTATOR);
        munitions.add(CLAN_ILLUMINATION_MUNITION_MUTATOR);
        munitions.add(CLAN_INFERNO_IV_MUNITION_MUTATOR);
        munitions.add(CLAN_LASER_INHIBITING_MUNITION_MUTATOR_UNOFFICIAL);
        munitions.add(CLAN_SMOKE_MUNITION_MUTATOR_FOR_ARROW_IV);
        munitions.add(CLAN_THUNDER_FASCAM_MUNITION_MUTATOR);
        munitions.add(CLAN_THUNDER_VIBRABOMB_IV_MUNITION_MUTATOR);
        AmmoType.createMunitions(clanArrowAmmos, munitions);

        // create the munition types for clan vehicular grenade launchers
        munitions.clear();
        munitions.add(CLAN_CHAFF_VEE_MUNITION_MUTATOR);
        munitions.add(CLAN_INDENCIARY_VEE_MUNITION_MUTATOR);
        munitions.add(CLAN_SMOKE_VEE_MUNITION_MUTATOR);
        AmmoType.createMunitions(clanVGLAmmos, munitions);

        // create the munition types for IS vehicular grenade launchers
        munitions.clear();
        munitions.add(CHAFF_VEE_MUNITION_MUTATOR);
        munitions.add(INCENDIARY_VEE_MUNITION_MUTATOR);
        munitions.add(SMOKE_VEE_MUNITION_MUTATOR);
        AmmoType.createMunitions(vglAmmos, munitions);

        // Create the munition types for Artillery launchers.
        munitions.clear();
        munitions.add(CLUSTER_ARTY_MUNITION_MUTATOR);
        munitions.add(COPPERHEAD_ARTY_MUNITION_MUTATOR);
        munitions.add(FASCAM_ARTY_MUNITION_MUTATOR);
        munitions.add(FLECHETTE_ARTY_MUNITION_MUTATOR);
        munitions.add(ILLUMINATION_ARTY_MUNITION_MUTATOR);
        munitions.add(SMOKE_ARTY_MUNITION_MUTATOR);
        munitions.add(FUEL_AIR_MUNITION_MUTATOR);
        AmmoType.createMunitions(sniperAmmos, munitions);
        AmmoType.createMunitions(thumperAmmos, munitions);

        // Make Davy Crockett-Ms for Long Toms, but not Thumper or Sniper.
        munitions.add(DAVY_CROCKETT_M_MUNITION_MUTATOR);
        AmmoType.createMunitions(longTomAmmos, munitions);
        munitions.clear();

        // Create the munition types for Artillery Cannons. These were taken out in
        // TacOps errata, so are unofficial.
        munitions.add(FUEL_AIR_ARTY_MUNITION_MUTATOR_UNOFFICIAL);
        AmmoType.createMunitions(sniperCannonAmmos, munitions);
        AmmoType.createMunitions(thumperCannonAmmos, munitions);
        AmmoType.createMunitions(longTomCannonAmmos, munitions);
        munitions.clear();

        munitions.add(SMOKE_MUNITION_MUTATOR_FOR_BA_TUBE);
        AmmoType.createMunitions(baTubeAmmos, munitions);

        // Create the munition types for SRT launchers.
        munitions.clear();
        munitions.add(ARTEMIS_CAPABLE_MUNITION_MUTATOR);
        AmmoType.createMunitions(srtAmmos, munitions);
        AmmoType.createMunitions(lrtAmmos, munitions);

        // Create the munition types for Clan SRT launchers.
        munitions.clear();
        munitions.add(CLAN_ARTEMIS_V_CAPABLE_MUNITION_MUTATOR_FOR_MISSILE_AND_TORPEDO);
        munitions.add(CLAN_ARTEMIS_CAPABLE_MUNTION_MUTATOR_FOR_TORPEDO);
        AmmoType.createMunitions(clanSrtAmmos, munitions);
        AmmoType.createMunitions(clanLrtAmmos, munitions);

        munitions.clear();
        // TODO : Need Corrosive, Flame-Retardant, Oil Slick, Paint and Water Ammo's for
        // all Fluid Guns/Sprayers
        //
        // Create the munition types for vehicle flamers Tech Progression tweaked to
        // combine IntOps with TRO Prototypes/3145 NTNU RS
        // December 2021 - CGL requested we move this to Advanced for all fluid gun
        // ammunition.
        munitions.add(COOLANT_MUNITION_MUTATOR);
        AmmoType.createMunitions(vehicleFlamerAmmos, munitions);

        munitions.clear();
        munitions.add(CLAN_COOLANT_MUNITION_MUTATOR);
        AmmoType.createMunitions(clanVehicleFlamerAmmos, munitions);

        // Create the munition types for heavy flamers
        munitions.clear();
        munitions.add(COOLANT_MUNITION_MUTATOR_FOR_HEAVY_FLAMER);
        AmmoType.createMunitions(heavyFlamerAmmos, munitions);

        munitions.clear();
        munitions.add(CLAN_COOLANT_MUNITION_MUTATOR_FOR_HEAVY_FLAMER);
        AmmoType.createMunitions(clanHeavyFlamerAmmos, munitions);

        // cache types that share a launcher for load out purposes
        for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
              equipmentTypes.hasMoreElements(); ) {
            EquipmentType equipmentType = equipmentTypes.nextElement();
            if (!(equipmentType instanceof AmmoType ammoType)) {
                continue;
            }

            AmmoTypeEnum nType = ammoType.getAmmoType();
            if (m_vaMunitions.get(nType) == null) {
                m_vaMunitions.put(nType, new Vector<>());
            }

            m_vaMunitions.get(nType).addElement(ammoType);
        }
    }

    private static void createMunitions(List<AmmoType> bases, List<MunitionMutator> munitions) {
        for (AmmoType base : bases) {
            for (MunitionMutator mutator : munitions) {
                EquipmentType.addType(mutator.createMunitionType(base));
            }
        }
    }

    // Anti-Missile Ammo

    private static AmmoType createISAMSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-Missile System Ammo [IS]";
        ammo.shortName = "AMS Ammo";
        ammo.setInternalName("ISAMS Ammo");
        ammo.addLookupName("IS Ammo AMS");
        ammo.addLookupName("IS AMS Ammo");
        ammo.damagePerShot = 1; // only used for ammo criticalSlots
        ammo.rackSize = 2; // only used for ammo criticalSlots
        ammo.ammoType = AmmoTypeEnum.AMS;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 2000;
        ammo.rulesRefs = "204, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2613, 2617, 3048, 2835, 3045)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.CC);
        return ammo;
    }

    private static AmmoType createCLAMSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-Missile System Ammo [Clan]";
        ammo.shortName = "AMS Ammo";
        ammo.setInternalName("CLAMS Ammo");
        ammo.addLookupName("Clan Ammo AMS");
        ammo.addLookupName("Clan AMS Ammo");
        ammo.damagePerShot = 1; // only used for ammo criticalSlots
        ammo.rackSize = 2; // only used for ammo criticalSlots
        ammo.ammoType = AmmoTypeEnum.AMS;
        ammo.shots = 24;
        ammo.bv = 22;
        ammo.cost = 2000;
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "204, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2824, 2831, 2835, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA);
        return ammo;
    }

    // Arrow Missile Launchers and Artillery Ammo - see Mutators above as well.

    private static AmmoType createISArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Arrow IV Ammo";
        ammo.shortName = "Arrow IV";
        ammo.setInternalName("ISArrowIVAmmo");
        ammo.addLookupName("ISArrowIV Ammo");
        ammo.addLookupName("IS Ammo Arrow");
        ammo.addLookupName("IS Arrow IV Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.ARROW_IV;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 10000;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3044)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createCLArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Arrow IV Ammo";
        ammo.shortName = "Arrow IV";
        ammo.setInternalName("CLArrowIVAmmo");
        ammo.addLookupName("CLArrowIV Ammo");
        ammo.addLookupName("Clan Ammo Arrow");
        ammo.addLookupName("Clan Arrow IV Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.ARROW_IV;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 10000;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(2593, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createLongTomAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Long Tom Ammo";
        ammo.shortName = "Long Tom";
        ammo.setInternalName("ISLongTomAmmo");
        ammo.addLookupName("ISLongTom Ammo");
        ammo.addLookupName("ISLongTomArtillery Ammo");
        ammo.addLookupName("IS Ammo Long Tom");
        ammo.addLookupName("IS Long Tom Ammo");
        ammo.addLookupName("CLLongTomAmmo");
        ammo.addLookupName("CLLongTom Ammo");
        ammo.addLookupName("CLLongTomArtillery Ammo");
        ammo.addLookupName("Clan Ammo Long Tom");
        ammo.addLookupName("Clan Long Tom Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoTypeEnum.LONG_TOM;
        ammo.shots = 5;
        ammo.bv = 46;
        ammo.cost = 10000;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setAdvancement(2445, 2500, 2520, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createSniperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Sniper Ammo";
        ammo.shortName = "Sniper";
        ammo.setInternalName("ISSniperAmmo");
        ammo.addLookupName("ISSniper Ammo");
        ammo.addLookupName("ISSniperArtillery Ammo");
        ammo.addLookupName("IS Ammo Sniper");
        ammo.addLookupName("IS Sniper Ammo");
        ammo.addLookupName("CLSniperAmmo");
        ammo.addLookupName("CLSniper Ammo");
        ammo.addLookupName("CLSniperArtillery Ammo");
        ammo.addLookupName("Clan Ammo Sniper");
        ammo.addLookupName("Clan Sniper Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.SNIPER;
        ammo.shots = 10;
        ammo.bv = 11;
        ammo.cost = 6000;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createThumperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thumper Ammo";
        ammo.shortName = "Thumper";
        ammo.setInternalName("ISThumperAmmo");
        ammo.addLookupName("ISThumper Ammo");
        ammo.addLookupName("ISThumperArtillery Ammo");
        ammo.addLookupName("IS Ammo Thumper");
        ammo.addLookupName("IS Thumper Ammo");
        ammo.addLookupName("CLThumperAmmo");
        ammo.addLookupName("CLThumper Ammo");
        ammo.addLookupName("CLThumperArtillery Ammo");
        ammo.addLookupName("Clan Ammo Thumper");
        ammo.addLookupName("Clan Thumper Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.THUMPER;
        ammo.shots = 20;
        ammo.bv = 5;
        ammo.cost = 4500;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    // Cruise Missiles

    private static AmmoType createISCruiseMissile50Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/50 Ammo";
        ammo.setInternalName("ISCruiseMissile50Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 50;
        ammo.ammoType = AmmoTypeEnum.CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 75;
        ammo.cost = 20000;
        ammo.tonnage = 25;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        blastRadius.put(ammo.getInternalName(), 1);
        return ammo;
    }

    private static AmmoType createISCruiseMissile70Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/70 Ammo";
        ammo.setInternalName("ISCruiseMissile70Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 70;
        ammo.ammoType = AmmoTypeEnum.CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 129;
        ammo.cost = 50000;
        ammo.tonnage = 35;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        blastRadius.put(ammo.getInternalName(), 2);
        return ammo;
    }

    private static AmmoType createISCruiseMissile90Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/90 Ammo";
        ammo.setInternalName("ISCruiseMissile90Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 90;
        ammo.ammoType = AmmoTypeEnum.CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 191;
        ammo.cost = 90000;
        ammo.tonnage = 45;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        blastRadius.put(ammo.getInternalName(), 3);
        return ammo;
    }

    private static AmmoType createISCruiseMissile120Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/120 Ammo";
        ammo.setInternalName("ISCruiseMissile120Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 120;
        ammo.ammoType = AmmoTypeEnum.CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 285;
        ammo.cost = 140000;
        ammo.tonnage = 60;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        blastRadius.put(ammo.getInternalName(), 4);
        return ammo;
    }

    // Artillery Cannon Shells

    private static AmmoType createISLongTomCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Long Tom Cannon Ammo";
        ammo.shortName = "Long Tom Cannon";
        ammo.setInternalName("ISLongTomCannonAmmo");
        ammo.addLookupName("ISLongTomCannon Ammo");
        ammo.addLookupName("ISLongTomArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Long Tom Cannon");
        ammo.addLookupName("IS Long Tom Cannon Ammo");
        ammo.addLookupName("CLLongTomCannonAmmo");
        ammo.addLookupName("CLLongTomCannon Ammo");
        ammo.addLookupName("CLLongTomArtilleryCannon Ammo");
        ammo.addLookupName("CL Ammo Long Tom Cannon");
        ammo.addLookupName("CL Long Tom Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LONG_TOM_CANNON;
        ammo.shots = 5;
        ammo.bv = 41;
        ammo.cost = 20000;
        ammo.rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.LC, Faction.CWF)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }
    /*
     * private static AmmoType createCLLongTomCannonAmmo() { AmmoType ammo = new
     * AmmoType();
     *
     * ammo.name = "Long Tom Cannon Ammo"; ammo.shortName = "Long Tom Cannon";
     * ammo.setInternalName("CLLongTomCannonAmmo");
     * ammo.addLookupName("CLLongTomCannon Ammo");
     * ammo.addLookupName("CLLongTomArtilleryCannon Ammo");
     * ammo.addLookupName("CL Ammo Long Tom Cannon");
     * ammo.addLookupName("CL Long Tom Cannon Ammo"); ammo.damagePerShot = 1;
     * ammo.rackSize = 20; ammo.ammoType = AmmoTypeEnum.LONG_TOM_CANNON; ammo.shots =
     * 5; ammo.bv = 41; ammo.cost = 20000; ammo.rulesRefs = "285, TO";
     *
     * ammo.techAdvancement.setTechBase(TechBase.CLAN);
     * ammo.techAdvancement.setClanAdvancement(3032, 3072, DATE_NONE);
     * ammo.techAdvancement.setTechRating(TechRating.B);
     * ammo.techAdvancement.setAvailability(AvailabilityValue.X, AvailabilityValue.F,
     * AvailabilityValue.E, AvailabilityValue.D }); return ammo; }
     */

    private static AmmoType createISSniperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Sniper Cannon Ammo";
        ammo.shortName = "Sniper Cannon";
        ammo.setInternalName("ISSniperCannonAmmo");
        ammo.addLookupName("ISSniperCannon Ammo");
        ammo.addLookupName("ISSniperArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Sniper Cannon");
        ammo.addLookupName("IS Sniper Cannon Ammo");
        ammo.addLookupName("CLSniperCannonAmmo");
        ammo.addLookupName("CLSniperCannon Ammo");
        ammo.addLookupName("CLSniperArtilleryCannon Ammo");
        ammo.addLookupName("CL Ammo Sniper Cannon");
        ammo.addLookupName("CL Sniper Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.SNIPER_CANNON;
        ammo.shots = 10;
        ammo.bv = 10;
        ammo.cost = 15000;
        ammo.rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.LC, Faction.CWF)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    /*
     * private static AmmoType createCLSniperCannonAmmo() { AmmoType ammo = new
     * AmmoType();
     *
     * ammo.name = "Sniper Cannon Ammo"; ammo.shortName = "Sniper Cannon";
     * ammo.setInternalName("CLSniperCannonAmmo");
     * ammo.addLookupName("CLSniperCannon Ammo");
     * ammo.addLookupName("CLSniperArtilleryCannon Ammo");
     * ammo.addLookupName("CL Ammo Sniper Cannon");
     * ammo.addLookupName("CL Sniper Cannon Ammo"); ammo.damagePerShot = 1;
     * ammo.rackSize = 10; ammo.ammoType = AmmoTypeEnum.SNIPER_CANNON; ammo.shots =
     * 10; ammo.bv = 10; ammo.cost = 15000; ammo.rulesRefs = "285, TO";
     *
     * ammo.techAdvancement.setTechBase(TechBase.CLAN);
     * ammo.techAdvancement.setClanAdvancement(3032, 3072, DATE_NONE);
     * ammo.techAdvancement.setTechRating(TechRating.B);
     * ammo.techAdvancement.setAvailability(AvailabilityValue.X, AvailabilityValue.F,
     * AvailabilityValue.E, AvailabilityValue.D }); return ammo; }
     */

    private static AmmoType createISThumperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thumper Cannon Ammo";
        ammo.shortName = "Thumper Cannon";
        ammo.setInternalName("ISThumperCannonAmmo");
        ammo.addLookupName("ISThumperCannon Ammo");
        ammo.addLookupName("ISThumperArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Thumper Cannon");
        ammo.addLookupName("IS Thumper Cannon Ammo");
        ammo.addLookupName("CLThumperCannonAmmo");
        ammo.addLookupName("CLThumperCannon Ammo");
        ammo.addLookupName("CLThumperArtilleryCannon Ammo");
        ammo.addLookupName("CL Ammo Thumper Cannon");
        ammo.addLookupName("CL Thumper Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.THUMPER_CANNON;
        ammo.shots = 20;
        ammo.bv = 5;
        ammo.cost = 10000;
        ammo.rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.LC, Faction.CWF)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    /*
     * private static AmmoType createCLThumperCannonAmmo() { AmmoType ammo = new
     * AmmoType();
     *
     * ammo.name = "Thumper Cannon Ammo"; ammo.shortName = "Thumper Cannon";
     * ammo.setInternalName("CLThumperCannonAmmo");
     * ammo.addLookupName("CLThumperCannon Ammo");
     * ammo.addLookupName("CLThumperArtilleryCannon Ammo");
     * ammo.addLookupName("CL Ammo Thumper Cannon");
     * ammo.addLookupName("CL Thumper Cannon Ammo"); ammo.damagePerShot = 1;
     * ammo.rackSize = 5; ammo.ammoType = AmmoTypeEnum.THUMPER_CANNON; ammo.shots =
     * 20; ammo.bv = 5; ammo.cost = 10000; ammo.rulesRefs = "285, TO";
     *
     * ammo.techAdvancement.setTechBase(TechBase.CLAN);
     * ammo.techAdvancement.setClanAdvancement(3032, 3072, DATE_NONE);
     * ammo.techAdvancement.setTechRating(TechRating.B);
     * ammo.techAdvancement.setAvailability(AvailabilityValue.X, AvailabilityValue.F,
     * AvailabilityValue.E, AvailabilityValue.D }); return ammo; }
     */

    private static AmmoType createBATubeArtyAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA Tube Artillery Ammo";
        ammo.shortName = "Tube Artillery";
        ammo.setInternalName("ISBATubeArtilleryAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.BA_TUBE;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 2;
        ammo.bv = 4;
        ammo.kgPerShot = 15;
        ammo.cost = 900;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3070, 3075, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS);
        return ammo;
    }

    // AUTOCANNON AND RIFLE AMMO

    private static AmmoType createISAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AC/2 Ammo";
        ammo.shortName = "AC/2";
        ammo.setInternalName("IS Ammo AC/2");
        ammo.addLookupName("ISAC2 Ammo");
        ammo.addLookupName("IS Autocannon/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 1000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2290, 2300, 2305, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2290, 2300, 2305, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AC/5 Ammo";
        ammo.shortName = "AC/5";
        ammo.setInternalName("IS Ammo AC/5");
        ammo.addLookupName("ISAC5 Ammo");
        ammo.addLookupName("IS Autocannon/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 20;
        ammo.bv = 9;
        ammo.cost = 4500;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2240, 2250, 2255, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2240, 2250, 2255, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AC/10 Ammo";
        ammo.shortName = "AC/10";
        ammo.setInternalName("IS Ammo AC/10");
        ammo.addLookupName("ISAC10 Ammo");
        ammo.addLookupName("IS Autocannon/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 10;
        ammo.bv = 15;
        ammo.cost = 6000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createISAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AC/20 Ammo";
        ammo.shortName = "AC/20";
        ammo.setInternalName("IS Ammo AC/20");
        ammo.addLookupName("ISAC20 Ammo");
        ammo.addLookupName("IS Autocannon/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 5;
        ammo.bv = 22;
        ammo.cost = 10000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
        return ammo;
    }

    private static AmmoType createISLAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LAC/2 Ammo";
        ammo.shortName = "LAC/2";
        ammo.setInternalName("IS Ammo LAC/2");
        ammo.addLookupName("ISLAC2 Ammo");
        ammo.addLookupName("IS Light Autocannon/2 Ammo");
        ammo.addLookupName("Light AC/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.LAC;
        ammo.shots = 45;
        ammo.bv = 4;
        ammo.cost = 2000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LAC/5 Ammo";
        ammo.shortName = "LAC/5";
        ammo.setInternalName("IS Ammo LAC/5");
        ammo.addLookupName("ISLAC5 Ammo");
        ammo.addLookupName("IS Light Autocannon/5 Ammo");
        ammo.addLookupName("Light AC/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LAC;
        ammo.shots = 20;
        ammo.bv = 8;
        ammo.cost = 5000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createCLPROAC2Ammo() {

        AmmoType ammo = new AmmoType();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 3 LINES
        ammo.name = "ProtoMech AC/2 Ammo";
        ammo.shortName = "Proto AC/2";
        ammo.setInternalName("Clan ProtoMech AC/2 Ammo");
        ammo.addLookupName("CLProtoAC2Ammo");
        ammo.addLookupName("CLProtoAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.PAC;
        ammo.shots = 40;
        ammo.bv = 4;
        ammo.cost = 1200;
        ammo.rulesRefs = "286, TO";
        ammo.kgPerShot = 25;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CBS)
              .setProductionFactions(Faction.CBS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLPROAC4Ammo() {
        AmmoType ammo = new AmmoType();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 3 LINES
        ammo.name = "ProtoMech AC/4 Ammo";
        ammo.shortName = "Proto AC/4";
        ammo.setInternalName("Clan ProtoMech AC/4 Ammo");
        ammo.addLookupName("CLProtoAC4Ammo");
        ammo.addLookupName("CLProtoAC4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.PAC;
        ammo.shots = 20;
        ammo.bv = 6;
        ammo.cost = 4800;
        ammo.rulesRefs = "286, TO";
        ammo.kgPerShot = 50;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CBS)
              .setProductionFactions(Faction.CBS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLPROAC8Ammo() {
        AmmoType ammo = new AmmoType();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 3 LINES
        ammo.name = "ProtoMech AC/8 Ammo";
        ammo.shortName = "Proto AC/8";
        ammo.setInternalName("Clan ProtoMech AC/8 Ammo");
        ammo.addLookupName("CLProtoAC8Ammo");
        ammo.addLookupName("CLProtoAC8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.PAC;
        ammo.shots = 10;
        ammo.bv = 8;
        ammo.cost = 6300;
        ammo.kgPerShot = 100;
        ammo.rulesRefs = "286, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CBS)
              .setProductionFactions(Faction.CBS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISHVAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "HVAC/2 Ammo";
        ammo.shortName = "HVAC/2";
        ammo.setInternalName("IS Ammo HVAC/2");
        ammo.addLookupName("ISHVAC2 Ammo");
        ammo.addLookupName("IS Hyper Velocity Autocannon/2 Ammo");
        ammo.addLookupName("Hyper Velocity AC/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.HYPER_VELOCITY;
        ammo.shots = 30;
        ammo.bv = 7;
        ammo.cost = 3000;
        ammo.rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3059, 3079)
              .setISApproximate(false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISHVAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "HVAC/5 Ammo";
        ammo.shortName = "HVAC/5";
        ammo.setInternalName("IS Ammo HVAC/5");
        ammo.addLookupName("ISHVAC5 Ammo");
        ammo.addLookupName("IS Hyper Velocity Autocannon/5 Ammo");
        ammo.addLookupName(" Hyper Velocity AC/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.HYPER_VELOCITY;
        ammo.shots = 15;
        ammo.bv = 14;
        ammo.cost = 10000;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3059, 3079)
              .setISApproximate(false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISHVAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "HVAC/10 Ammo";
        ammo.shortName = "HVAC/10";
        ammo.setInternalName("IS Ammo HVAC/10");
        ammo.addLookupName("ISHVAC10 Ammo");
        ammo.addLookupName("IS Hyper Velocity Autocannon/10 Ammo");
        ammo.addLookupName("Hyper Velocity AC/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.HYPER_VELOCITY;
        ammo.shots = 8;
        ammo.bv = 20;
        ammo.cost = 20000;
        ammo.rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3059, 3079)
              .setISApproximate(false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    // LB-X Cluster Ammos

    private static AmmoType createCLLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X Cluster Ammo";
        ammo.shortName = "LB 2-X Cluster";
        ammo.setInternalName("Clan LB 2-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 2-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC2 CL Ammo");
        ammo.addLookupName("Clan LB 2-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.cost = 3300;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.CGS);
        return ammo;
    }

    private static AmmoType createCLLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X Cluster Ammo";
        ammo.shortName = "LB 5-X Cluster";
        ammo.setInternalName("Clan LB 5-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 5-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC5 CL Ammo");
        ammo.addLookupName("Clan LB 5-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 20;
        ammo.bv = 12;
        ammo.cost = 15000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 10-X Cluster Ammo";
        ammo.shortName = "LB 10-X Cluster";
        ammo.setInternalName("Clan LB 10-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 10-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC10 CL Ammo");
        ammo.addLookupName("Clan LB 10-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 20000;
        ammo.kgPerShot = 100;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.CLAN)
              .setReintroductionFactions(Faction.CLAN);
        return ammo;
    }

    private static AmmoType createCLLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X Cluster Ammo";
        ammo.shortName = "LB 20-X Cluster";
        ammo.setInternalName("Clan LB 20-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 20-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC20 CL Ammo");
        ammo.addLookupName("Clan LB 20-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 34000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createISLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X Cluster Ammo";
        ammo.shortName = "LB 2-X Cluster";
        ammo.setInternalName("IS LB 2-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 2-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC2 CL Ammo");
        ammo.addLookupName("IS LB 2-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 3300;
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X Cluster Ammo";
        ammo.shortName = "LB 5-X Cluster";
        ammo.setInternalName("IS LB 5-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 5-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC5 CL Ammo");
        ammo.addLookupName("IS LB 5-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 20;
        ammo.bv = 10;
        ammo.cost = 15000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 10-X Cluster Ammo";
        ammo.shortName = "LB 10-X Cluster";
        ammo.setInternalName("IS LB 10-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 10-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC10 CL Ammo");
        ammo.addLookupName("IS LB 10-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 20000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2590, 2595, 3040, 2840, 3035)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X Cluster Ammo";
        ammo.shortName = "LB 20-X Cluster";
        ammo.setInternalName("IS LB 20-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 20-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC20 CL Ammo");
        ammo.addLookupName("IS LB 20-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 34000;
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createCLLB2XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X AC Ammo";
        ammo.shortName = "LB 2-X";
        ammo.setInternalName("Clan LB 2-X AC Ammo");
        ammo.addLookupName("Clan Ammo 2-X");
        ammo.addLookupName("CLLBXAC2 Ammo");
        ammo.addLookupName("Clan LB 2-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.cost = 2000;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.CGS);
        return ammo;
    }

    private static AmmoType createCLLB5XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X AC Ammo";
        ammo.shortName = "LB 5-X";
        ammo.setInternalName("Clan LB 5-X AC Ammo");
        ammo.addLookupName("Clan Ammo 5-X");
        ammo.addLookupName("CLLBXAC5 Ammo");
        ammo.addLookupName("Clan LB 5-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 20;
        ammo.bv = 12;
        ammo.cost = 9000;
        ammo.kgPerShot = 50;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLLB10XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 10-X AC Ammo";
        ammo.shortName = "LB 10-X";
        ammo.setInternalName("Clan LB 10-X AC Ammo");
        ammo.addLookupName("Clan Ammo 10-X");
        ammo.addLookupName("CLLBXAC10 Ammo");
        ammo.addLookupName("Clan LB 10-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 12000;
        ammo.kgPerShot = 100;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.CLAN)
              .setReintroductionFactions(Faction.CLAN);
        return ammo;
    }

    private static AmmoType createCLLB20XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X AC Ammo";
        ammo.shortName = "LB 20-X";
        ammo.setInternalName("Clan LB 20-X AC Ammo");
        ammo.addLookupName("Clan Ammo 20-X");
        ammo.addLookupName("CLLBXAC20 Ammo");
        ammo.addLookupName("Clan LB 20-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 20000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createISLB2XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X AC Ammo";
        ammo.shortName = "LB 2-X";
        ammo.setInternalName("IS LB 2-X AC Ammo");
        ammo.addLookupName("IS Ammo 2-X");
        ammo.addLookupName("ISLBXAC2 Ammo");
        ammo.addLookupName("IS LB 2-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 2000;
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLB5XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X AC Ammo";
        ammo.shortName = "LB 5-X";
        ammo.setInternalName("IS LB 5-X AC Ammo");
        ammo.addLookupName("IS Ammo 5-X");
        ammo.addLookupName("ISLBXAC5 Ammo");
        ammo.addLookupName("IS LB 5-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 20;
        ammo.bv = 10;
        ammo.cost = 9000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLB10XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 10-X AC Ammo";
        ammo.shortName = "LB 10-X";
        ammo.setInternalName("IS LB 10-X AC Ammo");
        ammo.addLookupName("IS Ammo 10-X");
        ammo.addLookupName("ISLBXAC10 Ammo");
        ammo.addLookupName("IS LB 10-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 12000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2590, 2595, 3040, 2840, 3035)
              .setISApproximate(false, false, false, false, true)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLB20XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X AC Ammo";
        ammo.shortName = "LB 20-X";
        ammo.setInternalName("IS LB 20-X AC Ammo");
        ammo.addLookupName("IS Ammo 20-X");
        ammo.addLookupName("ISLBXAC20 Ammo");
        ammo.addLookupName("IS LB 20-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_LBX;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 20000;
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createCLUltra2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/2 Ammo";
        ammo.shortName = "Ultra AC/2";
        ammo.setInternalName("Clan Ultra AC/2 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/2");
        ammo.addLookupName("CLUltraAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 8;
        ammo.cost = 1000;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CLAN)
              .setProductionFactions(Faction.CLAN);
        return ammo;
    }

    private static AmmoType createCLUltra5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/5 Ammo";
        ammo.shortName = "Ultra AC/5";
        ammo.setInternalName("Clan Ultra AC/5 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/5");
        ammo.addLookupName("CLUltraAC5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 15;
        ammo.cost = 9000;
        ammo.kgPerShot = 50;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CLAN)
              .setProductionFactions(Faction.CLAN);
        return ammo;
    }

    private static AmmoType createCLUltra10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/10 Ammo";
        ammo.shortName = "Ultra AC/10";
        ammo.setInternalName("Clan Ultra AC/10 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/10");
        ammo.addLookupName("CLUltraAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 12000;
        ammo.kgPerShot = 100;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CLAN)
              .setProductionFactions(Faction.CLAN);
        return ammo;
    }

    private static AmmoType createCLUltra20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/20 Ammo";
        ammo.shortName = "Ultra AC/20";
        ammo.setInternalName("Clan Ultra AC/20 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/20");
        ammo.addLookupName("CLUltraAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 42;
        ammo.cost = 20000;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CLAN)
              .setProductionFactions(Faction.CLAN);
        return ammo;
    }

    private static AmmoType createISUltra2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/2 Ammo";
        ammo.shortName = "Ultra AC/2";
        ammo.setInternalName("IS Ultra AC/2 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/2");
        ammo.addLookupName("ISUltraAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 7;
        ammo.cost = 1000;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISUltra5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/5 Ammo";
        ammo.shortName = "Ultra AC/5";
        ammo.setInternalName("IS Ultra AC/5 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/5");
        ammo.addLookupName("ISUltraAC5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 9000;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2635, 2640, 3040, 2915, 3035)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISUltra10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/10 Ammo";
        ammo.shortName = "Ultra AC/10";
        ammo.setInternalName("IS Ultra AC/10 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/10");
        ammo.addLookupName("ISUltraAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 12000;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISUltra20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/20 Ammo";
        ammo.shortName = "Ultra AC/20";
        ammo.setInternalName("IS Ultra AC/20 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/20");
        ammo.addLookupName("ISUltraAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 35;
        ammo.cost = 20000;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3057, 3060, 3061, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.LC, Faction.FW);
        return ammo;
    }

    private static AmmoType createISRotary2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/2 Ammo";
        ammo.shortName = "RAC/2";
        ammo.setInternalName("ISRotaryAC2 Ammo");
        ammo.addLookupName("IS Rotary AC/2 Ammo");
        ammo.addLookupName("ISRAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 45;
        ammo.bv = 15;
        ammo.cost = 3000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISRotary5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/5 Ammo";
        ammo.shortName = "RAC/5";
        ammo.setInternalName("ISRotaryAC5 Ammo");
        ammo.addLookupName("IS Rotary AC/5 Ammo");
        ammo.addLookupName("ISRAC5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 20;
        ammo.bv = 31;
        ammo.cost = 12000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createCLRotary2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/2 Ammo";
        ammo.shortName = "RAC/2";
        ammo.setInternalName("CLRotaryAC2 Ammo");
        ammo.addLookupName("CL Rotary AC/2 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 45;
        ammo.bv = 20;
        ammo.cost = 5000;
        ammo.kgPerShot = 22.2;
        ammo.rulesRefs = "286, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3073, DATE_NONE, 3104, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLRotary5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/5 Ammo";
        ammo.shortName = "RAC/5";
        ammo.setInternalName("CLRotaryAC5 Ammo");
        ammo.addLookupName("CL Rotary AC/5 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 20;
        ammo.bv = 43;
        ammo.cost = 13000;
        ammo.rulesRefs = "286, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3073, DATE_NONE, 3104, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISLightRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Rifle Ammo";
        ammo.shortName = "Light Rifle";
        ammo.setInternalName("IS Ammo Light Rifle");
        ammo.addLookupName("ISLight Rifle Ammo");
        ammo.addLookupName("ISLightRifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.RIFLE;
        ammo.shots = 18;
        ammo.bv = 3;
        ammo.cost = 1000;
        ammo.rulesRefs = "338, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.D)
              .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMediumRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium Rifle Ammo";
        ammo.shortName = "Medium Rifle";
        ammo.setInternalName("IS Ammo Medium Rifle");
        ammo.addLookupName("ISMedium Rifle Ammo");
        ammo.addLookupName("ISMediumRifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.RIFLE;
        ammo.shots = 9;
        ammo.bv = 6;
        ammo.cost = 1000;
        ammo.rulesRefs = "338, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.D)
              .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISHeavyRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Rifle Ammo";
        ammo.shortName = "Heavy Rifle";
        ammo.setInternalName("IS Ammo Heavy Rifle");
        ammo.addLookupName("ISHeavy Rifle Ammo");
        ammo.addLookupName("ISHeavyRifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.RIFLE;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.cost = 1000;
        ammo.rulesRefs = "338, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.D)
              .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Chemical Laser Ammos

    private static AmmoType createCLSmallChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Small Chemical Laser Ammo";
        ammo.shortName = "Small Chemical Laser";
        ammo.setInternalName("CLSmallChemLaserAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.CHEMICAL_LASER;
        ammo.shots = 60;
        ammo.bv = 1;
        ammo.cost = 30000;
        ammo.rulesRefs = "320, TO";
        ammo.kgPerShot = 16.6;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLMediumChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium Chemical Laser Ammo";
        ammo.shortName = "Medium Chemical Laser";
        ammo.setInternalName("CLMediumChemLaserAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.CHEMICAL_LASER;
        ammo.shots = 30;
        ammo.bv = 5;
        ammo.cost = 30000;
        ammo.rulesRefs = "320, TO";
        ammo.kgPerShot = 33.33;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLLargeChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Large Chemical Laser Ammo";
        ammo.shortName = "Large Chemical Laser";
        ammo.setInternalName("CLLargeChemLaserAmmo");
        ammo.damagePerShot = 8;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.CHEMICAL_LASER;
        ammo.shots = 10;
        ammo.bv = 12;
        ammo.cost = 30000;
        ammo.rulesRefs = "320, TO";
        ammo.kgPerShot = 100;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Flamer and Fluid Gun/Sprayer Ammo (Mutators Above)

    private static AmmoType createISHeavyFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Flamer Ammo";
        ammo.shortName = "Heavy Flamer";
        ammo.setInternalName("IS Heavy Flamer Ammo");
        ammo.addLookupName("IS Ammo Heavy Flamer");
        ammo.addLookupName("ISHeavyFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.HEAVY_FLAMER;
        ammo.shots = 10;
        ammo.bv = 2;
        ammo.cost = 2000;
        ammo.rulesRefs = "312, TO";

        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3068, 3079, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
        return ammo;
    }

    private static AmmoType createCLHeavyFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Flamer Ammo";
        ammo.shortName = "Heavy Flamer";
        ammo.setInternalName("CL Heavy Flamer Ammo");
        ammo.addLookupName("CL Ammo Heavy Flamer");
        ammo.addLookupName("Clan Ammo Heavy Flamer");
        ammo.addLookupName("Clan Heavy Flamer Ammo");
        ammo.addLookupName("CLHeavyFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.HEAVY_FLAMER;
        ammo.shots = 10;
        ammo.bv = 2;
        ammo.cost = 2000;
        ammo.rulesRefs = "312, TO";
        ammo.kgPerShot = 10;
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3065, 3067, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CJF)
              .setProductionFactions(Faction.CJF);
        return ammo;
    }

    private static AmmoType createISVehicleFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Vehicle Flamer Ammo";
        ammo.shortName = "Flamer";
        ammo.setInternalName("IS Vehicle Flamer Ammo");
        ammo.addLookupName("IS Ammo Vehicle Flamer");
        ammo.addLookupName("ISVehicleFlamer Ammo");
        ammo.addLookupName("Clan Vehicle Flamer Ammo");
        ammo.addLookupName("Clan Ammo Vehicle Flamer");
        ammo.addLookupName("CLVehicleFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.VEHICLE_FLAMER;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.rulesRefs = "218, TM";
        ammo.kgPerShot = 50;
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createISFluidGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Fluid Gun Ammo";
        ammo.shortName = "Fluid Gun";
        ammo.setInternalName("ISFluidGun Ammo");
        ammo.damagePerShot = 2; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.FLUID_GUN;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.explosive = false;
        ammo.rulesRefs = "313, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createCLFluidGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Fluid Gun Ammo";
        ammo.shortName = "Fluid Gun";
        ammo.setInternalName("CLFluidGun Ammo");
        ammo.damagePerShot = 2; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.FLUID_GUN;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.rulesRefs = "313, TO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    // Gauss Rifle Ammos

    private static AmmoType createISGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Gauss Rifle Ammo [IS]";
        ammo.shortName = "Gauss Ammo";
        ammo.setInternalName("IS Gauss Ammo");
        ammo.addLookupName("IS Ammo Gauss");
        ammo.addLookupName("ISGauss Ammo");
        ammo.addLookupName("IS Gauss Rifle Ammo");
        ammo.addLookupName("ISGaussRifle Ammo");
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.GAUSS;
        ammo.shots = 8;
        ammo.bv = 40;
        ammo.cost = 20000;
        ammo.rulesRefs = "219, TM";
        /*
         * This is going to be a rare difference between TT and MM. Removing the
         * Extinction date on Gauss ammo. The prototype share the base ammo and rather
         * than make a whole new ammo, just going say the IS can figure out how to make
         * large round steel balls.
         */
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2587, 2590, 3045, DATE_NONE, 3038)
              .setISApproximate(false, false, false, false, true)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FC, Faction.FW, Faction.DC);

        return ammo;
    }

    private static AmmoType createCLGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Gauss Rifle Ammo [Clan]";
        ammo.shortName = "Gauss Ammo";
        ammo.setInternalName("Clan Gauss Ammo");
        ammo.addLookupName("Clan Ammo Gauss");
        ammo.addLookupName("CLGauss Ammo");
        ammo.addLookupName("Clan Gauss Rifle Ammo");
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.GAUSS;
        ammo.shots = 8;
        ammo.bv = 40;
        ammo.cost = 20000;
        ammo.kgPerShot = 125;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(2822, 2828, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CBR)
              .setProductionFactions(Faction.CBR);
        return ammo;
    }

    private static AmmoType createISLTGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Gauss Rifle Ammo";
        ammo.shortName = "Light Gauss";
        ammo.setInternalName("IS Light Gauss Ammo");
        ammo.addLookupName("ISLightGauss Ammo");
        ammo.addLookupName("IS Light Gauss Rifle Ammo");
        ammo.addLookupName("ISLightGaussRifle Ammo");
        ammo.damagePerShot = 8;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.GAUSS_LIGHT;
        ammo.shots = 16;
        ammo.bv = 20;
        ammo.cost = 20000;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3049, 3056, 3065, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISHVGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Gauss Rifle Ammo";
        ammo.shortName = "Heavy Gauss";
        ammo.setInternalName("ISHeavyGauss Ammo");
        ammo.addLookupName("IS Heavy Gauss Rifle Ammo");
        ammo.addLookupName("ISHeavyGaussRifle Ammo");
        ammo.damagePerShot = 25; // actually variable
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.GAUSS_HEAVY;
        ammo.shots = 4;
        ammo.bv = 43;
        ammo.cost = 20000;
        ammo.rulesRefs = "218, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3051, 3061, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FC);
        return ammo;
    }

    private static AmmoType createCLAPGaussRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-Personnel Gauss Rifle Ammo";
        ammo.shortName = "AP Gauss Ammo";
        ammo.setInternalName("CLAPGaussRifle Ammo");
        ammo.addLookupName("Clan AP Gauss Rifle Ammo");
        ammo.addLookupName("Clan Anti-Personnel Gauss Rifle Ammo");
        ammo.damagePerShot = 3;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.APGAUSS;
        ammo.shots = 40;
        ammo.bv = 3;
        ammo.cost = 1000;
        ammo.kgPerShot = 25;
        ammo.rulesRefs = "218, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3065, 3069, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CJF)
              .setProductionFactions(Faction.CJF);
        return ammo;
    }

    private static AmmoType createCLHAG20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Hyper-Assault Gauss Rifle/20 Ammo";
        ammo.shortName = "HAG/20 Ammo";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG20 Ammo");
        ammo.addLookupName("Clan HAG 20 Ammo");
        ammo.addLookupName("HAG/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.HAG;
        ammo.shots = 6;
        ammo.bv = 33;
        ammo.cost = 30000;
        ammo.kgPerShot = 166.66;
        ammo.explosive = false;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3062, 3068, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createCLHAG30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Hyper-Assault Gauss Rifle/30 Ammo";
        ammo.shortName = "HAG/30 Ammo";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG30 Ammo");
        ammo.addLookupName("Clan HAG 30 Ammo");
        ammo.addLookupName("HAG/30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoTypeEnum.HAG;
        ammo.shots = 4;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3062, 3068, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createCLHAG40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Hyper-Assault Gauss Rifle/40 Ammo";
        ammo.shortName = "HAG/40 Ammo";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG40 Ammo");
        ammo.addLookupName("Clan HAG 40 Ammo");
        ammo.addLookupName("HAG/40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoTypeEnum.HAG;
        ammo.shots = 3;
        ammo.bv = 67;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3062, 3068, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createISIHVGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Heavy Gauss Rifle Ammo";
        ammo.shortName = "iHeavy Gauss Ammo";
        ammo.setInternalName("ISImprovedHeavyGauss Ammo");
        ammo.addLookupName("IS Improved Heavy Gauss Rifle Ammo");
        ammo.addLookupName("ISImprovedHeavyGaussRifle Ammo");
        ammo.damagePerShot = 22;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.IGAUSS_HEAVY;
        ammo.shots = 4;
        ammo.bv = 48;
        ammo.cost = 20000;
        ammo.rulesRefs = "313, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, DATE_NONE, 3081, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMagshotGRAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Magshot Gauss Rifle Ammo";
        ammo.shortName = "Magshot Ammo";
        ammo.setInternalName("ISMagshotGR Ammo");
        ammo.addLookupName("IS Magshot GR Ammo");
        ammo.damagePerShot = 2;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.MAGSHOT;
        ammo.shots = 50;
        ammo.bv = 2;
        ammo.cost = 1000;
        ammo.rulesRefs = "314, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(3059, 3072, 3078, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISSBGaussRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Silver Bullet Gauss Rifle Ammo";
        ammo.shortName = "Silver Bullet Ammo";
        ammo.setInternalName("Silver Bullet Gauss Ammo");
        ammo.addLookupName("IS SBGauss Rifle Ammo");
        ammo.addLookupName("ISSBGauss Ammo");
        ammo.addLookupName("ISSBGaussRifleAmmo");
        ammo.explosive = false;
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.SBGAUSS;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 8;
        ammo.bv = 25;
        ammo.cost = 25000;
        ammo.toHitModifier = -1;
        ammo.rulesRefs = "314, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3051, DATE_NONE, 3080, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Grenade Launcher Ammo (See Mutators above)

    private static AmmoType createISVGLAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Fragmentation Grenades [VGL]";
        ammo.shortName = "VGL Fragmentation";
        ammo.setInternalName("IS Ammo VGL");
        ammo.addLookupName("ISVehicularGrenadeLauncherAmmo");
        ammo.addLookupName("CL Ammo VGL");
        ammo.addLookupName("CLVehicularGrenadeLauncherAmmo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.VGL;
        ammo.munitionType = EnumSet.of(Munitions.M_STANDARD);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "315, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_PS, DATE_ES, 3080, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setClanAdvancement(DATE_PS, DATE_ES, 3080, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLVGLAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Fragmentation Grenades [VGL]";
        ammo.shortName = "VGL Fragmentation";
        ammo.setInternalName("CL Ammo VGL");
        ammo.addLookupName("CLVehicularGrenadeLauncherAmmo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.VGL;
        ammo.munitionType = EnumSet.of(Munitions.M_STANDARD);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "315, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setClanAdvancement(DATE_PS, DATE_ES, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, true, false);
        return ammo;
    }

    // Machine Gun Ammos
    // Standard MGs
    private static AmmoType createISMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Machine Gun Ammo [Full]";
        ammo.shortName = "MG Ammo";
        ammo.setInternalName("IS Ammo MG - Full");
        ammo.addLookupName("ISMG Ammo (200)");
        ammo.addLookupName("ISMG Ammo Full");
        ammo.addLookupName("IS Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, 2826, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createCLMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Machine Gun Ammo [Full]";
        ammo.shortName = "MG Ammo";
        ammo.setInternalName("Clan Machine Gun Ammo - Full");
        ammo.addLookupName("Clan Ammo MG - Full");
        ammo.addLookupName("CLMG Ammo (200)");
        ammo.addLookupName("Clan Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.A)
              .setClanAdvancement(2821, 2825, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createISMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Machine Gun Ammo [Half]";
        ammo.shortName = "MG Ammo";
        ammo.setInternalName("IS Machine Gun Ammo - Half");
        ammo.addLookupName("IS Ammo MG - Half");
        ammo.addLookupName("ISMG Ammo (100)");
        ammo.addLookupName("ISMG Ammo Half");
        ammo.addLookupName("IS Machine Gun Ammo (1/2 ton)");
        ammo.addLookupName("Half Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        ammo.cost = 500;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, 2826, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createCLMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Machine Gun Ammo [Half]";
        ammo.shortName = "MG Ammo";
        ammo.setInternalName("Clan Machine Gun Ammo - Half");
        ammo.addLookupName("Clan Ammo MG - Half");
        ammo.addLookupName("CLMG Ammo (100)");
        ammo.addLookupName("Clan Machine Gun Ammo (1/2 ton)");
        ammo.addLookupName("Half Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 500;
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.A)
              .setClanAdvancement(2821, 2825, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    // Light MGs

    private static AmmoType createISLightMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Full]";
        ammo.shortName = "LMG Ammo";
        ammo.setInternalName("IS Light Machine Gun Ammo - Full");
        ammo.addLookupName("ISLightMG Ammo (200)");
        ammo.addLookupName("IS Light Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3064, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC);
        return ammo;
    }

    private static AmmoType createCLLightMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Full]";
        ammo.shortName = "LMG Ammo";
        ammo.setInternalName("Clan Light Machine Gun Ammo - Full");
        ammo.addLookupName("CLLightMG Ammo (200)");
        ammo.addLookupName("Clan Light Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(3055, 3060, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createISLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Half]";
        ammo.shortName = "LMG Ammo";
        ammo.setInternalName("IS Light Machine Gun Ammo - Half");
        ammo.addLookupName("ISLightMG Ammo (100)");
        ammo.addLookupName("IS Light Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 250;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3064, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC);
        return ammo;
    }

    private static AmmoType createCLLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Half]";
        ammo.shortName = "LMG Ammo";
        ammo.setInternalName("Clan Light Machine Gun Ammo - Half");
        ammo.addLookupName("CLLightMG Ammo (100)");
        ammo.addLookupName("Clan Light Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 250;
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(3055, 3060, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    // Heavy MGs

    private static AmmoType createISHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Full]";
        ammo.shortName = "HMG Ammo";
        ammo.setInternalName("IS Heavy Machine Gun Ammo - Full");
        ammo.addLookupName("ISHeavyMG Ammo (100)");
        ammo.addLookupName("IS Heavy Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3063, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TC)
              .setProductionFactions(Faction.TC);
        return ammo;
    }

    private static AmmoType createISHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Half]";
        ammo.shortName = "HMG Ammo";
        ammo.setInternalName("IS Heavy Machine Gun Ammo - Half");
        ammo.addLookupName("ISHeavyMG Ammo (50)");
        ammo.addLookupName("IS Heavy Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 50;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 500;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3063, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TC)
              .setProductionFactions(Faction.TC);
        return ammo;
    }

    private static AmmoType createCLHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Full]";
        ammo.shortName = "HMG Ammo";
        ammo.setInternalName("Clan Heavy Machine Gun Ammo - Full");
        ammo.addLookupName("CLHeavyMG Ammo (100)");
        ammo.addLookupName("Clan Heavy Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(3054, 3059, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Half]";
        ammo.shortName = "HMG Ammo";
        ammo.setInternalName("Clan Heavy Machine Gun Ammo - Half");
        ammo.addLookupName("CLHeavyMG Ammo (50)");
        ammo.addLookupName("Clan Heavy Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 50;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 500;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(3054, 3059, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    // Mines - See Minefield.java
    // TODO - Need EMP mines (See IO pg 61 and TO 365)

    // Missile Launcher Munitions
    // Standard ATMs

    private static AmmoType createCLATM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/3 Ammo";
        ammo.shortName = "ATM 3";
        ammo.setInternalName("Clan Ammo ATM-3");
        ammo.addLookupName("CLATM3 Ammo");
        ammo.addLookupName("Clan ATM-3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/6 Ammo";
        ammo.shortName = "ATM 6";
        ammo.setInternalName("Clan Ammo ATM-6");
        ammo.addLookupName("CLATM6 Ammo");
        ammo.addLookupName("Clan ATM-6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/9 Ammo";
        ammo.shortName = "ATM 9";
        ammo.setInternalName("Clan Ammo ATM-9");
        ammo.addLookupName("CLATM9 Ammo");
        ammo.addLookupName("Clan ATM-9 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/12 Ammo";
        ammo.shortName = "ATM 12";
        ammo.setInternalName("Clan Ammo ATM-12");
        ammo.addLookupName("CLATM12 Ammo");
        ammo.addLookupName("Clan ATM-12 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // ATM Extended Range
    private static AmmoType createCLATM3ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/3 Ammo";
        ammo.shortName = "ATM 3 ER";
        ammo.setInternalName("Clan Ammo ATM-3 ER");
        ammo.addLookupName("CLATM3 ER Ammo");
        ammo.addLookupName("Clan ATM-3 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM6ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/6 Ammo";
        ammo.shortName = "ATM 6 ER";
        ammo.setInternalName("Clan Ammo ATM-6 ER");
        ammo.addLookupName("CLATM6 ER Ammo");
        ammo.addLookupName("Clan ATM-6 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM9ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/9 Ammo";
        ammo.shortName = "ATM 9 ER";
        ammo.setInternalName("Clan Ammo ATM-9 ER");
        ammo.addLookupName("CLATM9 ER Ammo");
        ammo.addLookupName("Clan ATM-9 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM12ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/12 Ammo";
        ammo.shortName = "ATM 12 ER";
        ammo.setInternalName("Clan Ammo ATM-12 ER");
        ammo.addLookupName("CLATM12 ER Ammo");
        ammo.addLookupName("Clan ATM-12 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // ATM HE AMMOs
    private static AmmoType createCLATM3HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/3 Ammo";
        ammo.shortName = "ATM 3 HE";
        ammo.setInternalName("Clan Ammo ATM-3 HE");
        ammo.addLookupName("CLATM3 HE Ammo");
        ammo.addLookupName("Clan ATM-3 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 75000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM6HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/6 Ammo";
        ammo.shortName = "ATM 6 HE";
        ammo.setInternalName("Clan Ammo ATM-6 HE");
        ammo.addLookupName("CLATM6 HE Ammo");
        ammo.addLookupName("Clan ATM-6 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 75000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM9HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/9 Ammo";
        ammo.shortName = "ATM 9 HE";
        ammo.setInternalName("Clan Ammo ATM-9 HE");
        ammo.addLookupName("CLATM9 HE Ammo");
        ammo.addLookupName("Clan ATM-9 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.cost = 75000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLATM12HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/12 Ammo";
        ammo.shortName = "ATM 12 HE";
        ammo.setInternalName("Clan Ammo ATM-12 HE");
        ammo.addLookupName("CLATM12 HE Ammo");
        ammo.addLookupName("Clan ATM-12 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.ATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.cost = 75000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // iATMs
    // iATM Standard

    private static AmmoType createCLIATM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/3 Ammo";
        ammo.shortName = "iATM 3";
        ammo.setInternalName("Clan Ammo iATM-3");
        ammo.addLookupName("CLIATM3 Ammo");
        ammo.addLookupName("Clan iATM-3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.shots = 20;
        ammo.bv = 21;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/6 Ammo";
        ammo.shortName = "iATM 6";
        ammo.setInternalName("Clan Ammo iATM-6");
        ammo.addLookupName("CLIATM6 Ammo");
        ammo.addLookupName("Clan iATM-6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.shots = 10;
        ammo.bv = 39;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/9 Ammo";
        ammo.shortName = "iATM 9";
        ammo.setInternalName("Clan Ammo iATM-9");
        ammo.addLookupName("CLIATM9 Ammo");
        ammo.addLookupName("Clan iATM-9 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.shots = 7;
        ammo.bv = 54;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/12 Ammo";
        ammo.shortName = "iATM 12";
        ammo.setInternalName("Clan Ammo iATM-12");
        ammo.addLookupName("CLIATM12 Ammo");
        ammo.addLookupName("Clan iATM-12 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.shots = 5;
        ammo.bv = 78;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // iATM ER
    private static AmmoType createCLIATM3ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/3 Ammo";
        ammo.shortName = "iATM 3 ER";
        ammo.setInternalName("Clan Ammo iATM-3 ER");
        ammo.addLookupName("CLIATM3 ER Ammo");
        ammo.addLookupName("Clan iATM-3 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 20;
        ammo.bv = 21;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM6ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/6 Ammo";
        ammo.shortName = "iATM 6 ER";
        ammo.setInternalName("Clan Ammo iATM-6 ER");
        ammo.addLookupName("CLIATM6 ER Ammo");
        ammo.addLookupName("Clan iATM-6 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 10;
        ammo.bv = 39;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM9ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/9 Ammo";
        ammo.shortName = "iATM 9 ER";
        ammo.setInternalName("Clan Ammo iATM-9 ER");
        ammo.addLookupName("CLIATM9 ER Ammo");
        ammo.addLookupName("Clan iATM-9 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 7;
        ammo.bv = 54;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM12ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/12 Ammo";
        ammo.shortName = "iATM 12 ER";
        ammo.setInternalName("Clan Ammo iATM-12 ER");
        ammo.addLookupName("CLIATM12 ER Ammo");
        ammo.addLookupName("Clan iATM-12 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_EXTENDED_RANGE);
        ammo.shots = 5;
        ammo.bv = 78;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // iATM HE Ammo
    private static AmmoType createCLIATM3HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/3 Ammo";
        ammo.shortName = "iATM 3 HE";
        ammo.setInternalName("Clan Ammo iATM-3 HE");
        ammo.addLookupName("CLIATM3 HE Ammo");
        ammo.addLookupName("Clan iATM-3 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 20;
        ammo.bv = 21;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM6HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/6 Ammo";
        ammo.shortName = "iATM 6 HE";
        ammo.setInternalName("Clan Ammo iATM-6 HE");
        ammo.addLookupName("CLIATM6 HE Ammo");
        ammo.addLookupName("Clan iATM-6 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 10;
        ammo.bv = 39;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM9HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/9 Ammo";
        ammo.shortName = "iATM 9 HE";
        ammo.setInternalName("Clan Ammo iATM-9 HE");
        ammo.addLookupName("CLIATM9 HE Ammo");
        ammo.addLookupName("Clan iATM-9 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 7;
        ammo.bv = 54;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLIATM12HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/12 Ammo";
        ammo.shortName = "iATM 12 HE";
        ammo.setInternalName("Clan Ammo iATM-12 HE");
        ammo.addLookupName("CLIATM12 HE Ammo");
        ammo.addLookupName("Clan iATM-12 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_HIGH_EXPLOSIVE);
        ammo.shots = 5;
        ammo.bv = 78;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // iATM Improved Inferno
    private static AmmoType createCLIATM3IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/3 Ammo";
        ammo.shortName = "iATM 3 IIW";
        ammo.setInternalName("Clan Ammo iATM-3 IIW");
        ammo.addLookupName("CLIATM3 IIW Ammo");
        ammo.addLookupName("Clan iATM-3 IIW Ammo");
        ammo.addLookupName("CLIIW3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IIW);
        ammo.shots = 20;
        ammo.bv = 27; // 21 * 1.3 = 27.3, round down (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        /*
         * IntOPs doesn't have an extinct date for IIW but code indicates its extinct.
         * Giving benefit of the doubt and assigning F code for Dark Age for Homeworld
         * Clans.
         */
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM6IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/6 Ammo";
        ammo.shortName = "iATM 6 IIW";
        ammo.setInternalName("Clan Ammo iATM-6 IIW");
        ammo.addLookupName("CLIATM6 IIW Ammo");
        ammo.addLookupName("Clan iATM-6 IIW Ammo");
        ammo.addLookupName("CLIIW6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IIW);
        ammo.shots = 10;
        ammo.bv = 51; // 50.7 round up (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        /*
         * IntOPs doesn't have an extinct date for IIW but code indicates its extinct.
         * Giving benefit of the doubt and assigning F code for Dark Age for Homeworld
         * Clans.
         */
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM9IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/9 Ammo";
        ammo.shortName = "iATM 9 IIW";
        ammo.setInternalName("Clan Ammo iATM-9 IIW");
        ammo.addLookupName("CLIATM9 IIW Ammo");
        ammo.addLookupName("Clan iATM-9 IIW Ammo");
        ammo.addLookupName("CLIIW9 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IIW);
        ammo.shots = 7;
        ammo.bv = 70; // 54 * 1.3 = 70.2, round down (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        /*
         * IntOPs doesn't have an extinct date for IIW but code indicates its extinct.
         * Giving benefit of the doubt and assigning F code for Dark Age for Homeworld
         * Clans.
         */
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM12IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/12 Ammo";
        ammo.shortName = "iATM 12 IIW";
        ammo.setInternalName("Clan Ammo iATM-12 IIW");
        ammo.addLookupName("CLIATM12 IIW Ammo");
        ammo.addLookupName("Clan iATM-12 IIW Ammo");
        ammo.addLookupName("CLIIW12 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IIW);
        ammo.shots = 5;
        ammo.bv = 101; // 78 * 1.3 = 101.4, round down (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        /*
         * IntOPs doesn't have an extinct date for IIW but code indicates its extinct.
         * Giving benefit of the doubt and assigning F code for Dark Age for Homeworld
         * Clans.
         */
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // iATM Improved Mag Pulse
    private static AmmoType createCLIATM3IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/3 Ammo";
        ammo.shortName = "iATM 3 IMP";
        ammo.setInternalName("Clan Ammo iATM-3 IMP");
        ammo.addLookupName("CLIATM3 IMP Ammo");
        ammo.addLookupName("Clan iATM-3 IMP Ammo");
        ammo.addLookupName("CLIMP3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IMP);
        ammo.shots = 20;
        ammo.bv = 42; // 21 * 2 = 42
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM6IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/6 Ammo";
        ammo.shortName = "iATM 6 IMP";
        ammo.setInternalName("Clan Ammo iATM-6 IMP");
        ammo.addLookupName("CLIATM6 IMP Ammo");
        ammo.addLookupName("Clan iATM-6 IMP Ammo");
        ammo.addLookupName("CLIMP6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IMP);
        ammo.shots = 10;
        ammo.bv = 78; // 39 * 2 = 78
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM9IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/9 Ammo";
        ammo.shortName = "iATM 9 IMP";
        ammo.setInternalName("Clan Ammo iATM-9 IMP");
        ammo.addLookupName("CLIATM9 IMP Ammo");
        ammo.addLookupName("Clan iATM-9 IMP Ammo");
        ammo.addLookupName("CLIMP9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IMP);
        ammo.shots = 7;
        ammo.bv = 108; // 54 * 2 = 108
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM12IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/12 Ammo";
        ammo.shortName = "iATM 12 IMP";
        ammo.setInternalName("Clan Ammo iATM-12 IMP");
        ammo.addLookupName("CLIATM12 IMP Ammo");
        ammo.addLookupName("Clan iATM-12 IMP Ammo");
        ammo.addLookupName("CLIMP12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.IATM;
        ammo.munitionType = EnumSet.of(Munitions.M_IATM_IMP);
        ammo.shots = 5;
        ammo.bv = 156; // 78 * 2 = 156
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
              .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // Standard LRMs (see Mutators Above)

    private static AmmoType createISLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("IS Ammo LRM-5");
        ammo.addLookupName("ISLRM5 Ammo");
        ammo.addLookupName("IS LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 24;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 10 Ammo";
        ammo.shortName = "LRM 10";
        ammo.setInternalName("IS Ammo LRM-10");
        ammo.addLookupName("ISLRM10 Ammo");
        ammo.addLookupName("IS LRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 15 Ammo";
        ammo.shortName = "LRM 15";
        ammo.setInternalName("IS Ammo LRM-15");
        ammo.addLookupName("ISLRM15 Ammo");
        ammo.addLookupName("IS LRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 20 Ammo";
        ammo.shortName = "LRM 20";
        ammo.setInternalName("IS Ammo LRM-20");
        ammo.addLookupName("ISLRM20 Ammo");
        ammo.addLookupName("IS LRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    // Enhanced LRMs

    private static AmmoType createISEnhancedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 5 Ammo";
        ammo.shortName = "NLRM 5";
        ammo.setInternalName("ISEnhancedLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.NLRM;
        ammo.shots = 24;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 7;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3058, DATE_NONE, 3082, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);

        return ammo;
    }

    private static AmmoType createISEnhancedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 10 Ammo";
        ammo.shortName = "NLRM 10";
        ammo.setInternalName("ISEnhancedLRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.NLRM;
        ammo.shots = 12;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 13;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3058, DATE_NONE, 3082, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISEnhancedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 15 Ammo";
        ammo.shortName = "NLRM 15";
        ammo.setInternalName("ISEnhancedLRM15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.NLRM;
        ammo.shots = 8;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 20;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3058, DATE_NONE, 3082, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISEnhancedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 20 Ammo";
        ammo.shortName = "NLRM 20";
        ammo.setInternalName("ISEnhancedLRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.NLRM;
        ammo.shots = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 26;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3058, DATE_NONE, 3082, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // EXTENDED LRMs
    private static AmmoType createISExtendedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended LRM 5 Ammo";
        ammo.shortName = "ELRM 5";
        ammo.setInternalName("IS Ammo Extended LRM-5");
        ammo.addLookupName("ISExtended LRM5 Ammo");
        ammo.addLookupName("ISExtendedLRM5 Ammo");
        ammo.addLookupName("IS Extended LRM 5 Ammo");
        ammo.addLookupName("ELRM-5 Ammo (THB)");
        ammo.addLookupName("ELRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.EXLRM;
        ammo.shots = 18;
        ammo.bv = 8;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISExtendedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended LRM 10 Ammo";
        ammo.shortName = "ELRM 10";
        ammo.setInternalName("IS Ammo Extended LRM-10");
        ammo.addLookupName("ISExtended LRM10 Ammo");
        ammo.addLookupName("ISExtendedLRM10 Ammo");
        ammo.addLookupName("IS Extended LRM 10 Ammo");
        ammo.addLookupName("ELRM-10 Ammo (THB)");
        ammo.addLookupName("ELRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.EXLRM;
        ammo.shots = 9;
        ammo.bv = 17;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISExtendedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended LRM 15 Ammo";
        ammo.shortName = "ELRM 15";
        ammo.setInternalName("IS Ammo Extended LRM-15");
        ammo.addLookupName("ISExtended LRM15 Ammo");
        ammo.addLookupName("ISExtendedLRM15 Ammo");
        ammo.addLookupName("IS Extended LRM 15 Ammo");
        ammo.addLookupName("ELRM-15 Ammo (THB)");
        ammo.addLookupName("ELRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.EXLRM;
        ammo.shots = 6;
        ammo.bv = 25;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISExtendedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended LRM 20 Ammo";
        ammo.shortName = "ELRM 20";
        ammo.setInternalName("IS Ammo Extended LRM-20");
        ammo.addLookupName("ISExtended LRM20 Ammo");
        ammo.addLookupName("ISExtendedLRM20 Ammo");
        ammo.addLookupName("IS Extended LRM 20 Ammo");
        ammo.addLookupName("ELRM-20 Ammo (THB)");
        ammo.addLookupName("ELRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.EXLRM;
        ammo.shots = 4;
        ammo.bv = 34;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3054, 3080, DATE_NONE, DATE_NONE)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // STANDARD CLAN LRMS
    private static AmmoType createCLLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("Clan Ammo LRM-5");
        ammo.addLookupName("CLLRM5 Ammo");
        ammo.addLookupName("Clan LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.cost = 30000;
        ammo.kgPerShot = 41.65;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 10 Ammo";
        ammo.shortName = "LRM 10";
        ammo.setInternalName("Clan Ammo LRM-10");
        ammo.addLookupName("CLLRM10 Ammo");
        ammo.addLookupName("Clan LRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.cost = 30000;
        ammo.kgPerShot = 83.3;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.shortName = "LRM 15";
        ammo.name = "LRM 15 Ammo";
        ammo.setInternalName("Clan Ammo LRM-15");
        ammo.addLookupName("CLLRM15 Ammo");
        ammo.addLookupName("Clan LRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 30000;
        ammo.kgPerShot = 124.95;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    private static AmmoType createCLLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 20 Ammo";
        ammo.shortName = "LRM 20";
        ammo.setInternalName("Clan Ammo LRM-20");
        ammo.addLookupName("CLLRM20 Ammo");
        ammo.addLookupName("Clan LRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.cost = 30000;
        ammo.kgPerShot = 166.6;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY);
        return ammo;
    }

    // CLAN STREAK LRMs

    private static AmmoType createCLStreakLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 5 Ammo";
        ammo.shortName = "Streak LRM 5";
        ammo.setInternalName("Clan Streak LRM 5 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-5");
        ammo.addLookupName("CLStreakLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.shots = 24;
        ammo.bv = 11;
        ammo.cost = 60000;
        ammo.kgPerShot = 41.65;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 10 Ammo";
        ammo.shortName = "Streak LRM 10";
        ammo.setInternalName("Clan Streak LRM 10 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-10");
        ammo.addLookupName("CLStreakLRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.shots = 12;
        ammo.bv = 22;
        ammo.cost = 60000;
        ammo.kgPerShot = 83.3;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 15 Ammo";
        ammo.shortName = "Streak LRM 15";
        ammo.setInternalName("Clan Streak LRM 15 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-15");
        ammo.addLookupName("CLStreakLRM15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.shots = 8;
        ammo.bv = 32;
        ammo.cost = 60000;
        ammo.kgPerShot = 124.95;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 20 Ammo";
        ammo.shortName = "Streak LRM 20";
        ammo.setInternalName("Clan Streak LRM 20 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-20");
        ammo.addLookupName("CLStreakLRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.shots = 6;
        ammo.bv = 43;
        ammo.cost = 60000;
        ammo.kgPerShot = 166.6;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Clan Streak LRMs - The protomek editions

    private static AmmoType createCLStreakLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 1 Ammo";
        ammo.shortName = "Streak LRM 1";
        ammo.setInternalName("Clan Streak LRM 1 Ammo");
        ammo.addLookupName("CLStreakLRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 8.33;
        ammo.bv = 0.016;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 2 Ammo";
        ammo.shortName = "Streak LRM 2";
        ammo.setInternalName("Clan Streak LRM 2 Ammo");
        ammo.addLookupName("CLStreakLRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 16.67;
        ammo.bv = 0.033;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 3 Ammo";
        ammo.shortName = "Streak LRM 3";
        ammo.setInternalName("Clan Streak LRM 3 Ammo");
        ammo.addLookupName("CLStreakLRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 24.99;
        ammo.bv = 0.05;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 4 Ammo";
        ammo.shortName = "Streak LRM 4";
        ammo.setInternalName("Clan Streak LRM 4 Ammo");
        ammo.addLookupName("CLStreakLRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 33.32;
        ammo.bv = 0.067;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 6 Ammo";
        ammo.shortName = "Streak LRM 6";
        ammo.setInternalName("Clan Streak LRM 6 Ammo");
        ammo.addLookupName("CLStreakLRM6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 49.98;
        ammo.bv = 0.1;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM7Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 7 Ammo";
        ammo.shortName = "Streak LRM 7";
        ammo.setInternalName("Clan Streak LRM 7 Ammo");
        ammo.addLookupName("CLStreakLRM7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 58.31;
        ammo.bv = 0.117;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 8 Ammo";
        ammo.shortName = "Streak LRM 8";
        ammo.setInternalName("Clan Streak LRM 8 Ammo");
        ammo.addLookupName("CLStreakLRM8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 66.64;
        ammo.bv = 0.133;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 9 Ammo";
        ammo.shortName = "Streak LRM 9";
        ammo.setInternalName("Clan Streak LRM 9 Ammo");
        ammo.addLookupName("CLStreakLRM9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 74.97;
        ammo.bv = 0.15;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM11Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 11 Ammo";
        ammo.shortName = "Streak LRM 11";
        ammo.setInternalName("Clan Streak LRM 11 Ammo");
        ammo.addLookupName("CLStreakLRM11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 91.63;
        ammo.bv = 0.183;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 12 Ammo";
        ammo.shortName = "Streak LRM 12";
        ammo.setInternalName("Clan Streak LRM 12 Ammo");
        ammo.addLookupName("CLStreakLRM12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 99.96;
        ammo.bv = 0.2;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM13Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 13 Ammo";
        ammo.shortName = "Streak LRM 13";
        ammo.setInternalName("Clan Streak LRM 13 Ammo");
        ammo.addLookupName("CLStreakLRM13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 108.29;
        ammo.bv = 0.216;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM14Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 14 Ammo";
        ammo.shortName = "Streak LRM 14";
        ammo.setInternalName("Clan Streak LRM 14 Ammo");
        ammo.addLookupName("CLStreakLRM14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 116.62;
        ammo.bv = 0.233;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM16Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 16 Ammo";
        ammo.shortName = "Streak LRM 16";
        ammo.setInternalName("Clan Streak LRM 16 Ammo");
        ammo.addLookupName("CLStreakLRM16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 133.28;
        ammo.bv = 0.266;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM17Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 17 Ammo";
        ammo.shortName = "Streak LRM 17";
        ammo.setInternalName("Clan Streak LRM 17 Ammo");
        ammo.addLookupName("CLStreakLRM17 mmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 141.61;
        ammo.bv = 0.283;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM18Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 18 Ammo";
        ammo.shortName = "Streak LRM 18";
        ammo.setInternalName("Clan Streak LRM 18 Ammo");
        ammo.addLookupName("CLStreakLRM18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 149.94;
        ammo.bv = 0.3;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM19Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 19 Ammo";
        ammo.shortName = "Streak LRM 19";
        ammo.setInternalName("Clan Streak LRM 19 Ammo");
        ammo.addLookupName("CLStreakLRM19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoTypeEnum.LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMEK);
        ammo.shots = 1;
        ammo.kgPerShot = 158.27;
        ammo.bv = 0.316;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CJF)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // CLAN PROTO LRMS
    private static AmmoType createCLLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 1 Ammo";
        ammo.shortName = "LRM 1";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-1");
        ammo.addLookupName("Clan Ammo LRM-1");
        ammo.addLookupName("CLLRM1 Ammo");
        ammo.addLookupName("Clan LRM 1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 8.33;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 2 Ammo";
        ammo.shortName = "LRM 2";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-2");
        ammo.addLookupName("Clan Ammo LRM-2");
        ammo.addLookupName("CLLRM2 Ammo");
        ammo.addLookupName("Clan LRM 2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.kgPerShot = 16.66;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 3 Ammo";
        ammo.shortName = "LRM 3";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-3");
        ammo.addLookupName("Clan Ammo LRM-3");
        ammo.addLookupName("CLLRM3 Ammo");
        ammo.addLookupName("Clan LRM 3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.kgPerShot = 24.99;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 4 Ammo";
        ammo.shortName = "LRM 4";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-4");
        ammo.addLookupName("Clan Ammo LRM-4");
        ammo.addLookupName("CLLRM4 Ammo");
        ammo.addLookupName("Clan LRM 4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 33.32;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 6 Ammo";
        ammo.shortName = "LRM 6";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-6");
        ammo.addLookupName("Clan Ammo LRM-6");
        ammo.addLookupName("CLLRM6 Ammo");
        ammo.addLookupName("Clan LRM 6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 9;
        ammo.kgPerShot = 49.98;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM7Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 7 Ammo";
        ammo.shortName = "LRM 7";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-7");
        ammo.addLookupName("Clan Ammo LRM-7");
        ammo.addLookupName("CLLRM7 Ammo");
        ammo.addLookupName("Clan LRM 7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 10;
        ammo.kgPerShot = 58.31;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 8 Ammo";
        ammo.shortName = "LRM 8";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-8");
        ammo.addLookupName("Clan Ammo LRM-8");
        ammo.addLookupName("CLLRM8 Ammo");
        ammo.addLookupName("Clan LRM 8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 11;
        ammo.kgPerShot = 66.64;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 9 Ammo";
        ammo.shortName = "LRM 9";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-9");
        ammo.addLookupName("Clan Ammo LRM-9");
        ammo.addLookupName("CLLRM9 Ammo");
        ammo.addLookupName("Clan LRM 9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 12;
        ammo.kgPerShot = 74.97;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM11Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 11 Ammo";
        ammo.shortName = "LRM 11";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-11");
        ammo.addLookupName("Clan Ammo LRM-11");
        ammo.addLookupName("CLLRM11 Ammo");
        ammo.addLookupName("Clan LRM 11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 91.63;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 12 Ammo";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-12");
        ammo.shortName = "LRM 12";
        ammo.addLookupName("Clan Ammo LRM-12");
        ammo.addLookupName("CLLRM12 Ammo");
        ammo.addLookupName("Clan LRM 12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 99.96;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM13Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 13 Ammo";
        ammo.shortName = "LRM 13";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-13");
        ammo.addLookupName("Clan Ammo LRM-13");
        ammo.addLookupName("CLLRM13 Ammo");
        ammo.addLookupName("Clan LRM 13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 20;
        ammo.kgPerShot = 108.29;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM14Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 14 Ammo";
        ammo.shortName = "LRM 14";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-14");
        ammo.addLookupName("Clan Ammo LRM-14");
        ammo.addLookupName("CLLRM14 Ammo");
        ammo.addLookupName("Clan LRM 14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 21;
        ammo.kgPerShot = 116.62;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM16Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 16 Ammo";
        ammo.shortName = "LRM 16";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-16");
        ammo.addLookupName("Clan Ammo LRM-16");
        ammo.addLookupName("CLLRM16 Ammo");
        ammo.addLookupName("Clan LRM 16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 133.28;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM17Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 17 Ammo";
        ammo.shortName = "LRM 17";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-17");
        ammo.addLookupName("Clan Ammo LRM-17");
        ammo.addLookupName("CLLRM17 Ammo");
        ammo.addLookupName("Clan LRM 17 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 141.61;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM18Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRM 18 Ammo";
        ammo.shortName = "LRM 18";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-18");
        ammo.addLookupName("Clan Ammo LRM-18");
        ammo.addLookupName("CLLRM18 Ammo");
        ammo.addLookupName("Clan LRM 18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 149.94;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRM19Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.shortName = "LRM 19";
        ammo.name = "LRM 19 Ammo";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRM-19");
        ammo.addLookupName("Clan Ammo LRM-19");
        ammo.addLookupName("CLLRM19 Ammo");
        ammo.addLookupName("Clan LRM 19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 158.27;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    // Standard MRMs
    private static AmmoType createISMRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 10 Ammo";
        ammo.shortName = "MRM 10";
        ammo.setInternalName("IS MRM 10 Ammo");
        ammo.addLookupName("ISMRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.cost = 5000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 20 Ammo";
        ammo.shortName = "MRM 20";
        ammo.setInternalName("IS MRM 20 Ammo");
        ammo.addLookupName("ISMRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.cost = 5000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 30 Ammo";
        ammo.shortName = "MRM 30";
        ammo.setInternalName("IS MRM 30 Ammo");
        ammo.addLookupName("ISMRM30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 5000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 40 Ammo";
        ammo.shortName = "MRM 40";
        ammo.setInternalName("IS MRM 40 Ammo");
        ammo.addLookupName("ISMRM40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.shots = 6;
        ammo.bv = 28;
        ammo.cost = 5000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    // Standard SRMs
    private static AmmoType createISSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 2 Ammo";
        ammo.shortName = "SRM 2";
        ammo.setInternalName("IS Ammo SRM-2");
        ammo.addLookupName("ISSRM2 Ammo");
        ammo.addLookupName("IS SRM 2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.andNot(F_BATTLEARMOR);
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createISSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 4 Ammo";
        ammo.shortName = "SRM 4";
        ammo.setInternalName("IS Ammo SRM-4");
        ammo.addLookupName("ISSRM4 Ammo");
        ammo.addLookupName("IS SRM 4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.andNot(F_BATTLEARMOR);
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createISSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 6 Ammo";
        ammo.shortName = "SRM 6";
        ammo.setInternalName("IS Ammo SRM-6");
        ammo.addLookupName("ISSRM6 Ammo");
        ammo.addLookupName("IS SRM 6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.andNot(F_BATTLEARMOR);
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(true)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    // Clan SRMs (Includes Proto ones)

    private static AmmoType createCLSRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 1 Ammo";
        ammo.shortName = "SRM 1";
        ammo.setInternalName("Clan Ammo SRM-1");
        ammo.addLookupName("CLSRM1 Ammo");
        ammo.addLookupName("Clan SRM 1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 2 Ammo";
        ammo.shortName = "SRM 2";
        ammo.setInternalName("Clan Ammo SRM-2");
        ammo.addLookupName("CLSRM2 Ammo");
        ammo.addLookupName("Clan SRM 2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC);
        return ammo;
    }

    private static AmmoType createCLSRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 3 Ammo";
        ammo.shortName = "SRM 3";
        ammo.setInternalName("Clan Ammo SRM-3");
        ammo.addLookupName("CLSRM3 Ammo");
        ammo.addLookupName("Clan SRM 3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.kgPerShot = 30;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 4 Ammo";
        ammo.shortName = "SRM 4";
        ammo.setInternalName("Clan Ammo SRM-4");
        ammo.addLookupName("CLSRM4 Ammo");
        ammo.addLookupName("Clan SRM 4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC);
        return ammo;
    }

    private static AmmoType createCLSRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 5 Ammo";
        ammo.shortName = "SRM 5";
        ammo.setInternalName("Clan Ammo SRM-5");
        ammo.addLookupName("CLSRM5 Ammo");
        ammo.addLookupName("Clan SRM 5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 50;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 6 Ammo";
        ammo.shortName = "SRM 6";
        ammo.setInternalName("Clan Ammo SRM-6");
        ammo.addLookupName("CLSRM6 Ammo");
        ammo.addLookupName("Clan SRM 6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC);
        return ammo;
    }

    // Multi-Missile Launcher(MMLs)
    private static AmmoType createISMML3LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 3 LRM Ammo";
        ammo.shortName = "MML 3/LRM";
        ammo.setInternalName("IS Ammo MML-3 LRM");
        ammo.addLookupName("ISMML3 LRM Ammo");
        ammo.addLookupName("IS MML-3 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.shots = 40;
        ammo.bv = 4;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML3SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 3 SRM Ammo";
        ammo.shortName = "MML 3/SRM";
        ammo.setInternalName("IS Ammo MML-3 SRM");
        ammo.addLookupName("ISMML3 SRM Ammo");
        ammo.addLookupName("IS MML-3 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.shots = 33;
        ammo.bv = 4;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML5LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 5 LRM Ammo";
        ammo.shortName = "MML 5/LRM";
        ammo.setInternalName("IS Ammo MML-5 LRM");
        ammo.addLookupName("ISMML5 LRM Ammo");
        ammo.addLookupName("IS MML-5 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML5SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 5 SRM Ammo";
        ammo.shortName = "MML 5/SRM";
        ammo.setInternalName("IS Ammo MML-5 SRM");
        ammo.addLookupName("ISMML5 SRM Ammo");
        ammo.addLookupName("IS MML-5 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.shots = 20;
        ammo.bv = 6;
        ammo.cost = 27000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML7LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 7 LRM Ammo";
        ammo.shortName = "MML 7/LRM";
        ammo.setInternalName("IS Ammo MML-7 LRM");
        ammo.addLookupName("ISMML7 LRM Ammo");
        ammo.addLookupName("IS MML-7 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.shots = 17;
        ammo.bv = 8;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML7SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 7 SRM Ammo";
        ammo.shortName = "MML 7/SRM";
        ammo.setInternalName("IS Ammo MML-7 SRM");
        ammo.addLookupName("ISMML7 SRM Ammo");
        ammo.addLookupName("IS MML-7 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.shots = 14;
        ammo.bv = 8;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML9LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 9 LRM Ammo";
        ammo.shortName = "MML 9/LRM";
        ammo.setInternalName("IS Ammo MML-9 LRM");
        ammo.addLookupName("ISMML9 LRM Ammo");
        ammo.addLookupName("IS MML-9 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.shots = 13;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMML9SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 9 SRM Ammo";
        ammo.shortName = "MML 9/SRM";
        ammo.setInternalName("IS Ammo MML-9 SRM");
        ammo.addLookupName("ISMML9 SRM Ammo");
        ammo.addLookupName("IS MML-9 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.MML;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.shots = 11;
        ammo.bv = 11;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.MERC, Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Rocket Launcher Ammo
    private static AmmoType createISRL10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 10 Ammo";
        ammo.setInternalName("IS Ammo RL-10");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setProductionFactions(Faction.MH);
        return ammo;
    }

    private static AmmoType createISRL15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 15 Ammo";
        ammo.setInternalName("IS Ammo RL-15");
        ammo.addLookupName("CL Ammo RL-Prototype-15");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setProductionFactions(Faction.MH);
        return ammo;
    }

    private static AmmoType createISRL20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 20 Ammo";
        ammo.setInternalName("IS Ammo RL-20");
        ammo.addLookupName("CL Ammo RL-Prototype-20");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 2000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setProductionFactions(Faction.MH);
        return ammo;
    }

    // Clan Standard Streak Launchers
    private static AmmoType createCLStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak SRM 2 Ammo";
        ammo.shortName = "Streak SRM 2";
        ammo.setInternalName("Clan Streak SRM 2 Ammo");
        ammo.addLookupName("Clan Ammo Streak-2");
        ammo.addLookupName("CLStreakSRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 5;
        ammo.cost = 54000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA);
        return ammo;
    }

    private static AmmoType createCLStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak SRM 4 Ammo";
        ammo.shortName = "Streak SRM 4";
        ammo.setInternalName("Clan Streak SRM 4 Ammo");
        ammo.addLookupName("Clan Ammo Streak-4");
        ammo.addLookupName("CLStreakSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 10;
        ammo.cost = 54000;
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA);
        return ammo;
    }

    private static AmmoType createCLStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak SRM 6 Ammo";
        ammo.shortName = "Streak SRM 6";
        ammo.setInternalName("Clan Streak SRM 6 Ammo");
        ammo.addLookupName("Clan Ammo Streak-6");
        ammo.addLookupName("CLStreakSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 15;
        ammo.cost = 54000;
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.CSA)
              .setProductionFactions(Faction.CSA);
        return ammo;
    }

    // Clan ProtoMek Streak Launchers
    private static AmmoType createCLStreakSRM1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 1 Ammo";
        ammo.shortName = "Streak SRM 1";
        ammo.setInternalName("Clan Streak SRM 1 Ammo");
        ammo.addLookupName("Clan Ammo Streak-1");
        ammo.addLookupName("CLStreakSRM1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.kgPerShot = 10;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * SRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLStreakSRM3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 3 Ammo";
        ammo.shortName = "Streak SRM 3";
        ammo.setInternalName("Clan Streak SRM 3 Ammo");
        ammo.addLookupName("Clan Ammo Streak-3");
        ammo.addLookupName("CLStreakSRM3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 7;
        ammo.kgPerShot = 30;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * SRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLStreakSRM5Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 5 Ammo";
        ammo.shortName = "Streak SRM 5";
        ammo.setInternalName("Clan Streak SRM 5 Ammo");
        ammo.addLookupName("Clan Ammo Streak-5");
        ammo.addLookupName("CLStreakSRM5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 13;
        ammo.kgPerShot = 50;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * SRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    // IS Streak Launchers
    private static AmmoType createISStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak SRM 2 Ammo";
        ammo.shortName = "Streak SRM 2";
        ammo.setInternalName("IS Streak SRM 2 Ammo");
        ammo.addLookupName("IS Ammo Streak-2");
        ammo.addLookupName("ISStreakSRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 4;
        ammo.cost = 54000;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2645, 2647, 2650, 2845, 3035)
              .setISApproximate(false, false, true, false, false)
              .setClanAdvancement(2645, 2647, 2650, 2845, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createISStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak SRM 4 Ammo";
        ammo.shortName = "Streak SRM 4";
        ammo.setInternalName("IS Streak SRM 4 Ammo");
        ammo.addLookupName("IS Ammo Streak-4");
        ammo.addLookupName("ISStreakSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 7;
        ammo.cost = 54000;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak SRM 6 Ammo";
        ammo.shortName = "Streak SRM 6";
        ammo.setInternalName("IS Streak SRM 6 Ammo");
        ammo.addLookupName("IS Ammo Streak-6");
        ammo.addLookupName("ISStreakSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 11;
        ammo.cost = 54000;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    // NARC PODS

    private static AmmoType createISNarcAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Narc Pods";
        ammo.shortName = "Narc";
        ammo.setInternalName("ISNarc Pods");
        ammo.addLookupName("IS Ammo Narc");
        ammo.addLookupName("IS Narc Missile Beacon Ammo");
        ammo.addLookupName("CLNarc Pods");
        ammo.addLookupName("Clan Ammo Narc");
        ammo.addLookupName("Clan Narc Missile Beacon Ammo");
        ammo.damagePerShot = 2; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.NARC;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 6000;
        ammo.kgPerShot = 150;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2580, 2587, 3049, 2795, 3035)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 2818, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createISNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Narc Explosive Pods";
        ammo.shortName = "Narc Explosive";
        ammo.setInternalName("ISNarc ExplosivePods");
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.NARC;
        ammo.munitionType = EnumSet.of(Munitions.M_NARC_EX);
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.kgPerShot = 150;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3054, 3060, 3064, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createCLNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Narc Explosive Pods";
        ammo.shortName = "Narc Explosive";
        ammo.setInternalName("CLNarc Explosive Pods");
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.NARC;
        ammo.munitionType = EnumSet.of(Munitions.M_NARC_EX);
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.kgPerShot = 150;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setClanAdvancement(3054, 3060, 3064, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    // TODO Shoot and Sit Narc Missiles - See IO pg 132

    private static AmmoType createISiNarcAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "iNarc Pods";
        ammo.shortName = "iNarc";
        ammo.setInternalName("ISiNarc Pods");
        ammo.addLookupName("IS Ammo iNarc");
        ammo.addLookupName("IS iNarc Missile Beacon Ammo");
        ammo.addLookupName("iNarc Ammo");
        ammo.damagePerShot = 3; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.INARC;
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 7500;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS, Faction.WB);
        return ammo;
    }

    private static AmmoType createISiNarcECMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "iNarc ECM Pods";
        ammo.shortName = "iNarc ECM";
        ammo.setInternalName("ISiNarc ECM Pods");
        ammo.addLookupName("iNarc ECM Ammo");
        ammo.damagePerShot = 3; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.INARC;
        ammo.munitionType = EnumSet.of(Munitions.M_ECM);
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 15000;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS, Faction.WB);
        return ammo;
    }

    private static AmmoType createISiNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "iNarc Explosive Pods";
        ammo.shortName = "iNarc Explosive";
        ammo.setInternalName("ISiNarc Explosive Pods");
        ammo.addLookupName("iNarc Explosive Ammo");
        ammo.damagePerShot = 6; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.INARC;
        ammo.munitionType = EnumSet.of(Munitions.M_EXPLOSIVE);
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS, Faction.WB);
        return ammo;
    }

    private static AmmoType createISiNarcHaywireAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "iNarc Haywire Pods";
        ammo.shortName = "iNarc Haywire";
        ammo.setInternalName("ISiNarc Haywire Pods");
        ammo.addLookupName("iNarc Haywire Ammo");
        ammo.damagePerShot = 3; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.INARC;
        ammo.munitionType = EnumSet.of(Munitions.M_HAYWIRE);
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 20000;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS, Faction.WB);
        return ammo;
    }

    private static AmmoType createISiNarcNemesisAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "iNarc Nemesis Pods";
        ammo.shortName = "iNarc Nemesis";
        ammo.setInternalName("ISiNarc Nemesis Pods");
        ammo.addLookupName("iNarc Nemesis Ammo");
        ammo.damagePerShot = 3; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.INARC;
        ammo.munitionType = EnumSet.of(Munitions.M_NEMESIS);
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 10000;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS, Faction.WB);
        return ammo;
    }

    // Torpedo Ammo (Damn the Torpedoes)
    private static AmmoType createISLRT5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 5 Ammo";
        ammo.shortName = "LRT 5";
        ammo.setInternalName("IS Ammo LRTorpedo-5");
        ammo.addLookupName("ISLRTorpedo5 Ammo");
        ammo.addLookupName("IS LRTorpedo 5 Ammo");
        ammo.addLookupName("ISLRT5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createISLRT10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 10 Ammo";
        ammo.shortName = "LRT 10";
        ammo.setInternalName("IS Ammo LRTorpedo-10");
        ammo.addLookupName("ISLRTorpedo10 Ammo");
        ammo.addLookupName("IS LRTorpedo 10 Ammo");
        ammo.addLookupName("ISLRT10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";

        ammo.techAdvancement.setTechBase(TechBase.IS);
        ammo.techAdvancement.setISAdvancement(2365, 2380, 2400);
        ammo.techAdvancement.setTechRating(TechRating.C);
        ammo.techAdvancement.setAvailability(AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C);
        return ammo;
    }

    private static AmmoType createISLRT15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 15 Ammo";
        ammo.shortName = "LRT 15";
        ammo.setInternalName("IS Ammo LRTorpedo-15");
        ammo.addLookupName("ISLRTorpedo15 Ammo");
        ammo.addLookupName("IS LRv 15 Ammo");
        ammo.addLookupName("ISLRT15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";

        ammo.techAdvancement.setTechBase(TechBase.IS);
        ammo.techAdvancement.setISAdvancement(2365, 2380, 2400);
        ammo.techAdvancement.setTechRating(TechRating.C);
        ammo.techAdvancement.setAvailability(AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C);
        return ammo;
    }

    private static AmmoType createISLRT20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 20 Ammo";
        ammo.shortName = "LRT 20";
        ammo.setInternalName("IS Ammo LRTorpedo-20");
        ammo.addLookupName("ISLRTorpedo20 Ammo");
        ammo.addLookupName("IS LRTorpedo 20 Ammo");
        ammo.addLookupName("ISLRT20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";

        ammo.techAdvancement.setTechBase(TechBase.IS);
        ammo.techAdvancement.setISAdvancement(2365, 2380, 2400);
        ammo.techAdvancement.setTechRating(TechRating.C);
        ammo.techAdvancement.setAvailability(AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.C);
        return ammo;
    }

    private static AmmoType createISSRT2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 2 Ammo";
        ammo.shortName = "SRT 2";
        ammo.setInternalName("IS Ammo SRTorpedo-2");
        ammo.addLookupName("ISSRTorpedo2 Ammo");
        ammo.addLookupName("IS SRTorpedo 2 Ammo");
        ammo.addLookupName("ISSRT2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2370, 2380, 2400)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISSRT4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 4 Ammo";
        ammo.shortName = "SRT 4";
        ammo.setInternalName("IS Ammo SRTorpedo-4");
        ammo.addLookupName("ISSRTorpedo4 Ammo");
        ammo.addLookupName("IS SRTorpedo 4 Ammo");
        ammo.addLookupName("ISSRT4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2370, 2380, 2400)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISSRT6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 6 Ammo";
        ammo.shortName = "SRT 6";
        ammo.setInternalName("IS Ammo SRTorpedo-6");
        ammo.addLookupName("ISSRTorpedo6 Ammo");
        ammo.addLookupName("IS SRTorpedo 6 Ammo");
        ammo.addLookupName("ISSRT6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2370, 2380, 2400)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FW);
        return ammo;
    }

    // Clan LRT

    private static AmmoType createCLLRT1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 1 Ammo";
        ammo.shortName = "LRT 1";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-1");
        ammo.addLookupName("Clan Ammo LRTorpedo-1");
        ammo.addLookupName("CLLRTorpedo1 Ammo");
        ammo.addLookupName("Clan LRTorpedo 1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 8.33;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 2 Ammo";
        ammo.shortName = "LRT 2";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-2");
        ammo.addLookupName("Clan Ammo LRTorpedo-2");
        ammo.addLookupName("CLLRTorpedo2 Ammo");
        ammo.addLookupName("Clan LRTorpedo 2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.kgPerShot = 16.66;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 3 Ammo";
        ammo.shortName = "LRT 3";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-3");
        ammo.addLookupName("Clan Ammo LRTorpedo-3");
        ammo.addLookupName("CLLRTorpedo3 Ammo");
        ammo.addLookupName("Clan LRTorpedo 3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 5;
        ammo.kgPerShot = 24.99;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 4 Ammo";
        ammo.shortName = "LRT 4";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-4");
        ammo.addLookupName("Clan Ammo LRTorpedo-4");
        ammo.addLookupName("CLLRTorpedo4 Ammo");
        ammo.addLookupName("Clan LRTorpedo 4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 33.32;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 5 Ammo";
        ammo.shortName = "LRT 5";
        ammo.setInternalName("Clan Ammo LRTorpedo-5");
        ammo.addLookupName("CLLRTorpedo5 Ammo");
        ammo.addLookupName("Clan LRTorpedo 5 Ammo");
        ammo.addLookupName("CLLRT5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.cost = 30000;
        ammo.kgPerShot = 41.65;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createCLLRT6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 6 Ammo";
        ammo.shortName = "LRT 6";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-6");
        ammo.addLookupName("Clan Ammo LRTorpedo-6");
        ammo.addLookupName("CLLRTorpedo6 Ammo");
        ammo.addLookupName("Clan LRTorpedo 6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 9;
        ammo.kgPerShot = 49.98;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT7Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 7 Ammo";
        ammo.shortName = "LRT 7";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-7");
        ammo.addLookupName("Clan Ammo LRTorpedo-7");
        ammo.addLookupName("CLLRTorpedo7 Ammo");
        ammo.addLookupName("Clan LRTorpedo 7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 10;
        ammo.kgPerShot = 58.31;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 8 Ammo";
        ammo.shortName = "LRT 8";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-8");
        ammo.addLookupName("Clan Ammo LRTorpedo-8");
        ammo.addLookupName("CLLRTorpedo8 Ammo");
        ammo.addLookupName("Clan LRTorpedo 8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 11;
        ammo.kgPerShot = 66.64;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 9 Ammo";
        ammo.shortName = "LRT 9";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-9");
        ammo.addLookupName("Clan Ammo LRTorpedo-9");
        ammo.addLookupName("CLLRTorpedo9 Ammo");
        ammo.addLookupName("Clan LRTorpedo 9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 12;
        ammo.kgPerShot = 74.97;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 10 Ammo";
        ammo.shortName = "LRT 10";
        ammo.setInternalName("Clan Ammo LRTorpedo-10");
        ammo.addLookupName("CLLRTorpedo10 Ammo");
        ammo.addLookupName("Clan LRTorpedo 10 Ammo");
        ammo.addLookupName("CLLRT10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.cost = 30000;
        ammo.kgPerShot = 83.3;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createCLLRT11Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 11 Ammo";
        ammo.shortName = "LRT 11";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-11");
        ammo.addLookupName("Clan Ammo LRTorpedo-11");
        ammo.addLookupName("CLLRTorpedo11 Ammo");
        ammo.addLookupName("Clan LRTorpedo 11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 91.63;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 12 Ammo";
        ammo.shortName = "LRT 12";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-12");
        ammo.addLookupName("Clan Ammo LRTorpedo-12");
        ammo.addLookupName("CLLRTorpedo12 Ammo");
        ammo.addLookupName("Clan LRTorpedo 12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 99.96;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT13Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 13 Ammo";
        ammo.shortName = "LRT 13";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-13");
        ammo.addLookupName("Clan Ammo LRTorpedo-13");
        ammo.addLookupName("CLLRTorpedo13 Ammo");
        ammo.addLookupName("Clan LRTorpedo 13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 20;
        ammo.kgPerShot = 108.29;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT14Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 14 Ammo";
        ammo.shortName = "LRT 14";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-14");
        ammo.addLookupName("Clan Ammo LRTorpedo-14");
        ammo.addLookupName("CLLRTorpedo14 Ammo");
        ammo.addLookupName("Clan LRTorpedo 14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 21;
        ammo.kgPerShot = 116.62;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 15 Ammo";
        ammo.shortName = "LRT 15";
        ammo.setInternalName("Clan Ammo LRTorpedo-15");
        ammo.addLookupName("CLLRTorpedo15 Ammo");
        ammo.addLookupName("Clan LRTorpedo 15 Ammo");
        ammo.addLookupName("CLLRT15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 30000;
        ammo.kgPerShot = 124.95;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createCLLRT16Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 16 Ammo";
        ammo.shortName = "LRT 16";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-16");
        ammo.addLookupName("Clan Ammo LRTorpedo-16");
        ammo.addLookupName("CLLRTorpedo16 Ammo");
        ammo.addLookupName("Clan LRTorpedo 16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 133.28;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT17Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 17 Ammo";
        ammo.shortName = "LRT 17";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-17");
        ammo.addLookupName("Clan Ammo LRTorpedo-17");
        ammo.addLookupName("CLLRTorpedo17 Ammo");
        ammo.addLookupName("Clan LRTorpedo 17 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 141.61;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT18Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 18 Ammo";
        ammo.shortName = "LRT 18";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-18");
        ammo.addLookupName("Clan Ammo LRTorpedo-18");
        ammo.addLookupName("CLLRTorpedo18 Ammo");
        ammo.addLookupName("Clan LRTorpedo 18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 149.94;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT19Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 19 Ammo";
        ammo.shortName = "LRT 19";
        // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
        ammo.setInternalName("Clan Ammo ProtoMech LRTorpedo-19");
        ammo.addLookupName("Clan Ammo LRTorpedo-19");
        ammo.addLookupName("CLLRTorpedo19 Ammo");
        ammo.addLookupName("Clan LRTorpedo 19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 158.27;
        /*
         * Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But
         * LRM Tech Base and Avail Ratings.
         */
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLLRT20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LRT 20 Ammo";
        ammo.shortName = "LRT 20";
        ammo.setInternalName("Clan Ammo LRTorpedo-20");
        ammo.addLookupName("CLLRTorpedo20 Ammo");
        ammo.addLookupName("Clan LRTorpedo 20 Ammo");
        ammo.addLookupName("CLLRT20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.cost = 30000;
        ammo.kgPerShot = 166.6;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    // Clan SRTs

    private static AmmoType createCLSRT1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 1 Ammo";
        ammo.shortName = "SRT 1";
        ammo.setInternalName("Clan Ammo SRTorpedo-1");
        ammo.addLookupName("CLSRTorpedo1 Ammo");
        ammo.addLookupName("Clan SRTorpedo 1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLSRT2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 2 Ammo";
        ammo.shortName = "SRT 2";
        ammo.setInternalName("Clan Ammo SRTorpedo-2");
        ammo.addLookupName("CLSRTorpedo2 Ammo");
        ammo.addLookupName("Clan SRTorpedo 2 Ammo");
        ammo.addLookupName("CLSRT2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createCLSRT3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 3 Ammo";
        ammo.shortName = "SRT 3";
        ammo.setInternalName("Clan Ammo SRTorpedo-3");
        ammo.addLookupName("CLSRTorpedo3 Ammo");
        ammo.addLookupName("Clan SRTorpedo 3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.kgPerShot = 30;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLSRT4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 4 Ammo";
        ammo.shortName = "SRT 4";
        ammo.setInternalName("Clan Ammo SRTorpedo-4");
        ammo.addLookupName("CLSRTorpedo4 Ammo");
        ammo.addLookupName("Clan SRTorpedo 4 Ammo");
        ammo.addLookupName("CLSRT4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createCLSRT5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRT 5 Ammo";
        ammo.shortName = "SRT 5";
        ammo.setInternalName("Clan Ammo SRTorpedo-5");
        ammo.addLookupName("CLSRTorpedo5 Ammo");
        ammo.addLookupName("Clan SRTorpedo 5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 50;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSJ)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLSRT6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.shortName = "SRT 6";
        ammo.name = "SRT 6 Ammo";
        ammo.setInternalName("Clan Ammo SRTorpedo-6");
        ammo.addLookupName("CLSRTorpedo6 Ammo");
        ammo.addLookupName("Clan SRTorpedo 6 Ammo");
        ammo.addLookupName("CLSRT6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_TORPEDO;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    // TODO Fusillade Ammo

    // MORTAR AMMOS - Most ammo are mutators that are listed above.

    private static AmmoType createISAPMortar1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 1 Ammo";
        ammo.shortName = "Mortar SC 1";
        ammo.setInternalName("IS Ammo SC Mortar-1");
        ammo.addLookupName("ISArmorPiercingMortarAmmo1");
        ammo.addLookupName("ISSCMortarAmmo1");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 24;
        ammo.bv = 1.2;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(2526, 2531, 3052, 2819, 3043)
              .setISApproximate(true, false, false, false, false)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createISAPMortar2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 2 Ammo";
        ammo.shortName = "Mortar SC 2";
        ammo.setInternalName("IS Ammo SC Mortar-2");
        ammo.addLookupName("ISArmorPiercingMortarAmmo2");
        ammo.addLookupName("ISSCMortarAmmo2");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 12;
        ammo.bv = 2.4;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(2526, 2531, 3052, 2819, 3043)
              .setISApproximate(true, false, false, false, false)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createISAPMortar4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 4 Ammo";
        ammo.shortName = "Mortar SC 4";
        ammo.setInternalName("IS Ammo SC Mortar-4");
        ammo.addLookupName("ISArmorPiercingMortarAmmo4");
        ammo.addLookupName("ISSCMortarAmmo4");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 6;
        ammo.bv = 3.6;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(2526, 2531, 3052, 2819, 3043)
              .setISApproximate(true, false, false, false, false)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createISAPMortar8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 8 Ammo";
        ammo.shortName = "Mortar SC 8";
        ammo.setInternalName("IS Ammo SC Mortar-8");
        ammo.addLookupName("ISArmorPiercingMortarAmmo8");
        ammo.addLookupName("ISSCMortarAmmo8");
        ammo.damagePerShot = 2;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 4;
        ammo.bv = 7.2;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(2526, 2531, 3052, 2819, 3043)
              .setISApproximate(true, false, false, false, false)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createCLAPMortar1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 1 Ammo";
        ammo.shortName = "Mortar SC 1";
        ammo.setInternalName("Clan Ammo SC Mortar-1");
        ammo.addLookupName("CLArmorPiercingMortarAmmo1");
        ammo.addLookupName("CLSCMortarAmmo1");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 24;
        ammo.bv = 1.2;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CBR)
              .setProductionFactions(Faction.CBR);
        return ammo;
    }

    private static AmmoType createCLAPMortar2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 2 Ammo";
        ammo.shortName = "Mortar SC 2";
        ammo.setInternalName("Clan Ammo SC Mortar-2");
        ammo.addLookupName("CLArmorPiercingMortarAmmo2");
        ammo.addLookupName("CLSCMortarAmmo2");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 12;
        ammo.bv = 2.4;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CBR)
              .setProductionFactions(Faction.CBR);
        return ammo;
    }

    private static AmmoType createCLAPMortar4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 4 Ammo";
        ammo.shortName = "Mortar SC 4";
        ammo.setInternalName("Clan Ammo SC Mortar-4");
        ammo.addLookupName("CLArmorPiercingMortarAmmo4");
        ammo.addLookupName("CLSCMortarAmmo4");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 6;
        ammo.bv = 3.6;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CBR)
              .setProductionFactions(Faction.CBR);
        return ammo;
    }

    private static AmmoType createCLAPMortar8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Shaped Charge Mortar 8 Ammo";
        ammo.shortName = "Mortar SC 8";
        ammo.setInternalName("Clan Ammo SC Mortar-8");
        ammo.addLookupName("CLArmorPiercingMortarAmmo8");
        ammo.addLookupName("CLSCMortarAmmo8");
        ammo.damagePerShot = 2;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.MEK_MORTAR;
        ammo.shots = 4;
        ammo.bv = 7.2;
        ammo.cost = 28000;
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CBR)
              .setProductionFactions(Faction.CBR);
        return ammo;
    }

    // PLASMA WEAPONS

    private static AmmoType createISPlasmaRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Plasma Rifle Ammo";
        ammo.shortName = "Plasma Rifle";
        ammo.setInternalName("ISPlasmaRifleAmmo");
        ammo.addLookupName("ISPlasmaRifle Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.PLASMA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.rulesRefs = "234, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3061, 3068, 3072, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC);
        return ammo;
    }

    private static AmmoType createCLPlasmaCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Plasma Cannon Ammo";
        ammo.shortName = "Plasma Cannon";
        ammo.setInternalName("CLPlasmaCannonAmmo");
        ammo.addLookupName("CLPlasmaCannon Ammo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.PLASMA;
        ammo.shots = 10;
        ammo.bv = 21;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.rulesRefs = "234, TM";
        ammo.kgPerShot = 100;
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3068, 3069, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    // RISC APDS
    private static AmmoType createISAPDSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RISC Advanced Point Defense System Ammo";
        ammo.shortName = "RISC APDS";
        ammo.setInternalName("ISAPDS Ammo");
        ammo.damagePerShot = 1; // only used for ammo criticalSlots
        ammo.rackSize = 2; // only used for ammo criticalSlots
        ammo.ammoType = AmmoTypeEnum.APDS;
        ammo.shots = 12;
        ammo.bv = 22;
        ammo.cost = 2000;
        ammo.rulesRefs = "91, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
              .setISAdvancement(3134, 3137, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.RS)
              .setProductionFactions(Faction.RS);
        return ammo;
    }

    // Mek Taser

    private static AmmoType createISMekTaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Taser Ammo";
        ammo.shortName = "Taser";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("MekTaserAmmo");
        ammo.damagePerShot = 6;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.TASER;
        ammo.shots = 5;
        ammo.bv = 5;
        ammo.cost = 2000;
        ammo.rulesRefs = "346, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3065, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    // CAPITAL AND SUB-CAP WEAPONS

    // naval ammo
    /*
     * Because ammo by ton is not in whole number I am doing this as single shot
     * with a function to change the number of shots which will be called from the
     * BLK file. This means I also have to convert BV and cost per ton to BV and
     * cost per shot
     */

    private static AmmoType createLightMassDriverAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Mass Driver Ammo";
        ammo.setInternalName("Ammo Light Mass Driver");
        ammo.addLookupName("LightMassDriver Ammo");
        ammo.damagePerShot = 60;
        ammo.ammoType = AmmoTypeEnum.LMASS;
        ammo.shots = 1;
        ammo.tonnage = 30;
        ammo.bv = 882;
        ammo.cost = 150000;
        ammo.ammoRatio = 1;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createMediumMassDriverAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium Mass Driver Ammo";
        ammo.setInternalName("Ammo Medium Mass Driver");
        ammo.addLookupName("MediumMassDriver Ammo");
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoTypeEnum.MMASS;
        ammo.shots = 1;
        ammo.tonnage = 60;
        ammo.bv = 1470;
        ammo.cost = 300000;
        ammo.ammoRatio = 1;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createHeavyMassDriverAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Mass Driver Ammo";
        ammo.setInternalName("Ammo Heavy Mass Driver");
        ammo.addLookupName("HeavyMassDriver Ammo");
        ammo.damagePerShot = 140;
        ammo.ammoType = AmmoTypeEnum.HMASS;
        ammo.shots = 1;
        ammo.tonnage = 90;
        ammo.bv = 2058;
        ammo.cost = 600000;
        ammo.ammoRatio = 1;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createLightNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light N-Gauss Ammo";
        ammo.setInternalName("Ammo Light N-Gauss");
        ammo.addLookupName("LightNGauss Ammo");
        ammo.damagePerShot = 15;
        ammo.ammoType = AmmoTypeEnum.LIGHT_NGAUSS;
        ammo.shots = 1;
        ammo.tonnage = 0.2;
        ammo.bv = 378;
        ammo.cost = 45000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052)
              .setISApproximate(true, true, false, true, false)
              .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createMediumNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium N-Gauss Ammo";
        ammo.setInternalName("Ammo Medium N-Gauss");
        ammo.addLookupName("MediumNGauss Ammo");
        ammo.damagePerShot = 25;
        ammo.ammoType = AmmoTypeEnum.MED_NGAUSS;
        ammo.shots = 1;
        ammo.tonnage = 0.4;
        ammo.bv = 630;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052)
              .setISApproximate(true, true, false, true, false)
              .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createHeavyNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy N-Gauss Ammo";
        ammo.setInternalName("Ammo Heavy N-Gauss");
        ammo.addLookupName("HeavyNGauss Ammo");
        ammo.damagePerShot = 40;
        ammo.ammoType = AmmoTypeEnum.HEAVY_NGAUSS;
        ammo.shots = 1;
        ammo.tonnage = 0.5;
        ammo.bv = 756;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052)
              .setISApproximate(true, true, false, true, false)
              .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createNAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/10 Ammo";
        ammo.setInternalName("Ammo NAC/10");
        ammo.addLookupName("NAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.2;
        ammo.bv = 237;
        ammo.cost = 30000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createNAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/20 Ammo";
        ammo.setInternalName("Ammo NAC/20");
        ammo.addLookupName("NAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.4;
        ammo.bv = 474;
        ammo.cost = 60000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createNAC25Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/25 Ammo";
        ammo.setInternalName("Ammo NAC/25");
        ammo.addLookupName("NAC25 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoTypeEnum.NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.6;
        ammo.bv = 593;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.6;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createNAC30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/30 Ammo";
        ammo.setInternalName("Ammo NAC/30");
        ammo.addLookupName("NAC30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoTypeEnum.NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.8;
        ammo.bv = 711;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.8;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createNAC35Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/35 Ammo";
        ammo.setInternalName("Ammo NAC/35");
        ammo.addLookupName("NAC35 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 35;
        ammo.ammoType = AmmoTypeEnum.NAC;
        ammo.shots = 1;
        ammo.tonnage = 1;
        ammo.bv = 620;
        ammo.cost = 105000;
        ammo.ammoRatio = 1.0;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createNAC40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/40 Ammo";
        ammo.setInternalName("Ammo NAC/40");
        ammo.addLookupName("NAC40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoTypeEnum.NAC;
        ammo.shots = 1;
        ammo.tonnage = 1.2;
        ammo.bv = 708;
        ammo.cost = 120000;
        ammo.ammoRatio = 1.2;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
              .setISApproximate(false, true, false, true, false)
              .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    // Standard Cap Missiles
    private static AmmoType createBarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Barracuda Ammo";
        ammo.shortName = "B";
        ammo.setInternalName("Ammo Barracuda");
        ammo.addLookupName("Barracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoTypeEnum.BARRACUDA;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2200, 2305, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createWhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "White Shark Ammo";
        ammo.shortName = "WS";
        ammo.setInternalName("Ammo White Shark");
        ammo.addLookupName("WhiteShark Ammo");
        ammo.addLookupName("White Shark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoTypeEnum.WHITE_SHARK;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.ammoRatio = 40;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2200, 2305, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createKillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Killer Whale Ammo";
        ammo.setInternalName("Ammo Killer Whale");
        ammo.shortName = "KW";
        ammo.addLookupName("KillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoTypeEnum.KILLER_WHALE;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2200, 2305, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    // Tele-Operated Missiles

    private static AmmoType createBarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Barracuda (Tele-Operated) Ammo";
        ammo.shortName = "B-T";
        ammo.setInternalName("Ammo Barracuda-T");
        ammo.addLookupName("BarracudaT Ammo");
        ammo.shortName = "Barracuda-T";
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoTypeEnum.BARRACUDA_T;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3056, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createWhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "White Shark (Tele-Operated) Ammo";
        ammo.shortName = "WS-T";
        ammo.setInternalName("Ammo White Shark-T");
        ammo.addLookupName("WhiteSharkT Ammo");
        ammo.shortName = "White Shark-T";
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoTypeEnum.WHITE_SHARK_T;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3056, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createKillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Killer Whale (Tele-Operated) Ammo";
        ammo.shortName = "KW-T";
        ammo.setInternalName("Ammo Killer Whale-T");
        ammo.addLookupName("KillerWhaleT Ammo");
        ammo.shortName = "Killer Whale-T";
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoTypeEnum.KILLER_WHALE_T;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3056, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createKrakenAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Kraken (Tele-Operated) Ammo";
        ammo.shortName = "KR-T";
        ammo.setInternalName("Ammo KrakenT");
        ammo.addLookupName("KrakenT Ammo");
        ammo.shortName = "Kraken-T";
        ammo.damagePerShot = 10;
        ammo.ammoType = AmmoTypeEnum.KRAKEN_T;
        ammo.shots = 1;
        ammo.tonnage = 100.0;
        ammo.bv = 288;
        ammo.cost = 55000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createKrakenMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Kraken Ammo";
        ammo.shortName = "KR";
        ammo.setInternalName("Ammo Kraken");
        ammo.addLookupName("Kraken Ammo");
        ammo.damagePerShot = 10;
        ammo.ammoType = AmmoTypeEnum.KRAKENM;
        ammo.shots = 1;
        ammo.bv = 288;
        ammo.cost = 55000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createScreenLauncherAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Screen Launcher Ammo";
        ammo.setInternalName("Ammo Screen");
        ammo.addLookupName("ScreenLauncher Ammo");
        ammo.damagePerShot = 0;
        ammo.ammoType = AmmoTypeEnum.SCREEN_LAUNCHER;
        ammo.shots = 1;
        ammo.tonnage = 10.0;
        ammo.bv = 20;
        ammo.cost = 10000;
        ammo.flags = ammo.flags.or(F_SCREEN);
        ammo.rulesRefs = "237, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3053, 3055, 3057, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    // Sub-Capital Cannons
    private static AmmoType createLightSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light SCC Ammo";
        ammo.setInternalName("Ammo Light SCC");
        ammo.addLookupName("Light SCC Ammo");
        ammo.addLookupName("LightSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SCC;
        ammo.shots = 1;
        ammo.tonnage = 0.5;
        ammo.bv = 47;
        ammo.cost = 10000;
        ammo.ammoRatio = 2;
        ammo.capital = true;
        ammo.rulesRefs = "343, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createMediumSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium SCC Ammo";
        ammo.setInternalName("Ammo Medium SCC");
        ammo.addLookupName("Medium SCC Ammo");
        ammo.addLookupName("MediumSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.SCC;
        ammo.shots = 1;
        ammo.bv = 89;
        ammo.cost = 18000;
        ammo.ammoRatio = 1;
        ammo.capital = true;
        ammo.rulesRefs = "343, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createHeavySCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy SCC Ammo";
        ammo.setInternalName("Ammo Heavy SCC");
        ammo.addLookupName("Heavy SCC Ammo");
        ammo.addLookupName("HeavySCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoTypeEnum.SCC;
        ammo.shots = 1;
        ammo.tonnage = 2;
        ammo.bv = 124;
        ammo.cost = 25000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.rulesRefs = "343, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Sub-Capital Missiles

    private static AmmoType createMantaRayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Manta Ray Ammo";
        ammo.setInternalName("Ammo Manta Ray");
        ammo.addLookupName("MantaRay Ammo");
        ammo.addLookupName("Manta Ray Ammo");
        ammo.damagePerShot = 5;
        ammo.ammoType = AmmoTypeEnum.MANTA_RAY;
        ammo.shots = 1;
        ammo.tonnage = 18.0;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.ammoRatio = 18;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createSwordfishAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Swordfish Ammo";
        ammo.setInternalName("Ammo Swordfish");
        ammo.addLookupName("Swordfish Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoTypeEnum.SWORDFISH;
        ammo.shots = 1;
        ammo.tonnage = 15.0;
        ammo.bv = 40;
        ammo.cost = 25000;
        ammo.capital = true;
        ammo.ammoRatio = 15;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createStingrayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Stingray Ammo";
        ammo.setInternalName("Ammo Stingray");
        ammo.addLookupName("Stingray Ammo");
        ammo.addLookupName("ClStingray Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoTypeEnum.STINGRAY;
        ammo.shots = 1;
        ammo.tonnage = 12.0;
        ammo.bv = 62;
        ammo.cost = 19000;
        ammo.ammoRatio = 12;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createPiranhaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Piranha Ammo";
        ammo.setInternalName("Ammo Piranha");
        ammo.addLookupName("Piranha Ammo");
        ammo.addLookupName("PiranhaAmmo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoTypeEnum.PIRANHA;
        ammo.shots = 1;
        ammo.tonnage = 10.0;
        ammo.bv = 71;
        ammo.cost = 15000;
        ammo.ammoRatio = 10;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // AR10 Ammo
    // TODO - Check to see if these can be eliminated as the AR10 Fires standard
    // missiles.

    private static AmmoType createAR10BarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Barracuda Ammo";
        ammo.shortName = "B";
        ammo.setInternalName("Ammo AR10 Barracuda");
        ammo.addLookupName("AR10 Barracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        // Set the date TP of these weapons to match the AR10 and the ratings to match
        // the missiles
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2540, 2550, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createAR10KillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Killer Whale Ammo";
        ammo.shortName = "KW";
        ammo.setInternalName("Ammo AR10 Killer Whale");
        ammo.addLookupName("AR10 KillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date TP of these weapons to match the AR10 and the ratings to match
        // the missiles
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2540, 2550, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createAR10WhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 White Shark Ammo";
        ammo.shortName = "WS";
        ammo.setInternalName("Ammo AR10 White Shark");
        ammo.addLookupName("AR10 WhiteShark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date TP of these weapons to match the AR10 and the ratings to match
        // the missiles
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2540, 2550, 3055, 2950, 3051)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    // AR10s cannot launch tele missiles, so the AR10 tele-missiles are unofficial.

    private static AmmoType createAR10BarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Barracuda (Tele-Operated) Ammo";
        ammo.shortName = "B-T";
        ammo.setInternalName("Ammo AR10 Barracuda-T");
        ammo.addLookupName("AR10 BarracudaT Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        // Set the date of these weapons to match the Tele Missile itself
        ammo.rulesRefs = "251, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3053, 3056, 3060)
              .setISApproximate(true, false, false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC)
              .setReintroductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return ammo;
    }

    private static AmmoType createAR10KillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Killer Whale (Tele-Operated) Ammo";
        ammo.shortName = "KW-T";
        ammo.setInternalName("Ammo AR10 Killer Whale-T");
        ammo.addLookupName("AR10 KillerWhaleT Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date of these weapons to match the Tele Missile itself
        ammo.rulesRefs = "251, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3053, 3056, 3060)
              .setISApproximate(true, false, false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC)
              .setReintroductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return ammo;
    }

    private static AmmoType createAR10WhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 White Shark (Tele-Operated) Ammo";
        ammo.shortName = "WS-T";
        ammo.setInternalName("Ammo AR10 White Shark-T");
        ammo.addLookupName("AR10 WhiteSharkT Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date of these weapons to match the Tele Missile itself
        ammo.rulesRefs = "251, TW";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3053, 3056, 3060)
              .setISApproximate(true, false, false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setPrototypeFactions(Faction.CS, Faction.DC)
              .setProductionFactions(Faction.DC)
              .setReintroductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return ammo;
    }

    // Industrial Munitions

    private static AmmoType createISNailRivetGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Nail/Rivet Gun Ammo";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("IS Ammo Nail/Rivet - Full");
        ammo.addLookupName("ISNailRivetGun Ammo (300)");
        ammo.addLookupName("CL Ammo Nail/Rivet - Full");
        ammo.addLookupName("CLNailRivetGun Ammo (300)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.NAIL_RIVET_GUN;
        ammo.shots = 300;
        ammo.bv = 1;
        ammo.cost = 300;
        ammo.tonnage = 1f;
        ammo.explosive = false;
        ammo.rulesRefs = "246, TM";
        ammo.kgPerShot = 3.33;
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
              .setISApproximate(true, true, false, false, false)
              .setClanAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISNailRivetGunAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Nail/Rivet Gun Ammo (Half-ton)";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("IS Ammo Nail/Rivet - Half");
        ammo.addLookupName("CL Ammo Nail/Rivet - Half");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.NAIL_RIVET_GUN;
        ammo.shots = 150;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        ammo.cost = 150;
        ammo.explosive = false;
        ammo.rulesRefs = "246, TM";
        ammo.kgPerShot = 3.33;
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
              .setISApproximate(true, true, false, false, false)
              .setClanAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
        return ammo;
    }

    private static AmmoType createISC3RemoteSensorAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "C3 Remote Sensors";
        ammo.shortName = "C3 Remote Sensor";
        ammo.setInternalName("ISC3Sensors");
        ammo.explosive = false;
        ammo.damagePerShot = 0; // only used for ammo criticalSlots
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.C3_REMOTE_SENSOR;
        ammo.shots = 4;
        ammo.bv = 6;
        ammo.cost = 100000;

        ammo.techAdvancement.setTechBase(TechBase.IS);
        ammo.techAdvancement.setISAdvancement(3072, DATE_NONE, DATE_NONE);
        ammo.techAdvancement.setTechRating(TechRating.E);
        ammo.techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.X,
              AvailabilityValue.F,
              AvailabilityValue.X);
        return ammo;
    }

    // THUNDERBOLT LRMs
    private static AmmoType createISThunderbolt5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thunderbolt 5 Ammo";
        ammo.shortName = "Thunderbolt 5";
        ammo.setInternalName("IS Ammo Thunderbolt-5");
        ammo.addLookupName("ISThunderbolt5 Ammo");
        ammo.addLookupName("IS Thunderbolt 5 Ammo");
        ammo.addLookupName("ISTBolt5 Ammo");
        ammo.damagePerShot = 5;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.TBOLT_5;
        ammo.shots = 12;
        ammo.bv = 8;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISThunderbolt10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thunderbolt 10 Ammo";
        ammo.shortName = "Thunderbolt 10";
        ammo.setInternalName("IS Ammo Thunderbolt-10");
        ammo.addLookupName("ISThunderbolt10 Ammo");
        ammo.addLookupName("IS Thunderbolt 10 Ammo");
        ammo.addLookupName("ISTBolt10 Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.TBOLT_10;
        ammo.shots = 6;
        ammo.bv = 16;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISThunderbolt15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thunderbolt 15 Ammo";
        ammo.shortName = "Thunderbolt 15";
        ammo.setInternalName("IS Ammo Thunderbolt-15");
        ammo.addLookupName("ISThunderbolt15 Ammo");
        ammo.addLookupName("IS Thunderbolt 15 Ammo");
        ammo.addLookupName("ISTBolt15 Ammo");
        ammo.damagePerShot = 15;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.TBOLT_15;
        ammo.shots = 4;
        ammo.bv = 29;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISThunderbolt20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thunderbolt 20 Ammo";
        ammo.shortName = "Thunderbolt 20";
        ammo.setInternalName("IS Ammo Thunderbolt-20");
        ammo.addLookupName("ISThunderbolt20 Ammo");
        ammo.addLookupName("IS Thunderbolt 20 Ammo");
        ammo.addLookupName("ISTBolt20 Ammo");
        ammo.damagePerShot = 20;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.TBOLT_20;
        ammo.shots = 3;
        ammo.bv = 38;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // TODO : PRIMITIVE and PROTOTYPE

    // PRIMITIVE AMMOs
    private static AmmoType createISAC2pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/2 Ammo";
        ammo.shortName = "AC/2p";
        ammo.setInternalName("IS Ammo AC/2 Primitive");
        ammo.addLookupName("ISAC2p Ammo");
        ammo.addLookupName("IS Autocannon/2 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_PRIMITIVE;
        ammo.shots = 34;
        ammo.bv = 5;
        ammo.cost = 1000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2290, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISAC5pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/5 Ammo";
        ammo.shortName = "AC/5p";
        ammo.setInternalName("IS Ammo AC/5 Primitive");
        ammo.addLookupName("ISAC5p Ammo");
        ammo.addLookupName("IS Autocannon/5 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_PRIMITIVE;
        ammo.shots = 15;
        ammo.bv = 9;
        ammo.cost = 4500;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2240, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISAC10pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/10 Ammo";
        ammo.shortName = "AC/10p";
        ammo.setInternalName("IS Ammo AC/10 Primitive");
        ammo.addLookupName("ISAC10p Ammo");
        ammo.addLookupName("IS Autocannon/10 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_PRIMITIVE;
        ammo.shots = 8;
        ammo.bv = 12;
        ammo.cost = 12000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2450, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISAC20pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/20 Ammo";
        ammo.shortName = "AC/20p";
        ammo.setInternalName("IS Ammo AC/20 Primitive");
        ammo.addLookupName("ISAC20p Ammo");
        ammo.addLookupName("IS Autocannon/20 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_PRIMITIVE;
        ammo.shots = 4;
        ammo.bv = 22;
        ammo.cost = 10000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2488, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM5pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 5 Ammo";
        ammo.shortName = "LRM 5p";
        ammo.setInternalName("IS Ammo LRM-5 Primitive");
        ammo.addLookupName("ISLRM5p Ammo");
        ammo.addLookupName("IS LRM 5 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM_PRIMITIVE;
        ammo.shots = 18;
        ammo.bv = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM10pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 10 Ammo";
        ammo.shortName = "LRM 10p";
        ammo.setInternalName("IS Ammo LRM-10 Primitive");
        ammo.addLookupName("ISLRM10p Ammo");
        ammo.addLookupName("IS LRM 10 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM_PRIMITIVE;
        ammo.shots = 9;
        ammo.bv = 11;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM15pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 15 Ammo";
        ammo.shortName = "LRM 15p";
        ammo.setInternalName("IS Ammo LRM-15 Primitive");
        ammo.addLookupName("ISLRM15p Ammo");
        ammo.addLookupName("IS LRM 15 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM_PRIMITIVE;
        ammo.shots = 6;
        ammo.bv = 17;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISLRM20pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 20 Ammo";
        ammo.shortName = "LRM 20p";
        ammo.setInternalName("IS Ammo LRM-20 Primitive");
        ammo.addLookupName("ISLRM20p Ammo");
        ammo.addLookupName("IS LRM 20 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM_PRIMITIVE;
        ammo.shots = 5;
        ammo.bv = 23;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISSRM2pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype SRM 2 Ammo";
        ammo.shortName = "SRM 2p";
        ammo.setInternalName("IS Ammo SRM-2 Primitive");
        ammo.addLookupName("ISSRM2p Ammo");
        ammo.addLookupName("IS SRM 2 Primitive Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_PRIMITIVE;
        ammo.flags = ammo.flags.andNot(F_BATTLEARMOR);
        ammo.shots = 38;
        ammo.bv = 3;
        ammo.cost = 27000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISSRM4pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype SRM 4 Ammo";
        ammo.shortName = "SRM 4p";
        ammo.setInternalName("IS Ammo SRM-4 Primitive");
        ammo.addLookupName("ISSRM4p Ammo");
        ammo.addLookupName("IS SRM 4 Primitive Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_PRIMITIVE;
        ammo.flags = ammo.flags.andNot(F_BATTLEARMOR);
        ammo.shots = 19;
        ammo.bv = 5;
        ammo.cost = 27000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISSRM6pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype SRM 6 Ammo";
        ammo.shortName = "SRM 6p";
        ammo.setInternalName("IS Ammo SRM-6 Primitive");
        ammo.addLookupName("ISSRM6p Ammo");
        ammo.addLookupName("IS SRM 6 Primitive Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_PRIMITIVE;
        ammo.flags = ammo.flags.andNot(F_BATTLEARMOR);
        ammo.shots = 11;
        ammo.bv = 7;
        ammo.cost = 27000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createISPrimitiveLongTomAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Long Tom Artillery Ammo";
        ammo.shortName = "Primitive Long Tom";
        ammo.setInternalName("ISPrimitiveLongTomAmmo");
        ammo.addLookupName("ISPrimitiveLongTomArtillery Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoTypeEnum.LONG_TOM_PRIM;
        ammo.shots = 4;
        ammo.bv = 35;
        ammo.cost = 10000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around. This to cover some of the back worlds
        // in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);
        return ammo;
    }

    private static AmmoType createPrototypeArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Prototype Arrow IV Ammo";
        ammo.shortName = "pArrow IV";
        ammo.setInternalName("ProtoTypeArrowIVAmmo");
        ammo.addLookupName("ProtoArrowIV Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.ARROWIV_PROTO;
        ammo.shots = 4;
        ammo.bv = 30;
        ammo.cost = 40000;
        ammo.rulesRefs = "217, IO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3044)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    // Clan Improved Stuff.
    private static AmmoType createCLImprovedAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/2 Ammo";
        ammo.shortName = "iAC/2 Ammo";
        ammo.setInternalName("CLIMPAmmoAC2");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_IMP;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 1000;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
              .setClanApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.CLAN)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/5 Ammo";
        ammo.shortName = "iAC/5 Ammo";
        ammo.setInternalName("CLIMPAmmoAC5");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_IMP;
        ammo.shots = 20;
        ammo.bv = 9;
        ammo.cost = 4500;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
              .setClanApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.CLAN)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/10 Ammo";
        ammo.shortName = "iAC/10 Ammo";
        ammo.setInternalName("CLIMPAmmoAC10");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_IMP;
        ammo.shots = 10;
        ammo.bv = 15;
        ammo.cost = 6000;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
              .setClanApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.CLAN)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/20 Ammo";
        ammo.shortName = "iAC/20 Ammo";
        ammo.setInternalName("CLIMPAmmoAC20");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_IMP;
        ammo.shots = 5;
        ammo.bv = 22;
        ammo.cost = 10000;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
              .setClanApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.CLAN)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    // CLAN IMPROVED LRMS
    private static AmmoType createCLImprovedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 5 Ammo";
        ammo.shortName = "iLRM 5";
        ammo.setInternalName("ClanImprovedLRM5Ammo");
        ammo.addLookupName("CLImpLRM5Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM_IMP;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2818, 2820, 2831, 3080)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 10 Ammo";
        ammo.shortName = "iLRM 10";
        ammo.setInternalName("ClanImprovedLRM10Ammo");
        ammo.addLookupName("CLImpLRM10Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LRM_IMP;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2818, 2820, 2831, 3080)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 15 Ammo";
        ammo.setInternalName("ClanImprovedLRM15Ammo");
        ammo.addLookupName("CLImpLRM15Ammo");
        ammo.shortName = "iLRM 15";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.LRM_IMP;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2818, 2820, 2831, 3080)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 20 Ammo";
        ammo.shortName = "iLRM 20";
        ammo.setInternalName("ClanImprovedLRM20Ammo");
        ammo.addLookupName("CLImpLRM20Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LRM_IMP;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2818, 2820, 2831, 3080)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CCY)
              .setProductionFactions(Faction.CCY)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    private static AmmoType createCLImprovedGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Gauss Rifle Ammo";
        ammo.shortName = "iGauss";
        ammo.setInternalName("CLImpGaussAmmo");
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.GAUSS_IMP;
        ammo.shots = 8;
        ammo.bv = 40;
        ammo.cost = 20000;
        ammo.kgPerShot = 125;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E)
              .setClanAdvancement(2818, 2821, 2822, 2837, 3080)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS)
              .setReintroductionFactions(Faction.EI);
        return ammo;
    }

    // Clan Improved SRMs
    private static AmmoType createCLImprovedSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved SRM 2 Ammo";
        ammo.shortName = "iSRM 2";
        ammo.setInternalName("ClanImpAmmoSRM2");
        ammo.addLookupName("CLImpSRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_IMP;
        ammo.shots = 50;
        ammo.bv = 4;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2817, 2819, 2828, 3080)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLImprovedSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved SRM 4 Ammo";
        ammo.shortName = "iSRM 4";
        ammo.setInternalName("ClImpAmmoSRM4");
        ammo.addLookupName("CLImpSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_IMP;
        ammo.shots = 25;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2817, 2819, 2828, 3080)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLImprovedSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved SRM 6 Ammo";
        ammo.shortName = "iSRM 6";
        ammo.setInternalName("CLImpAmmoSRM6");
        ammo.addLookupName("CLImpSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_IMP;
        ammo.shots = 15;
        ammo.bv = 10;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.X, AvailabilityValue.X)
              .setClanAdvancement(2815, 2817, 2819, 2828, 3080)
              .setClanApproximate(true, false, false, true, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // TODO - To be Sorted

    // Start BattleArmor and ProtoMek ammo

    private static AmmoType createBAMicroBombAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Micro Bomb Ammo";
        ammo.shortName = "Micro Bomb";
        ammo.setInternalName("BA-Micro Bomb Ammo");
        ammo.addLookupName("BAMicroBomb Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.BA_MICRO_BOMB;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.kgPerShot = 0;
        ammo.bv = 0;
        ammo.cost = 500;
        ammo.rulesRefs = "253, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CCC)
              .setProductionFactions(Faction.CCC);
        return ammo;
    }

    private static AmmoType createCLTorpedoLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Torpedo/LRM 5 Ammo";
        ammo.shortName = "Torpedo/LRM 5";
        ammo.setInternalName("Clan Torpedo/LRM5 Ammo");
        ammo.addLookupName("CLTorpedoLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM_TORPEDO_COMBO;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 7;
        ammo.cost = 30000;

        ammo.techAdvancement.setTechBase(TechBase.CLAN);
        ammo.techAdvancement.setClanAdvancement(DATE_NONE, DATE_NONE, 2820);
        ammo.techAdvancement.setTechRating(TechRating.C);
        ammo.techAdvancement.setAvailability(AvailabilityValue.X,
              AvailabilityValue.C,
              AvailabilityValue.C,
              AvailabilityValue.X);
        return ammo;
    }

    private static AmmoType createBACompactNarcAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Compact Narc Ammo";
        ammo.shortName = "Narc";
        ammo.setInternalName(BattleArmor.DISPOSABLE_NARC_AMMO);
        ammo.addLookupName("BACompactNarc Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.NARC;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR).or(F_ENCUMBERING);
        ammo.shots = 1;
        ammo.explosive = false;
        ammo.bv = 0;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "263, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_NONE, DATE_NONE, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2870, 2875, 3065, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CSV)
              .setProductionFactions(Faction.CSV);
        return ammo;
    }

    private static AmmoType createBAMineLauncherAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Pop-up Mine Ammo";
        ammo.shortName = "Mine";
        ammo.setInternalName("BA-Mine Launcher Ammo");
        ammo.addLookupName("BAMineLauncher Ammo");
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MINE;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 15000;
        ammo.rulesRefs = "267, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.F)
              .setISAdvancement(DATE_NONE, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createCLPROHeavyMGAmmo() {
        // Need special processing to allow non-standard ammo loads.
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo";
        ammo.shortName = "Heavy Machine Gun";
        ammo.setInternalName("Clan Heavy Machine Gun Ammo - Proto");
        ammo.addLookupName("CLHeavyMG Ammo");
        ammo.addLookupName("Clan Heavy Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMEK);
        ammo.shots = 100;
        ammo.kgPerShot = 10;
        ammo.bv = 1;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But MG
        // Tech Base and Avail Ratings.
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setClanAdvancement(3055, 3060, 3060, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLPROMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("Clan Machine Gun Ammo - Proto");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MG;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMEK);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.kgPerShot = 5;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But MG
        // Tech Base and Avail Ratings.
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.A)
              .setClanAdvancement(3055, 3060, 3060, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    private static AmmoType createCLPROLightMGAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Light Machine Gun Ammo";
        ammo.shortName = "Light Machine Gun";
        ammo.setInternalName("Clan Light Machine Gun Ammo - Proto");
        ammo.addLookupName("CLLightMG Ammo");
        ammo.addLookupName("Clan Light Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMEK);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.kgPerShot = 5;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression. But MG
        // Tech Base and Avail Ratings.
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(3055, 3060, 3060, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CSJ);
        return ammo;
    }

    // IS BA LRM Missile Launchers
    private static AmmoType createBAISLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 1 Ammo";
        ammo.shortName = "LRM 1";
        ammo.setInternalName("IS BA Ammo LRM-1");
        ammo.addLookupName("BAISLRM1 Ammo");
        ammo.addLookupName("BAISLRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.3;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createBAISLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 2 Ammo";
        ammo.shortName = "LRM 2";
        ammo.setInternalName("IS BA Ammo LRM-2");
        ammo.addLookupName("BAISLRM2 Ammo");
        ammo.addLookupName("BAISLRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 16.6;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createBAISLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 3 Ammo";
        ammo.shortName = "LRM 3";
        ammo.setInternalName("IS BA Ammo LRM-3");
        ammo.addLookupName("BAISLRM3 Ammo");
        ammo.addLookupName("BAISLRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 25;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createBAISLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 4 Ammo";
        ammo.shortName = "LRM 4";
        ammo.setInternalName("IS BA Ammo LRM-4");
        ammo.addLookupName("BAISLRM4 Ammo");
        ammo.addLookupName("BAISLRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 5;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 33.4;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createBAISLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("IS BA Ammo LRM-5");
        ammo.addLookupName("BAISLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 41.5;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    // Clan BA LRM Missile Launcher
    private static AmmoType createBACLLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 1 Ammo";
        ammo.shortName = "LRM 1";
        ammo.setInternalName("BACL Ammo LRM-1");
        ammo.addLookupName("BACLLRM1 Ammo");
        ammo.addLookupName("BACL LRM 1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 8.3;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 2 Ammo";
        ammo.shortName = "LRM 2";
        ammo.setInternalName("BACL Ammo LRM-2");
        ammo.addLookupName("BACLLRM2 Ammo");
        ammo.addLookupName("BACL LRM 2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.kgPerShot = 16.6;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 3 Ammo";
        ammo.shortName = "LRM 3";
        ammo.setInternalName("BACL Ammo LRM-3");
        ammo.addLookupName("BACLLRM3 Ammo");
        ammo.addLookupName("BACL LRM 3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 25;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 4 Ammo";
        ammo.shortName = "LRM 4";
        ammo.setInternalName("BACL Ammo LRM-4");
        ammo.addLookupName("BACLLRM4 Ammo");
        ammo.addLookupName("BACL LRM 4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.kgPerShot = 33.3;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("BACL Ammo LRM-5");
        ammo.addLookupName("BACLLRM5 Ammo");
        ammo.addLookupName("BACL LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 7;
        ammo.kgPerShot = 41.5;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CGS)
              .setProductionFactions(Faction.CGS);
        return ammo;
    }

    // BA SRM
    private static AmmoType createBASRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA SRM 1 Ammo";
        ammo.shortName = "SRM 1";
        ammo.setInternalName("BA-SRM1 Ammo");
        ammo.addLookupName("BASRM-1 Ammo");
        ammo.addLookupName("BASRM1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF, Faction.LC, Faction.FS)
              .setProductionFactions(Faction.CWF, Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBASRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA SRM 2 Ammo";
        ammo.shortName = "SRM 2";
        ammo.setInternalName("BA-SRM2 Ammo");
        ammo.addLookupName("BASRM-2 Ammo");
        ammo.addLookupName("BASRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF, Faction.LC, Faction.FS)
              .setProductionFactions(Faction.CWF, Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBASRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA SRM 3 Ammo";
        ammo.shortName = "SRM 3";
        ammo.setInternalName("BA-SRM3 Ammo");
        ammo.addLookupName("BASRM-3 Ammo");
        ammo.addLookupName("BASRM3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 30;
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF, Faction.LC, Faction.FS)
              .setProductionFactions(Faction.CWF, Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBASRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA SRM 4 Ammo";
        ammo.shortName = "SRM 4";
        ammo.setInternalName("BA-SRM4 Ammo");
        ammo.addLookupName("BASRM-4 Ammo");
        ammo.addLookupName("BASRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 5;
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF, Faction.LC, Faction.FS)
              .setProductionFactions(Faction.CWF, Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBASRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA SRM 5 Ammo";
        ammo.shortName = "SRM 5";
        ammo.setInternalName("BA-SRM5 Ammo");
        ammo.addLookupName("BASRM-5 Ammo");
        ammo.addLookupName("BASRM5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.kgPerShot = 50;
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF, Faction.LC, Faction.FS)
              .setProductionFactions(Faction.CWF, Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBASRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA SRM 6 Ammo";
        ammo.shortName = "SRM 6";
        ammo.setInternalName("BA-SRM6 Ammo");
        ammo.addLookupName("BASRM-6 Ammo");
        ammo.addLookupName("BASRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 7;
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWF, Faction.LC, Faction.FS)
              .setProductionFactions(Faction.CWF, Faction.FS, Faction.LC);
        return ammo;
    }

    // Advanced SRMs
    private static AmmoType createAdvancedSRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Advanced SRM 1 Ammo";
        ammo.shortName = "Advanced SRM 1";
        ammo.setInternalName("BA-Advanced SRM-1 Ammo");
        ammo.addLookupName("BAAdvanced SRM1 Ammo");
        ammo.addLookupName("BAAdvancedSRM1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Advanced SRM 2 Ammo";
        ammo.shortName = "Advanced SRM 2";
        ammo.setInternalName("BA-Advanced SRM-2 Ammo");
        ammo.addLookupName("BA-Advanced SRM-2 Ammo OS");
        ammo.addLookupName("BAAdvancedSRM2 Ammo");
        ammo.addLookupName("BAAdvanced SRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Advanced SRM 3 Ammo";
        ammo.shortName = "Advanced SRM 3";
        ammo.setInternalName("BA-Advanced SRM-3 Ammo");
        ammo.addLookupName("BAAdvanced SRM3 Ammo");
        ammo.addLookupName("BAAdvancedSRM3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.kgPerShot = 30;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Advanced SRM 4 Ammo";
        ammo.shortName = "Advanced SRM 4";
        ammo.setInternalName("BA-Advanced SRM-4 Ammo");
        ammo.addLookupName("BAAdvanced SRM4 Ammo");
        ammo.addLookupName("BAAdvancedSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 8;
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Advanced SRM 5 Ammo";
        ammo.shortName = "Advanced SRM 5";
        ammo.setInternalName("BA-Advanced SRM-5 Ammo");
        ammo.addLookupName("BAAdvancedSRM5 Ammo");
        ammo.addLookupName("BAAdvanced SRM5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 10;
        ammo.kgPerShot = 50;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Advanced SRM 6 Ammo";
        ammo.shortName = "Advanced SRM 6";
        ammo.setInternalName("BA-Advanced SRM-6 Ammo");
        ammo.addLookupName("BAAdvanced SRM6 Ammo");
        ammo.addLookupName("BAAdvancedSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 12;
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CHH)
              .setProductionFactions(Faction.CHH);
        return ammo;
    }

    // BA MRMs
    private static AmmoType createISMRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 1 Ammo";
        ammo.shortName = "MRM 1";
        ammo.setInternalName("IS MRM 1 Ammo");
        ammo.addLookupName("ISMRM1 Ammo");
        ammo.addLookupName("ISBAMRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 1;
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.B)
              .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 2 Ammo";
        ammo.shortName = "MRM 2";
        ammo.setInternalName("IS MRM 2 Ammo");
        ammo.addLookupName("ISMRM2 Ammo");
        ammo.addLookupName("ISBAMRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.B)
              .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 3 Ammo";
        ammo.shortName = "MRM 3";
        ammo.setInternalName("IS MRM 3 Ammo");
        ammo.addLookupName("ISMRM3 Ammo");
        ammo.addLookupName("ISBAMRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 15;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.B)
              .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 4 Ammo";
        ammo.shortName = "MRM 4";
        ammo.setInternalName("IS MRM 4 Ammo");
        ammo.addLookupName("ISMRM4 Ammo");
        ammo.addLookupName("ISBAMRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.B)
              .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISMRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MRM 5 Ammo";
        ammo.shortName = "MRM 5";
        ammo.setInternalName("IS MRM 5 Ammo");
        ammo.addLookupName("ISMRM5 Ammo");
        ammo.addLookupName("ISBAMRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 25;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.B)
              .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC);
        return ammo;
    }

    private static AmmoType createISBATaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA Taser Ammo";
        ammo.shortName = "Taser";
        ammo.setInternalName(ammo.name);
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.TASER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(3067, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.WB)
              .setProductionFactions(Faction.WB);
        return ammo;
    }

    // BA Rocket Launchers
    private static AmmoType createBARL1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 1 Ammo";
        ammo.setInternalName("BARL1 Ammo");
        ammo.addLookupName("LAW Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBARL2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 2 Ammo";
        ammo.setInternalName("BARL2 Ammo");
        ammo.addLookupName("LAW 2 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-2 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBARL3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 3 Ammo";
        ammo.setInternalName("BARL3 Ammo");
        ammo.addLookupName("LAW 3 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-3 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBARL4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 4 Ammo";
        ammo.setInternalName("BARL4 Ammo");
        ammo.addLookupName("LAW 4 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-4 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    private static AmmoType createBARL5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 5 Ammo";
        ammo.setInternalName("BARL5 Ammo");
        ammo.addLookupName("LAW 5 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-5 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC);
        return ammo;
    }

    // Misc Stuff. (Pods)

    private static AmmoType createISCoolantPod() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Coolant Pod";
        ammo.shortName = "Coolant Pod";
        ammo.setInternalName(EquipmentTypeLookup.COOLANT_POD);
        ammo.addLookupName("IS Coolant Pod");
        ammo.addLookupName("Clan Coolant Pod");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.COOLANT_POD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 50000;

        // TODO : modes is a bodge because there is no proper end phase
        String[] theModes = { "safe", "efficient", "off", "dump" };
        ammo.setModes(theModes);
        ammo.setInstantModeSwitch(true);
        ammo.rulesRefs = "303, TO";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setISAdvancement(DATE_NONE, 3049, 3079, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setClanAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC, Faction.CJF)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MPod Ammo";
        ammo.setInternalName("IS M-Pod Ammo");
        ammo.addLookupName("IS MPod Ammo");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.MPOD;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "330, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3064, 3099, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
        return ammo;
    }

    // Per IO pg 40 - There is no Clan MPod.
    private static AmmoType createCLMPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MPod Ammo";
        ammo.setInternalName("Clan M-Pod Ammo");
        ammo.addLookupName("Clan MPod Ammo");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.MPOD;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3064, 3099, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
        return ammo;
    }

    private static AmmoType createISBPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-BattleArmor Pods (B-Pods) Ammo";
        ammo.setInternalName("ISBPodAmmo");
        ammo.shortName = "B-Pod";
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.BPOD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "204, TM";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3068, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(3065, 3068, 3070, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CWX, Faction.LC, Faction.WB, Faction.FW)
              .setProductionFactions(Faction.CWX);
        return ammo;
    }

    // UNOFFICIAL AMMOs
    private static AmmoType createISAC15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AC/15 Ammo";
        ammo.shortName = "AC/15";
        ammo.setInternalName("IS Ammo AC/15");
        ammo.addLookupName("ISAC15 Ammo");
        ammo.addLookupName("IS Autocannon/15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 7;
        ammo.bv = 22;
        ammo.cost = 8500;
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setUnofficial(true)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
        return ammo;
    }

    private static AmmoType createISTHBLB2XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X AC Ammo (THB)";
        ammo.shortName = "LB 2-X";
        ammo.setInternalName("IS LB 2-X AC Ammo (THB)");
        ammo.addLookupName("IS Ammo 2-X (THB)");
        ammo.addLookupName("ISLBXAC2 Ammo (THB)");
        ammo.addLookupName("IS LB 2-X AC Ammo - Slug (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_LBX_THB;
        ammo.shots = 40;
        ammo.bv = 5;
        ammo.cost = 3000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBLB5XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X AC Ammo (THB)";
        ammo.shortName = "LB 5-X";
        ammo.setInternalName("IS LB 5-X AC Ammo (THB)");
        ammo.addLookupName("IS Ammo 5-X (THB)");
        ammo.addLookupName("ISLBXAC5 Ammo (THB)");
        ammo.addLookupName("IS LB 5-X AC Ammo - Slug (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_LBX_THB;
        ammo.shots = 16;
        ammo.bv = 11;
        ammo.cost = 15000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBLB20XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X AC Ammo (THB)";
        ammo.shortName = "LB 20-X";
        ammo.setInternalName("IS LB 20-X AC Ammo (THB)");
        ammo.addLookupName("IS Ammo 20-X (THB)");
        ammo.addLookupName("ISLBXAC20 Ammo (THB)");
        ammo.addLookupName("IS LB 20-X AC Ammo - Slug (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_LBX_THB;
        ammo.shots = 4;
        ammo.bv = 26;
        ammo.cost = 30000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X Cluster Ammo (THB)";
        ammo.setInternalName("IS LB 2-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 2-X (CL) (THB)");
        ammo.shortName = "LB 2-X Cluster";
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC2 CL Ammo (THB)");
        ammo.addLookupName("IS LB 2-X AC Ammo - Cluster (THB)");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_LBX_THB;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 40;
        ammo.bv = 5;
        ammo.cost = 4950;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.setInternalName("IS LB 5-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 5-X (CL) (THB)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC5 CL Ammo (THB)");
        ammo.addLookupName("IS LB 5-X AC Ammo - Cluster (THB)");
        ammo.name = "LB 5-X Cluster Ammo (THB)";
        ammo.shortName = "LB 5-X Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoTypeEnum.AC_LBX_THB;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 16;
        ammo.bv = 11;
        ammo.cost = 25000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X Cluster Ammo (THB)";
        ammo.shortName = "LB 20-X Cluster";
        ammo.setInternalName("IS LB 20-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 20-X (CL) (THB)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC20 CL Ammo (THB)");
        ammo.addLookupName("IS LB 20-X AC Ammo - Cluster (THB)");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_LBX_THB;
        ammo.munitionType = EnumSet.of(Munitions.M_CLUSTER);
        ammo.shots = 4;
        ammo.bv = 26;
        ammo.cost = 51000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBUltra2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/2 Ammo (THB)";
        ammo.shortName = "Ultra AC/2";
        ammo.setInternalName("IS Ultra AC/2 Ammo (THB)");
        ammo.addLookupName("IS Ammo Ultra AC/2 (THB)");
        ammo.addLookupName("ISUltraAC2 Ammo (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA_THB;
        ammo.shots = 45;
        ammo.bv = 8;
        ammo.cost = 2000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBUltra10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/10 Ammo (THB)";
        ammo.shortName = "Ultra AC/10";
        ammo.setInternalName("IS Ultra AC/10 Ammo (THB)");
        ammo.addLookupName("IS Ammo Ultra AC/10 (THB)");
        ammo.addLookupName("ISUltraAC10 Ammo (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA_THB;
        ammo.shots = 10;
        ammo.bv = 31;
        ammo.cost = 15000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISTHBUltra20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Ultra AC/20 Ammo (THB)";
        ammo.shortName = "Ultra AC/20";
        ammo.setInternalName("IS Ultra AC/20 Ammo (THB)");
        ammo.addLookupName("IS Ammo Ultra AC/20 (THB)");
        ammo.addLookupName("ISUltraAC20 Ammo (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_ULTRA_THB;
        ammo.shots = 5;
        ammo.bv = 42;
        ammo.cost = 30000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISRotary10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/10 Ammo";
        ammo.shortName = "RAC/10";
        ammo.setInternalName("ISRotaryAC10 Ammo");
        ammo.addLookupName("IS Rotary AC/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 10;
        ammo.bv = 37;
        ammo.cost = 30000;

        ammo.techAdvancement.setTechBase(TechBase.IS);
        ammo.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3067);
        ammo.techAdvancement.setTechRating(TechRating.E);
        ammo.techAdvancement.setAvailability(AvailabilityValue.E,
              AvailabilityValue.E,
              AvailabilityValue.E,
              AvailabilityValue.E);
        return ammo;
    }

    private static AmmoType createISRotary20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/20 Ammo";
        ammo.shortName = "RAC/20";
        ammo.setInternalName("ISRotaryAC20 Ammo");
        ammo.addLookupName("IS Rotary AC/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 5;
        ammo.bv = 59;
        ammo.cost = 80000;

        ammo.techAdvancement.setTechBase(TechBase.IS);
        ammo.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3067);
        ammo.techAdvancement.setTechRating(TechRating.E);
        ammo.techAdvancement.setAvailability(AvailabilityValue.E,
              AvailabilityValue.E,
              AvailabilityValue.E,
              AvailabilityValue.E);
        return ammo;
    }

    private static AmmoType createCLRotary10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/10 Ammo";
        ammo.shortName = "RAC/10";
        ammo.setInternalName("CLRotaryAC10 Ammo");
        ammo.addLookupName("CL Rotary AC/10 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 10;
        ammo.bv = 74;
        ammo.cost = 16000;
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3073, 3104, 3145, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createCLRotary20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rotary AC/20 Ammo";
        ammo.shortName = "RAC/20";
        ammo.setInternalName("CLRotaryAC20 Ammo");
        ammo.addLookupName("CL Rotary AC/20 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.AC_ROTARY;
        ammo.shots = 5;
        ammo.bv = 118;
        ammo.cost = 24000;
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3073, 3104, 3145, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CSF);
        return ammo;
    }

    private static AmmoType createISLAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LAC/10 Ammo";
        ammo.shortName = "LAC/10";
        ammo.setInternalName("IS Ammo LAC/10");
        ammo.addLookupName("ISLAC10 Ammo");
        ammo.addLookupName("IS Light Autocannon/10 Ammo");
        ammo.addLookupName("Light AC/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.LAC;
        ammo.shots = 10;
        ammo.bv = 9;
        ammo.cost = 10000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.C)
              .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISLAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LAC/20 Ammo";
        ammo.shortName = "LAC/20";
        ammo.setInternalName("IS Ammo LAC/20");
        ammo.addLookupName("ISLAC20 Ammo");
        ammo.addLookupName("IS Light Autocannon/20 Ammo");
        ammo.addLookupName("Light AC/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoTypeEnum.LAC;
        ammo.shots = 5;
        ammo.bv = 15;
        ammo.cost = 20000;
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.C)
              .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISRailGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Rail Gun Ammo";
        ammo.shortName = "Rail Gun";
        ammo.setInternalName("ISRailGun Ammo");
        ammo.addLookupName("IS Rail Gun Ammo");
        ammo.damagePerShot = 22;
        ammo.explosive = false;
        ammo.ammoType = AmmoTypeEnum.RAIL_GUN;
        ammo.shots = 5;
        ammo.bv = 51;
        ammo.cost = 20000;
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3051, 3061, 3067, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FC);
        return ammo;
    }

    private static AmmoType createISAC10iAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AC/10i Ammo";
        ammo.shortName = "AC/10i";
        ammo.setInternalName("IS Ammo AC/10i");
        ammo.addLookupName("ISAC10i Ammo");
        ammo.addLookupName("IS Autocannon/10i Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoTypeEnum.ACi;
        ammo.shots = 10;
        ammo.bv = 21;
        ammo.cost = 12000;
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setUnofficial(true)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
              .setClanApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);
        return ammo;
    }

    private static AmmoType createISGAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/2 Ammo";
        ammo.shortName = "GAC/2";
        ammo.setInternalName("IS Ammo GAC/2");
        ammo.addLookupName("ISGAC2 Ammo");
        ammo.addLookupName("IS Gatling AC/2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 22;
        ammo.bv = 12;
        ammo.cost = 1000;
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISGAC4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/4 Ammo";
        ammo.shortName = "GAC/4";
        ammo.setInternalName("IS Ammo GAC/4");
        ammo.addLookupName("ISGAC4 Ammo");
        ammo.addLookupName("IS Gatling AC/4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 11;
        ammo.bv = 22;
        ammo.cost = 1000;
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISGAC6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/6 Ammo";
        ammo.shortName = "GAC/6";
        ammo.setInternalName("IS Ammo GAC/6");
        ammo.addLookupName("ISGAC6 Ammo");
        ammo.addLookupName("IS Gatling AC/6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 7;
        ammo.bv = 40;
        ammo.cost = 1000;
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    private static AmmoType createISGAC8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/8 Ammo";
        ammo.shortName = "GAC/8";
        ammo.setInternalName("IS Ammo GAC/8");
        ammo.addLookupName("ISGAC8 Ammo");
        ammo.addLookupName("IS Gatling AC/8 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoTypeEnum.AC;
        ammo.shots = 5;
        ammo.bv = 53;
        ammo.cost = 1000;
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(true)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS);
        return ammo;
    }

    // TODO - THINGS NUCLEAR
    private static AmmoType createAR10PeacemakerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Peacemaker Ammo";
        ammo.shortName = "PM-N";
        ammo.setInternalName("Ammo AR10 Peacemaker");
        ammo.addLookupName("AR10 Peacemaker Ammo");
        ammo.damagePerShot = 1000;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.tonnage = 50.0;
        ammo.shots = 1;
        ammo.bv = 10000;
        ammo.cost = 40000000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_PEACEMAKER);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setISAdvancement(2300)
              .setPrototypeFactions(Faction.TA)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createPeacemakerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Peacemaker Ammo";
        ammo.setInternalName("Ammo Peacemaker");
        ammo.addLookupName("Peacemaker Ammo");
        ammo.addLookupName("CLPeacemaker Ammo");
        ammo.addLookupName("Ammo Clan Peacemaker");
        ammo.damagePerShot = 1000;
        ammo.ammoType = AmmoTypeEnum.KILLER_WHALE;
        ammo.tonnage = 50.0;
        ammo.shots = 1;
        ammo.bv = 10000;
        ammo.cost = 40000000;
        ammo.flags = ammo.flags.or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_PEACEMAKER);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setISAdvancement(2300)
              .setPrototypeFactions(Faction.TA)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createAR10SantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Santa Anna Ammo";
        ammo.shortName = "SA-N";
        ammo.setInternalName("Ammo AR10 Santa Anna");
        ammo.addLookupName("AR10 SantaAnna Ammo");
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoTypeEnum.AR10;
        ammo.tonnage = 40.0;
        ammo.shots = 1;
        ammo.bv = 1000;
        ammo.cost = 15000000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_SANTA_ANNA);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setISAdvancement(2300)
              .setPrototypeFactions(Faction.TA)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createSantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Santa Anna Ammo";
        ammo.setInternalName("Ammo Santa Anna");
        ammo.addLookupName("SantaAnna Ammo");
        ammo.addLookupName("CLSantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoTypeEnum.WHITE_SHARK;
        ammo.tonnage = 40.0;
        ammo.shots = 1;
        ammo.bv = 1000;
        ammo.cost = 15000000;
        ammo.flags = ammo.flags.or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_SANTA_ANNA);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setISAdvancement(2300)
              .setPrototypeFactions(Faction.TA)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createAlamoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Alamo Ammo";
        ammo.setInternalName("Ammo Alamo");
        ammo.addLookupName("Alamo Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoTypeEnum.ALAMO;
        ammo.shots = 1;
        ammo.bv = 100;
        ammo.cost = 1000000;
        ammo.flags = ammo.flags.or(F_NUCLEAR);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setISAdvancement(2200)
              .setPrototypeFactions(Faction.TA)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // Generic infantry ammo, stats are determined by the weapon it's linked to
    private static AmmoType createInfantryAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Standard Ammo";
        ammo.setInternalName(EquipmentTypeLookup.INFANTRY_AMMO);
        ammo.ammoType = AmmoTypeEnum.INFANTRY;
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.A)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createInfantryInfernoAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Inferno Ammo";
        ammo.setInternalName(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        ammo.ammoType = AmmoTypeEnum.INFANTRY;
        ammo.munitionType = EnumSet.of(Munitions.M_INFERNO);
        ammo.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.A)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    @Override
    public String toString() {
        return "[Ammo] " + internalName;
    }

    public static boolean canClearMinefield(AmmoType at) {
        if (at != null) {
            if (at.getMunitionType().contains(Munitions.M_MINE_CLEARANCE)) {
                return true;
            }
            // LRM-20's, RL-20's, and MRM 20, 30, and 40 can clear minefields
            if (((at.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (at.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (at.getAmmoType() == AmmoTypeEnum.LRM_STREAK) ||
                  (at.getAmmoType() == AmmoTypeEnum.EXLRM) ||
                  (at.getAmmoType() == AmmoTypeEnum.MRM) ||
                  (at.getAmmoType() == AmmoTypeEnum.ROCKET_LAUNCHER)) &&
                  (at.getRackSize() >= 20) &&
                  ((at.getMunitionType().contains(Munitions.M_STANDARD)) ||
                        (at.getMunitionType().contains(Munitions.M_ARTEMIS_CAPABLE)) ||
                        (at.getMunitionType().contains(Munitions.M_ARTEMIS_V_CAPABLE)) ||
                        (at.getMunitionType().contains(Munitions.M_NARC_CAPABLE)))) {
                return true;
            }
            // ATMs
            if ((at.getAmmoType() == AmmoTypeEnum.ATM) &&
                  ((at.getRackSize() >= 12 && !(at.getMunitionType().contains(Munitions.M_EXTENDED_RANGE)) ||
                        (at.getRackSize() >= 9 && at.getMunitionType().contains(Munitions.M_HIGH_EXPLOSIVE))))) {
                return true;
            }
            // Artillery
            return ((at.getAmmoType() == AmmoTypeEnum.ARROW_IV) ||
                  (at.getAmmoType() == AmmoTypeEnum.LONG_TOM) ||
                  (at.getAmmoType() == AmmoTypeEnum.SNIPER) ||
                  (at.getAmmoType() == AmmoTypeEnum.THUMPER)) && (at.getMunitionType().contains(Munitions.M_STANDARD));
        }
        // TODO: mine clearance munitions

        return false;
    }

    public static boolean canDeliverMinefield(AmmoType at) {
        return (at != null) &&
              ((at.getAmmoType() == AmmoTypeEnum.LRM) ||
                    (at.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                    (at.getAmmoType() == AmmoTypeEnum.MML)) &&
              ((at.getMunitionType().contains(Munitions.M_THUNDER)) ||
                    (at.getMunitionType().contains(Munitions.M_THUNDER_INFERNO)) ||
                    (at.getMunitionType().contains(Munitions.M_THUNDER_AUGMENTED)) ||
                    (at.getMunitionType().contains(Munitions.M_THUNDER_VIBRABOMB)) ||
                    (at.getMunitionType().contains(Munitions.M_THUNDER_ACTIVE)));
    }

    private void addToEnd(AmmoType base, String modifier) {
        Enumeration<String> n = base.getNames();
        while (n.hasMoreElements()) {
            String s = n.nextElement();
            addLookupName(s + modifier);
        }
    }

    private void addBeforeString(AmmoType base, String keyWord, String modifier) {
        Enumeration<String> names = base.getNames();
        while (names.hasMoreElements()) {
            String s = names.nextElement();
            StringBuilder sb = new StringBuilder(s);
            sb.insert(s.lastIndexOf(keyWord), modifier);
            addLookupName(sb.toString());
        }
    }

    /**
     * Helper class for creating munition types.
     */
    private static class MunitionMutator {
        /**
         * The name of this munition type.
         */
        private final String name;

        /**
         * The weight ratio of a round of this munition to a standard round.
         */
        private final double weight;

        /**
         * The munition flag(s) for this type.
         */
        private final EnumSet<Munitions> type;

        protected String rulesRefs;

        private final TechAdvancement techAdvancement;

        // PLAyTEST3 changed to float for weightRatio
        public MunitionMutator(String munitionName, double weightRatio, Munitions munitionType,
              TechAdvancement techAdvancement, String rulesRefs) {
            name = munitionName;
            weight = weightRatio;
            type = EnumSet.of(munitionType);
            this.techAdvancement = new TechAdvancement(techAdvancement);
            this.rulesRefs = rulesRefs;
        }

        /**
         * Create the <code>AmmoType</code> for this munition type for the given rack size.
         *
         * @param base - the <code>AmmoType</code> of the base round.
         *
         * @return this munition's <code>AmmoType</code>.
         */
        public AmmoType createMunitionType(AmmoType base) {
            StringBuilder nameBuf;
            StringBuilder internalName;
            int index;

            // Create an uninitialized munition object.
            AmmoType munition = new AmmoType();
            munition.setTonnage(base.getTonnage(null));
            munition.subMunitionName = name;
            munition.base = base;

            // Manipulate the base round's names, depending on ammoType.
            switch (base.ammoType) {
                case AC:
                case AC_PRIMITIVE:
                case LAC:
                case AC_IMP:
                case AC_ROTARY:
                case PAC:
                    // Add the munition name to the beginning of the display name.
                    nameBuf = new StringBuilder(name);
                    nameBuf.append(" ");
                    nameBuf.append(base.name);
                    munition.name = nameBuf.toString();

                    // Add the munition name to the end of the TDB ammo name.
                    nameBuf = new StringBuilder(" - ");
                    nameBuf.append(name);
                    munition.addToEnd(base, " - " + name);

                    // The munition name appears in the middle of the other names.
                    nameBuf = new StringBuilder(base.internalName);
                    index = base.internalName.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    nameBuf.insert(index, name);
                    munition.setInternalName(nameBuf.toString());
                    munition.shortName = munition.name.replace(base.name, base.shortName);
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case ARROWIV_PROTO:
                case ARROW_IV:
                    // The munition name appears in the middle of all names.
                    nameBuf = new StringBuilder(base.name);
                    index = base.name.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    // Do special processing for munition names ending in "IV".
                    // Note: this does not work for The Drawing Board
                    if (name.endsWith("-IV")) {
                        StringBuilder tempName = new StringBuilder(name);
                        tempName.setLength(tempName.length() - 3);
                        nameBuf.insert(index, tempName);
                    } else {
                        nameBuf.insert(index, name);
                    }
                    munition.name = nameBuf.toString();

                    nameBuf = new StringBuilder(base.internalName);
                    index = base.internalName.lastIndexOf("Ammo");
                    nameBuf.insert(index, name);
                    munition.setInternalName(nameBuf.toString());

                    // ADA full name is embarrassingly long.
                    if (base.name.contains("ADA")) {
                        munition.shortName = "ADA Missile";
                        munition.addLookupName("ADA");
                    } else {
                        munition.shortName = munition.name.replace("Prototype ", "p");
                    }

                    munition.addBeforeString(base, "Ammo", name + " ");
                    munition.addToEnd(base, " - " + name);
                    if (name.equals("Homing")) {
                        munition.addToEnd(base, " (HO)"); // mep
                    }
                    break;
                case SRM:
                case SRM_PRIMITIVE:
                case SRM_IMP:
                case MRM:
                case LRM:
                case LRM_PRIMITIVE:
                case LRM_IMP:
                case MML:
                case NLRM:
                case SRM_TORPEDO:
                case LRM_TORPEDO:
                    // Add the munition name to the end of some ammo names.
                    nameBuf = new StringBuilder(" ");
                    nameBuf.append(name);
                    munition.setInternalName(base.internalName + nameBuf);
                    munition.addToEnd(base, nameBuf.toString());
                    nameBuf.insert(0, " -");
                    munition.addToEnd(base, nameBuf.toString());

                    // The munition name appears in the middle of the other names.
                    nameBuf = new StringBuilder(base.name);
                    index = base.name.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    nameBuf.insert(index, name);
                    munition.name = nameBuf.toString();
                    nameBuf = new StringBuilder(base.shortName);
                    nameBuf.append(' ');
                    nameBuf.append(name.replace("-capable", ""));
                    munition.shortName = nameBuf.toString();
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case VGL:
                    // Replace "Fragmentation" with the submunition name
                    munition.name = base.name.replace("Fragmentation", name);

                    munition.shortName = base.shortName.replace("Fragmentation", name);
                    internalName = new StringBuilder(base.getInternalName());
                    munition.setInternalName(internalName.insert(internalName.lastIndexOf("Ammo"), name + " ")
                          .toString());
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case MEK_MORTAR:
                    // Replace "Shaped Charge" with the submunition name
                    munition.name = base.name.replace("Shaped Charge", name);
                    String abr = "SC";
                    if (type.contains(Munitions.M_AIRBURST)) {
                        abr = "AB";
                    } else if (type.contains(Munitions.M_ANTI_PERSONNEL)) {
                        abr = "AP";
                    } else if (type.contains(Munitions.M_FLARE)) {
                        abr = "FL";
                    } else if (type.contains(Munitions.M_SMOKE_WARHEAD)) {
                        abr = "SM";
                    } else if (type.contains(Munitions.M_SEMIGUIDED)) {
                        abr = "SG";
                    }
                    munition.shortName = base.shortName.replace("SC", abr);
                    internalName = new StringBuilder(base.getInternalName().replace("SC", abr));
                    munition.setInternalName(internalName.toString());
                    break;
                case LONG_TOM:
                case LONG_TOM_PRIM:
                case SNIPER:
                case THUMPER:
                case LONG_TOM_CANNON:
                case SNIPER_CANNON:
                case THUMPER_CANNON:
                case VEHICLE_FLAMER:
                case HEAVY_FLAMER:
                case FLUID_GUN:
                case BA_TUBE:
                    // Add the munition name to the beginning of the display name.
                    nameBuf = new StringBuilder(name);
                    nameBuf.append(" ");
                    nameBuf.append(base.name);
                    munition.name = nameBuf.toString();
                    munition.setInternalName(munition.name);
                    munition.addToEnd(base, munition.name);

                    munition.shortName = munition.name;
                    // The munition name appears in the middle of the other names.
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                default:
                    throw new IllegalArgumentException("Don't know how to create munitions for " + base.ammoType);
            }

            munition.shortName = munition.shortName.replace("(Clan) ", "");
            munition.subMunitionName = munition.shortName;

            // Assign our munition type.
            munition.munitionType = type;

            // Make sure the tech level is now correct.
            if (techAdvancement.getIntroductionDate() > 0) {
                munition.techAdvancement = new TechAdvancement(techAdvancement);
            } else {
                munition.techAdvancement = new TechAdvancement(base.techAdvancement);
            }
            munition.techAdvancement.setStaticTechLevel(SimpleTechLevel.max(techAdvancement.getStaticTechLevel(),
                  base.techAdvancement.getStaticTechLevel()));

            munition.rulesRefs = rulesRefs;

            // Reduce base number of shots to reflect the munition's weight.
            if (munition.getMunitionType().contains(Munitions.M_CASELESS)) {
                munition.shots = Math.max(1, base.shots * 2);
                munition.kgPerShot = base.kgPerShot * (weight / 2.0);
            } else {
                // PLAYTEST3 Changed weight to be double from int, so casting it back.
                munition.shots = Math.max(1, (int) (base.shots / weight));
                munition.kgPerShot = base.kgPerShot * weight;
            }

            // copy base ammoType
            munition.ammoType = base.ammoType;
            // check for cost
            double cost = base.cost;
            double bv = base.bv;
            if (((munition.getAmmoType() == AmmoTypeEnum.LONG_TOM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LONG_TOM_CANNON) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SNIPER) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SNIPER_CANNON) ||
                  (munition.getAmmoType() == AmmoTypeEnum.THUMPER) ||
                  (munition.getAmmoType() == AmmoTypeEnum.THUMPER_CANNON)) &&
                  munition.getMunitionType().contains(Munitions.M_FAE)) {
                bv *= 1.4;
                cost *= 3;
            }

            if ((munition.getAmmoType() == AmmoTypeEnum.AC) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LAC) ||
                  (munition.getAmmoType() == AmmoTypeEnum.PAC)) {
                // PLAYTEST3 ammo changes
                if (munition.getMunitionType().contains(Munitions.M_ARMOR_PIERCING) || munition.getMunitionType().contains(Munitions.M_ARMOR_PIERCING_PLAYTEST)) {
                    cost *= 4;
                } else if ((munition.getMunitionType().contains(Munitions.M_FLECHETTE)) ||
                      (munition.getMunitionType().contains(Munitions.M_FLAK))) {
                    cost *= 1.5;
                } else if (munition.getMunitionType().contains(Munitions.M_TRACER)) {
                    cost *= 1.5;
                    bv *= 1.25;
                } else if (munition.getMunitionType().contains(Munitions.M_INCENDIARY_AC)) {
                    cost *= 2;
                } else if (munition.getMunitionType().contains(Munitions.M_PRECISION) || munition.getMunitionType().contains(Munitions.M_PRECISION_PLAYTEST)) {
                    cost *= 6;
                } else if (munition.getMunitionType().contains(Munitions.M_CASELESS)) {
                    cost *= 1.5;
                    bv *= 2.0;
                }
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_AX_HEAD))) {
                cost *= 0.5;
                bv *= 2;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_SMOKE_WARHEAD))) {
                cost *= 0.5;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_INCENDIARY_LRM))) {
                cost *= 1.5;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML)) &&
                  (munition.getMunitionType().contains(Munitions.M_INFERNO))) {
                cost = 13500;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_SEMIGUIDED))) {
                cost *= 3;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_SWARM))) {
                cost *= 2;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_SWARM_I))) {
                cost *= 3;
                bv *= 1.2;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_THUNDER))) {
                cost *= 2;
                // TO:AUE, pp.185,197,198: Half the rack size on 7 hexes; standard mines
                bv = base.rackSize * munition.shots / 5.0 * 4;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_THUNDER_AUGMENTED))) {
                cost *= 4;
                // TO:AUE, pp.185,197,198: Half the rack size on 7 hexes; standard mines
                bv = Math.ceil(base.rackSize / 2.0) * 7 * munition.shots / 5.0 * 4;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_THUNDER_INFERNO))) {
                cost *= 1;
                // TO:AUE, pp.185,197,198
                bv = base.rackSize * munition.shots;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_THUNDER_VIBRABOMB))) {
                cost *= 2.5;
                // TO:AUE, pp.185,197,198
                bv = base.rackSize * munition.shots;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_THUNDER_ACTIVE))) {
                cost *= 3;
                // TO:AUE, pp.185,197,198
                bv = base.rackSize * munition.shots / 5.0 * 6;
            }

            if (munition.getMunitionType().contains(Munitions.M_HOMING)) {
                cost = 15000;
                // Allow Homing munitions to instantly switch between modes
                munition.instantModeSwitch = true;
                munition.setModes("Homing", "Non-Homing");
            }

            if (munition.getMunitionType().contains(Munitions.M_FASCAM)) {
                cost *= 1.5;
                // TO:AR, p.152 and TO:AUE, pp.197,198
                int rackSize = base.getRackSize();
                if (munition.getAmmoType() == AmmoTypeEnum.ARROW_IV) {
                    rackSize = munition.isClan() ? 30 : 20;
                }
                bv = rackSize * munition.shots / 5.0 * 4;
            }

            if (munition.getMunitionType().contains(Munitions.M_INFERNO_IV)) {
                cost *= 1;
            }

            if (munition.getMunitionType().contains(Munitions.M_VIBRABOMB_IV)) {
                // TO:AR 152 and TO:AUE 197,198
                bv = 20 * munition.shots;
                cost *= 2;
            }

            // This is just a hack to make it expensive.
            // We don't have a price for this.
            if (munition.getMunitionType().contains(Munitions.M_DAVY_CROCKETT_M)) {
                cost *= 50;
            }
            if (munition.getMunitionType().contains(Munitions.M_LASER_INHIB)) {
                cost *= 4;
            }
            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_NARC_CAPABLE))) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_ARTEMIS_CAPABLE))) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_LISTEN_KILL))) {
                cost *= 1.1;
            }
            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  ((munition.getMunitionType().contains(Munitions.M_ANTI_TSM)) ||
                        (munition.getMunitionType().contains(Munitions.M_FRAGMENTATION)))) {
                cost *= 2;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP)) &&
                  (munition.getMunitionType().contains(Munitions.M_ARTEMIS_V_CAPABLE))) {
                cost *= 2;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP)) &&
                  ((munition.getMunitionType().contains(Munitions.M_TANDEM_CHARGE)))) {
                cost *= 5;
                bv *= 2.0;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  ((munition.getMunitionType().contains(Munitions.M_HEAT_SEEKING)) ||
                        (munition.getMunitionType().contains(Munitions.M_FOLLOW_THE_LEADER)))) {
                cost *= 2;
                bv *= 1.5;
            }

            if (munition.getMunitionType().contains(Munitions.M_DEAD_FIRE)) {
                cost *= 0.6;
                if (munition.getAmmoType() == AmmoTypeEnum.MML) {
                    if (base.rackSize == 3) {
                        bv = 6;
                    } else if (base.rackSize == 5) {
                        bv = base.hasFlag(F_MML_LRM) ? 9 : 8;
                    } else if (base.rackSize == 7) {
                        bv = base.hasFlag(F_MML_LRM) ? 12 : 11;
                    } else if (base.rackSize == 9) {
                        bv = base.hasFlag(F_MML_LRM) ? 17 : 15;
                    }
                } else {
                    if (base.rackSize == 2) {
                        bv = 4;
                    } else if (base.rackSize == 4) {
                        bv = 7;
                    } else if (base.rackSize == 5) {
                        bv = 9;
                    } else if (base.rackSize == 6) {
                        bv = 10;
                    } else if (base.rackSize == 10) {
                        bv = 17;
                    } else if (base.rackSize == 15) {
                        bv = 26;
                    } else if (base.rackSize == 20) {
                        bv = 35;
                    }
                }
            }

            if (munition.getMunitionType().contains(Munitions.M_LISTEN_KILL)) {
                if (munition.getAmmoType() == AmmoTypeEnum.MML) {
                    if (base.rackSize == 3) {
                        bv = base.hasFlag(F_MML_LRM) ? 9 : 4;
                    } else if (base.rackSize == 5) {
                        bv = base.hasFlag(F_MML_LRM) ? 15 : 7;
                    } else if (base.rackSize == 7) {
                        bv = base.hasFlag(F_MML_LRM) ? 21 : 10;
                    } else if (base.rackSize == 9) {
                        bv = base.hasFlag(F_MML_LRM) ? 27 : 13;
                    }
                } else {
                    if (base.rackSize == 2) {
                        bv = 6;
                    } else if (base.rackSize == 4) {
                        bv = 12;
                    } else if (base.rackSize == 6) {
                        bv = 18;
                    } else if (base.rackSize == 5) {
                        bv = 7;
                    } else if (base.rackSize == 10) {
                        bv = 14;
                    } else if (base.rackSize == 15) {
                        bv = 21;
                    } else if (base.rackSize == 20) {
                        bv = 28;
                    }
                }
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.VEHICLE_FLAMER) ||
                  (munition.getAmmoType() == AmmoTypeEnum.HEAVY_FLAMER) ||
                  (munition.getAmmoType() == AmmoTypeEnum.FLUID_GUN)) &&
                  (munition.getMunitionType().contains(Munitions.M_COOLANT))) {
                cost = 3000;
            }

            if (((munition.getAmmoType() == AmmoTypeEnum.LRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.LRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.MML) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM) ||
                  (munition.getAmmoType() == AmmoTypeEnum.SRM_IMP) ||
                  (munition.getAmmoType() == AmmoTypeEnum.NLRM)) &&
                  (munition.getMunitionType().contains(Munitions.M_ARAD))) {
                bv *= 1.3;
                cost *= 3.0;
            }

            // Account for floating point imprecision
            munition.bv = Math.round(bv * 1000.0) / 1000.0;
            munition.cost = Math.round(cost * 1000.0) / 1000.0;

            // Copy over all other values.
            munition.damagePerShot = base.damagePerShot;
            munition.rackSize = base.rackSize;
            munition.ammoType = base.ammoType;
            munition.flags = base.flags;
            munition.hittable = base.hittable;
            munition.explosive = base.explosive;
            munition.toHitModifier = base.toHitModifier;

            // Return the new munition.
            return munition;
        }
    } // End private class MunitionMutator

    /** @return The battle value for ProtoMek or BA ammo loads. */
    public double getKgPerShotBV(int shots) {
        return ((kgPerShot * shots) / 1000) * bv;
    }

    public String getBaseName() {
        return (base != null) ? base.getShortName() : getShortName();
    }

    public String getSubMunitionName() {
        return subMunitionName.isBlank() ? getShortName() : subMunitionName;
    }

    /**
     * Checks to ensure that the given ammo can be used with the given weapon type. Performs the following tests:<br>
     * {@code ammo} != null<br> {@link Mounted#getType()} instanceof {@link AmmoType}<br>
     * {@link Mounted#isAmmoUsable()}<br> {@link #isAmmoValid(AmmoType, WeaponType)}.
     *
     * @param ammo       The ammunition to be tested.
     * @param weaponType The weapon the ammo is to be used with.
     *
     * @return TRUE if the ammo and weapon are compatible.
     */
    public static boolean isAmmoValid(Mounted<?> ammo, WeaponType weaponType) {
        if (ammo == null) {
            return false;
        } else if (!(ammo.getType() instanceof AmmoType)) {
            return false;
        } else if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)) {
            return ammo.getUsableShotsLeft() > 0 && isAmmoValid((AmmoType) ammo.getType(), weaponType);
        } else {
            return ammo.isAmmoUsable() && isAmmoValid((AmmoType) ammo.getType(), weaponType);
        }
    }

    /**
     * Checks to ensure that the given ammunition type is compatible with the given weapon type. Performs the following
     * tests:<br> {@code ammoType} != null<br> {@link AmmoType#getAmmoType()} == {@link WeaponType#getAmmoType()}<br>
     * {@link AmmoType#getRackSize()} == {@link WeaponType#getRackSize()}
     *
     * @param ammoType   The type of ammo to be tested.
     * @param weaponType The type of weapon the ammo is to be used with.
     *
     * @return TRUE if the ammo type and weapon type are compatible.
     */
    public static boolean isAmmoValid(AmmoType ammoType, WeaponType weaponType) {
        if (ammoType == null) {
            return false;
        } else if (ammoType.getAmmoType() != weaponType.getAmmoType()) {
            return false;
        } else {
            return ammoType.getRackSize() == weaponType.getRackSize();
        }
    }

    /**
     * Whether the given weapon can switch to the given ammo type
     *
     * @param weapon    The weapon being considered
     * @param otherAmmo The other ammo type being considered
     *
     * @return true/false - null arguments or linked ammo bin for the weapon result in false
     */
    public static boolean canSwitchToAmmo(WeaponMounted weapon, AmmoType otherAmmo) {
        // no ammo switching if the weapon doesn't exist
        // or if it doesn't have an ammo bin
        // or the other ammo type doesn't exist
        if ((weapon == null) || (weapon.getLinkedAmmo() == null) || (otherAmmo == null)) {
            return false;
        }

        AmmoType currentAmmoType = weapon.getLinkedAmmo().getType();

        // Ammo of the same type and rack size should be allowed
        boolean ammoOfSameType = currentAmmoType.equalsAmmoTypeOnly(otherAmmo) &&
              (currentAmmoType.getRackSize() == otherAmmo.getRackSize());

        // MMLs can swap between different specific ammo types, so we have a special
        // case check here
        boolean mmlAmmoMatch = (currentAmmoType.getAmmoType() == AmmoTypeEnum.MML) &&
              (otherAmmo.getAmmoType() == AmmoTypeEnum.MML) &&
              (currentAmmoType.getRackSize() == otherAmmo.getRackSize());

        // AR10 ammo is explicitly excluded in equalsAmmoTypeOnly(), therefore check
        // here
        boolean ar10Match = (currentAmmoType.getAmmoType() == AmmoTypeEnum.AR10) &&
              (otherAmmo.getAmmoType() == AmmoTypeEnum.AR10);

        // LBXs can swap between cluster and slug ammo types
        boolean lbxAmmoMatch = (currentAmmoType.getAmmoType() == AmmoTypeEnum.AC_LBX) &&
              (otherAmmo.getAmmoType() == AmmoTypeEnum.AC_LBX) &&
              (currentAmmoType.getRackSize() == otherAmmo.getRackSize());

        boolean caselessLoaded = currentAmmoType.getMunitionType().contains(Munitions.M_CASELESS);
        boolean otherBinCaseless = otherAmmo.getMunitionType().contains(Munitions.M_CASELESS);
        boolean caselessMismatch = caselessLoaded != otherBinCaseless;

        boolean hasStaticFeed = weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_STATIC_FEED);
        boolean staticFeedMismatch = hasStaticFeed &&
              (currentAmmoType.getMunitionType() != otherAmmo.getMunitionType());

        return (ammoOfSameType || mmlAmmoMatch || lbxAmmoMatch || ar10Match) &&
              !caselessMismatch &&
              !staticFeedMismatch;
    }

    @Override
    public boolean isEligibleForBeingArmored() {
        // Coolant pods are implemented as ammo, but are not ammo bins for rules purposes
        return getAmmoType() == AmmoTypeEnum.COOLANT_POD;
    }

    @Override
    public Map<String, Object> getYamlData() {
        Map<String, Object> data = super.getYamlData();
        data.put("type", "ammo");
        if (kgPerShot > 0) {
            data.put("kgPerShot", this.getKgPerShot());
        }
        return data;
    }
}

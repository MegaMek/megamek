/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.MegaMek;
import megamek.common.BattleForceElement.WeaponLocation;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.bayweapons.ArtilleryBayWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.missiles.MissileWeapon;

import static megamek.common.ASUnitType.*;
import static megamek.common.BattleForceElement.*;
import static megamek.common.BattleForceSPA.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Static AlphaStrike Conversion class; contains all information for conversion except for some weapon specifics
 * handled in WeaponType/AmmoType/MiscType.
 *
 * Original conversion code by neoancient
 * Update by Simon (Juliez)
 */
public final class AlphaStrikeConverter {

    public static AlphaStrikeElement convert(Entity entity) {
        return convert(entity, false);
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot) {
        Objects.requireNonNull(entity);
        if (!canConvert(entity)) {
            MegaMek.getLogger().error("Cannot convert this type of Entity: " + entity.getShortName());
            return null; 
        }
        Entity undamagedEntity = getUndamagedEntity(entity);
        if (undamagedEntity == null) {
            MegaMek.getLogger().error("Could not obtain clean Entity for AlphaStrike conversion.");
            return null;
        }

        //System.out.println("----------Unit: "+entity.getShortName());
        var result = new AlphaStrikeElement();
        result.name = undamagedEntity.getShortName();
        result.setQuirks(undamagedEntity.getQuirks());
        result.model = undamagedEntity.getModel();
        result.chassis = undamagedEntity.getChassis();
        result.role = UnitRoleHandler.getRoleFor(undamagedEntity);
        result.asUnitType = ASUnitType.getUnitType(undamagedEntity);
        result.size = getSize(undamagedEntity);
        if (includePilot) {
            result.setSkill((entity.getCrew().getPiloting() + entity.getCrew().getGunnery()) / 2);
        }
        result.movement = getMovement(undamagedEntity);
        result.tmm = getTMM(result);
        result.armor = getArmor(undamagedEntity);
        result.structure = calcStructure(undamagedEntity);
        result.threshold = getThreshold(undamagedEntity, result);
        getSpecialUnitAbilities(undamagedEntity, result);
        initWeaponLocations(undamagedEntity, result);
        result.heat = new int[result.rangeBands];
        calcDamage(undamagedEntity, result);
        finalizeSpecials(result);
        result.points = getPointValue(undamagedEntity, result);
        adjustPVforSkill(result);
        return result;
    }
    
    /** 
     * Returns true if the given entity can be converted to AlphaStrike. This is only
     * false for entities of some special types such as TeleMissile or GunEmplacement. 
     */
    public static boolean canConvert(Entity entity) {
        return !((entity instanceof TeleMissile) || (entity instanceof FighterSquadron)
                || (entity instanceof EscapePods) || (entity instanceof EjectedCrew)
                || (entity instanceof ArmlessMech) || (entity instanceof GunEmplacement));
    }

    // AP weapon mounts have a set damage value.
    static final double AP_MOUNT_DAMAGE = 0.05;

    /** Mech Structure, AlphaStrike Companion, p.98 */
    private final static int[][] AS_MECH_STRUCTURE = new int[][] {
        { 1, 1, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 8, 8, 8, 8, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15 },
        { 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 7, 7, 8, 8, 9, 10, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20 },
        { 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11, 11, 12, 12 },
        { 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10 },
        { 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8 ,8, 8, 8, 8, 9 },
        { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 8 },
        { 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6 },
        { 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7 },
        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5 } 
    };

    private final static int[] TROOP_FACTOR = {
            0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
            13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };

    /** Retrieves a fresh (undamaged && unmodified) copy of the given entity. */
    private static Entity getUndamagedEntity(Entity entity) {
        try {
            MechSummary ms = MechSummaryCache.getInstance().getMech(entity.getShortNameRaw());
            return new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Size conversion, AlphaStrike Companion, p.92 */
    private static int getSize(Entity entity) {
        // if hasAlphaStrikeEquivalent(entity)
        if ((entity instanceof Tank) && (entity.isSupportVehicle())) {
            if (entity.getWeight() < 5) {
                return 1;
            }
            int mediumCeil = 0;
            int largeCeil = 0;
            int veryLargeCeil = 0;
            switch (entity.movementMode) {
                case TRACKED:
                    mediumCeil = 100;
                    largeCeil = 200;
                    break;
                case WHEELED:
                    mediumCeil = 80;
                    largeCeil = 160;
                    break;
                case HOVER:
                    mediumCeil = 50;
                    largeCeil = 100;
                    break;
                case NAVAL:
                case HYDROFOIL:
                case SUBMARINE:
                    mediumCeil = 300;
                    largeCeil = 6000;
                    veryLargeCeil = 30000;
                    break;
                case WIGE:
                    mediumCeil = 80;
                    largeCeil = 240;
                    break;
                case RAIL:
                    mediumCeil = 300;
                    largeCeil = 600;
                    break;
                case AIRSHIP:
                    mediumCeil = 300;
                    largeCeil = 600;
                    veryLargeCeil = 900;
                    break;
                case VTOL:
                    mediumCeil = 30;
                    largeCeil = 60;
                default:
                    break;
            }
            if (entity.getWeight() <= mediumCeil) {
                return 2;
            } else if (entity.getWeight() <= largeCeil) {
                return 3;
            } else if ((entity.getWeight() <= veryLargeCeil) || (veryLargeCeil == 0)) {
                return 4;
            } else {
                return 5;
            }

        } else if (entity instanceof Infantry) {
            return 1;

        } else if (entity instanceof Warship) {
            if (entity.getWeight() < 500000) {
                return 1;
            } else if (entity.getWeight() < 800000) {
                return 2;
            } else if (entity.getWeight() < 1200000) {
                return 3;
            } else {
                return 4;
            }

        } else if (entity instanceof Jumpship) {
            if (entity.getWeight() < 100000) {
                return 1;
            } else if (entity.getWeight() < 300000) {
                return 2;
            } else {
                return 3;
            }

        } else if (entity instanceof SmallCraft) {
            if (entity.getWeight() < 2500) {
                return 1;
            } else if (entity.getWeight() < 10000) {
                return 2;
            } else {
                return 3;
            }

        } else if (entity instanceof FixedWingSupport) {
            if (entity.getWeight() < 5) {
                return 1;
            } else if (entity.getWeight() <= 100) {
                return 2;
            } else {
                return 3;
            }

        } else if (entity instanceof Aero) {
            if (entity.getWeight() < 50) {
                return 1;
            } else if (entity.getWeight() < 75) {
                return 2;
            } else {
                return 3;
            }

        } else {
            if (entity.getWeight() < 40) {
                return 1;
            } else if (entity.getWeight() < 60) {
                return 2;
            } else if (entity.getWeight() < 80) {
                return 3;
            } else {
                return 4;
            }
        }
    }

    /** Move conversion, AlphaStrike Companion, p.92 */
    private static LinkedHashMap<String, Integer> getMovement(Entity entity) {
        if (entity instanceof Infantry) {
            return getMovementForInfantry(entity);
        } else if (entity instanceof Aero) {
            return getMovementForAero(entity);
        } else {
            return getMovementForNonInfantry(entity);
        }
    }

    private static LinkedHashMap<String, Integer> getMovementForAero(Entity entity) {
        var result = new LinkedHashMap<String, Integer>();
        if (entity instanceof Warship) {
            result.put("", entity.getWalkMP());
        } else if (entity instanceof Jumpship) {
            result.put("k", (int)(((Jumpship) entity).getStationKeepingThrust() * 10));
        } else {
            result.put(getMovementCode(entity), entity.getWalkMP());
        }
        return result;
    }

    private static LinkedHashMap<String, Integer> getMovementForNonInfantry(Entity entity) {
        var result = new LinkedHashMap<String, Integer>();
        double walkMP = entity.getOriginalWalkMP();
        //System.out.println("Original: "+walkMP);
        int jumpMove = entity.getJumpMP() * 2;
        if (hasSupercharger(entity) && hasMechMASC(entity)) {
            walkMP *= 1.5;
        } else if (hasSupercharger(entity) || hasMechMASC(entity) || hasJetBooster(entity)) {
            walkMP *= 1.25;
        }
        walkMP = Math.round(walkMP);

        if ((entity instanceof Mech) && ((Mech)entity).hasMPReducingHardenedArmor()) {
            walkMP--;
        }
        if (entity.hasModularArmor()) {
            walkMP--;
        }
        if (hasMPReducingShield(entity)) {
            walkMP--;
        }

        int baseMove = ((int)Math.round(walkMP * 2));
        if (baseMove % 2 == 1) {
            baseMove++;
        }

        if ((jumpMove == baseMove) && (jumpMove > 0) && getMovementCode(entity).equals("")) {
            result.put("j", baseMove);
        } else {
            result.put(getMovementCode(entity), baseMove);
            if (jumpMove > 0) {
                result.put("j", jumpMove);
            }
        }
        addUMUMovement(result, entity);
        return result;
    }

    private static LinkedHashMap<String, Integer> getMovementForInfantry(Entity entity) {
        var result = new LinkedHashMap<String, Integer>();
        int walkingMP = entity.getWalkMP(false, true, true);
        int jumpingMP = entity.getJumpMP();
        if (entity instanceof BattleArmor) {
            walkingMP = ((BattleArmor)entity).getWalkMP(true, true, true, true, true);
            jumpingMP = ((BattleArmor)entity).getJumpMP(true, true, true);
        }

        if ((walkingMP > jumpingMP) || (jumpingMP == 0)) {
            result.put(getMovementCode(entity), walkingMP * 2);
        } else {
            result.put("j", jumpingMP * 2);
        }

        addUMUMovement(result, entity);
        return result;
    }

    private static void addUMUMovement(Map<String, Integer> moves, Entity entity) {
        int umu = entity.getAllUMUCount();
        if (umu > 0) {
            moves.put("s", umu * 2);
        }
    }

    /** Returns true if the given entity has a Supercharger, regardless of its state (convert as if undamaged). */
    private static boolean hasSupercharger(Entity entity) {
        return entity.getMisc().stream()
                .map(m -> (MiscType) m.getType())
                .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && m.hasSubType(MiscType.S_SUPERCHARGER)));
    }

    /** Returns true if the given entity has a Jet Booster, regardless of its state (convert as if undamaged). */
    private static boolean hasJetBooster(Entity entity) {
        return entity.getMisc().stream()
                .map(m -> (MiscType) m.getType())
                .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && m.hasSubType(MiscType.S_JETBOOSTER)));
    }

    /** Returns true if the given entity is a Mech and has MASC, regardless of its state (convert as if undamaged). */
    private static boolean hasMechMASC(Entity entity) {
        return (entity instanceof Mech) 
                && entity.getMisc().stream()
                .map(m -> (MiscType) m.getType())
                .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && !m.hasSubType(MiscType.S_SUPERCHARGER)));
    }

    /** Returns true if the given entity has a movement reducing shield, regardless of its state (convert as if undamaged). */
    private static boolean hasMPReducingShield(Entity entity) {
        return entity.getMisc().stream()
                .map(m -> (MiscType) m.getType())
                .anyMatch(m -> (m.hasFlag(MiscType.F_CLUB) 
                        && (m.hasSubType(MiscType.S_SHIELD_LARGE)
                                || m.hasSubType(MiscType.S_SHIELD_MEDIUM))));
    }

    /** Returns the AlphaStrike movement type code letter such as "v" for VTOL. */
    public static String getMovementCode(Entity entity) {
        if (entity instanceof QuadVee) {
            if (((QuadVee)entity).getMotiveType() == QuadVee.MOTIVE_TRACK) {
                return "qt";
            } else {
                return "qw";
            }
        }
        switch (entity.getMovementMode()) {
            case NONE:
            case BIPED:
            case BIPED_SWIM:
            case QUAD:
            case QUAD_SWIM:
            case TRIPOD:
                return "";
            case TRACKED:
                return "t";
            case WHEELED:
                return "w";
            case HOVER:
                return "h";
            case VTOL:
                return "v";
            case NAVAL:
            case HYDROFOIL:
                return "n";
            case SUBMARINE:
            case INF_UMU:
                return "s";
            case INF_LEG:
                return "f";
            case INF_MOTORIZED:
                return "m";
            case INF_JUMP:
                return "j";
            case WIGE:
                return "g";
            case AERODYNE:
                return "a";
            case SPHEROID:
                return "p";
            default:
                return "ERROR";
        }
    }

    private static int getArmor(Entity entity) {
        if (entity instanceof Infantry) {
            double divisor = ((Infantry) entity).calcDamageDivisor();
            if (((Infantry) entity).isMechanized()) {
                divisor /= 2.0;
            }
            return (int) Math.round(divisor / 15.0d * ((Infantry) entity).getShootingStrength());
        }

        double armorPoints = 0;

        for (int loc = 0; loc < entity.locations(); loc++) {
            double armorMod = 1;
            switch (entity.getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMod = .5;
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                    armorMod = 1.2;
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMod = 2;
                    break;
            }
            
            if ((entity.getBARRating(0) < 9) && (entity.getArmorType(loc) != EquipmentType.T_ARMOR_COMMERCIAL)) {
                armorMod *= 0.1 * entity.getBARRating(0);
            }
            
            armorPoints += armorMod * entity.getArmor(loc);
            if (entity.hasRearArmor(loc)) {
                armorPoints += armorMod * entity.getArmor(loc, true);
            }
        }

        if (entity.hasModularArmor()) {
            // Modular armor is always "regular" armor
            armorPoints += 10 * entity.getEquipment().stream()
                    .filter(m -> m.getType() instanceof MiscType)
                    .filter(m -> m.getType().hasFlag(MiscType.F_MODULAR_ARMOR))
                    .count();
        }

        if (entity.isCapitalScale()) {
            return (int)Math.round(armorPoints * 0.33);
        }

        return (int) Math.round(armorPoints / 30);
    }
    
    private static int getThreshold(Entity entity, AlphaStrikeElement element) {
        if (entity instanceof Aero) {
            return roundUp((double)element.getFinalArmor() / 3 / (entity.isFighter() ? 1 : 4));
        } else {
            return -1;
        }
    }

    /** TMM determination, AlphaStrike Companion Errata v1.4, p.8 */
    private static int getTMM(AlphaStrikeElement element) {
        int base = element.getPrimaryMovementValue();
        if (element.asUnitType == CI || element.asUnitType == BA) {
            for (String moveMode : element.movement.keySet()) {
                if (!moveMode.equals("f")) {
                    base = element.movement.get(moveMode);
                    break;
                }
            }
        }
        return tmmForMovement(base);
    }
    
    /** Returns the TMM for the given movement value in inches, AlphaStrike Companion Errata v1.4, p.8 */
    private static int tmmForMovement(int movement) {
        if (movement > 34) {
            return 5;
        } else if (movement > 18) {
            return 4;
        } else if (movement > 12) {
            return 3;
        } else if (movement > 8) {
            return 2;
        } else if (movement > 4) {
            return 1;
        } else {
            return 0;
        }
    }

    /** TMM determination, AlphaStrike Companion Errata v1.4, p.8 */
    private static int calcStructure(Entity entity) {
        int battleForceStructure;

        if (entity instanceof Mech) {
            battleForceStructure = AS_MECH_STRUCTURE[getEngineIndex(entity)][getWeightIndex(entity)];
            if (entity.getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE) {
                battleForceStructure = (int) Math.ceil(battleForceStructure * .5);
            } else if (entity.getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED) {
                battleForceStructure *= 2;
            }
            return battleForceStructure;
        } else if (entity instanceof Warship) {
            return (int) Math.ceil(((Warship) entity).getSI() * 0.66);
        } else if (entity instanceof BattleArmor) {
            return 2;
        } else if ((entity instanceof Infantry) || (entity instanceof Jumpship)
                || (entity instanceof Protomech)) {
            return 1;
        } else if (entity instanceof Tank) {
            int divisor = 10;
            if (entity.isSupportVehicle()) {
                switch (entity.movementMode) {
                    case NAVAL:
                    case HYDROFOIL:
                    case SUBMARINE:
                        if (entity.getWeight() >= 30000.5) {
                            divisor = 35;
                        } else if (entity.getWeight() >= 12000.5) {
                            divisor = 30;
                        } else if (entity.getWeight() >= 6000.5) {
                            divisor = 25;
                        } else if (entity.getWeight() >= 500.5) {
                            divisor = 20;
                        } else if (entity.getWeight() >= 300.5) {
                            divisor = 15;
                        }
                        break;    
                    default:
                }
            }
            double struct = 0;
            for (int i = 0; i < entity.getLocationNames().length; i++) {
                struct += entity.getInternal(i);
            }
            return (int) Math.ceil(struct / divisor);
        } else if (entity instanceof Aero) {
            return (int) Math.ceil(0.5 * ((Aero) entity).getSI());
        }

        // Error: should not arrive here
        return -1;
    }

    private static int getWeightIndex(Entity entity) {
        return ((int) entity.getWeight() / 5) - 2;
    }

    private static int getEngineIndex(Entity entity) {
        if (entity.getEngine().isClan()) {
            if (entity.getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                        return 4;
                    case Engine.XXL_ENGINE:
                        return 7;
                }
            } else {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                        return 3;
                    case Engine.XXL_ENGINE:
                        return 5;
                    default:
                        return 0;
                }
            }
        } else {
            if (entity.getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                    case Engine.LIGHT_ENGINE:
                        return 4;
                    case Engine.XXL_ENGINE:
                        return 8;
                    default:
                        return 2;
                }
            } else {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                        return 4;
                    case Engine.COMPACT_ENGINE:
                        return 1;
                    case Engine.LIGHT_ENGINE:
                        return 3;
                    case Engine.XXL_ENGINE:
                        return 6;
                    default:
                        return 0;
                }
            }
        }
        MegaMek.getLogger().error("Mech Engine type cannot be converted!");
        return -1;
    }

    private static void calcDamage(Entity entity, AlphaStrikeElement result) {
        double[] baseDamage = new double[result.rangeBands];
        boolean hasTC = entity.hasTargComp();
        int[] ranges;
        double pointDefense = 0;
        int bombRacks = 0;
        double baseIFDamage = 0;
        //TODO: multiple turrets
        var turretsArcs = new HashMap<String,ASArcSummary>();
        var arcsTurrets = new ArrayList<ASArcSummary>();
        if (result.usesArcs()) {
            turretsArcs.put("NOSE", ASArcSummary.createArcSummary());
            turretsArcs.put("LEFT", ASArcSummary.createArcSummary());
            turretsArcs.put("RIGHT", ASArcSummary.createArcSummary());
            turretsArcs.put("REAR", ASArcSummary.createArcSummary());
            arcsTurrets.add(ASArcSummary.createArcSummary());
            arcsTurrets.add(ASArcSummary.createArcSummary());
            arcsTurrets.add(ASArcSummary.createArcSummary());
            arcsTurrets.add(ASArcSummary.createArcSummary());
        } else {
            for (int loc = 0; loc < result.weaponLocations.length; loc++) {
                if (result.locationNames[loc].startsWith("TUR")) {
                    turretsArcs.put(result.locationNames[loc], ASArcSummary.createTurretSummary());
                    arcsTurrets.add(ASArcSummary.createTurretSummary());
                }
            }
        }
        //Track weapons we've already calculated ammunition for
        HashMap<String,Boolean> ammoForWeapon = new HashMap<>();

        ArrayList<Mounted> weaponsList = entity.getWeaponList();

        for (int pos = 0; pos < weaponsList.size(); pos++) {
            Arrays.fill(baseDamage, 0);
            double damageModifier = 1;
            Mounted mount = weaponsList.get(pos);
            if ((mount == null)
                    || (entity.getEntityType() == Entity.ETYPE_INFANTRY
                        && mount.getLocation() == Infantry.LOC_INFANTRY)) {
                continue;
            }

            WeaponType weapon = (WeaponType) mount.getType();
            //System.out.println(weapon.getName());
            
            ranges = weapon.isCapital() ? CAPITAL_RANGES : STANDARD_RANGES;

            if (weapon.getAmmoType() == AmmoType.T_INARC) {
                result.addSPA(INARC, 1);
                findArcTurret(result, entity, mount, turretsArcs).forEach(a -> a.addSPA(INARC, 1));
                continue;
            } else if (weapon.getAmmoType() == AmmoType.T_NARC) {
                if (weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
                    result.addSPA(CNARC, 1);
                } else {
                    result.addSPA(SNARC, 1);
                    findArcTurret(result, entity, mount, turretsArcs).forEach(a -> a.addSPA(SNARC, 1));
                }
                continue;
            }

            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
                result.addSPA(SCR, 1);
                continue;
            }

            //TODO: really not Aero? Where is AMS pointdefense handled else?
            if (weapon.hasFlag(WeaponType.F_AMS)) {
                if (locationMultiplier(entity, 0, mount) > 0) {
                    if (entity instanceof Aero) {
                        pointDefense += 0.3;
                    } else {
                        result.addSPA(AMS);
                    }
                }
                for (ASArcSummary arcTurret : findArcTurret(result, entity, mount, turretsArcs)) {
                    if (entity instanceof Aero) {
                        arcTurret.addSPA(PNT, 0.3);
                    } else {
                        arcTurret.addSPA(AMS);
                    }
                }
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_TAG)) {
                if (weapon.hasFlag(WeaponType.F_C3MBS)) {
                    result.addSPA(C3BSM, 1);
                    result.addSPA(MHQ, 6);
                } else if (weapon.hasFlag(WeaponType.F_C3M)) {
                    result.addSPA(C3M, 1);
                    result.addSPA(MHQ, 5);
                }
                if (weapon.getShortRange() < 5) {
                    result.addSPA(LTAG);
                    findArcTurret(result, entity, mount, turretsArcs).forEach(a -> a.addSPA(LTAG));
                } else {
                    result.addSPA(TAG);
                    findArcTurret(result, entity, mount, turretsArcs).forEach(a -> a.addSPA(TAG));
                }
                continue;
            }
            
            if (weapon.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                if (!((entity instanceof Aero) && isArtilleryCannon(weapon))) {
                    result.addSPA(getArtilleryType(weapon), 1);
                    findArcTurret(result, entity, mount, turretsArcs).forEach(a -> a.addSPA(getArtilleryType(weapon), 1));
                    continue;
                }
            }
            if (weapon instanceof ArtilleryBayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = entity.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        result.addSPA(getArtilleryType((WeaponType)m.getType()), 1);
                        findArcTurret(result, entity, mount, turretsArcs)
                                .forEach(a -> a.addSPA(getArtilleryType((WeaponType)m.getType()), 1));
                    }
                }
            }
            
            if (weapon.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
                bombRacks++;
                continue;
            }
            
            if (weapon.getAmmoType() == AmmoType.T_TASER) {
                if (entity instanceof BattleArmor) {
                    result.addSPA(BTAS, 1);
                } else {
                    result.addSPA(MTAS, 1);
                }
                continue;
            }
            
            if (weapon.hasFlag(WeaponType.F_TSEMP)) {
                BattleForceSPA spa = weapon.hasFlag(WeaponType.F_ONESHOT) ? TSEMPO : TSEMP;
                result.addSPA(spa, 1);
                findArcTurret(result, entity, mount, turretsArcs).forEach(a -> a.addSPA(spa, 1));
                continue;
            }
            
            if (weapon instanceof InfantryAttack) {
                result.addSPA(AM);
                continue;
            }

            // Check ammo weapons first since they had a hidden modifier
            if ((weapon.getAmmoType() != AmmoType.T_NA)
                    && !weapon.hasFlag(WeaponType.F_ONESHOT)
                    && (!(entity instanceof BattleArmor) || weapon instanceof MissileWeapon)) {
                if (!ammoForWeapon.containsKey(weapon.getName())) {
                    int weaponsForAmmo = 1;
                    for (int nextPos = 0; nextPos < weaponsList.size(); nextPos++) {
                        if (nextPos == pos) {
                            continue;
                        }
                        
                        Mounted nextWeapon = weaponsList.get(nextPos);
    
                        if (nextWeapon == null) {
                            continue;
                        }
    
                        if (nextWeapon.getType().equals(weapon)) {
                            weaponsForAmmo++;
                        }
    
                    }
                    int ammoCount = 0;
                    // Check if they have enough ammo for all the guns to last at least 10 rounds
                    // RACs and UACs require 60 / 20 shots per weapon
                    int divisor = 1;
                    for (Mounted ammo : entity.getAmmo()) {
    
                        AmmoType at = (AmmoType) ammo.getType();
                        if ((at.getAmmoType() == weapon.getAmmoType())
                                && (at.getRackSize() == weapon.getRackSize())) {
                            ammoCount += at.getShots();
                            if (at.getAmmoType() == AmmoType.T_AC_ROTARY) {
                                divisor = 6;
                            } else if (at.getAmmoType() == AmmoType.T_AC_ULTRA
                                    || at.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                                divisor = 2;
                            }
                        }
                    }
    
                    ammoForWeapon.put(weapon.getName(), (ammoCount / weaponsForAmmo / divisor) >= 10);
                }
                if (!ammoForWeapon.get(weapon.getName())) {
                    damageModifier *= 0.75;
                }
            }

            if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                damageModifier *= .1;
            }
            
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = entity.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        for (int r = 0; r < result.rangeBands; r++) {
                            baseDamage[r] += ((WeaponType)m.getType()).getBattleForceDamage(ranges[r], m.getLinkedBy());
                            result.heat[r] += ((WeaponType)m.getType()).getBattleForceHeatDamage(ranges[r]);
                        }
                    }
                }
            } else {
                for (int r = 0; r < result.rangeBands; r++) {
                    if (entity instanceof BattleArmor) {
                        baseDamage[r] = baseDamage[r] = getBattleArmorDamage(weapon, ranges[r], ((BattleArmor)entity),
                                mount.isAPMMounted());
                        baseIFDamage = baseDamage[RANGE_BAND_LONG];
                    } else {
                        baseDamage[r] = weapon.getBattleForceDamage(ranges[r], mount.getLinkedBy());
                        // Disregard any Artemis bonus for IF:
                        baseIFDamage = baseDamage[RANGE_BAND_LONG];
                        if (MountedHelper.isAnyArtemis(mount.getLinkedBy()) && isSRMorLRMSpecial(weapon)) {
                            baseIFDamage = weapon.getBattleForceDamage(ranges[r], null);
                        }
                    }
                    for (int loc = 0; loc < result.weaponLocations.length; loc++) {
                        Integer ht = result.weaponLocations[loc].heatDamage.get(r);
                        if (ht == null) {
                            ht = 0;
                        }
                        ht += (int)(locationMultiplier(entity, loc, mount) * weapon.getBattleForceHeatDamage(ranges[r]));
                        result.weaponLocations[loc].heatDamage.set(r, ht);
                    }
                }
            }

            // Targetting Computer
            if (hasTC && weapon.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
                damageModifier *= 1.10;
            }
            
            // Actuator Enhancement System
            if (entity.hasWorkingMisc(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM, -1, mount.getLocation())
                    && ((mount.getLocation() == Mech.LOC_LARM) || (mount.getLocation() == Mech.LOC_RARM))) { 
                damageModifier *= 1.05;
            }
            
            for (int loc = 0; loc < result.weaponLocations.length; loc++) {
                double locMultiplier = locationMultiplier(entity, loc, mount);
                if (locMultiplier == 0) {
                    continue;
                }
                for (int r = 0; r < result.rangeBands; r++) {
                    double dam = baseDamage[r] * damageModifier * locMultiplier;
//                    System.out.println(result.locationNames[loc] + ": " + mount.getName() + " " + "SMLE".substring(r, r+1) + ": " + dam + " Mul: " + damageModifier);
                    if (!weapon.isCapital() && weapon.getBattleForceClass() != WeaponType.BFCLASS_TORP) {
                        // Standard Damage
                        result.weaponLocations[loc].addDamage(r, dam);
                    }
                    // Special Damage (SRM/LRM blocked by Artemis)
                    if (!(MountedHelper.isAnyArtemis(mount.getLinkedBy()) && isSRMorLRMSpecial(weapon))) {
                        if (weapon.getBattleForceClass() == WeaponType.BFCLASS_MML) {
                            if (r == RANGE_BAND_SHORT) {
                                result.weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam);
                            } else if (r == RANGE_BAND_MEDIUM) {
                                result.weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam / 2.0);
                                result.weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam / 2.0);
                            } else {
                                result.weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam);                            
                            }
                        } else {
                            result.weaponLocations[loc].addDamage(weapon.getBattleForceClass(), r, dam);
                        }
                    }
                    if (r == RANGE_BAND_LONG && !(entity instanceof Aero) && weapon.hasAlphaStrikeIndirectFire()) {
                        result.weaponLocations[loc].addIF(baseIFDamage * damageModifier * locMultiplier);
                    }
                }
            }
            if (entity instanceof Aero && weapon.isAlphaStrikePointDefense()) {
                pointDefense += baseDamage[RANGE_BAND_SHORT] * damageModifier * locationMultiplier(entity, 0, mount);
            }
        }
        
        if (entity.getEntityType() == Entity.ETYPE_INFANTRY) {
            int baseRange = 0;
            if (((Infantry)entity).getSecondaryWeapon() != null && ((Infantry)entity).getSecondaryN() >= 2) {
                baseRange = ((Infantry)entity).getSecondaryWeapon().getInfantryRange();
            } else if (((Infantry)entity).getPrimaryWeapon() != null){
                baseRange = ((Infantry)entity).getPrimaryWeapon().getInfantryRange();
            }
            int range = baseRange * 3;
            for (int r = 0; r < STANDARD_RANGES.length; r++) {
                if (range >= STANDARD_RANGES[r]) {
                    result.weaponLocations[0].addDamage(r, getConvInfantryStandardDamage(
                            (Infantry)entity));
                } else {
                    break;
                }
            }
        }
        
        if (entity instanceof BattleArmor) {
            int vibroClaws = entity.countWorkingMisc(MiscType.F_VIBROCLAW);
            if (vibroClaws > 0) {
                result.weaponLocations[0].addDamage(0, vibroClaws);
                result.weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, vibroClaws);
            }
            if (bombRacks > 0) {
                result.addSPA(BOMB, (bombRacks * ((BattleArmor)entity).getShootingStrength()) / 5);
            }
        }
        
        if (entity instanceof Aero && roundUp(pointDefense) > 0) {
            result.addSPA(PNT, roundUp(pointDefense));
        }
        
        adjustForHeat(entity, result);
        
        // Big Aero (using Arcs)
        if (result.usesArcs()) {
//            arc
        }

        // Standard HT
        int htS = resultingHTValue(result.weaponLocations[0].heatDamage.get(0));
        int htM = resultingHTValue(result.weaponLocations[0].heatDamage.get(1));
        int htL = resultingHTValue(result.weaponLocations[0].heatDamage.get(2));
        if (htS + htM + htL > 0) {
            result.addSPA(HT, ASDamageVector.createNormRndDmg(htS, htM, htL));
        }

        // IF
        if (result.weaponLocations[0].getIF() > 0) {
            result.addSPA(IF, ASDamageVector.createNormRndDmg(result.weaponLocations[0].getIF()));
        }

        // LRM ... IATM specials
        for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
            // Aero do not get LRM/SRM/AC/IATM
            if ((entity instanceof Aero) && i != WeaponType.BFCLASS_FLAK) {
                continue;
            }
            BattleForceSPA spa = BattleForceSPA.getSPAForDmgClass(i);
            List<Double> dmg = result.weaponLocations[0].specialDamage.get(i);
            if ((dmg != null) && qualifiesForSpecial(dmg, spa)) {
                if (spa == SRM) {
                    result.addSPA(SRM, ASDamageVector.createNormRndDmgNoMin(dmg, 2));
                } else if ((spa == LRM) || (spa == AC) || (spa == IATM)) {
                    result.addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, result.rangeBands));
                } else if ((spa == FLK) || (spa == TOR)) {
                    result.addSPA(spa, ASDamageVector.createNormRndDmg(dmg, result.rangeBands));
                } else if (spa == REL) {
                    result.addSPA(spa);
                }
            }
        }

        // REL
        if (result.weaponLocations[0].hasDamageClass(WeaponType.BFCLASS_REL)) {
            result.addSPA(REL);
        }
        
        // Standard damage

//        result.weaponLocations[0].standardDamage.forEach(System.out::println);
        result.standardDamage = ASDamageVector.createUpRndDmg(
                result.weaponLocations[0].standardDamage, result.rangeBands);

        // REAR damage
        int rearLoc = getRearLocation(result);
        if (rearLoc != -1 && result.weaponLocations[rearLoc].hasDamage()) {
            // Double check; Moray Heavy Attack Sub has only LTR in the rear leading to REAR-/-/- otherwise
            ASDamageVector rearDmg = ASDamageVector.createNormRndDmg(
                    result.weaponLocations[rearLoc].standardDamage, result.rangeBands);
            if (rearDmg.hasDamage()) {
                result.addSPA(REAR, rearDmg);
            }

        }

        // Turrets have SRM, LRM, AC, FLK, HT, TSEMP, TOR, AMS, TAG, ARTx, xNARC, IATM, REL
        //TODO: 2 Turrets?
        for (int loc = 0; loc < result.weaponLocations.length; loc++) {
            if (turretsArcs.containsKey(result.locationNames[loc])) {
                ASArcSummary arcTurret = turretsArcs.get(result.locationNames[loc]);
                arcTurret.setStdDamage(ASDamageVector.createUpRndDmgMinus(result.weaponLocations[loc].standardDamage,
                        result.rangeBands));
                for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
                    BattleForceSPA spa = BattleForceSPA.getSPAForDmgClass(i);
                    List<Double> dmg = result.weaponLocations[loc].specialDamage.get(i);
                    if ((dmg != null) && qualifiesForSpecial(dmg, spa)) {
                        if (spa == SRM) {
                            arcTurret.addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, 2));
                        } else if ((spa == LRM) || (spa == TOR) || (spa == AC) || (spa == IATM)) {
                            arcTurret.addSPA(spa, ASDamageVector.createNormRndDmgNoMin(dmg, result.rangeBands));
                        } else if (spa == FLK) {
                            arcTurret.addSPA(spa, ASDamageVector.createNormRndDmg(dmg, result.rangeBands));
                        } else if (spa == REL) {
                            arcTurret.addSPA(spa);
                        }
                    }
                }
                if (result.weaponLocations[loc].getIF() > 0) {
                    arcTurret.addSPA(IF, ASDamageVector.createNormRndDmg(result.weaponLocations[loc].getIF()));
                }
                htS = resultingHTValue(result.weaponLocations[loc].heatDamage.get(0));
                htM = resultingHTValue(result.weaponLocations[loc].heatDamage.get(1));
                htL = resultingHTValue(result.weaponLocations[loc].heatDamage.get(2));
                if (htS + htM + htL > 0) {
                    arcTurret.addSPA(HT, ASDamageVector.createNormRndDmg(htS, htM, htL));
                }
                if (!arcTurret.isEmpty()) {
                    //TODO: If this is an arc??
                    result.addSPA(TUR, arcTurret);
                }
            }
        }

    }


    private static void finalizeSpecials(AlphaStrikeElement element) {
        // For MHQ, the values may contain decimals, but the the final MHQ value is rounded down to an int.
        if (element.getSPA(MHQ) instanceof Double) {
            double mhqValue = (double) element.getSPA(MHQ);
            element.replaceSPA(MHQ, (int) mhqValue);
        }
        
        // Cannot have both CASEII and CASE
        if (element.hasSPA(CASEII)) {
            element.removeSPA(CASE);
        }
        
        // Implicit rule: AECM overrides ECM
        if (element.hasSPA(AECM)) {
            element.removeSPA(ECM);
        }

        // Some SUAs are accompanied by RCN
        if (element.hasAnySPAOf(PRB, LPRB, NOVA, BH, WAT)) {
            element.addSPA(RCN);
        }

        // CT value may be decimal but replace it with an integer value if it is integer
        if (element.hasSPA(CT) && (element.getSPA(CT) instanceof Double)) {
            double ctValue = (double) element.getSPA(CT);
            if ((int) ctValue == ctValue) {
                element.replaceSPA(CT, (int) ctValue);
            }
        }
        if (element.hasSPA(IT) && (element.getSPA(IT) instanceof Double)) {
            double ctValue = (double) element.getSPA(IT);
            if ((int) ctValue == ctValue) {
                element.replaceSPA(IT, (int) ctValue);
            }
        }
    }

    /**
     * Returns true when the heat-adjusted and tenth-rounded damage values in the List allow the given spa.
     * Only use for the damage specials LRM, SRM, TOR, IATM, AC, FLK
     */
    private static boolean qualifiesForSpecial(List<Double> dmg, BattleForceSPA spa) {
        if (((spa == FLK) || (spa == TOR)) && dmg.stream().mapToDouble(Double::doubleValue).sum() > 0) {
            return true;
        } else {
            return (dmg.size() > 1) && (dmg.get(1) >= 1);
        }
    }

    private static int resultingHTValue(int heatSum) {
        if (heatSum > 10) {
            return 2;
        } else if (heatSum > 4) {
            return 1;
        } else {
            return 0;
        }
    }
    
    private static double locationMultiplier(Entity en, int loc, Mounted mount) {
        if (en.getBattleForceLocationName(loc).startsWith("TUR") && (en instanceof Mech) && mount.isMechTurretMounted()) {
            return 1;
        } else if (en.getBattleForceLocationName(loc).startsWith("TUR") && (en instanceof Tank)
                && (mount.isPintleTurretMounted() || mount.isSponsonTurretMounted())) {
            return 1;
        } else {
            return en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
        }
    }

    private static List<ASArcSummary> findArcTurret(AlphaStrikeElement element, Entity entity,
                                                    Mounted mount, Map<String, ASArcSummary> arcsTurrets) {
        var result = new ArrayList<ASArcSummary>();
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            if (element.locationNames[loc].equals("TUR") && (locationMultiplier(entity, loc, mount) != 0)) {
                result.add(arcsTurrets.get(element.locationNames[loc]));
            }
        }
        return result;
    }

    /** Returns true when the given Mounted blocks ENE. */
    private static boolean isExplosive(Mounted m) {
        // LAM Bomb Bays are explosive
        if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_BOMB_BAY)) {
            return true;
        }
        // Oneshot weapons internally have normal ammo allocated to them which must 
        // be disqualified as explosive; such ammo has no location
        return m.getType().isExplosive(null) && (m.getExplosionDamage() > 0)
                && (m.getLocation() != Entity.LOC_NONE);
    }
    
    private static void getSpecialUnitAbilities(Entity entity, AlphaStrikeElement element) {
        boolean hasExplosiveComponent = false;
        for (Mounted m : entity.getEquipment()) {
            
            if (isExplosive(m)) {
                hasExplosiveComponent = true;
            }
            
            if (!(m.getType() instanceof MiscType)) {
                continue;
            }

            if (m.getType().getInternalName().equals(Sensor.BAP)
                    || m.getType().getInternalName().equals(Sensor.BAPP)
                    || m.getType().getInternalName().equals(Sensor.CLAN_AP)) {
                element.addSPA(PRB);
            } else if (m.getType().getInternalName().equals(Sensor.LIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.ISBALIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.EW_EQUIPMENT)) {
                element.addSPA(LPRB);
            } else if (m.getType().getInternalName().equals(Sensor.BLOODHOUND)) {
                element.addSPA(BH);
            } else if (m.getType().getInternalName().equals(Sensor.WATCHDOG)) {
                element.addSPA(LPRB);
                element.addSPA(ECM);
                element.addSPA(WAT);
            } else if (m.getType().getInternalName().equals(Sensor.NOVA)) {
                element.addSPA(NOVA);
                element.addSPA(PRB);
                element.addSPA(ECM);
                element.addSPA(MHQ, 1.5);
            } else if (m.getType().hasFlag(MiscType.F_ECM)) {
                if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    element.addSPA(AECM);
                } else if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    element.addSPA(LECM);
                } else {
                    element.addSPA(ECM);
                }
            } else if (m.getType().hasFlag(MiscType.F_BOOBY_TRAP) && !element.isAnyTypeOf(PM, CI, BA)) {
                element.addSPA(BT);
            } else if (m.getType().hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)) {
                element.addSPA(BRID);
            } else if (m.getType().hasFlag(MiscType.F_C3S)) {
                element.addSPA(C3S);
                element.addSPA(MHQ, 1);
                if (m.getType().hasFlag(MiscType.F_C3EM)) {
                    element.addSPA(C3EM, 1);
                }
            } else if (m.getType().hasFlag(MiscType.F_C3SBS)) {
                element.addSPA(C3BSS, 1);
                element.addSPA(MHQ, 2);
            } else if (m.getType().hasFlag(MiscType.F_C3I)) {
                if ((entity.getEntityType() & Entity.ETYPE_AERO) == Entity.ETYPE_AERO) {
                    element.addSPA(NC3);
                } else {
                    element.addSPA(C3I);
                    if (m.getType().hasFlag(MiscType.F_BA_EQUIPMENT)) {
                        element.addSPA(MHQ, 2);
                    } else {
                        element.addSPA(MHQ, 2.5);
                    }
                }
            } else if (m.getType().hasFlag(MiscType.F_CASE) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.addSPA(CASE);
            } else if (m.getType().hasFlag(MiscType.F_CASEP) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.addSPA(CASEP);
            } else if (m.getType().hasFlag(MiscType.F_CASEII) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.addSPA(CASEII);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                element.addSPA(DRO);
            } else if (m.getType().hasFlag(MiscType.F_SRCS)
                    || m.getType().hasFlag(MiscType.F_SASRCS)
                    || m.getType().hasFlag(MiscType.F_CASPAR)
                    || m.getType().hasFlag(MiscType.F_CASPARII)) {
                element.addSPA(RBT);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                element.addSPA(DCC, (int) m.getSize());
            } else if (m.getType().hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                element.addSPA(DCC, 1);
            } else if (m.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                element.addSPA(ES);
            } else if (m.getType().hasFlag(MiscType.F_BULLDOZER)) {
                element.addSPA(ENG);
            } else if (m.getType().hasFlag(MiscType.F_HAND_WEAPON)) {
                element.addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_TALON)) {
                element.addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_CLUB)) {
                element.addSPA(MEL);
                if ((m.getType().getSubType() &
                        (MiscType.S_BACKHOE | MiscType.S_PILE_DRIVER
                                | MiscType.S_MINING_DRILL | MiscType.S_ROCK_CUTTER
                                | MiscType.S_WRECKING_BALL)) != 0) {
                    element.addSPA(ENG);
                } else if ((m.getType().getSubType() &
                        (MiscType.S_DUAL_SAW | MiscType.S_CHAINSAW
                                | MiscType.S_BUZZSAW | MiscType.S_RETRACTABLE_BLADE)) != 0) {
                    element.addSPA(SAW);
                }
            } else if (m.getType().hasFlag(MiscType.F_SPIKES)) {
                element.addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                element.addSPA(FR);
            } else if (m.getType().hasFlag(MiscType.F_MOBILE_HPG)) {
                element.addSPA(HPG);
            } else if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                element.addSPA(MHQ, (int) m.getTonnage());
                if (m.getTonnage() >= entity.getWeight() / 20.0) {
                    element.addSPA(RCN);
                }
            } else if (m.getType().hasFlag(MiscType.F_SENSOR_DISPENSER)) {
                element.addSPA(RSD, 1);
                element.addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_LOOKDOWN_RADAR)
                    || m.getType().hasFlag(MiscType.F_RECON_CAMERA)
                    || m.getType().hasFlag(MiscType.F_HIRES_IMAGER)
                    || m.getType().hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || m.getType().hasFlag(MiscType.F_INFRARED_IMAGER)) {
                element.addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)) {
                element.addSPA(SRCH);
            } else if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                element.addSPA(RHS);
            } else if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                element.addSPA(ECS);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)) {
                element.addSPA(DJ);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)) {
                element.addSPA(HJ);
            } else if (m.getType().hasFlag(MiscType.F_CARGO)) {
                //System.out.println("Tonnage: "+ m.getTonnage());
                element.addSPA(CT, m.getTonnage());
            }
            
            if (m.getType().hasFlag(MiscType.F_SPACE_MINE_DISPENSER) && (entity instanceof Aero)) {
                element.addSPA(MDS, 2);
            }

            if (entity instanceof Mech) {
                if (m.getType().hasFlag(MiscType.F_HARJEL)) {
                    element.addSPA(BHJ);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                    element.addSPA(BHJ2);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                    element.addSPA(BHJ3);
                } else if (((MiscType)m.getType()).isShield()) {
                    element.addSPA(SHLD);
                } else if (m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                    element.addSPA(ITSM);
                } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                    element.addSPA(TSM);
                } else if (m.getType().hasFlag(MiscType.F_VOIDSIG)) {
                    element.addSPA(MAS);
                } else if (((Mech) entity).isIndustrial() && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    element.addSPA(SEAL);
                    if (entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE) {
                        element.addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_NULLSIG)
                        || m.getType().hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                    element.addSPA(STL);
                    element.addSPA(ECM);
                } else if (m.getType().hasFlag(MiscType.F_UMU)) {
                    element.addSPA(UMU);
                } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_INTERFACE) {
                    element.addSPA(DN);
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)) {
                    element.addSPA(ECM);
                }
            }

            if (entity instanceof Protomech) {
                if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    if (entity.getWeight() < 10) {
                        element.addSPA(MCS);
                    } else {
                        element.addSPA(UCS);                    
                    }
                }
            }
            
            if (entity instanceof Tank) {
                if (m.getType().hasFlag(MiscType.F_ADVANCED_FIRECONTROL)) {
                    element.addSPA(AFC);
                } else if (m.getType().hasFlag(MiscType.F_BASIC_FIRECONTROL)) {
                    element.addSPA(BFC);
                } else if (m.getType().hasFlag(MiscType.F_AMPHIBIOUS) || m.getType().hasFlag(MiscType.F_FULLY_AMPHIBIOUS)
                        || m.getType().hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)) {
                    element.addSPA(AMP);
                } else if (m.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                    element.addSPA(ARS);
                } else if (m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    element.addSPA(SEAL);
                    if (entity.hasEngine() && entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE
                            && entity.getEngine().getEngineType() != Engine.STEAM) {
                        element.addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) { 
                    element.addSPA(MDS, 2);
                } else if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                    element.addSPA(MSW);
                } else if (m.getType().hasFlag(MiscType.F_MASH)) {
                    element.addSPA(MASH, (int) m.getSize());
                } else if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                    element.addSPA(MFB);
                } else if (m.getType().hasFlag(MiscType.F_OFF_ROAD)) {
                    element.addSPA(ORO);
                } else if (m.getType().hasFlag(MiscType.F_DUNE_BUGGY)) {
                    element.addSPA(DUN);
                } else if (m.getType().hasFlag(MiscType.F_TRACTOR_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_TRAILER_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_HITCH)) {
                    element.addSPA(HTC);
                } else if (m.getType().hasFlag(MiscType.F_COMMAND_CONSOLE)) {
                    element.addSPA(MHQ, 1);
                }
            }

            if (entity instanceof BattleArmor) {
                if (m.getType().hasFlag(MiscType.F_VISUAL_CAMO)
                        && !m.getType().getName().equals(BattleArmor.MIMETIC_ARMOR)) {
                    element.addSPA(LMAS);
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    element.addSPA(MDS, 1);
                } else if (m.getType().hasFlag(MiscType.F_TOOLS)
                        && (m.getType().getSubType() & MiscType.S_MINESWEEPER) == MiscType.S_MINESWEEPER) {
                    element.addSPA(BattleForceSPA.MSW);
                } else if (m.getType().hasFlag(MiscType.F_SPACE_ADAPTATION)) {
                    element.addSPA(BattleForceSPA.SOA);
                } else if (m.getType().hasFlag(MiscType.F_PARAFOIL)) {
                    element.addSPA(BattleForceSPA.PARA);
                } else if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    element.addSPA(BattleForceSPA.XMEC);
                }
            }
        }

        // TODO: why doesnt this work?
        if (element.hasQuirk(OptionsConstants.QUIRK_POS_TRAILER_HITCH)) {
            element.addSPA(HTC);
        }

        if (entity.isOmni() && ((entity instanceof Mech) || (entity instanceof Tank))) {
            element.addSPA(OMNI);
        }
        
        if (entity.getBARRating(0) >= 1 && entity.getBARRating(0) <= 9) {
            element.addSPA(BAR);
        }
        
        //TODO: Variable Range targeting is not implemented
        if (!entity.hasPatchworkArmor()) {
            switch (entity.getArmorType(0)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    element.addSPA(BAR);
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_HARDENED:
                    element.addSPA(CR);
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
                case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                case EquipmentType.T_ARMOR_BA_STEALTH:
                case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
                case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
                case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                    element.addSPA(STL);
                    break;
                case EquipmentType.T_ARMOR_BA_MIMETIC:
                    element.addSPA(MAS);
                    break;
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    element.addSPA(ABA);
                    break;
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    element.addSPA(BRA);
                    break;
                case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    element.addSPA(FR);
                    break;
                case EquipmentType.T_ARMOR_IMPACT_RESISTANT:
                    element.addSPA(IRA);
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                    element.addSPA(RCA);
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                    element.addSPA(RFA);
                    break;
            }
        }

        if (!element.isInfantry()) {
            if (!hasExplosiveComponent) {
                element.addSPA(ENE);
            } else if (entity.isClan() && element.isAnyTypeOf(BM, IM, SV, CV, MS)) {
                element.addSPA(CASE);
            }
        }
        
        if (entity.getAmmo().stream().map(m -> (AmmoType)m.getType())
                .anyMatch(at -> at.hasFlag(AmmoType.F_TELE_MISSILE))) {
            element.addSPA(TELE);
        }

        if (entity.hasEngine()) {
            if (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                element.addSPA(EE);
            } else if (entity.getEngine().getEngineType() == Engine.FUEL_CELL) {
                element.addSPA(FC);
            }
        }
        
        for (Transporter t : entity.getTransports()) {
//            //System.out.println("Transport " + t.getClass());
            if (t instanceof ASFBay) {
                element.addSPA(AT, (int)((ASFBay)t).getCapacity());
                element.addSPA(ATxD, ((ASFBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof CargoBay) {
                element.addSPA(CT, ((CargoBay)t).getCapacity());
                element.addSPA(CTxD, ((CargoBay)t).getDoors());
            } else if (t instanceof DockingCollar) {
                element.addSPA(DT, 1);
            } else if (t instanceof InfantryBay) {
                element.addSPA(IT, ((InfantryBay)t).getCapacity());
            } else if (t instanceof TroopSpace) {
                element.addSPA(IT, t.getUnused());
            } else if (t instanceof MechBay) {
                element.addSPA(MT, (int)((MechBay)t).getCapacity());
                element.addSPA(MTxD, ((MechBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof ProtomechBay) {
                element.addSPA(PT, (int)((ProtomechBay)t).getCapacity());
                element.addSPA(PTxD, ((ProtomechBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof SmallCraftBay) {
                element.addSPA(ST, (int)((SmallCraftBay)t).getCapacity());
                element.addSPA(STxD, ((SmallCraftBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof LightVehicleBay) {
                element.addSPA(VTM, (int)((LightVehicleBay)t).getCapacity());
                element.addSPA(VTMxD, ((LightVehicleBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof HeavyVehicleBay) {
                element.addSPA(VTH, (int)((HeavyVehicleBay)t).getCapacity());
                element.addSPA(VTHxD, ((HeavyVehicleBay)t).getDoors());
                element.addSPA(MFB);
            }
        }

        topLoop: for (int location = 0; location < entity.locations(); location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);
                if (null != crit) {
                    if (crit.isArmored()) {
                        element.addSPA(ARM);
                        break topLoop;
                    } else if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mount = crit.getMount();
                        if (mount.isArmored()) {
                            element.addSPA(ARM);
                            break topLoop;
                        }
                    }
                }
            }
        }

        if (entity instanceof Aero) {
            if (((Aero) entity).getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            }
            if (entity.isFighter()) {
                element.addSPA(BOMB, element.getSize());
            }
            if ((entity.getEntityType() & (Entity.ETYPE_JUMPSHIP | Entity.ETYPE_CONV_FIGHTER)) == 0) {
                element.addSPA(SPC);
            }
            if (((Aero) entity).isVSTOL()) {
                element.addSPA(VSTOL);
            }
            if (element.isType(AF)) {
                element.addSPA(FUEL, (int) Math.round(0.05 * ((Aero) entity).getFuel()));
            }
        }

        if (entity instanceof Infantry) {
            element.addSPA(CAR, (int)Math.ceil(entity.getWeight()));
            if (entity.getMovementMode().equals(EntityMovementMode.INF_UMU)) {
                element.addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.FIRE_ENGINEERS)) {
                element.addSPA(FF);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MINE_ENGINEERS)) {
                element.addSPA(MSW);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
                element.addSPA(MTN);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.PARATROOPS)) {
                element.addSPA(PARA);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.SCUBA)) {
                element.addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.TRENCH_ENGINEERS)) {
                element.addSPA(TRN);
            }
            if (entity.hasAbility("tsm_implant")) {
                element.addSPA(TSI);
            }
            if ((entity instanceof BattleArmor) && ((BattleArmor) entity).canDoMechanizedBA()) {
                element.addSPA(MEC);
            }
        }

        if (entity instanceof Mech) {
            if (((Mech) entity).getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_VRRP) {
                element.addSPA(VR, 1);
            }
            if (((Mech) entity).isIndustrial()) {
                if (((Mech) entity).getCockpitType() == Mech.COCKPIT_STANDARD) {
                    element.addSPA(AFC);
                } else {
                    element.addSPA(BFC);
                }
            } else {
                element.addSPA(SOA);
                element.addSPA(SRCH);
            }
        }

        if (entity instanceof Protomech) {
            element.addSPA(SOA);
            if (entity.getMovementMode().equals(EntityMovementMode.WIGE)) {
                element.addSPA(GLD);
            }
        }
        
        if (entity instanceof Tank && !entity.isSupportVehicle()) {
            element.addSPA(SRCH);
        }
        
        if ((element.getUnitType() == SC) || (element.getUnitType() == DS)) {
            if (element.getSize() == 1) {
                element.addSPA(LG);
            } else if (element.getSize() == 2) {
                element.addSPA(VLG);
            } else {
                element.addSPA(SLG);
            }
        }
        
        if (element.getMovementModes().contains("j") && element.getMovementModes().contains("")) {
            int jumpTMM = tmmForMovement(element.getMovement("j"));
            int walkTMM = tmmForMovement(element.getMovement(""));
            if (jumpTMM > walkTMM) {
                element.addSPA(JMPS, jumpTMM - walkTMM);
            } else if (jumpTMM < walkTMM) {
                element.addSPA(JMPW, walkTMM - jumpTMM);
            }
        }

        if (element.getMovementModes().contains("s") && element.getMovementModes().contains("")) {
            int umuTMM = tmmForMovement(element.getMovement("s"));
            int walkTMM = tmmForMovement(element.getMovement(""));
            if (umuTMM > walkTMM) {
                element.addSPA(SUBS, umuTMM - walkTMM);
            } else if (umuTMM < walkTMM) {
                element.addSPA(SUBW, walkTMM - umuTMM);
            }
        }
        
        if (element.isType(CF) || (entity instanceof VTOL)) {
            element.addSPA(ATMO);
        }
        
        if (entity instanceof LandAirMech) {
            LandAirMech lam = (LandAirMech) entity;
            double bombs = entity.countWorkingMisc(MiscType.F_BOMB_BAY);
            int bombValue = roundUp(bombs / 5);
            if (bombValue > 0) {
                element.addSPA(BOMB, bombValue);
            }
            element.addSPA(FUEL, (int) Math.round(0.05 * lam.getFuel()));
            var lamMoves = new HashMap<String, Integer>();
            lamMoves.put("g", lam.getAirMechCruiseMP(false, false) * 2);
            lamMoves.put("a", lam.getCurrentThrust());
            if (lam.getLAMType() == LandAirMech.LAM_BIMODAL) {
                element.addBimSPA(lamMoves);
            } else {
                element.addLamSPA(lamMoves);
            }
        }
        
        if (entity instanceof QuadVee) {
            element.addSPA(QV);
        }
        
        if (element.hasAutoSeal()) {
            element.addSPA(SEAL);
        }

    }

    private static @Nullable BattleForceSPA getArtilleryType(WeaponType weapon) {
        switch (weapon.getAmmoType()) {
            case AmmoType.T_ARROW_IV:
                if (weapon.getInternalName().charAt(0) == 'C') {
                    return BattleForceSPA.ARTAC;
                } else {
                    return BattleForceSPA.ARTAIS;
                }
            case AmmoType.T_LONG_TOM:
                return BattleForceSPA.ARTLT;
            case AmmoType.T_SNIPER:
                return BattleForceSPA.ARTS;
            case AmmoType.T_THUMPER:
                return BattleForceSPA.ARTT;
            case AmmoType.T_LONG_TOM_CANNON:
                return BattleForceSPA.ARTLTC;
            case AmmoType.T_SNIPER_CANNON:
                return BattleForceSPA.ARTSC;
            case AmmoType.T_THUMPER_CANNON:
                return BattleForceSPA.ARTTC;
            case AmmoType.T_CRUISE_MISSILE:
                switch(weapon.getRackSize()) {
                    case 50:
                        return BattleForceSPA.ARTCM5;
                    case 70:
                        return BattleForceSPA.ARTCM7;
                    case 90:
                        return BattleForceSPA.ARTCM9;
                    case 120:
                        return BattleForceSPA.ARTCM12;
                }
            case AmmoType.T_BA_TUBE:
                return BattleForceSPA.ARTBA;
        }
        return null;
    }

    private static boolean isArtilleryCannon(WeaponType weapon) {
        return (weapon.getAmmoType() == AmmoType.T_LONG_TOM_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_SNIPER_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_THUMPER_CANNON);
    }

    private static double getConvInfantryStandardDamage(Infantry inf) {
        return inf.getDamagePerTrooper() * TROOP_FACTOR[Math.min(inf.getShootingStrength(), 30)] / 10.0;
    }

    private static double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba, boolean apmMount) {
        double dam = 0;
        if (apmMount) {
            if (range == 0) {
                dam = AP_MOUNT_DAMAGE;
            }
        } else {
            dam = weapon.getBattleForceDamage(range);
        }
        return dam * (TROOP_FACTOR[Math.min(ba.getShootingStrength(), 30)] + 0.5);
    }
    
    /** 
     * Adjusts all damage values for overheat, if applicable (i.e., if the unit tracks heat,
     * if its heat output is sufficiently over its heat dissipation and for L/E values, if
     * the prerequisites for OVL are fulfilled. 
     * Also assigns OVL where applicable.
     */
    private static void adjustForHeat(Entity entity, AlphaStrikeElement element) {
        if (!entity.tracksHeat()) {
            return; 
        }
        double totalFrontHeat = getHeatGeneration(entity, element,false, false);
        int heatCapacity = getHeatCapacity(entity, element);
//        System.out.println("Total Heat Medium Range: "+totalFrontHeat);
//        System.out.println("Heat Capacity: "+heatCapacity);
        if (totalFrontHeat - 4 <= heatCapacity) {
            return;
        }


        // Determine OV from the medium range damage
        //TODO: this should not be necessary:
        while (element.weaponLocations[0].standardDamage.size() < 4) {
            element.weaponLocations[0].standardDamage.add(0.0);
        }
        double nonRounded = element.weaponLocations[0].standardDamage.get(RANGE_BAND_MEDIUM);
//        System.out.println("Total M damage before heat adjust: " + nonRounded);
        element.overheat = Math.min(heatDelta(nonRounded, heatCapacity, totalFrontHeat), 4);
        
        // Determine OVL from long range damage and heat
        if (element.overheat > 0 && element.usesOVL()) {
            double heatLong = getHeatGeneration(entity, element, false, true);
//            System.out.println("Long Range Heat: " + heatLong);
            if (heatLong - 4 > heatCapacity) {
                double nonRoundedL = element.weaponLocations[0].standardDamage.get(RANGE_BAND_LONG);
//                System.out.println("Long Range Damage: " + nonRoundedL);
                if (heatDelta(nonRoundedL, heatCapacity, heatLong) >= 1) {
                    element.addSPA(OVL);
                }
            }
        }

        // Adjust all weapon damages (L/E depending on OVL)
        int maxAdjustmentRange = 1 + (element.hasSPA(OVL) ? RANGE_BAND_EXTREME : RANGE_BAND_MEDIUM);
        double frontadjustment = heatCapacity / (totalFrontHeat - 4);
        double rearHeat = getHeatGeneration(entity, element,true, false);
        double rearAdjustment = rearHeat - 4 > heatCapacity ? heatCapacity / (rearHeat - 4) : 1;
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            WeaponLocation wloc = element.weaponLocations[loc];
            double adjustment = element.locationNames[loc].equals("REAR") ? rearAdjustment : frontadjustment;
            for (int i = 0; i < Math.min(maxAdjustmentRange, wloc.standardDamage.size()); i++) {
                wloc.standardDamage.set(i, heatAdjust(wloc.standardDamage.get(i), adjustment));
            }
            for (List<Double> damage : wloc.specialDamage.values()) {
                for (int i = 0; i < Math.min(maxAdjustmentRange, damage.size()); i++) {
                    damage.set(i, heatAdjust(damage.get(i), adjustment));
                }
            }
            // IF is long-range fire; only adjust when the unit can overheat even in long range 
            if (element.hasSPA(OVL)) {
                //TODO: really adjust this with round up to tenth?
                wloc.indirect = heatAdjust(wloc.indirect, adjustment);
            }
        }
    }
    
    private static double heatAdjust(double value, double adjustment) {
        return roundUpToTenth(value * adjustment);
    }
    
    /** 
     * Returns the delta between the unadjusted rounded and heat-adjusted rounded damage value,
     * according to ASC - Converting Heat Errata v1.2.
     * Use only for determining if a unit has OV or OVL.  
     */
    private static int heatDelta(double damage, int heatCapacity, double heat) {
        int roundedUp = roundUp(roundUpToTenth(damage));
        int roundedUpAdjustedL = roundUp(roundUpToTenth(damage * heatCapacity / (heat - 4)));
        return roundedUp - roundedUpAdjustedL;
    }
    
    /** 
     * Returns the total generated heat (weapons and movement) for a Mech or Aero for 
     * the purpose of finding OV / OVL values.
     * If onlyRear is true, only rear-facing weapons are included, otherwise only front-
     * facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included. 
     */
    private static int getHeatGeneration(Entity entity, AlphaStrikeElement element, boolean onlyRear, boolean onlyLongRange) {
        if (entity instanceof Mech) {
            return getMechHeatGeneration((Mech)entity, element, onlyRear, onlyLongRange);
        } else {
            return getAeroHeatGeneration((Aero)entity, onlyRear, onlyLongRange);
        }
    }
    
    /** 
     * Returns the total generated heat (weapons and movement) for a Mech for 
     * the purpose of finding OV / OVL values.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-
     * facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included. 
     */
    private static int getMechHeatGeneration(Mech entity, AlphaStrikeElement element, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = 0;

        if (entity.getJumpMP() > 0) {
            totalHeat += getJumpHeat(entity, element);
        } else if (!entity.isIndustrial() && entity.hasEngine()) {
            totalHeat += entity.getEngine().getRunHeat(entity);
        }

//        System.out.println("Total Heat Movement: " + totalHeat);

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                || (onlyRear && !mount.isRearMounted())
                || (!onlyRear && mount.isRearMounted())
                || (onlyLongRange && weapon.getBattleForceDamage(LONG_RANGE) == 0)) {
                continue;
            }
            if (weapon.getAmmoType() == AmmoType.T_AC_ROTARY) {
                totalHeat += weapon.getHeat() * 6;
//                //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 6);
            } else if (weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                    || weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                totalHeat += weapon.getHeat() * 2;
//                //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 2);
            } else {
                totalHeat += weapon.getHeat();
//                System.out.println(weapon.getName() + " Heat: " + weapon.getHeat());
            }
        }

//        System.out.println("Total Heat After wps: " + totalHeat);

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)
                || entity.hasWorkingMisc(MiscType.F_VOIDSIG, -1)
                || entity.hasWorkingMisc(MiscType.F_NULLSIG, -1)) {
            totalHeat += 10;
        }

        if (entity.hasWorkingMisc(MiscType.F_CHAMELEON_SHIELD, -1)) {
            totalHeat += 6;
        }

        return totalHeat;
    }

    private static int getJumpHeat(Entity entity, AlphaStrikeElement element) {
        if ((entity.getJumpType() == Mech.JUMP_IMPROVED)
                && (entity.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
            return Math.max(3, element.getJumpMove() / 2);
        } else if (entity.getJumpType() == Mech.JUMP_IMPROVED) {
            return Math.max(3, roundUp(0.25 * element.getJumpMove()));
        } else if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
            return Math.max(6, element.getJumpMove());
        } else {
            return Math.max(3, element.getJumpMove() / 2);
        }
    }
    
    /** 
     * Returns the total generated heat (weapons and movement) for an Aero for 
     * the purpose of finding OV / OVL values.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-
     * facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included. 
     */
    private static int getAeroHeatGeneration(Aero entity, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = 0;

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    totalHeat += entity.getEquipment(index).getType().getHeat();
                }
            } else {
                if (weapon.hasFlag(WeaponType.F_ONESHOT)
                        || (onlyRear && !mount.isRearMounted() && mount.getLocation() != Aero.LOC_AFT)
                        || (!onlyRear && (mount.isRearMounted() || mount.getLocation() == Aero.LOC_AFT))
                        || (onlyLongRange && weapon.getLongRange() < LONG_RANGE)) {
                    continue;
                }
                if (weapon.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    totalHeat += weapon.getHeat() * 6;
//                    //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 6);
                } else if (weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                        || weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                    totalHeat += weapon.getHeat() * 2;
                    //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat() * 2);
                } else {
                    totalHeat += weapon.getHeat();
                    //System.out.println(weapon.getName() + " Heat: " + weapon.getHeat());
                }
            }
        }

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)) {
            totalHeat += 10;
        }

        return totalHeat;
    }
    
    /** 
     * Returns the heat dissipation for Mechs and ASFs, according to ASC - Converting Heat
     * Errata v1.2. 
     */
    private static int getHeatCapacity(Entity entity, AlphaStrikeElement element) {
        int result = 0;
        if (entity instanceof Mech) {
            result = ((Mech)entity).getHeatCapacity(false, false);
        } else if (entity.isFighter()) {
            result = entity.getHeatCapacity(false);
        }
        result += entity.getEquipment().stream().filter(Mounted::isCoolantPod).count();
        result += entity.hasWorkingMisc(MiscType.F_PARTIAL_WING) ? 3 : 0;
        result += element.hasSPA(RHS) ? 1 : 0;
        result += element.hasSPA(ECS) ? 1 : 0;
        return result;
    }
    
    private static void initWeaponLocations(Entity entity, AlphaStrikeElement result) {
        if (entity instanceof Aero) {
            result.rangeBands = RANGEBANDS_SMLE;
        }
        result.weaponLocations = new WeaponLocation[entity.getNumBattleForceWeaponsLocations()];
        result.locationNames = new String[result.weaponLocations.length];
        for (int loc = 0; loc < result.locationNames.length; loc++) {
            result.weaponLocations[loc] = result.new WeaponLocation();
            result.locationNames[loc] = entity.getBattleForceLocationName(loc);
        }
    }
    
    private static int getPointValue(Entity entity, AlphaStrikeElement element) {
        if (element.isGround()) {
            int dmgS = element.getDmgS();
            int dmgM = element.getDmgM();
            int dmgL = element.getDmgL();
            double offensiveValue = dmgS + dmgM + dmgM + dmgL;
            offensiveValue += element.isMinimalDmgS() ? 0.5 : 0;
            offensiveValue += element.isMinimalDmgM() ? 1 : 0;
            offensiveValue += element.isMinimalDmgL() ? 0.5 : 0;
            
            if (element.isAnyTypeOf(BM, PM)) {
                offensiveValue += 0.5 * element.getSize();
            }
            
            if (element.getOverheat() >= 1) {
                offensiveValue += 1 + 0.5 * (element.getOverheat() - 1);
            }

            offensiveValue += getGroundOffensiveSPAMod(entity, element);
            offensiveValue *= getGroundOffensiveBlanketMod(entity, element);

            double defensiveValue = 0.125 * getHighestMove(element);
            if (element.movement.containsKey("j")) {
                defensiveValue += 0.5;
            }
            defensiveValue += getGroundDefensiveSPAMod(element);
            defensiveValue += getDefensiveDIR(element);
            double subTotal = offensiveValue + defensiveValue;
            double bonus = agileBonus(element);
            bonus += c3Bonus(element) ? 0.05 * subTotal : 0;
            bonus -= subTotal * brawlerMalus(element);
            subTotal += bonus;
            subTotal += forceBonus(element);
            return Math.max(1, (int)Math.round(subTotal));
            
        } else if (element.isAnyTypeOf(AF, CF) 
                || (element.isType(SV) && (entity instanceof Aero))) {
            int dmgS = element.getDmgS();
            int dmgM = element.getDmgM();
            int dmgL = element.getDmgL();
            double offensiveValue = dmgS + dmgM + dmgM + dmgL;

            if (element.getOverheat() >= 1) {
                double overheatFactor = 1 + 0.5 * (element.getOverheat() - 1);
                overheatFactor /= (dmgM + dmgL == 0) ? 2 : 1;
                offensiveValue += overheatFactor;
            }
            
            offensiveValue += getAeroOffensiveSPAMod(entity, element);
            offensiveValue *= getAeroOffensiveBlanketMod(element);
            offensiveValue = roundUpToHalf(offensiveValue);

            double defensiveValue = 0.25 * getHighestMove(element);
            defensiveValue += getHighestMove(element) >= 10 ? 1 : 0;
            defensiveValue += getAeroDefensiveSPAMod(element);
            defensiveValue += getAeroDefensiveFactors(element);

            double subTotal = offensiveValue + defensiveValue;
            subTotal += forceBonus(element);
            
            return Math.max(1, (int)Math.round(subTotal));
        }
        return 0;
    }

    private static void adjustPVforSkill(AlphaStrikeElement element) {
            int multiplier = 1;
        if (element.getSkill() > 4) {
            if (element.getFinalPoints() > 14) {
                multiplier += (element.getFinalPoints() - 5) / 10;
            }
            element.points -= (element.getSkill() - 4) * multiplier;
        } else if (element.getSkill() < 4) {
            if (element.getFinalPoints() > 7) {
                multiplier += (element.getFinalPoints() - 3) / 5;
            }
            element.points += (4 - element.getSkill()) * multiplier;
        }
    }
    
    private static double getGroundOffensiveSPAMod(Entity entity, AlphaStrikeElement element) {
        double result = element.hasSPA(TAG) ? 0.5 : 0;
        result += element.hasSPA(SNARC) ? (int)element.getSPA(SNARC) : 0;
        result += element.hasSPA(INARC) ? (int)element.getSPA(INARC) : 0;
        result += element.hasSPA(TSM) ? 1 : 0;
        result += element.hasSPA(CNARC) ? 0.5 * (int)element.getSPA(CNARC) : 0;
        result += element.hasSPA(LTAG) ? 0.25 : 0;
        result += element.hasSPA(ECS) ? 0.25 : 0;
        result += element.hasSPA(MEL) ? 0.5 : 0;
        result += element.hasSPA(MDS) ? (int)element.getSPA(MDS) : 0;
        result += element.hasSPA(MTAS) ? (int)element.getSPA(MTAS) : 0;
        result += element.hasSPA(BTAS) ? 0.25 * (int)element.getSPA(BTAS) : 0;
        result += element.hasSPA(TSEMP) ? 5 * (int)element.getSPA(TSEMP) : 0;
        result += element.hasSPA(TSEMPO) ? Math.min(5, (int)element.getSPA(TSEMPO)) : 0;
        result += element.hasSPA(BT) ? 0.5 * getHighestMove(element) * element.getSize() : 0;
        result += element.hasSPA(IATM) ? ((ASDamageVector)element.getSPA(IATM)).L.damage : 0;
        result += element.hasSPA(OVL) ? 0.25 * element.getOverheat() : 0;
        if (element.hasSPA(HT)) {
            ASDamageVector ht = (ASDamageVector) element.getSPA(HT);
            result += Math.max(ht.S.damage, Math.max(ht.M.damage, ht.L.damage));
            result += ht.M.damage > 0 ? 0.5 : 0;
        }
        if (element.hasSPA(IF)) {
            result += element.isMinimalIF() ? 0.5 : ((ASDamageVector)element.getSPA(IF)).S.damage;
        }
        if (element.hasSPA(RHS)) {
            if (element.hasSPA(OVL)) {
                result += 1;
            } else if (element.getOverheat() > 0) {
                result += 0.5;
            } else {
                result += 0.25;
            }
        }
        result += getArtyOffensiveSPAMod(entity, element);
        return result;
    }
    
    private static double getArtyOffensiveSPAMod(Entity entity, AlphaStrikeElement element) {
        double result = element.hasSPA(ARTAIS) ? 12 * (int)element.getSPA(ARTAIS) : 0;
        result += element.hasSPA(ARTAC) ? 12 * (int)element.getSPA(ARTAC) : 0;
        result += element.hasSPA(ARTT) ? 6 * (int)element.getSPA(ARTT) : 0;
        result += element.hasSPA(ARTS) ? 12 * (int)element.getSPA(ARTS) : 0;
        result += element.hasSPA(ARTBA) ? 6 * (int)element.getSPA(ARTBA) : 0;
        result += element.hasSPA(ARTLTC) ? 2 * 6 * (int)element.getSPA(ARTLTC) : 0;
        result += element.hasSPA(ARTSC) ? 1 * 6 * (int)element.getSPA(ARTSC) : 0;
        result += element.hasSPA(ARTCM5) ? 5 * 6 * (int)element.getSPA(ARTCM5) : 0;
        result += element.hasSPA(ARTCM7) ? (7 * 6 + 2 * 3 + 2 * 3) * (int)element.getSPA(ARTCM7) : 0;
        result += element.hasSPA(ARTCM9) ? (9 * 6 + 4 * 3 + 2 * 3) * (int)element.getSPA(ARTCM9) : 0;
        result += element.hasSPA(ARTCM12) ? (12 * 6 + 5 * 3 + 2 * 3) * (int)element.getSPA(ARTCM12) : 0;
        result += element.hasSPA(ARTLT) ? (3 * 6 + 1 * 3 + 2 * 3) * (int)element.getSPA(ARTLT) : 0;
        result += element.hasSPA(ARTTC) ? 0.5 * 6 * (int)element.getSPA(ARTTC) : 0;
        return result;
    }
    
    private static double getGroundOffensiveBlanketMod(Entity entity, AlphaStrikeElement element) {
        double result = 1;
        result += element.hasSPA(VRT) ? 0.1 : 0;
        result -= element.hasSPA(BFC) ? 0.1 : 0;
        result -= element.hasSPA(SHLD) ? 0.1 : 0;
        if (element.asUnitType == SV || element.asUnitType == IM) {
            result -= !element.hasAnySPAOf(AFC, BFC) ? 0.2 : 0;
        }
        return result;
    }
    
    private static double getAeroOffensiveSPAMod(Entity entity, AlphaStrikeElement element) {
        double result = element.hasSPA(SNARC) ? 1 : 0;
        result += element.hasSPA(INARC) ? 1 : 0;
        result += element.hasSPA(CNARC) ? 0.5 : 0;
        result += element.hasSPA(BT) ? 0.5 * getHighestMove(element) * element.getSize() : 0;
        result += element.hasSPA(OVL) ? 0.25 * element.getOverheat() : 0;
        if (element.hasSPA(HT)) {
            ASDamageVector ht = (ASDamageVector) element.getSPA(HT);
            result += Math.max(ht.S.damage, Math.max(ht.M.damage, ht.L.damage));
            result += ht.M.damage > 0 ? 0.5 : 0;
        }
        result += getArtyOffensiveSPAMod(entity, element);
        return result;
    }
    
    private static double getAeroOffensiveBlanketMod(AlphaStrikeElement element) {
        double result = 1;
        result += element.hasSPA(ATAC) ? 0.1 : 0;
        result += element.hasSPA(VRT) ? 0.1 : 0;
        result -= element.hasSPA(BFC) ? 0.1 : 0;
        result -= element.hasSPA(SHLD) ? 0.1 : 0; // So says the AS Companion
        result -= element.hasSPA(DRO) ? 0.1 : 0;
        result -= (element.isType(SV) && !element.hasAnySPAOf(AFC, BFC)) ? 0.2 : 0;
        return result;
    }
   
    private static double getGroundDefensiveSPAMod(AlphaStrikeElement element) {
        double result = element.hasSPA(ABA) ? 0.5 : 0;
        result += element.hasSPA(AMS) ? 1 : 0;
        result += element.hasSPA(CR) && element.getStructure() >= 3 ? 0.25 : 0;
        result += element.hasSPA(FR) ? 0.5 : 0;
        result += element.hasSPA(RAMS) ? 1.25 : 0;
        result += (element.hasSPA(ARM) && element.structure > 1) ? 0.5 : 0;
        
        double armorThird = Math.floor((double)element.getFinalArmor() / 3);
        double barFactor = element.hasSPA(BAR) ? 0.5 : 1;
        result += element.hasSPA(BHJ2) ? barFactor * armorThird : 0;
        result += element.hasSPA(RCA) ? barFactor * armorThird : 0;
        result += element.hasSPA(SHLD) ? barFactor * armorThird : 0;
        result += element.hasSPA(BHJ3) ? barFactor * 1.5 * armorThird: 0;
        result += element.hasSPA(BRA) ? barFactor * 0.75 * armorThird : 0;
        result += element.hasSPA(IRA) ? barFactor * 0.5 * armorThird: 0;
        return result;
    }
    
    private static double getAeroDefensiveSPAMod(AlphaStrikeElement element) {
        double result = element.hasSPA(PNT) ? (int)element.getSPA(PNT) : 0;
        result += element.hasSPA(STL) ? 2 : 0;
        if (element.hasSPA(RCA)) {
            double armorThird = Math.floor((double)element.getFinalArmor() / 3);
            double barFactor = element.hasSPA(BAR) ? 0.5 : 1;
            result += armorThird * barFactor;
        }
        return result;
    }
    
    private static double getDefensiveDIR(AlphaStrikeElement element) {
        double result = element.getFinalArmor() * getArmorFactorMult(element);
        result += element.getStructure() * getStructureMult(element);
        result *= getDefenseFactor(element);
        result = 0.5 * Math.round(result * 2);
        return result;
    }
    
    private static double getArmorFactorMult(AlphaStrikeElement element) {
        double result = 2;
        if (element.asUnitType == CV) {
            if (element.getMovementModes().contains("t") || element.getMovementModes().contains("n")) {
                result = 1.8;
            } else if (element.getMovementModes().contains("h") || element.getMovementModes().contains("w")) {
                result = 1.7;
            } else if (element.getMovementModes().contains("v") || element.getMovementModes().contains("g")) {
                result = 1.5;
            }
            result += element.hasSPA(ARS) ? 0.1 : 0;
        }
        result /= element.hasSPA(BAR) ? 2 : 1;
        return result;
    }
    
    private static double getStructureMult(AlphaStrikeElement element) {
        if (element.asUnitType == BA || element.asUnitType == CI) {
            return 2;
        } else  if (element.asUnitType == IM || element.hasSPA(BAR)) {
            return 0.5;
        } else  {
            return 1;
        }
    }
    
    private static double getAeroDefensiveFactors(AlphaStrikeElement element) {
        double barFactor = element.hasSPA(BAR) ? 0.5 : 1;
        double thresholdMultiplier = Math.min(1.3 + 0.1 * element.getThreshold(), 1.9);
        double result = thresholdMultiplier * barFactor * element.getFinalArmor();
        result += element.getStructure();
        return result;
    }
    
    private static double getDefenseFactor(AlphaStrikeElement element) {
        double result = 0;
        double movemod = getMovementMod(element);
        if (element.hasSPA(MAS) && (3 > movemod)) {
            result += 3;
        } else if (element.hasSPA(LMAS) && (2 > movemod)) {
            result += 2;
        } else {
            result += movemod;
        }
        result += element.isAnyTypeOf(BA, PM) ? 1 : 0;
        if ((element.isType(CV)) && (element.getMovementModes().contains("g")
                || element.getMovementModes().contains("v"))) {
            result++;
        }
        result += element.hasSPA(STL) ? 1 : 0;
        result -= element.hasAnySPAOf(LG, SLG, VLG)  ? 1 : 0;
        result = 1 + (result <= 2 ? 0.1 : 0.25) * Math.max(result, 0);
        return result;
    }
    
    /** 
     * Returns the movement modifier (for the Point Value DIR calculation only),
     * AlphaStrike Companion Errata v1.4, p.17 
     */
    private static double getMovementMod(AlphaStrikeElement element) {
        int highestNonJumpMod = -1;
        int highestJumpMod = -1;
        for (String mode : element.getMovementModes()) {
            int mod = tmmForMovement(element.getMovement(mode));
            if (mode.equals("j")) {
                highestJumpMod = mod;
            } else {
                highestNonJumpMod = Math.max(highestNonJumpMod, mod);
            }
        }
        double result = highestNonJumpMod == -1 ? highestJumpMod : highestNonJumpMod;
        result += element.isInfantry() && element.isJumpCapable() ? 1 : 0;
        return result;
    }

    /** Brawler Malus, AlphaStrike Companion Errata v1.4, p.17 */
    private static double brawlerMalus(AlphaStrikeElement element) {
        int move = getHighestMove(element);
        if (move >= 2 && !element.hasAnySPAOf(BT, ARTS, C3BSM, C3BSS, C3EM, 
                C3I, C3M, C3S, AC3, NC3, NOVA, C3RS, ECM, AECM, ARTAC, ARTAIS, ARTBA, ARTCM12, ARTCM5, ARTCM7,
                ARTCM9, ARTLT, ARTLTC, ARTSC, ARTT, ARTTC)) {
            int dmgS = element.standardDamage.S.damage;
            int dmgM = element.standardDamage.M.damage;
            int dmgL = element.standardDamage.L.damage;
            
            boolean onlyShortRange = (dmgM + dmgL) == 0 && (dmgS > 0);
            boolean onlyShortMediumRange = (dmgL == 0) && (dmgS + dmgM > 0);
            if ((move >= 6) && (move <= 10) && onlyShortRange) {
                return 0.25;
            } else if ((move < 6) && onlyShortRange) {
                return 0.5;
            } else if ((move < 6) && onlyShortMediumRange) {
                return 0.25;
            }
        }
        return 0;
    }
    
    /** Brawler Malus, AlphaStrike Companion Errata v1.4, p.17 */
    private static double agileBonus(AlphaStrikeElement element) {
        double result = 0;
        if (element.getTMM() >= 2) {
            double dmgS = element.isMinimalDmgS() ? 0.5 : element.getDmgS();
            double dmgM = element.isMinimalDmgM() ? 0.5 : element.getDmgM();
            if (dmgM > 0) {
                result = (element.getTMM() - 1) * dmgM;
            } else if (element.getTMM() >= 3) {
                result = (element.getTMM() - 2) * dmgS;
            }
        }
        return roundToHalf(result);
    }
    
    /** Brawler Malus, AlphaStrike Companion Errata v1.4, p.17 */
    private static boolean c3Bonus(AlphaStrikeElement element) {
        return element.hasAnySPAOf(C3BSM, C3BSS, C3EM, C3I, C3M, C3S, AC3, NC3, NOVA);
    }
    
    private static double forceBonus(AlphaStrikeElement element) {
        double result = element.hasSPA(AECM) ? 3 : 0;
        result += element.hasSPA(BH) ? 2 : 0;
        result += element.hasSPA(C3RS) ? 2 : 0;
        result += element.hasSPA(ECM) ? 2 : 0;
        result += element.hasSPA(LECM) ? 0.5 : 0;
        result += element.hasSPA(MHQ) ? (int)element.getSPA(MHQ) : 0;
        result += element.hasSPA(PRB) ? 1 : 0;
        result += element.hasSPA(LPRB) ? 1 : 0;
        result += element.hasSPA(RCN) ? 2 : 0;
        result += element.hasSPA(TRN) ? 2 : 0;
        return result;
    }
    
    private static double roundToHalf(double number) {
        return 0.5 * Math.round(number * 2);
    }
    
    private static double roundUpToHalf(double number) {
        return 0.5 * Math.round(number * 2 + 0.3);
    }
    
    /** Returns the given number, rounded up to the nearest tenth. */
    public static double roundUpToTenth(double number) {
        return 0.1 * roundUp(number * 10);
    }
    
    public static int roundUp(double number) {
        return (int) Math.round(number + 0.4); 
    }

    private static int getHighestMove(AlphaStrikeElement element) {
        return element.movement.values().stream().mapToInt(m -> m).max().orElse(0);
    }
    
    /** Returns true when the given weapon contributes to the LRM/SRM specials. */
    private static boolean isSRMorLRMSpecial(WeaponType weapon) {
        return weapon.getBattleForceClass() == WeaponType.BFCLASS_SRM 
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_LRM
                || weapon.getBattleForceClass() == WeaponType.BFCLASS_MML;
    }
    
    private static int getRearLocation(AlphaStrikeElement element) {
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            if (element.locationNames[loc].equals("REAR")) {
                return loc;
            }
        }
        return -1;
    }

}

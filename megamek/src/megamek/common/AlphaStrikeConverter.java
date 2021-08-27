package megamek.common;

import megamek.MegaMek;
import megamek.common.BattleForceElement.WeaponLocation;
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

public final class AlphaStrikeConverter {

    public static AlphaStrikeElement convertToAlphaStrike(Entity entity) {
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

        System.out.println("----------Unit: "+entity.getShortName());
        var result = new AlphaStrikeElement();
        result.name = undamagedEntity.getShortName();
        result.model = undamagedEntity.getModel();
        result.chassis = undamagedEntity.getChassis();
        result.role = UnitRoleHandler.getRoleFor(undamagedEntity);
        result.asUnitType = getType(undamagedEntity);
        result.size = getSize(undamagedEntity);
        result.movement = getMovement(undamagedEntity);
        result.tmm = getTMM(result);
        result.armor = getArmor(undamagedEntity);
        result.structure = getStructure(undamagedEntity);
        result.threshold = getThreshold(undamagedEntity, result);
        addBattleForceSpecialAbilities(undamagedEntity, result);
        initWeaponLocations(undamagedEntity, result);
        result.heat = new int[result.rangeBands];
        computeDamage(undamagedEntity, result);
        result.points = getPointValue(undamagedEntity, result);
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

    /** Type conversion, AlphaStrike Companion, p.91 */
    private static ASUnitType getType(Entity entity) {
        if (entity instanceof Mech) {
            return ((Mech)entity).isIndustrial() ? IM : BM;
        } else if (entity instanceof Protomech) {
            return PM;
        } else if (entity instanceof Tank) {
            return entity.isSupportVehicle() ? SV : CV;
        } else if (entity instanceof BattleArmor) {
            return BA;
        } else if (entity instanceof Infantry) {
            return CI;
        } else if (entity instanceof SpaceStation) {
            return SS;
        } else if (entity instanceof Warship) {
            return WS;
        } else if (entity instanceof Jumpship) {
            return JS;
        } else if (entity instanceof Dropship) {
            return ((Dropship)entity).isSpheroid() ? DS : DA;
        } else if (entity instanceof SmallCraft) {
            return SC;
        } else if (entity instanceof FixedWingSupport) {
            return SV;
        } else if (entity instanceof ConvFighter) {
            return CF;
        } else if (entity instanceof Aero) {
            return AF;
        }
        return null;
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
        double walkMP = entity.getWalkMP();
        int jumpMove = entity.getJumpMP() * 2;
        if (entity instanceof Mech) {
            if (hasSupercharger(entity) && hasMechMASC(entity)) {
                walkMP *= 1.5;
            } else if (hasSupercharger(entity) || hasMechMASC(entity)) {
                walkMP *= 1.25;
            }

            if (((Mech)entity).hasMPReducingHardenedArmor()) {
                walkMP--;
            }

            if (hasMPReducingShield(entity)) {
                walkMP--;
            }
        } else if (entity instanceof VTOL) {
            if (hasJetBooster(entity)) {
                walkMP *= 1.25;
            }
        } else if (entity instanceof Tank) {
            if (hasSupercharger(entity)) {
                walkMP *= 1.25;
            }
        }
        int baseMove = ((int)Math.round(walkMP)) * 2;

        if (jumpMove == baseMove) {
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
        int walkingMP = ((Infantry)entity).getWalkMP(false, true, true);
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
        double armorPoints = 0;

        for (int loc = 0; loc < entity.locations(); loc++) {
            double armorMod = 1;
            switch (entity.getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMod = .5;
                    break;
                case EquipmentType.T_ARMOR_INDUSTRIAL:
                case EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL:
                    armorMod = entity.getBARRating(0) / 10;
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                    armorMod = 1.2;
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMod = 1.5;
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_REACTIVE:
                    armorMod = .75;
                    break;
            }
            armorPoints += armorMod * entity.getArmor(loc);
            if (entity.hasRearArmor(loc)) {
                armorPoints += armorMod * entity.getArmor(loc, true);
            }
        }

        if (entity.hasModularArmor()) {
            // Modular armor is always "regular" armor
            for (Mounted mount : entity.getEquipment()) {
                if ((mount.getType() instanceof MiscType)
                        && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                    armorPoints += 10;
                }
            }
        }

        if (entity.isCapitalScale()) {
            return (int)Math.round(armorPoints * 0.33);
        }

        if (entity.getEntityType() == Entity.ETYPE_INFANTRY) {
            double divisor = ((Infantry) entity).calcDamageDivisor();
            if (((Infantry) entity).isMechanized()) {
                divisor /= 2.0;
            }
            armorPoints *= divisor;
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
    private static int getStructure(Entity entity) {
        int battleForceStructure = 0;

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
        if (entity.isClan()) {
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
                        return 4;
                    case Engine.XXL_ENGINE:
                        return 8;
                    case Engine.LIGHT_ENGINE:
                        return 4;
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

    public static void computeDamage(Entity entity, AlphaStrikeElement result) {
        double[] baseDamage = new double[result.rangeBands];
        boolean hasTC = entity.hasTargComp();
        int[] ranges;
        double pointDefense = 0;
        int bombRacks = 0;
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
            
            ranges = weapon.isCapital() ? CAPITAL_RANGES : STANDARD_RANGES;
            
            if (weapon.getAmmoType() == AmmoType.T_INARC) {
               result.addSPA(INARC, 1);
               continue;
            } else if (weapon.getAmmoType() == AmmoType.T_NARC) {
                if (weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
                    result.addSPA(CNARC, 1);
                } else {
                    result.addSPA(SNARC, 1);
                }
                continue;
            }
            
            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
                result.addSPA(SCR, 1);
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_AMS)) {
                result.addSPA(AMS);
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
                } else {
                    result.addSPA(TAG);
                }
                continue;
            }
            
            if (weapon.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                addArtillery(weapon, result);
                continue;
            }
            if (weapon instanceof ArtilleryBayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = entity.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        addArtillery((WeaponType)m.getType(), result);
                    }
                }
            }
            
            if (weapon.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
                bombRacks++;
                continue;
            }
            
            if (weapon.getAmmoType() == AmmoType.T_TASER) {
                if (entity instanceof BattleArmor) {
                    result.addSPA(BTA, 1);
                } else {
                    result.addSPA(MTA, 1);
                }
                continue;
            }
            
            if (weapon.hasFlag(WeaponType.F_TSEMP)) {
                if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                    result.addSPA(TSEMPO, 1);
                } else {
                    result.addSPA(TSEMP, 1);
                }
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
                        baseDamage[r] = getBattleArmorDamage(weapon, ranges[r], ((BattleArmor)entity));
                    } else {
                        baseDamage[r] = weapon.getBattleForceDamage(ranges[r], mount.getLinkedBy());
                    }
                    result.heat[r] += weapon.getBattleForceHeatDamage(ranges[r]);
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
                    if (!weapon.isCapital()) {
                        if (r==1) System.out.println(mount.getName() + " " + dam + " Mul: " + damageModifier);
                        result.weaponLocations[loc].addDamage(r, dam);
                    }
                    if (!MountedHelper.isAnyArtemis(mount.getLinkedBy())) {
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
                    if (r == RANGE_BAND_LONG && !(entity instanceof Aero) && weapon.hasIndirectFire()) {
                        result.weaponLocations[loc].addIF(dam);
                    }
                }
            }
            if (entity instanceof Aero && weapon.getAtClass() == WeaponType.CLASS_POINT_DEFENSE) {
                pointDefense += baseDamage[RANGE_BAND_SHORT] * damageModifier;
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
                    result.weaponLocations[0].addDamage(r, getConvInfantryStandardDamage(STANDARD_RANGES[r],
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
        
        System.out.println("Point Defense: "+pointDefense);
        if (entity instanceof Aero && pointDefense > 0) {
            result.addSPA(PNT, (int)Math.ceil(pointDefense / 10.0));
        }
        
        adjustForHeat(entity, result);
        
        // Rules state that all flamer and plasma weapons on the unit contribute to the heat rating, so we don't separate by arc
        int htS = resultingHTValue(result.heat[RANGE_BAND_SHORT]);
        int htM = resultingHTValue(result.heat[RANGE_BAND_MEDIUM]);
        int htL = resultingHTValue(result.heat[RANGE_BAND_LONG]);
        if (htS + htM + htL > 0) {
            result.addSPA(HT, ASDamageVector.createRoundedNormal(htS, htM, htL));
        }
        
        // Aero do not get IF, i.e. all units with IF use SML damage values and may only have TUR or REAR with IF
        double sumIF = 0;
        for (int loc = 0; loc < result.weaponLocations.length; loc++) {
            if (result.locationNames[loc].isBlank() || result.locationNames[loc].equals("TUR")) {
                sumIF += result.weaponLocations[loc].getIF();
            }
        }
        if (sumIF > 0) {
            System.out.println(sumIF);
            result.addSPA(IF, ASDamage.createDualRoundedNormal(sumIF));
        }
        
        // Aero do not get LRM/SRM/AC
        if (!(entity instanceof Aero)) {
            double sumLRMs = 0;
            double sumLRMm = 0;
            double sumLRMl = 0;
            for (int loc = 0; loc < result.weaponLocations.length; loc++) {
                if (result.locationNames[loc].isBlank() || result.locationNames[loc].equals("TUR")) {
                    sumLRMs += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_LRM, RANGE_BAND_SHORT);
                    sumLRMm += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_LRM, RANGE_BAND_MEDIUM);
                    sumLRMl += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_LRM, RANGE_BAND_LONG);
                }
            }
            if (roundUpToTenth(sumLRMm) >= 1) {
                result.addSPA(LRM, ASDamageVector.createRoundedNormalNoMinimal(sumLRMs, sumLRMm, sumLRMl));
            }

            double sumACs = 0;
            double sumACm = 0;
            double sumACl = 0;
            for (int loc = 0; loc < result.weaponLocations.length; loc++) {
                if (result.locationNames[loc].isBlank() || result.locationNames[loc].equals("TUR")) {
                    sumACs += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_AC, RANGE_BAND_SHORT);
                    sumACm += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_AC, RANGE_BAND_MEDIUM);
                    sumACl += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_AC, RANGE_BAND_LONG);
                }
            }
            if (roundUpToTenth(sumACm) >= 1) {
                result.addSPA(AC, ASDamageVector.createRoundedNormalNoMinimal(sumACs, sumACm, sumACl));
            }
            
            double sumSRMs = 0;
            double sumSRMm = 0;
            for (int loc = 0; loc < result.weaponLocations.length; loc++) {
                if (result.locationNames[loc].isBlank() || result.locationNames[loc].equals("TUR")) {
                    sumSRMs += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_SRM, RANGE_BAND_SHORT);
                    sumSRMm += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_SRM, RANGE_BAND_MEDIUM);
                }
            }
            //TODO: move the rounduptenth to the heat adjustment, it happens everywhere and first
            if (roundUpToTenth(sumSRMm) >= 1) {
                result.addSPA(SRM, ASDamageVector.createRoundedNormalNoMinimal(sumSRMs, sumSRMm, 0));
            }
        }

        double sumFlaks = 0;
        double sumFlakm = 0;
        double sumFlakl = 0;
        for (int loc = 0; loc < result.weaponLocations.length; loc++) {
            if (result.locationNames[loc].isBlank() || result.locationNames[loc].equals("TUR")) {
                sumFlaks += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_FLAK, RANGE_BAND_SHORT);
                sumFlakm += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_FLAK, RANGE_BAND_MEDIUM);
                sumFlakl += result.weaponLocations[loc].getDamage(WeaponType.BFCLASS_FLAK, RANGE_BAND_LONG);
            }
        }
        if (roundUpToTenth(sumFlaks) + roundUpToTenth(sumFlakm) + roundUpToTenth(sumFlakl) > 0) {
            result.addSPA(FLK, ASDamageVector.createDualRoundedNormal(sumFlaks, sumFlakm, sumFlakl));
        }
        
        
        
        double sums = 0;
        double summ = 0;
        double suml = 0;
        double sume = 0;
        for (int loc = 0; loc < result.weaponLocations.length; loc++) {
            if (result.locationNames[loc].isBlank() || result.locationNames[loc].equals("TUR")) {
                sums += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_SHORT);
                summ += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_MEDIUM);
                suml += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_LONG);
                if (result.rangeBands == RANGEBANDS_SMLE) {
                    sume += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_EXTREME);
                }
            }
        }
        result.standardDamage = ASDamageVector.createDualRoundedUp(sums, summ, suml, sume);

        double sumRears = 0;
        double sumRearm = 0;
        double sumRearl = 0;
        double sumReare = 0;
        for (int loc = 0; loc < result.weaponLocations.length; loc++) {
            if (result.locationNames[loc].equals("REAR") && result.weaponLocations[loc].hasDamage()) {
                sumRears += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_SHORT);
                sumRearm += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_MEDIUM);
                sumRearl += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_LONG);
                if (result.rangeBands == RANGEBANDS_SMLE) {
                    sumReare += result.weaponLocations[loc].standardDamage.get(RANGE_BAND_EXTREME);
                }
            }
        }
        if (sumRears + sumRearm + sumRearl + sumReare > 0) {
            result.addSPA(REAR, ASDamageVector.createRoundedNormal(sumRears, sumRearm, sumRearl, sumReare));
        }

        var turList = new ArrayList<List<Object>>();
        for (int loc = 0; loc < result.weaponLocations.length; loc++) {
            if (result.locationNames[loc].equals("TUR")) {
                var curTurret = new ArrayList<Object>();
                turList.add(curTurret);
                if (result.rangeBands == RANGEBANDS_SML) {
                    var std = ASDamageVector.createDualRoundedUp(
                            result.weaponLocations[loc].standardDamage.get(0),
                            result.weaponLocations[loc].standardDamage.get(1),
                            result.weaponLocations[loc].standardDamage.get(2));
                    curTurret.add(std);
                    var specialMap = new HashMap<BattleForceSPA, Object>();
                    curTurret.add(specialMap);
                    for (int i = WeaponType.BFCLASS_LRM; i <= WeaponType.BFCLASS_REL; i++) {
                        if (result.weaponLocations[loc].hasDamageClass(i)) {
                            var dmg = ASDamageVector.createDualRoundedUp(
                                    result.weaponLocations[loc].specialDamage.get(i).get(0),
                                    result.weaponLocations[loc].specialDamage.get(i).get(1),
                                    result.weaponLocations[loc].specialDamage.get(i).get(2));
                            specialMap.put(BattleForceSPA.getSPAForDmgClass(i), dmg);
                        }
                    }
                } else {
                    result.addSPA(TUR, ASDamageVector.createDualRoundedUp(
                            result.weaponLocations[loc].standardDamage.get(0),
                            result.weaponLocations[loc].standardDamage.get(1),
                            result.weaponLocations[loc].standardDamage.get(2),
                            result.weaponLocations[loc].standardDamage.get(3)));
                }
            }
        }
        if (!turList.isEmpty()) {
            result.addTurSPA(turList);
        }
        
        
        // For MHQ, the values may contain decimals, but the the final MHQ value is rounded down to an int.
        if (result.getSPA(MHQ) instanceof Double) {
            double mhqValue = (double) result.getSPA(MHQ);
            result.replaceSPA(MHQ, (int) mhqValue);
        }
        
        // Cannot have both CASEII and CASE
        if (result.hasSPA(CASEII)) {
            result.removeSPA(CASE);
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
        return en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
    }
    
    private static void addBattleForceSpecialAbilities(Entity entity, AlphaStrikeElement element) {
        boolean hasExplosiveComponent = false;
        for (Mounted m : entity.getEquipment()) {
            
            if (m.getExplosionDamage() > 0) {
                hasExplosiveComponent = true;
            }
            
            if (!(m.getType() instanceof MiscType)) {
                continue;
            }
            if (m.getType().hasFlag(MiscType.F_BAP)) {
                element.addSPA(RCN);
                if (m.getType().hasFlag(MiscType.F_BLOODHOUND)) {
                    element.addSPA(BH);
                } else if (m.getType().hasFlag(MiscType.F_BA_EQUIPMENT)) {
                    element.addSPA(LPRB);
                } else if (m.getType().hasFlag(MiscType.F_WATCHDOG)) {
                    element.addSPA(WAT);
                    element.addSPA(LPRB);
                    element.addSPA(ECM);
                } else {
                    element.addSPA(PRB);
                }
                if (m.getType().hasFlag(MiscType.F_NOVA)) {
                    element.addSPA(NOVA);
                    element.addSPA(ECM);
                    element.addSPA(MHQ, 3); // count half-tons
                }
            } else if (m.getType().hasFlag(MiscType.F_ECM)) {
                if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    element.addSPA(AECM);
                } else if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    element.addSPA(LECM);
                } else {
                    element.addSPA(ECM);
                }
            } else if (m.getType().hasFlag(MiscType.F_BOOBY_TRAP)) {
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
            } else if (m.getType().hasFlag(MiscType.F_CASE)) {
                element.addSPA(CASE);
            } else if (m.getType().hasFlag(MiscType.F_CASEP)) { //in BF seems to work the same as CASE
                element.addSPA(CASE);
            } else if (m.getType().hasFlag(MiscType.F_CASEII)) {
                element.addSPA(CASEII);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                element.addSPA(DRO);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                element.addSPA(DCC, (int) m.getSize());
            } else if (m.getType().hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                element.addSPA(DCC, 1);
            } else if (m.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                element.addSPA(ES);
            } else if (m.getType().hasFlag(MiscType.F_BULLDOZER)) {
                element.addSPA(ENG);
            } else if (m.getType().hasFlag(MiscType.F_CLUB)) {
                element.addSPA(MEL);
                if ((m.getType().getSubType() &
                        (MiscType.S_BACKHOE | MiscType.S_PILE_DRIVER
                                | MiscType.S_MINING_DRILL | MiscType.S_ROCK_CUTTER
                                | MiscType.S_WRECKING_BALL)) != 0) {
                    element.addSPA(ENG);
                } else if ((m.getType().getSubType() &
                        (MiscType.S_DUAL_SAW | MiscType.S_CHAINSAW
                                | MiscType.S_BUZZSAW)) != 0) {
                    element.addSPA(SAW);
                }
            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                element.addSPA(FR);
            } else if (m.getType().hasFlag(MiscType.F_MOBILE_HPG)) {
                element.addSPA(HPG);
            } else if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                element.addSPA(MHQ, (int) m.getTonnage() * 2);
                if (m.getTonnage() >= entity.getWeight() / 20.0) {
                    element.addSPA(RCN);
                }
            } else if (m.getType().hasFlag(MiscType.F_SENSOR_DISPENSER)) {
                element.addSPA(BattleForceSPA.RSD, 1);
                element.addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_LOOKDOWN_RADAR)
                    || m.getType().hasFlag(MiscType.F_RECON_CAMERA)
                    || m.getType().hasFlag(MiscType.F_HIRES_IMAGER)
                    || m.getType().hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || m.getType().hasFlag(MiscType.F_INFRARED_IMAGER)) {
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
            } 
            
            if (m.getType().hasFlag(MiscType.F_SPACE_MINE_DISPENSER) && (entity instanceof Aero)) {
                element.addSPA(MDS, 1);
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
                } else if (((Mech) entity).isIndustrial() && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)
                        && entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE) {
                    element.addSPA(SOA);
                } else if (m.getType().hasFlag(MiscType.F_NULLSIG)
                        || m.getType().hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                    element.addSPA(STL);
                    element.addSPA(ECM);
                } else if (m.getType().hasFlag(MiscType.F_UMU)) {
                    element.addSPA(UMU);
                } else if (m.getType().hasFlag(MiscType.F_BATTLEMECH_NIU)) {
                    element.addSPA(DN);
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
                } else if (m.getType().hasFlag(MiscType.F_AMPHIBIOUS)) {
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
                    element.addSPA(MDS, 1);
                } else if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                    element.addSPA(MSW);
                } else if (m.getType().hasFlag(MiscType.F_MASH)) {
                    element.addSPA(MASH, (int) m.getSize());
                } else if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                    element.addSPA(MFB);
                } else if (m.getType().hasFlag(MiscType.F_COMMAND_CONSOLE)) {
                    element.addSPA(MHQ, 1);
                } else if (m.getType().hasFlag(MiscType.F_OFF_ROAD)) {
                    element.addSPA(ORO);
                } else if (m.getType().hasFlag(MiscType.F_DUNE_BUGGY)) {
                    element.addSPA(DUN);
                } else if (m.getType().hasFlag(MiscType.F_TRACTOR_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_TRAILER_MODIFICATION)) {
                    element.addSPA(HTC);
                }
                //TODO: Fire-resistant chassis mod
            }

        }

        if (entity.isOmni() && ((entity instanceof Mech) || (entity instanceof Tank))) {
            element.addSPA(OMNI);
        }
        
        //TODO: Variable Range targeting is not implemented

        if (!entity.hasPatchworkArmor()) {
            switch (entity.getArmorType(0)) {
            case EquipmentType.T_ARMOR_COMMERCIAL:
            case EquipmentType.T_ARMOR_INDUSTRIAL:
            case EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL:
                element.addSPA(BAR);
                break;
            case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                element.addSPA(CR);
                break;
            case EquipmentType.T_ARMOR_STEALTH:
            case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                element.addSPA(STL);
                break;
            case EquipmentType.T_ARMOR_BA_STEALTH:
            case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
            case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
            case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                element.addSPA(STL);
                break;
            case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                element.addSPA(ABA);
                break;
            case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                element.addSPA(BRA);
                break;
            case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
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

        if (!hasExplosiveComponent) {
            element.addSPA(ENE);
        } else if (entity.isClan() && !(entity instanceof Aero)) {
            element.addSPA(CASE);
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
            if (t instanceof ASFBay) {
                element.addSPA(AT, (int)((ASFBay)t).getCapacity());
                element.addSPA(ATxD, ((ASFBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof CargoBay) {
                element.addSPA(CT, (int)((CargoBay)t).getCapacity());
                element.addSPA(CTxD, ((CargoBay)t).getDoors());
            } else if (t instanceof DockingCollar) {
                element.addSPA(DT, 1);
            } else if (t instanceof InfantryBay) {
                // We do not record number of doors for infantry
                element.addSPA(IT, (int)((InfantryBay)t).getCapacity());
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
            if ((entity.getEntityType() & (Entity.ETYPE_SMALL_CRAFT | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_FIXED_WING_SUPPORT)) == 0) {
                element.addSPA(BOMB, entity.getWeightClass() + 1);
            }
            if ((entity.getEntityType() & (Entity.ETYPE_JUMPSHIP | Entity.ETYPE_CONV_FIGHTER)) == 0) {
                element.addSPA(SPC);
            }
            if (((Aero) entity).isVSTOL()) {
                element.addSPA(VSTOL);
            }
            if (element.isType(AF)) {
                element.addSPA(FUEL, Math.round(((Aero) entity).getFuel() / 20));
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
        }

        if (entity instanceof Mech) {
            element.addSPA(SRCH);
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
        
        if (element.isType(CF)) {
            element.addSPA(ATMO);
        }

    }
    
    private static void addArtillery(WeaponType weapon, AlphaStrikeElement result) {
        BattleForceSPA artType = null;
        switch (weapon.getAmmoType()) {
        case AmmoType.T_ARROW_IV:
            if (weapon.getInternalName().substring(0, 1).equals("C")) {
                artType = BattleForceSPA.ARTAC;
            } else {
                artType = BattleForceSPA.ARTAIS;
            }
            break;
        case AmmoType.T_LONG_TOM:
            artType = BattleForceSPA.ARTLT;
            break;
        case AmmoType.T_SNIPER:
            artType = BattleForceSPA.ARTS;
            break;
        case AmmoType.T_THUMPER:
            artType = BattleForceSPA.ARTT;
            break;
        case AmmoType.T_LONG_TOM_CANNON:
            artType = BattleForceSPA.ARTLTC;
            break;
        case AmmoType.T_SNIPER_CANNON:
            artType = BattleForceSPA.ARTSC;
            break;
        case AmmoType.T_THUMPER_CANNON:
            artType = BattleForceSPA.ARTTC;
            break;
        case AmmoType.T_CRUISE_MISSILE:
            switch(weapon.getRackSize()) {
            case 50:
                artType = BattleForceSPA.ARTCM5;
                break;
            case 70:
                artType = BattleForceSPA.ARTCM7;
                break;
            case 90:
                artType = BattleForceSPA.ARTCM9;
                break;
            case 120:
                artType = BattleForceSPA.ARTCM12;
                break;
            }
        case AmmoType.T_BA_TUBE:
            artType = BattleForceSPA.ARTBA;
            break;
        }
        if (artType != null) {
            result.addSPA(artType, 1);
        }        
    }
    
    /* BattleForce and AlphaStrike calculate infantry damage differently */
    private static double getConvInfantryStandardDamage(int range, Infantry inf) {
        if (inf.getPrimaryWeapon() == null) {
            int baseDamage = (int)Math.ceil(inf.getDamagePerTrooper() * inf.getShootingStrength());
            return Compute.calculateClusterHitTableAmount(7, baseDamage) / 10.0;
        } else {
            return 0;
        }
    }
    
    private static double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba) {
        return weapon.getBattleForceDamage(range, ba.getShootingStrength());        
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
        double totalHeat = entity.getBattleForceTotalHeatGeneration(false);
        int heatCapacity = getHeatCapacity(entity, element);
        if (totalHeat - 4 <= heatCapacity) {
            return;   
        }
        
        System.out.println("Total Heat Medium Range: "+totalHeat);
        System.out.println("Heat Capacity: "+heatCapacity);
        
        double adjustment = heatCapacity / (totalHeat - 4);
        // Determine OV from the medium range damage
        double nonRounded = element.weaponLocations[0].standardDamage.get(RANGE_BAND_MEDIUM);
        element.overheat = Math.min(heatDelta(nonRounded, heatCapacity, totalHeat), 4);
        
        // Determine OVL from long range damage and heat
        if (element.overheat > 0 && element.usesOVL()) {
            double heatLong = getHeatGeneration(entity, false, true);
            System.out.println("Long Range Heat: " + heatLong);
            if (heatLong - 4 > heatCapacity) {
                double nonRoundedL = element.weaponLocations[0].standardDamage.get(RANGE_BAND_LONG);
                if (heatDelta(nonRoundedL, heatCapacity, heatLong) >= 1) {
                    element.addSPA(OVL);
                }
            }
        }

        // Adjust all weapon damages (L/E depending on OVL)
        int maxAdjustmentRange = 1 + (element.hasSPA(OVL) ? RANGE_BAND_EXTREME : RANGE_BAND_MEDIUM); 
        for (int loc = 0; loc < element.weaponLocations.length; loc++) {
            WeaponLocation wloc = element.weaponLocations[loc];
            for (int i = 0; i < Math.min(maxAdjustmentRange, wloc.standardDamage.size()); i++) {
                wloc.standardDamage.set(i, wloc.standardDamage.get(i) * adjustment);
            }
            for (List<Double> damage : wloc.specialDamage.values()) {
                for (int i = 0; i < Math.min(maxAdjustmentRange, damage.size()); i++) {
                    damage.set(i, damage.get(i) * adjustment);
                }
            }
            // IF is long-range fire; only adjust when the unit can overheat even in long range 
            if (element.hasSPA(OVL)) {
                wloc.indirect *= adjustment; 
            }
        }
    }
    
    /** 
     * Returns the delta between the unadjusted rounded and heat-adjusted rounded damage value,
     * according to ASC - Converting Heat Errata v1.2.
     * Use only for determining if a unit has OV or OVL.  
     */
    private static int heatDelta(double damage, int heatCapacity, double heat) {
        int roundedUp = roundUp(damage);
        int roundedUpAdjustedL = roundUp(roundUpToTenth(damage * heatCapacity / (heat - 4)));
        return roundedUp - roundedUpAdjustedL;
    }
    
    /** 
     * Returns the total generated heat (weapons and movement) for a Mech or Aero for 
     * the purpose of finding OV / OVL values.
     * If allowRear is true, rear-facing weapons are included.
     * If onlyLongRange is true, only weapons with an L damage value are included. 
     */
    private static int getHeatGeneration(Entity entity, boolean allowRear, boolean onlyLongRange) {
        if (entity instanceof Mech) {
            return getMechHeatGeneration((Mech)entity, false, true);
        } else {
            return getAeroHeatGeneration((Aero)entity, false, true);
        }
    }
    
    /** 
     * Returns the total generated heat (weapons and movement) for a Mech for 
     * the purpose of finding OV / OVL values.
     * If allowRear is true, rear-facing weapons are included.
     * If onlyLongRange is true, only weapons with an L damage value are included. 
     */
    private static int getMechHeatGeneration(Mech entity, boolean allowRear, boolean onlyLongRange) {
        int totalHeat = 0;

        if (entity.getJumpMP() > 0) {
            totalHeat += entity.getJumpHeat(entity.getJumpMP());
        } else if (!entity.isIndustrial() && entity.hasEngine()) {
            totalHeat += entity.getEngine().getRunHeat(entity);
        }

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                || (allowRear && !mount.isRearMounted())
                || (!allowRear && mount.isRearMounted())
                || (onlyLongRange && weapon.getBattleForceDamage(LONG_RANGE) == 0)) {
                System.out.println(weapon.getName()+" - not in long range: ");
                continue;
            }
            if (weapon.getAmmoType() == AmmoType.T_AC_ROTARY) {
                totalHeat += weapon.getHeat() * 6;
            } else if (weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                    || weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                totalHeat += weapon.getHeat() * 2;
            } else {
                totalHeat += weapon.getHeat();
            }
        }

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)) {
            totalHeat += 10;
        }

        return totalHeat;
    }
    
    /** 
     * Returns the total generated heat (weapons and movement) for an Aero for 
     * the purpose of finding OV / OVL values.
     * If allowRear is true, rear-facing weapons are included.
     * If onlyLongRange is true, only weapons with an L damage value are included. 
     */
    private static int getAeroHeatGeneration(Aero entity, boolean allowRear, boolean onlyLongRange) {
        int totalHeat = 0;

        for (Mounted mount : entity.getWeaponList()) {
            WeaponType weapon = (WeaponType) mount.getType();
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    totalHeat += ((WeaponType) (entity.getEquipment(index).getType())).getHeat();
                }
            }
            if (weapon.hasFlag(WeaponType.F_ONESHOT)
                    || (allowRear && !mount.isRearMounted() && mount.getLocation() != Aero.LOC_AFT)
                    || (!allowRear && (mount.isRearMounted() || mount.getLocation() == Aero.LOC_AFT))
                    || (onlyLongRange && weapon.getLongRange() < LONG_RANGE)) {
                continue;
            }
            totalHeat += weapon.getHeat();
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
            result = ((Aero)entity).getHeatCapacity(false);
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
        if ((entity instanceof Mech) || (entity instanceof Infantry) || (entity instanceof Tank)) {
            int dmgS = element.getDmgS();
            int dmgM = element.getDmgM();
            int dmgL = element.getDmgL();
            double offensiveValue = dmgS + dmgM + dmgM + dmgL;
            offensiveValue += element.isMinimalDmgS() ? 0.5 : 0;
            offensiveValue += element.isMinimalDmgM() ? 1 : 0;
            offensiveValue += element.isMinimalDmgL() ? 0.5 : 0;
            
            if (element.asUnitType == ASUnitType.BM) {
                offensiveValue += 0.5 * element.getSize();
            }
            
            if (element.getOverheat() >= 1) {
                offensiveValue += 1 + 0.5 * (element.getOverheat() - 1);
            }
            
            offensiveValue += getGroundOffensiveSPAMod(entity, element);
            offensiveValue *= getGroundOffensiveBlanketMod(entity, element);
            
            double defensiveValue = 0.125 * getHighestMove(element);
            if (element.movement.keySet().contains("j")) {
                defensiveValue += 0.5;
            }
            defensiveValue += getGroundDefensiveSPAMod(entity, element);
            defensiveValue += getDefensiveDIR(entity, element);
            
            double subTotal = offensiveValue + defensiveValue;
            double bonus = agileBonus(entity, element);
            bonus += c3Bonus(entity, element) ? 0.05 * subTotal : 0;
            bonus -= subTotal * brawlerMalus(entity, element);
            subTotal += bonus;
            subTotal += forceBonus(entity, element);
            
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
            offensiveValue *= getAeroOffensiveBlanketMod(entity, element);
            offensiveValue = roundUpToHalf(offensiveValue);
            
            double defensiveValue = 0.25 * getHighestMove(element);
            defensiveValue += getHighestMove(element) >= 10 ? 1 : 0;
            defensiveValue += getAeroDefensiveSPAMod(entity, element);
            defensiveValue += getAeroDefensiveFactors(entity, element);
            
            double subTotal = offensiveValue + defensiveValue;
            subTotal += forceBonus(entity, element);
            
            return Math.max(1, (int)Math.round(subTotal));
        }
        return 0;
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
        result += element.hasSPA(MTA) ? (int)element.getSPA(MTA) : 0;
        result += element.hasSPA(BTA) ? 0.25 * (int)element.getSPA(BTA) : 0;
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
            result += element.isMinimalIF() ? 0.5 : ((ASDamage)element.getSPA(IF)).damage;
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
    
    private static double getAeroOffensiveBlanketMod(Entity entity, AlphaStrikeElement element) {
        double result = 1;
        result += element.hasSPA(ATAC) ? 0.1 : 0;
        result += element.hasSPA(VRT) ? 0.1 : 0;
        result -= element.hasSPA(BFC) ? 0.1 : 0;
        result -= element.hasSPA(SHLD) ? 0.1 : 0; // So says the AS Companion
        result -= element.hasSPA(DRO) ? 0.1 : 0;
        result -= (element.isType(SV) && !element.hasAnySPAOf(AFC, BFC)) ? 0.2 : 0;
        return result;
    }
   
    private static double getGroundDefensiveSPAMod(Entity entity, AlphaStrikeElement element) {
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
    
    private static double getAeroDefensiveSPAMod(Entity entity, AlphaStrikeElement element) {
        double result = element.hasSPA(PNT) ? (int)element.getSPA(PNT) : 0;
        result += element.hasSPA(STL) ? 2 : 0;
        if (element.hasSPA(RCA)) {
            double armorThird = Math.floor((double)element.getFinalArmor() / 3);
            double barFactor = element.hasSPA(BAR) ? 0.5 : 1;
            result += armorThird * barFactor;
        }
        return result;
    }
    
    private static double getDefensiveDIR(Entity entity, AlphaStrikeElement element) {
        double result = element.getFinalArmor() * getArmorFactorMult(entity, element);
        result += element.getStructure() * getStructureMult(entity, element);
        result *= getDefenseFactor(entity, element);
        result = 0.5 * Math.round(result * 2);
        return result;
    }
    
    private static double getArmorFactorMult(Entity entity, AlphaStrikeElement element) {
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
    
    private static double getStructureMult(Entity entity, AlphaStrikeElement element) {
        if (element.asUnitType == BA || element.asUnitType == CI) {
            return 2;
        } else  if (element.asUnitType == IM || element.hasSPA(BAR)) {
            return 0.5;
        } else  {
            return 1;
        }
    }
    
    private static double getAeroDefensiveFactors(Entity entity, AlphaStrikeElement element) {
        double barFactor = element.hasSPA(BAR) ? 0.5 : 1;
        double thresholdMultiplier = Math.min(1.3 + 0.1 * element.getThreshold(), 1.9);
        double result = thresholdMultiplier * barFactor * element.getFinalArmor();
        result += element.getStructure();
        return result;
    }
    
    private static double getDefenseFactor(Entity entity, AlphaStrikeElement element) {
        double result = 0;
        result += getMovementMod(entity, element);
        result += element.asUnitType == BA ? 1 : 0;
        result += element.asUnitType == PM ? 1 : 0;
        if ((element.asUnitType == CV) && (element.getMovementModes().contains("g") 
                || element.getMovementModes().contains("v"))) {
            result++;
        }
        result += element.hasSPA(STL) ? 1 : 0;
        if ((element.hasSPA(MAS) && ((int)element.getSPA(MAS) > element.getTMM())) 
                || (element.hasSPA(LMAS) && ((int)element.getSPA(LMAS) > element.getTMM()))) {
            result += 3;
        }
        result -= element.hasAnySPAOf(LG, SLG, VLG)  ? 1 : 0;
        if (result <= 2) {
            result = 1 + 0.1 * Math.max(result, 0);
        } else {
            result = 1 + 0.25 * Math.max(result, 0);
        }
        return result;
    }
    
    /** 
     * Returns the movement modifier (for the Point Value DIR calculation only),
     * AlphaStrike Companion Errata v1.4, p.17 
     */
    private static double getMovementMod(Entity entity, AlphaStrikeElement element) {
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
        if ((element.asUnitType == CI || element.asUnitType == BA) && element.isJumpCapable()) {
            result++;
        }
        return result;
    }

    /** Brawler Malus, AlphaStrike Companion Errata v1.4, p.17 */
    private static double brawlerMalus(Entity entity, AlphaStrikeElement element) {
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
    private static double agileBonus(Entity entity, AlphaStrikeElement element) {
        double result = 0;
        if (element.getTMM() >= 2) {
            int dmgS = element.getDmgS();
            int dmgM = element.getDmgM();
            if (dmgM >= 1) {
                result = (element.getTMM() - 1) * dmgM;
            } else if (element.getTMM() >= 3) {
                result = (element.getTMM() - 2) * dmgS;
            }
        }
        return roundToHalf(result);
    }
    
    /** Brawler Malus, AlphaStrike Companion Errata v1.4, p.17 */
    private static boolean c3Bonus(Entity entity, AlphaStrikeElement element) {
        return element.hasAnySPAOf(C3BSM, C3BSS, C3EM, C3I, C3M, C3S, AC3, NC3, NOVA);
    }
    
    private static double forceBonus(Entity entity, AlphaStrikeElement element) {
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
        return element.movement.values().stream().mapToInt(m -> m).max().getAsInt();
    }

}

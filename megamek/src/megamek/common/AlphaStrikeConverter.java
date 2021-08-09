package megamek.common;

import megamek.MegaMek;
import megamek.common.AlphaStrikeElement.ASUnitType;
import megamek.common.loaders.EntityLoadingException;

import static megamek.common.AlphaStrikeElement.ASUnitType.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AlphaStrikeConverter {
    
    public static AlphaStrikeElement convertToAlphaStrike(Entity entity) {
        Entity undamagedEntity = getUndamagedEntity(entity);
        if (undamagedEntity == null) {
            MegaMek.getLogger().error("Could not obtain clean Entity for AlphaStrike conversion.");
            return null;
        }
      //TODO: log an error if entity is a TeleMissile, FighterSquadron or the like
        
        var result = new AlphaStrikeElement();
        result.asUnitType = getType(undamagedEntity);
        result.size = getSize(undamagedEntity);
        result.movement = getMovement(undamagedEntity);
        return result;
    }
    
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
        var result = new LinkedHashMap<String, Integer>();

        if (entity instanceof Infantry) {

            int walkingMP = ((Infantry)entity).getWalkMP(false, true, true);
            int jumpingMP = entity.getJumpMP();
            if (entity instanceof BattleArmor) {
                //            if (entity.getMovementMode().equals(EntityMovementMode.INF_JUMP)) {
                //                walkMP = ((BattleArmor)entity).getJumpMP(true, true, true);
                jumpingMP = ((BattleArmor)entity).getJumpMP(true, true, true);
                //            } else if (entity.getMovementMode().equals(EntityMovementMode.INF_UMU)) {
                //                moves.put("s", entity.getActiveUMUCount() * 2);
                //            } else {
                walkingMP = ((BattleArmor)entity).getWalkMP(true, true, true, true, true);
                //                jumpMove = 0;
                //                moves.put(getMovementModeAsBattleForceString(), getOriginalWalkMP() * 2);
            }
            if (walkingMP > jumpingMP) {
                result.put("f", walkingMP * 2);
            } else {
                result.put("j", jumpingMP * 2);
            }
            addUMUMovement(result, entity);
            return result;

        }

        
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
//        } else if (entity instanceof BattleArmor) {
//            if (entity.getMovementMode().equals(EntityMovementMode.INF_JUMP)) {
//                walkMP = ((BattleArmor)entity).getJumpMP(true, true, true);
//                jumpMove = 0;
////            } else if (entity.getMovementMode().equals(EntityMovementMode.INF_UMU)) {
////                moves.put("s", entity.getActiveUMUCount() * 2);
//            } else {
//                walkMP = ((BattleArmor)entity).getWalkMP(true, true, true, true, true);
//                jumpMove = 0;
////                moves.put(getMovementModeAsBattleForceString(), getOriginalWalkMP() * 2);
//            }
//        } else if (entity instanceof Infantry) {
//            walkMP = Math.max(walkMP, entity.getJumpMP());
//            jumpMove = 0;
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
//        int umu = entity.getAllUMUCount();
//        if (umu > 0) {
//            result.put("s", umu * 2);
//        }
        return result;
    }
    
    private static void addUMUMovement(Map<String, Integer> moves, Entity entity) {
        int umu = entity.getAllUMUCount();
        if (umu > 0) {
            moves.put("s", umu * 2);
        }
    }
    
    //TODO: with a clean unit, the following are obsolete:
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
    
    /** Returns true if the given entity has MASC, regardless of its state (convert as if undamaged). */
    private static boolean hasMechMASC(Entity entity) {
        return (entity instanceof Mech) 
                && entity.getMisc().stream()
                    .map(m -> (MiscType) m.getType())
                    .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && !m.hasSubType(MiscType.S_SUPERCHARGER)));
    }
    
    /** Returns true if the given entity has MASC, regardless of its state (convert as if undamaged). */
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

}

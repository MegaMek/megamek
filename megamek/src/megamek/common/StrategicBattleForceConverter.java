package megamek.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import megamek.MegaMek;
import megamek.common.force.Force;

public final class StrategicBattleForceConverter {

    public static SBFFormation createSbfFormationFromAS(
            List<AlphaStrikeElement> firstUnit, List<AlphaStrikeElement>... furtherUnits) {
        var result = new SBFFormation();
        result.getUnits().add(createSbfUnit(firstUnit));
        for (Collection<AlphaStrikeElement> furtherUnit : furtherUnits) {
            result.getUnits().add(createSbfUnit(furtherUnit));
        }
        // convertUnitsToFormation
        
        return result;
    }
    
    public static SBFFormation createSbfFormationFromAS(List<List<AlphaStrikeElement>> units) {
        var result = new SBFFormation();
        for (Collection<AlphaStrikeElement> furtherUnit : units) {
            result.getUnits().add(createSbfUnit(furtherUnit));
        }
        // convertUnitsToFormation
        
        return result;
    }

    public static SBFFormation createSbfFormationFromTW(List<Entity> firstUnit, List<Entity>... furtherUnits) {
        List<List<AlphaStrikeElement>> asUnits = new ArrayList<>();
        List<AlphaStrikeElement> currentAsUnit = new ArrayList<>(); 
        asUnits.add(currentAsUnit);
        for (Entity entity : firstUnit) {
            currentAsUnit.add(AlphaStrikeConverter.convertToAlphaStrike(entity));
        }
        for (Collection<Entity> furtherUnit : furtherUnits) {
            currentAsUnit = new ArrayList<>(); 
            asUnits.add(currentAsUnit);
            for (Entity entity : furtherUnit) {
                currentAsUnit.add(AlphaStrikeConverter.convertToAlphaStrike(entity));
            }
        }
        return createSbfFormationFromAS(asUnits);
    }

    public static SBFFormation createSbfFormationFromForce(Force force, IGame game) {
        return new SBFFormation();
    }

    public static SBFUnit createSbfUnit(Collection<AlphaStrikeElement> elements) {
        if (!canConvertToSbfUnit(elements)) {
            return null;
        }
        var result = new SBFUnit();
        result.setType(getUnitType(elements));
        result.setSize(getUnitSize(elements));
        result.setArmor(getUnitArmor(elements));
        result.setMovement(getUnitMove(elements));
        result.setJumpMove(getUnitJumpMove(elements));
        result.setTmm(getUnitTMM(elements));
        result.setPointValue(getUnitPointValue(elements));
        return result;
    }

    public static boolean canConvertToSbfUnit(Collection<AlphaStrikeElement> elements) {
        //TODO
        return true;
    }

    private static SBFElementType getUnitType(Collection<AlphaStrikeElement> elements) {
        if (elements.isEmpty()) {
            MegaMek.getLogger().error("Cannot determine SBF Element Type for an empty list of AS Elements.");
            return null;
        }
        int majority = (int) Math.round(2.0 / 3 * elements.size());
        int highestOccurrence = elements.stream()
                .map(e -> SBFElementType.getUnitType(e))
                .distinct()
                .mapToInt(e -> Collections.frequency(elements, e))
                .max().getAsInt();

        if (highestOccurrence < majority) {
            return SBFElementType.MX;
        } else {
            return elements.stream()
                    .map(e -> SBFElementType.getUnitType(e))
                    .distinct()
                    .filter(e -> Collections.frequency(elements, e) == highestOccurrence)
                    .findFirst().get();
        }
    }

    private static int getUnitSize(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(e -> e.getSize()).average().orElse(0));
    }

    private static int getUnitTMM(Collection<AlphaStrikeElement> elements) {
      //TODO
        return 0;

    }

    private static int getUnitMove(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getPrimaryMovementValue).average().orElse(0)) / 2;
    }

    private static int getUnitJumpMove(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getJumpMove).average().orElse(0)) / 4;
    }

    private static int getUnitArmor(Collection<AlphaStrikeElement> elements) {
      int result = (int) elements.stream().mapToInt(AlphaStrikeElement::getFinalArmor).sum() / 3;
      return result;
    }
    
    private static int getUnitPointValue(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(BattleForceElement::getFinalPoints).sum());
    }
    
    private static int getFormationTactics(SBFFormation formation) {
        //TODO
//        return 10 - formation.getMovement() - formation.getSkill();
        return 10;
    }
}

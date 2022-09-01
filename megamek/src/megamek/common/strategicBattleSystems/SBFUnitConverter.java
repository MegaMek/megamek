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
package megamek.common.strategicBattleSystems;

import megamek.common.alphaStrike.*;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;
import static megamek.common.strategicBattleSystems.SBFElementType.*;

public class SBFUnitConverter {

    /**
     *  Returns an SBF Unit formed from the given AS elements and having the given name.
     *  Does not check validity of the conversion.
     */
    public static SBFUnit createSbfUnit(Collection<AlphaStrikeElement> elements, String name,
                                        Collection<AlphaStrikeElement> elementsBaseSkill) {
        var result = new SBFUnit();
        result.setName(name);
        result.setType(getUnitType(elements));
        result.setSize(getUnitSize(elements));
        result.setArmor(calcUnitArmor(elements));
        result.setMovement(calcUnitMove(elements));
        result.setJumpMove(getUnitJumpMove(elements));
        result.setTmm(getUnitTMM(elements, result));
        calcUnitSpecialAbilities(elements, result);
        setMovementMode(result, elements);
        result.setDamage(calcUnitDamage(elements, result));
        result.setSkill(calcUnitSkill(elements, result));
        result.setPointValue(calcUnitPointValue(elementsBaseSkill, result));
        return result;
    }
    private static SBFElementType getUnitType(Collection<AlphaStrikeElement> elements) {
        if (elements.isEmpty()) {
            LogManager.getLogger().error("Cannot determine SBF Element Type for an empty list of AS Elements.");
            return null;
        }
        int majority = (int) Math.round(2.0 / 3 * elements.size());
        List<SBFElementType> types = elements.stream().map(SBFElementType::getUnitType).collect(toList());
        Map<SBFElementType, Long> frequencies = types.stream().collect(groupingBy(Function.identity(), counting()));
        long highestOccurrence = frequencies.values().stream().max(Long::compare).orElse(0L);
        if (highestOccurrence < majority) {
            return SBFElementType.MX;
        } else {
            return types.stream()
                    .filter(e -> Collections.frequency(types, e) == highestOccurrence)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static int getUnitSize(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getSize).average().orElse(0));
    }

    private static int getUnitTMM(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        int avgMove = (int)Math.round(elements.stream()
                .mapToInt(AlphaStrikeElement::getPrimaryMovementValue).average().orElse(0)) / 2;
        int result = getTmmFromMove(avgMove);
        result += (unit.getType() == BA) || (unit.getType() == PM) ? 1 : 0;
        // TODO: movement type?
        return result;
    }

    private static ASDamageVector calcUnitDamage(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        double dmgS = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.S)).sum();
        double dmgM = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.M)).sum();
        double dmgL = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.L)).sum();
        double dmgE = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.E)).sum();
        double artTC = elements.stream().filter(e -> e.hasSUA(ARTTC)).count() * SBFFormation.getSbfArtilleryDamage(ARTTC);
        double artLTC = elements.stream().filter(e -> e.hasSUA(ARTLTC)).count() * SBFFormation.getSbfArtilleryDamage(ARTLTC);
        double artSC = elements.stream().filter(e -> e.hasSUA(ARTSC)).count() * SBFFormation.getSbfArtilleryDamage(ARTSC);
        dmgS += elements.stream().mapToDouble(AlphaStrikeElement::getOV).sum() / 2;
        dmgS += unit.isAnyTypeOf(BA, CI) && unit.hasSPA(AM) ? 1 : 0;
        dmgS += artTC + artLTC + artSC;
        dmgM += elements.stream().filter(e -> e.getStandardDamage().M.damage >= 1).mapToDouble(AlphaStrikeElement::getOV).sum() / 2;
        dmgM += artTC + artLTC + artSC;
        dmgL += elements.stream().filter(e -> e.getStandardDamage().L.damage >= 1).mapToDouble(AlphaStrikeElement::getOV).sum() / 2;
        dmgL += artTC + artLTC;
        dmgE += artTC + artLTC;
        if (unit.getType() == AS) {
            return ASDamageVector.createUpRndDmg(Math.round(dmgS / 3), Math.round(dmgM / 3),
                    Math.round(dmgL / 3), Math.round(dmgE / 3));
        } else {
            return ASDamageVector.createUpRndDmg(Math.round(dmgS / 3), Math.round(dmgM / 3),
                    Math.round(dmgL / 3));
        }
    }

    private static void calcUnitSpecialAbilities(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        addUnitSpasIfAny(elements, unit, PRB, AECM, BHJ2, BHJ3, BH, BT, ECM, HPG, LPRB, LECM, TAG);
        addUnitSpasIfHalf(elements, unit, AMS, ARM, ARS, BAR, BFC, CR, ENG, RCN, RBT, SRCH, SHLD);
        addUnitSpasIfAll(elements, unit, AMP, AM, BHJ, XMEC, MCS, UCS, MEC, PARA, SAW, TRN);
        sumUnitSpas(elements, unit, OMNI, CAR, CK, CT, IT, CRW, DCC, MDS, MASH, RSD, VTM, VTH,
                VTS, AT, DT, MT, PT, ST, SCR, PNT);
        sumUnitSpasDivideBy3(elements, unit, ATAC, BOMB, IF);
        sumUnitArtillery(elements, unit, ARTLT, ARTS, ARTT, ARTBA, ARTCM5, ARTCM7, ARTCM9, ARTCM12);
        calcUnitMhq(elements, unit);
        calcUnitRcn(elements, unit);

        double flkMSum = elements.stream().filter(e -> e.hasSUA(FLK)).map(e -> (ASDamageVector)e.getSUA(FLK)).mapToDouble(dv -> sbfDamage(dv.M)).sum();
        int flkM = (int) Math.round(flkMSum / 3);
        double flkLSum = elements.stream().filter(e -> e.hasSUA(FLK)).map(e -> (ASDamageVector)e.getSUA(FLK)).mapToDouble(dv -> sbfDamage(dv.L)).sum();
        int flkL = (int) Math.round(flkLSum / 3);
        if (flkM + flkL > 0) {
            unit.addSPA(FLK, ASDamageVector.createNormRndDmgNoMin(0, flkM, flkL));
        }

        if (((spaCount(elements, C3M) >= 1) || (spaCount(elements, C3BSM) >= 1))
                && (spaCount(elements, C3M) + spaCount(elements, C3S) + spaCount(elements, C3BSS) >= elements.size() / 2)) {
            unit.addSPA(AC3);
        }

        if (spaCount(elements, C3I) >= elements.size() / 2) {
            unit.addSPA(AC3);
        }

        if (unit.hasSPA(ATAC)) {
            unit.replaceSPA(ATAC, Math.round(1.0d / 3 * (int) unit.getSPA(ATAC)));
        }
    }


    /**
     * Returns the number of the given AlphaStrike elements that have the given spa (regardless of its
     * associated objects)
     */
    private static int spaCount(Collection<AlphaStrikeElement> elements, BattleForceSUA spa) {
        return (int)elements.stream().filter(e -> e.hasSUA(spa)).count();
    }

    private static void addUnitSpasIfAny(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSUA... spas) {
        addUnitSpas(elements, unit, 1, spas);
    }

    private static void addUnitSpasIfHalf(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSUA... spas) {
        addUnitSpas(elements, unit, Math.max(1, elements.size() / 2), spas);
    }

    private static void addUnitSpasIfAll(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSUA... spas) {
        addUnitSpas(elements, unit, elements.size(), spas);
    }

    private static void addUnitSpas(Collection<AlphaStrikeElement> elements, SBFUnit unit,
                                    int threshold, BattleForceSUA[] spas) {
        for (BattleForceSUA spa : spas) {
            if (spaCount(elements, spa) >= threshold) {
                unit.addSPA(spa);
            }
        }
    }

    private static void sumUnitSpas(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSUA... spas) {
        for (BattleForceSUA spa : spas) {
            for (AlphaStrikeElement element : elements) {
                if (element.hasSUA(spa)) {
                    if (element.getSUA(spa) == null) {
                        unit.addSPA(spa, 1);
                    } else if (element.getSUA(spa) instanceof Integer) {
                        unit.addSPA(spa, (Integer) element.getSUA(spa));
                    } else if (element.getSUA(spa) instanceof Double) {
                        unit.addSPA(spa, (Double) element.getSUA(spa));
                    } else if (element.getSUA(spa) instanceof ASDamageVector
                            && ((ASDamageVector)element.getSUA(spa)).rangeBands == 1) {
                        unit.addSPA(spa, sbfDamage(((ASDamageVector) element.getSUA(spa)).S));
                    }
                }
            }
        }
    }

    private static void sumUnitArtillery(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSUA... spas) {
        for (BattleForceSUA spa : spas) {
            double artSum = elements.stream().filter(e -> e.hasSUA(spa)).count() * SBFFormation.getSbfArtilleryDamage(spa);
            int value = (int) Math.round(artSum / 3);
            if (value > 0) {
                unit.addSPA(spa, value);
            }
        }
    }

    private static void sumUnitSpasDivideBy3(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSUA... spas) {
        for (BattleForceSUA spa : spas) {
            for (AlphaStrikeElement element : elements) {
                if (element.hasSUA(spa)) {
                    if (element.getSUA(spa) == null) {
                        unit.addSPA(spa, 1);
                    } else if (element.getSUA(spa) instanceof Integer) {
                        unit.addSPA(spa, (Integer) element.getSUA(spa));
                    } else if (element.getSUA(spa) instanceof Double) {
                        unit.addSPA(spa, (Double) element.getSUA(spa));
                    } else if (element.getSUA(spa) instanceof ASDamageVector
                            && ((ASDamageVector)element.getSUA(spa)).rangeBands == 1) {
                        unit.addSPA(spa, sbfDamage(((ASDamageVector) element.getSUA(spa)).S));
                    }
                }
            }
            if (unit.hasSPA(spa)) {
                int oneThird = (int) Math.round(1.0d / 3 * (double) unit.getSPA(spa));
                if (oneThird == 0) {
                    unit.removeSPA(spa);
                } else {
                    unit.replaceSPA(spa, oneThird);
                }
            }
        }
    }

    private static void calcUnitMhq(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        double mhq = elements.stream().filter(e -> e.hasSUA(MHQ)).mapToInt(e -> Math.max(0, (int) e.getSUA(MHQ) - 1)).sum();
        int oneThird = (int) Math.round(mhq / 3);
        if (oneThird > 0) {
            unit.addSPA(MHQ, oneThird);
        }
    }

    private static void calcUnitRcn(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        if (elements.stream().filter(e -> e.hasSUA(RCN) || isConsideredRcn(e)).count() >= 2) {
            unit.addSPA(RCN);
        }
    }

    private static boolean isConsideredRcn(AlphaStrikeElement el) {
        return (el.isType(ASUnitType.BM) && el.getSize() <= 2 && el.getPrimaryMovementValue() >= 14)
                || (el.isType(ASUnitType.BM, ASUnitType.PM) && el.getJumpMove() >= 12)
                || (el.isGround() && el.getSize() <= 2 && el.getPrimaryMovementValue() >= 18)
                || el.getName().contains("Scout")
                || el.getName().contains("Recon")
                || el.getName().contains("Sensor")
                || el.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS);
    }

    /** Returns the SBF damage of an AlphaStrike damage (0.5 for minimal damage, the AS damage otherwise). */
    private static double sbfDamage(ASDamage asDamage) {
        return asDamage.minimal ? 0.5 : asDamage.damage;
    }

    private static int getTmmFromMove(int move) {
        if (move >= 18) {
            return 5;
        } else if (move >= 10) {
            return 4;
        } else if (move >= 7) {
            return 3;
        } else if (move >= 5) {
            return 2;
        } else if (move >= 3) {
            return 1;
        } else if (move >= 1) {
            return 0;
        } else {
            return -4;
        }
    }

    private static int calcUnitMove(Collection<AlphaStrikeElement> elements) {
        double result = elements.stream().mapToInt(AlphaStrikeElement::getPrimaryMovementValue).average().orElse(0);
        if (elements.stream().anyMatch(AlphaStrikeElement::isInfantry)) {
            int minInfantryMove = elements.stream()
                    .filter(AlphaStrikeElement::isInfantry)
                    .mapToInt(AlphaStrikeElement::getPrimaryMovementValue)
                    .min()
                    .orElse(0);
            result = Math.min(result, minInfantryMove);
        }
        return (int) Math.round(result / 2);
    }

    private static int getUnitJumpMove(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getJumpMove).average().orElse(0) / 4);
    }

    private static void setMovementMode(SBFUnit unit, Collection<AlphaStrikeElement> elements) {
        SBFMoveMode currentMode = new SBFMoveMode("", Integer.MAX_VALUE);
        for (AlphaStrikeElement element : elements) {
            if (restrictionLevel(unit, element).rank < currentMode.rank) {
                currentMode = restrictionLevel(unit, element);
            }
        }
        unit.setMoveType(currentMode.key);
    }

    private static SBFMoveMode restrictionLevel(SBFUnit unit, AlphaStrikeElement element) {
        if (unit.isAerospace() && element.isGround()) {
            if (element.hasSUA(BIM)) {
                return new SBFMoveMode("l", 50);
            } else if (element.hasSUA(LAM)) {
                return new SBFMoveMode("l", 65);
            } else if (element.hasSUA(SOA)) {
                return new SBFMoveMode("k", 10);
            } else {
                return new SBFMoveMode("unknown", 0);
            }
        }
        switch (element.getPrimaryMovementType()) {
            case "":
                if (element.isType(ASUnitType.BM, ASUnitType.PM, ASUnitType.IM)) {
                    return new SBFMoveMode("l", 60);
                } else if (element.isType(ASUnitType.WS)) {
                    return new SBFMoveMode("aw", 31);
                } else if (element.isType(ASUnitType.BA)) {
                    return new SBFMoveMode("l", 50);
                }
                break;
            case "w":
            case "w(b)":
            case "w(m)":
            case "m":
                return new SBFMoveMode("w", 20);
            case "v":
                return new SBFMoveMode("v", 80);
            case "f":
                return new SBFMoveMode("f", 52);
            case "i":
                return new SBFMoveMode("i", 41);
            case "r":
                return new SBFMoveMode("r", 10);
            case "h":
                return new SBFMoveMode("h", 30);
            case "g":
                return new SBFMoveMode("g", 81);
            case "p":
                return new SBFMoveMode("p", 21);
            case "a":
                return new SBFMoveMode("a", 54);
            case "qt":
                return new SBFMoveMode("qt", 63);
            case "qw":
                return new SBFMoveMode("qw", 64);
            case "k":
                return new SBFMoveMode("k", 11);
            case "n":
                return new SBFMoveMode("n", 0);
            case "t":
                return new SBFMoveMode("t", 40);
            case "s":
                if (element.isType(ASUnitType.CV, ASUnitType.SV)) {
                    return new SBFMoveMode("s", 0);
                } else if (element.isType(ASUnitType.BM, ASUnitType.PM)) {
                    return new SBFMoveMode("s", 62);
                } else if (element.isType(ASUnitType.BA)) {
                    return new SBFMoveMode("s", 51);
                }
                break;
            case "j":
                if (element.isType(ASUnitType.BM, ASUnitType.PM)) {
                    return new SBFMoveMode("j", 70);
                } else if (element.isType(ASUnitType.BA)) {
                    return new SBFMoveMode("j", 61);
                } else if (element.isType(ASUnitType.CI)) {
                    return new SBFMoveMode("j", 53);
                }
                break;
        }
        return new SBFMoveMode("unknown", 0);
    }

    private static class SBFMoveMode {
        String key;
        int rank;

        SBFMoveMode(String key, int rank) {
            this.key = key;
            this.rank = rank;
        }
    }

    private static int calcUnitArmor(Collection<AlphaStrikeElement> elements) {
        double result = 0;
        for (AlphaStrikeElement element : elements) {
            result += element.getFullArmor() + element.getFullStructure();
            result += (element.getFullStructure() >= 3 || element.hasAnySUAOf(AMS, CASE)) ? 0.5 : 0;
            result += (element.hasAnySUAOf(ENE, CASEII, CR, RAMS)) ? 1 : 0;
        }
        return (int)Math.round(result / 3);
    }

    private static int calcUnitPointValue(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        int intermediate = (int) Math.round( 1.0d / 3 * elements.stream().mapToInt(AlphaStrikeElement::getPointValue).sum());
        double result = intermediate;
        if (unit.getSkill() > 4) {
            result = (1.0d + (unit.getSkill() - 4) * 0.1) * intermediate;
        } else if (unit.getSkill() < 4) {
            result = (1.0d + (4 - unit.getSkill()) * 0.2) * intermediate;
            result = Math.max(intermediate + 4 - unit.getSkill(), result);
        }
        return (int) Math.round(result);
    }

    private static int calcUnitSkill(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        int skill = (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getSkill).average().orElse(4));
        skill -= unit.hasSPA(DN) ? 1 : 0;
        skill += unit.hasAnySPAOf(BFC, DRO, RBT) ? 1 : 0;
        skill = Math.min(skill, 7);
        skill = Math.max(skill, 0);
        return skill;
    }

}

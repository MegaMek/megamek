/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.alphaStrike;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.Aero;
import megamek.common.Entity;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSPA.*;

public class ASPointValueConverter {

    static int getPointValue(ASConverter.ConversionData conversionData) {
        AlphaStrikeElement element = conversionData.element;
        Entity entity = conversionData.entity;

        if (element.isGround()) {
            double offensiveValue = getPointValueSDamage(element)
                    + getPointValueMDamage(element) * 2
                    + getPointValueLDamage(element);

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
            defensiveValue += getDefensiveDIR(conversionData);
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
            offensiveValue = ASConverter.roundUpToHalf(offensiveValue);

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

    static void adjustPVforSkill(AlphaStrikeElement element) {
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

    private static double getDefensiveDIR(ASConverter.ConversionData conversionData) {
        AlphaStrikeElement element = conversionData.element;

        double result = element.getFinalArmor() * getArmorFactorMult(element);
        result += element.getStructure() * getStructureMult(element);
        result *= getDefenseFactor(conversionData);
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

    private static double getDefenseFactor(ASConverter.ConversionData conversionData) {
        AlphaStrikeElement element = conversionData.element;

        double result = 0;
        double movemod = getMovementMod(conversionData);
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
    private static double getMovementMod(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        AlphaStrikeElement element = conversionData.element;

        int highestNonJumpMod = -1;
        int highestJumpMod = -1;
        for (String mode : element.getMovementModes()) {
            int mod = ASConverter.tmmForMovement(element.getMovement(mode), conversionData);
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

    /**
     * Determines the Brawler Malus, AlphaStrike Companion Errata v1.4, p.17
     * This is 0 if not applicable.
     */
    private static double brawlerMalus(AlphaStrikeElement element) {
        int move = getHighestMove(element);
        if (move >= 2 && !element.hasAnySPAOf(BT, ARTS, C3BSM, C3BSS, C3EM,
                C3I, C3M, C3S, AC3, NC3, NOVA, C3RS, ECM, AECM, ARTAC, ARTAIS, ARTBA, ARTCM12, ARTCM5, ARTCM7,
                ARTCM9, ARTLT, ARTLTC, ARTSC, ARTT, ARTTC)) {
            double dmgS = getPointValueSDamage(element);
            double dmgM = getPointValueMDamage(element);
            double dmgL = getPointValueLDamage(element);

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

    /**
     * Determines the Agile Bonus, AlphaStrike Companion Errata v1.4, p.17
     * This is 0 if not applicable.
     */
    private static double agileBonus(AlphaStrikeElement element) {
        double result = 0;
        if (element.getTMM() >= 2) {
            double dmgS = element.isMinimalDmgS() ? 0.5 : element.getDmgS();
            double dmgM = element.isMinimalDmgM() ? 1 : element.getDmgM();
            if (dmgM > 0) {
                result = (element.getTMM() - 1) * dmgM;
            } else if (element.getTMM() >= 3) {
                result = (element.getTMM() - 2) * dmgS;
            }
        }
        return roundToHalf(result);
    }

    /** C3 Bonus, AlphaStrike Companion Errata v1.4, p.17 */
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

    private static double getPointValueSDamage(AlphaStrikeElement element) {
        return element.isMinimalDmgS() ? 0.5 : element.getDmgS();
    }

    private static double getPointValueMDamage(AlphaStrikeElement element) {
        return element.isMinimalDmgM() ? 1 : element.getDmgM();
    }

    private static double getPointValueLDamage(AlphaStrikeElement element) {
        return element.isMinimalDmgL() ? 0.5 : element.getDmgL();
    }

    private static int getHighestMove(AlphaStrikeElement element) {
        return element.movement.values().stream().mapToInt(m -> m).max().orElse(0);
    }

    static double roundToHalf(double number) {
        return 0.5 * Math.round(number * 2);
    }

    // Make non-instantiable
    private ASPointValueConverter() { }
}

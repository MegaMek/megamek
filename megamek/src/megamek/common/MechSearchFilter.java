/*
* MegaMek -
* Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

import megamek.client.ui.swing.unitSelector.TWAdvancedSearchPanel;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class to perform filtering on units. This class stores a list of
 * constraints and for a given <code>MechSummary</code> it can tell whether
 * that <code>MechSummary</code> meets the constraints or not.
 *
 * @author JSmyrloglou
 * @author Arlith
 */
public class MechSearchFilter {

    public enum BoolOp { AND, OR, NOP }
    public String sStartWalk;
    public String sEndWalk;
    public String sStartJump;
    public String sEndJump;
    public int iArmor;
    public int iOmni;
    public int iMilitary;
    public int iIndustrial;
    public int iNaval;
    public int iSupportVehicle;
    public int iAerospaceFighter;
    public String sStartTankTurrets;
    public String sEndTankTurrets;
    public String sStartLowerArms;
    public String sEndLowerArms;
    public String sStartHands;
    public String sEndHands;
    public int iClanEngine;
    public int iOfficial;
    public int iCanon;
    public int iPatchwork;
    public String source;
    public int iInvalid;
    public int iFailedToLoadEquipment;
    public String sStartTroopSpace;
    public String sEndTroopSpace;
    public String sStartASFBays;
    public String sEndASFBays;
    public String sStartASFDoors;
    public String sEndASFDoors;
    public String sStartASFUnits;
    public String sEndASFUnits;
    public String sStartSmallCraftBays;
    public String sEndSmallCraftBays;
    public String sStartSmallCraftDoors;
    public String sEndSmallCraftDoors;
    public String sStartSmallCraftUnits;
    public String sEndSmallCraftUnits;
    public String sStartMechBays;
    public String sEndMechBays;
    public String sStartMechDoors;
    public String sEndMechDoors;
    public String sStartMechUnits;
    public String sEndMechUnits;
    public String sStartHeavyVehicleBays;
    public String sEndHeavyVehicleBays;
    public String sStartHeavyVehicleDoors;
    public String sEndHeavyVehicleDoors;
    public String sStartHeavyVehicleUnits;
    public String sEndHeavyVehicleUnits;
    public String sStartLightVehicleBays;
    public String sEndLightVehicleBays;
    public String sStartLightVehicleDoors;
    public String sEndLightVehicleDoors;
    public String sStartLightVehicleUnits;
    public String sEndLightVehicleUnits;
    public String sStartProtomechBays;
    public String sEndProtomechBays;
    public String sStartProtomechDoors;
    public String sEndProtomechDoors;
    public String sStartProtomechUnits;
    public String sEndProtomechUnits;
    public String sStartBattleArmorBays;
    public String sEndBattleArmorBays;
    public String sStartBattleArmorDoors;
    public String sEndBattleArmorDoors;
    public String sStartBattleArmorUnits;
    public String sEndBattleArmorUnits;
    public String sStartInfantryBays;
    public String sEndInfantryBays;
    public String sStartInfantryDoors;
    public String sEndInfantryDoors;
    public String sStartInfantryUnits;
    public String sEndInfantryUnits;
    public String sStartSuperHeavyVehicleBays;
    public String sEndSuperHeavyVehicleBays;
    public String sStartSuperHeavyVehicleDoors;
    public String sEndSuperHeavyVehicleDoors;
    public String sStartSuperHeavyVehicleUnits;
    public String sEndSuperHeavyVehicleUnits;
    public String sStartDropshuttleBays;
    public String sEndDropshuttleBays;
    public String sStartDropshuttleDoors;
    public String sEndDropshuttleDoors;
    public String sStartDropshuttleUnits;
    public String sEndDropshuttleUnits;
    public String sStartDockingCollars;
    public String sEndDockingCollars;
    public String sStartBattleArmorHandles;
    public String sEndBattleArmorHandles;
    public String sStartCargoBayUnits;
    public String sEndCargoBayUnits;
    public String sStartNavalRepairFacilities;
    public String sEndNavalRepairFacilities;
    public String sStartYear;
    public String sEndYear;
    public String sStartTons;
    public String sEndTons;
    public String sStartBV;
    public String sEndBV;
    public boolean isDisabled;
    public List<String> engineType = new ArrayList<>();
    public List<String> engineTypeExclude = new ArrayList<>();
    public List<Integer> armorType = new ArrayList<>();
    public List<Integer> armorTypeExclude = new ArrayList<>();
    public List<Integer> internalsType = new ArrayList<>();
    public List<Integer> internalsTypeExclude = new ArrayList<>();

    public List<Integer> cockpitType = new ArrayList<>();
    public List<Integer> cockpitTypeExclude = new ArrayList<>();

    public List<String> techLevel = new ArrayList<>();
    public List<String> techLevelExclude = new ArrayList<>();

    public List<String> techBase = new ArrayList<>();
    public List<String> techBaseExclude = new ArrayList<>();
    public int quirkInclude;
    public int quirkExclude;
    public List<String> quirkType = new ArrayList<>();
    public List<String> quirkTypeExclude = new ArrayList<>();
    public int weaponQuirkInclude;
    public int weaponQuirkExclude;
    public List<String> weaponQuirkType = new ArrayList<>();
    public List<String> weaponQuirkTypeExclude = new ArrayList<>();
    public boolean checkEquipment;
    public int filterMech;
    public int filterBipedMech;
    public int filterProtomech;
    public int filterLAM;
    public int filterTripod;
    public int filterQuad;
    public int filterQuadVee;
    public int filterAero;
    public int filterFixedWingSupport;
    public int filterConvFighter;
    public int filterSmallCraft;
    public int filterDropship;
    public int filterJumpship;
    public int filterWarship;
    public int filterSpaceStation;
    public int filterInfantry;
    public int filterBattleArmor;
    public int filterTank;
    public int filterVTOL;
    public int filterSupportVTOL;
    public int filterGunEmplacement;
    public int filterSupportTank;
    public int filterLargeSupportTank;
    public int filterSuperHeavyTank;

    public ExpressionTree equipmentCriteria;


    public MechSearchFilter()
    {
        isDisabled = true;
        checkEquipment = false;
        equipmentCriteria = new ExpressionTree();
    }

    /**
     * Deep copy constructor. New instantiations of all state variables are
     * created.
     *
     * @param sf The <code>MechSearchFilter</code> to create a copy of.
     */
    public MechSearchFilter(MechSearchFilter sf) {
        if (sf != null) {
            isDisabled = sf.isDisabled;
            checkEquipment = sf.checkEquipment;
            equipmentCriteria = new ExpressionTree(sf.equipmentCriteria);
        } else {
            isDisabled = true;
            checkEquipment = false;
            equipmentCriteria = new ExpressionTree();
        }
    }

    /**
     * Creates an Expressiontree from a collection of tokens.
     */
    public void createFilterExpressionFromTokens(Vector<TWAdvancedSearchPanel.FilterTokens> toks)
            throws FilterParsingException {
        equipmentCriteria = new ExpressionTree();
        if (!toks.isEmpty()) {
            equipmentCriteria.root = createFTFromTokensRecursively(toks.iterator(), null);
            checkEquipment = true;
        } else {
            checkEquipment = false;
        }
    }

    private ExpNode createFTFromTokensRecursively(Iterator<TWAdvancedSearchPanel.FilterTokens> toks,
                                                  ExpNode currNode) {
        // Base case. We're out of tokens, so we're done.
        if (!toks.hasNext()) {
            return currNode;
        }

        TWAdvancedSearchPanel.FilterTokens filterTok = toks.next();

        // Parsing Parenthesis
        if (filterTok instanceof TWAdvancedSearchPanel.ParensFT) {
            if (((TWAdvancedSearchPanel.ParensFT) filterTok).parens.equals("(")) {
                if (currNode == null) {
                    return createFTFromTokensRecursively(toks, null);
                } else {
                    currNode.children.add(createFTFromTokensRecursively(toks, null));
                    return currNode;
                }
            } else if (((TWAdvancedSearchPanel.ParensFT) filterTok).parens.equals(")")) {
                ExpNode nextNode = createFTFromTokensRecursively(toks, null);
                // This right paren is the end of the expression
                if (nextNode == null) {
                    return currNode;
                } else { //Otherwise, we make a new root
                    nextNode.children.add(currNode);
                    return nextNode;
                }
            }
        }

        // Parsing an Operation
        if (filterTok instanceof TWAdvancedSearchPanel.OperationFT) {
            TWAdvancedSearchPanel.OperationFT ft = (TWAdvancedSearchPanel.OperationFT) filterTok;
            ExpNode newNode = new ExpNode();
            // If currNode is null, we came from a right paren
            if (currNode == null) {
                newNode.operation = ft.op;
                ExpNode nextNode = createFTFromTokensRecursively(toks, null);
                if ((nextNode.operation == newNode.operation) || (nextNode.operation == BoolOp.NOP)) {
                    newNode.children.addAll(nextNode.children);
                } else {
                    newNode.children.add(nextNode);
                }
                return newNode;
            // If we are already working on the same operation, keeping adding children to it
            } else if ((currNode.operation == ft.op) || (currNode.operation == BoolOp.NOP)) {
                currNode.operation = ft.op;
                // We're already parsing this operation, continue on
                return createFTFromTokensRecursively(toks, currNode);
            } else { //Mismatching operation
                // In the case of an AND, since AND has a higher precedence,
                //  take the last seen operand, then the results of further
                //  parsing becomes a child of the current node
                if (ft.op == BoolOp.AND) {
                    ExpNode leaf = currNode.children.remove(currNode.children.size() - 1);
                    newNode.operation = BoolOp.AND;
                    newNode.children.add(leaf);
                    ExpNode sibling = createFTFromTokensRecursively(toks, newNode);
                    if (sibling.operation == currNode.operation) {
                        currNode.children.addAll(sibling.children);
                    } else {
                        currNode.children.add(sibling);
                    }
                    return currNode;
                } else { //BoolOp.OR
                    newNode.operation = BoolOp.OR;
                    newNode.children.add(currNode);
                    newNode.children.add(createFTFromTokensRecursively(toks, null));
                    return newNode;
                }
            }
        }

        //Parsing an Operand
        if (filterTok instanceof TWAdvancedSearchPanel.EquipmentFT) {
          if (currNode == null) {
              currNode = new ExpNode();
          }
          TWAdvancedSearchPanel.EquipmentFT ft = (TWAdvancedSearchPanel.EquipmentFT) filterTok;
          ExpNode newChild = new ExpNode(ft.internalName, ft.qty);
          currNode.children.add(newChild);
          return createFTFromTokensRecursively(toks, currNode);

        }

        if (filterTok instanceof TWAdvancedSearchPanel.WeaponClassFT) {
            if (currNode == null) {
                currNode = new ExpNode();
            }

            TWAdvancedSearchPanel.WeaponClassFT ft = (TWAdvancedSearchPanel.WeaponClassFT) filterTok;
            ExpNode newChild = new ExpNode(ft.weaponClass, ft.qty);
            currNode.children.add(newChild);
            return createFTFromTokensRecursively(toks, currNode);
        }
        return null;
    }


    public void clearEquipmentCriteria() {
        checkEquipment = false;
        equipmentCriteria = new ExpressionTree();
    }

    public String getEquipmentExpression() {
        return equipmentCriteria.toString();
    }

    public static boolean isTechMatch(MechSummary mech, int nTechType) {
        return ((nTechType == TechConstants.T_ALL)
                || (nTechType == mech.getType())
                || ((nTechType == TechConstants.T_IS_TW_ALL)
                && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                || (mech.getType() == TechConstants.T_INTRO_BOXSET)))
                || ((nTechType == TechConstants.T_TW_ALL)
                && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                || (mech.getType() <= TechConstants.T_INTRO_BOXSET)
                || (mech.getType() <= TechConstants.T_CLAN_TW)))
                || ((nTechType == TechConstants.T_ALL_IS)
                && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                || (mech.getType() == TechConstants.T_INTRO_BOXSET)
                || (mech.getType() == TechConstants.T_IS_ADVANCED)
                || (mech.getType() == TechConstants.T_IS_EXPERIMENTAL)
                || (mech.getType() == TechConstants.T_IS_UNOFFICIAL)))
                || ((nTechType == TechConstants.T_ALL_CLAN)
                && ((mech.getType() == TechConstants.T_CLAN_TW)
                || (mech.getType() == TechConstants.T_CLAN_ADVANCED)
                || (mech.getType() == TechConstants.T_CLAN_EXPERIMENTAL)
                || (mech.getType() == TechConstants.T_CLAN_UNOFFICIAL))));

    }

    private static int toInt(String s, int i) {
        if (s.isEmpty()) {
            return i;
        }

        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return i;
        }
    }

    private static boolean isBetween(double value, String sStart, String sEnd) {
        if (sStart.isEmpty() && sEnd.isEmpty()) {
            return true;
        }

        int iStart = toInt(sStart, Integer.MIN_VALUE);
        int iEnd = toInt(sEnd, Integer.MAX_VALUE);

        if ((value < iStart) || (value > iEnd)) {
            return false;
        }

        return true;
    }

    private static boolean isMatch(int i, boolean b) {
        if (i == 1) {
            return b;
        } else if (i == 2) {
            return !b;
        }

        return true;
    }

    private static boolean anyMatch(List<String> list, String search) {
        return list.stream().anyMatch(search::contains);
    }
    private static boolean allMatch(List<String> list, String search) {
        return list.stream().allMatch(search::contains);
    }

    private static boolean anyMatch(List<Integer> list, HashSet<Integer> search) {
        return list.stream().anyMatch(search::contains);
    }

    public static boolean isMatch(MechSummary mech, MechSearchFilter f) {
        if (f == null || f.isDisabled) {
            return true;
        }

        // Check armor criteria
        int sel = f.iArmor;
        if (sel > 0) {
            int armor = mech.getTotalArmor();
            int maxArmor = mech.getTotalInternal() * 2 + 3;
            if (sel == 1) {
                if (armor < (maxArmor * .25)) {
                    return false;
                }
            } else if (sel == 2) {
                if (armor < (maxArmor * .5)) {
                    return false;
                }
            } else if (sel == 3) {
                if (armor < (maxArmor * .75)) {
                    return false;
                }
            } else if (sel == 4) {
                if (armor < (maxArmor * .9)) {
                    return false;
                }
            }
        }

        List<String> eqNames = mech.getEquipmentNames();
        List<Integer> qty = mech.getEquipmentQuantities();
        //Evaluate the expression tree, if there's not a match, return false
        if (f.checkEquipment && !f.evaluate(eqNames, qty)) {
            return false;
        }

        if ((!f.source.isEmpty()) && (!mech.getSource().contains(f.source))) {
            return false;
        }

        if (!isMatch(f.iInvalid, mech.getInvalid())) {
            return false;
        }

        if (!isMatch(f.iFailedToLoadEquipment, mech.getFailedToLoadEquipment())) {
            return false;
        }

        if (!isMatch(f.iOmni, mech.getOmni())) {
            return false;
        }

        if (!isMatch(f.iMilitary, mech.getMilitary())) {
            return false;
        }

        if (!isMatch(f.iIndustrial, mech.getIndustrial())) {
            return false;
        }

        if (!isMatch(f.iNaval, mech.getNaval())) {
            return false;
        }

        if (!isMatch(f.iSupportVehicle, mech.getSupportVehicle())) {
            return false;
        }

        if (!isMatch(f.iOfficial, (mech.getMulId() != -1))) {
            return false;
        }

        if (!isMatch(f.iCanon, mech.isCanon())) {
            return false;
        }

        if (!isMatch(f.iPatchwork, mech.isPatchwork())) {
            return false;
        }

        String msg_clan = Messages.getString("Engine.Clan");
        if (!isMatch(f.iClanEngine, mech.getEngineName().contains(msg_clan))) {
            return false;
        }

        //Check walk criteria
        if (!isBetween(mech.getWalkMp(), f.sStartWalk, f.sEndWalk)) {
            return false;
        }

        // Check jump criteria
        if (!isBetween(mech.getJumpMp(), f.sStartJump, f.sEndJump)) {
            return false;
        }

        // Check year criteria
        if (!isBetween(mech.getYear(), f.sStartYear, f.sEndYear)) {
            return false;
        }

        // Check Tonnage criteria
        if (!isBetween((int) mech.getTons(), f.sStartTons, f.sEndTons)) {
            return false;
        }

        // Check BV criteria
        if (!isBetween(mech.getBV(), f.sStartBV, f.sEndBV)) {
            return false;
        }

        if (!isBetween(mech.getTankTurrets(), f.sStartTankTurrets, f.sEndTankTurrets)) {
            return false;
        }

        if (!isBetween(mech.getLowerArms(), f.sStartLowerArms, f.sEndLowerArms)) {
            return false;
        }

        if (!isBetween(mech.getHands(), f.sStartHands, f.sEndHands)) {
            return false;
        }

        if (!isBetween(mech.getTroopCarryingSpace(), f.sStartTroopSpace, f.sEndTroopSpace)) {
            return false;
        }

        if (!isBetween(mech.getASFBays(), f.sStartASFBays, f.sEndASFBays)) {
            return false;
        }

        if (!isBetween(mech.getASFDoors(), f.sStartASFDoors, f.sEndASFDoors)) {
            return false;
        }

        if (!isBetween(mech.getASFUnits(), f.sStartASFUnits, f.sEndASFUnits)) {
            return false;
        }

        if (!isBetween(mech.getSmallCraftBays(), f.sStartSmallCraftBays, f.sEndSmallCraftBays)) {
            return false;
        }

        if (!isBetween(mech.getSmallCraftDoors(), f.sStartSmallCraftDoors, f.sEndSmallCraftDoors)) {
            return false;
        }

        if (!isBetween(mech.getSmallCraftUnits(), f.sStartSmallCraftUnits, f.sEndSmallCraftUnits)) {
            return false;
        }

        if (!isBetween(mech.getMechBays(), f.sStartMechBays, f.sEndMechBays)) {
            return false;
        }

        if (!isBetween(mech.getMechDoors(), f.sStartMechDoors, f.sEndMechDoors)) {
            return false;
        }

        if (!isBetween(mech.getMechUnits(), f.sStartMechUnits, f.sEndMechUnits)) {
            return false;
        }

        if (!isBetween(mech.getHeavyVehicleBays(), f.sStartHeavyVehicleBays, f.sEndHeavyVehicleBays)) {
            return false;
        }

        if (!isBetween(mech.getHeavyVehicleDoors(), f.sStartHeavyVehicleDoors, f.sEndHeavyVehicleDoors)) {
            return false;
        }

        if (!isBetween(mech.getHeavyVehicleUnits(), f.sStartHeavyVehicleUnits, f.sEndHeavyVehicleUnits)) {
            return false;
        }

        if (!isBetween(mech.getLightVehicleBays(), f.sStartLightVehicleBays, f.sEndLightVehicleBays)) {
            return false;
        }

        if (!isBetween(mech.getLightVehicleDoors(), f.sStartLightVehicleDoors, f.sEndLightVehicleDoors)) {
            return false;
        }

        if (!isBetween(mech.getLightVehicleUnits(), f.sStartLightVehicleUnits, f.sEndLightVehicleUnits)) {
            return false;
        }

        if (!isBetween(mech.getProtoMecheBays(), f.sStartProtomechBays, f.sEndProtomechBays)) {
            return false;
        }

        if (!isBetween(mech.getProtoMechDoors(), f.sStartProtomechDoors, f.sEndProtomechDoors)) {
            return false;
        }

        if (!isBetween(mech.getProtoMechUnits(), f.sStartProtomechUnits, f.sEndProtomechUnits)) {
            return false;
        }

        if (!isBetween(mech.getBattleArmorBays(), f.sStartBattleArmorBays, f.sEndBattleArmorBays)) {
            return false;
        }

        if (!isBetween(mech.getBattleArmorDoors(), f.sStartBattleArmorDoors, f.sEndBattleArmorDoors)) {
            return false;
        }

        if (!isBetween(mech.getBattleArmorUnits(), f.sStartBattleArmorUnits, f.sEndBattleArmorUnits)) {
            return false;
        }

        if (!isBetween(mech.getInfantryBays(), f.sStartInfantryBays, f.sEndInfantryBays)) {
            return false;
        }

        if (!isBetween(mech.getInfantryDoors(), f.sStartInfantryDoors, f.sEndInfantryDoors)) {
            return false;
        }

        if (!isBetween(mech.getInfantryUnits(), f.sStartInfantryUnits, f.sEndInfantryUnits)) {
            return false;
        }

        if (!isBetween(mech.getSuperHeavyVehicleBays(), f.sStartSuperHeavyVehicleBays, f.sEndSuperHeavyVehicleBays)) {
            return false;
        }

        if (!isBetween(mech.getSuperHeavyVehicleDoors(), f.sStartSuperHeavyVehicleDoors, f.sEndSuperHeavyVehicleDoors)) {
            return false;
        }

        if (!isBetween(mech.getSuperHeavyVehicleUnits(), f.sStartSuperHeavyVehicleUnits, f.sEndSuperHeavyVehicleUnits)) {
            return false;
        }

        if (!isBetween(mech.getDropshuttleBays(), f.sStartDropshuttleBays, f.sEndDropshuttleBays)) {
            return false;
        }

        if (!isBetween(mech.getDropshuttleDoors(), f.sStartDropshuttleDoors, f.sEndDropshuttleDoors)) {
            return false;
        }

        if (!isBetween(mech.getDropshuttelUnits(), f.sStartDropshuttleUnits, f.sEndDropshuttleUnits)) {
            return false;
        }

        if (!isBetween(mech.getDockingCollars(), f.sStartDockingCollars, f.sEndDockingCollars)) {
            return false;
        }

        if (!isBetween(mech.getBattleArmorHandles(), f.sStartBattleArmorHandles, f.sEndBattleArmorHandles)) {
            return false;
        }

        if (!isBetween(mech.getCargoBayUnits(), f.sStartCargoBayUnits, f.sEndCargoBayUnits)) {
            return false;
        }

        if (!isBetween(mech.getNavalRepairFacilities(), f.sStartNavalRepairFacilities, f.sEndNavalRepairFacilities)) {
            return false;
        }

        if ((!f.internalsType.isEmpty()) && (!f.internalsType.contains(mech.getInternalsType()))) {
            return false;
        }

        if (f.internalsTypeExclude.contains(mech.getInternalsType())) {
            return false;
        }

        if ((!f.cockpitType.isEmpty()) && (!f.cockpitType.contains(mech.getCockpitType()))) {
            return false;
        }

        if (f.cockpitTypeExclude.contains(mech.getCockpitType())) {
            return false;
        }

        if ((!f.armorType.isEmpty()) && (!anyMatch(f.armorType, mech.getArmorType()))) {
            return false;
        }

        if ((!f.armorTypeExclude.isEmpty()) && (anyMatch(f.armorTypeExclude, mech.getArmorType()))) {
            return false;
        }

        if ((!f.engineType.isEmpty()) && (!anyMatch(f.engineType, mech.getEngineName()))) {
            return false;
        }

        if ((!f.engineTypeExclude.isEmpty()) && (anyMatch(f.engineTypeExclude, mech.getEngineName()))) {
            return false;
        }

        if ((!f.techLevel.isEmpty()) && (!anyMatch(f.techLevel, mech.getTechLevel()))) {
            return false;
        }

        if ((!f.techLevelExclude.isEmpty()) && (anyMatch(f.techLevelExclude, mech.getTechLevel()))) {
            return false;
        }

        if ((!f.techBase.isEmpty()) && (!anyMatch(f.techBase, mech.getTechBase()))) {
            return false;
        }

        if ((!f.techBaseExclude.isEmpty()) && (anyMatch(f.techBaseExclude, mech.getTechBase()))) {
            return false;
        }

        if (f.quirkInclude == 0) {
            if ((!f.quirkType.isEmpty()) && (!allMatch(f.quirkType, mech.getQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.quirkType.isEmpty()) && (!anyMatch(f.quirkType, mech.getQuirkNames()))) {
                return false;
            }
        }

        if (f.quirkExclude == 0) {
            if ((!f.quirkTypeExclude.isEmpty()) && (allMatch(f.quirkTypeExclude, mech.getQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.quirkTypeExclude.isEmpty()) && (anyMatch(f.quirkTypeExclude, mech.getQuirkNames()))) {
                return false;
            }
        }

        if (f.weaponQuirkInclude == 0) {
            if ((!f.weaponQuirkType.isEmpty()) && (!allMatch(f.weaponQuirkType, mech.getWeaponQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.weaponQuirkType.isEmpty()) && (!anyMatch(f.weaponQuirkType, mech.getWeaponQuirkNames()))) {
                return false;
            }
        }

        if (f.weaponQuirkInclude == 0) {
            if ((!f.weaponQuirkTypeExclude.isEmpty()) && (allMatch(f.weaponQuirkTypeExclude, mech.getWeaponQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.weaponQuirkTypeExclude.isEmpty()) && (anyMatch(f.weaponQuirkTypeExclude, mech.getWeaponQuirkNames()))) {
                return false;
            }
        }

        long aerospacefighter = 1L << 62;
        long entityType = mech.getEntityType();

        if (mech.getAerospaceFighter()) {
            entityType = entityType | aerospacefighter;
        }

        long entityTypes = 0;
        if (f.filterMech == 1) {
            entityTypes = entityTypes | Entity.ETYPE_MECH;
        }
        if (f.filterBipedMech == 1) {
            entityTypes = entityTypes | Entity.ETYPE_BIPED_MECH;
        }
        if (f.filterProtomech == 1) {
            entityTypes = entityTypes | Entity.ETYPE_PROTOMECH;
        }
        if (f.filterLAM == 1) {
            entityTypes = entityTypes | Entity.ETYPE_LAND_AIR_MECH;
        }
        if (f.filterTripod == 1) {
            entityTypes = entityTypes | Entity.ETYPE_TRIPOD_MECH;
        }
        if (f.filterQuad == 1) {
            entityTypes = entityTypes | Entity.ETYPE_QUAD_MECH;
        }
        if (f.filterAero == 1) {
            entityTypes = entityTypes | Entity.ETYPE_AERO;
        }
        if (f.filterFixedWingSupport == 1) {
            entityTypes = entityTypes | Entity.ETYPE_FIXED_WING_SUPPORT;
        }
        if (f.filterConvFighter == 1) {
            entityTypes = entityTypes | Entity.ETYPE_CONV_FIGHTER;
        }
        if (f.filterSmallCraft == 1) {
            entityTypes = entityTypes | Entity.ETYPE_SMALL_CRAFT;
        }
        if (f.filterDropship == 1) {
            entityTypes = entityTypes | Entity.ETYPE_DROPSHIP;
        }
        if (f.filterJumpship == 1) {
            entityTypes = entityTypes | Entity.ETYPE_JUMPSHIP;
        }
        if (f.filterWarship == 1) {
            entityTypes = entityTypes | Entity.ETYPE_WARSHIP;
        }
        if (f.filterSpaceStation == 1) {
            entityTypes = entityTypes | Entity.ETYPE_SPACE_STATION;
        }
        if (f.filterInfantry == 1) {
            entityTypes = entityTypes | Entity.ETYPE_INFANTRY;
        }
        if (f.filterBattleArmor == 1) {
            entityTypes = entityTypes | Entity.ETYPE_BATTLEARMOR;
        }
        if (f.filterTank == 1) {
            entityTypes = entityTypes | Entity.ETYPE_TANK;
        }
        if (f.filterVTOL == 1) {
            entityTypes = entityTypes | Entity.ETYPE_VTOL;
        }
        if (f.filterSupportVTOL == 1) {
            entityTypes = entityTypes | Entity.ETYPE_SUPPORT_VTOL;
        }
        if (f.filterGunEmplacement == 1) {
            entityTypes = entityTypes | Entity.ETYPE_GUN_EMPLACEMENT;
        }
        if (f.filterSupportTank == 1) {
            entityTypes = entityTypes | Entity.ETYPE_SUPPORT_TANK;
        }
        if (f.filterLargeSupportTank == 1) {
            entityTypes = entityTypes | Entity.ETYPE_LARGE_SUPPORT_TANK;
        }
        if (f.filterSuperHeavyTank == 1) {
            entityTypes = entityTypes | Entity.ETYPE_SUPER_HEAVY_TANK;
        }
        if (f.iAerospaceFighter == 1) {
            entityTypes = entityTypes | aerospacefighter;
        }

        if ((!((entityType & entityTypes) > 0) && (entityTypes != 0))) {
            return false;
        }

        entityTypes = 0;
        if (f.filterMech == 2) {
            entityTypes = entityTypes | Entity.ETYPE_MECH;
        }
        if (f.filterBipedMech == 2) {
            entityTypes = entityTypes | Entity.ETYPE_BIPED_MECH;
        }
        if (f.filterProtomech == 2) {
            entityTypes = entityTypes | Entity.ETYPE_PROTOMECH;
        }
        if (f.filterLAM == 2) {
            entityTypes = entityTypes | Entity.ETYPE_LAND_AIR_MECH;
        }
        if (f.filterTripod == 2) {
            entityTypes = entityTypes | Entity.ETYPE_TRIPOD_MECH;
        }
        if (f.filterQuad == 2) {
            entityTypes = entityTypes | Entity.ETYPE_QUAD_MECH;
        }
        if (f.filterAero == 2) {
            entityTypes = entityTypes | Entity.ETYPE_AERO;
        }
        if (f.filterFixedWingSupport == 2) {
            entityTypes = entityTypes | Entity.ETYPE_FIXED_WING_SUPPORT;
        }
        if (f.filterConvFighter == 2) {
            entityTypes = entityTypes | Entity.ETYPE_CONV_FIGHTER;
        }
        if (f.filterSmallCraft == 2) {
            entityTypes = entityTypes | Entity.ETYPE_SMALL_CRAFT;
        }
        if (f.filterDropship == 2) {
            entityTypes = entityTypes | Entity.ETYPE_DROPSHIP;
        }
        if (f.filterJumpship == 2) {
            entityTypes = entityTypes | Entity.ETYPE_JUMPSHIP;
        }
        if (f.filterWarship == 2) {
            entityTypes = entityTypes | Entity.ETYPE_WARSHIP;
        }
        if (f.filterSpaceStation == 2) {
            entityTypes = entityTypes | Entity.ETYPE_SPACE_STATION;
        }
        if (f.filterInfantry == 2) {
            entityTypes = entityTypes | Entity.ETYPE_INFANTRY;
        }
        if (f.filterBattleArmor == 2) {
            entityTypes = entityTypes | Entity.ETYPE_BATTLEARMOR;
        }
        if (f.filterTank == 2) {
            entityTypes = entityTypes | Entity.ETYPE_TANK;
        }
        if (f.filterVTOL == 2) {
            entityTypes = entityTypes | Entity.ETYPE_VTOL;
        }
        if (f.filterSupportVTOL == 2) {
            entityTypes = entityTypes | Entity.ETYPE_SUPPORT_VTOL;
        }
        if (f.filterGunEmplacement == 2) {
            entityTypes = entityTypes | Entity.ETYPE_GUN_EMPLACEMENT;
        }
        if (f.filterSupportTank == 2) {
            entityTypes = entityTypes | Entity.ETYPE_SUPPORT_TANK;
        }
        if (f.filterLargeSupportTank == 2) {
            entityTypes = entityTypes | Entity.ETYPE_LARGE_SUPPORT_TANK;
        }
        if (f.filterSuperHeavyTank == 2) {
            entityTypes = entityTypes | Entity.ETYPE_SUPER_HEAVY_TANK;
        }
        if (f.iAerospaceFighter == 2) {
            entityTypes = entityTypes | aerospacefighter;
        }

        if (((entityType & entityTypes) > 0) && (entityTypes != 0)) {
            return false;
        }

        return true;
    }

    /**
     * Evalutes the given list of equipment names and quantities against the
     * expression tree in this filter.
     *
     * @param eq    Collection of equipment names
     * @param qty   The number of each piece of equipment
     * @return      True if the provided lists satisfy the expression tree
     */
    public boolean evaluate(List<String> eq, List<Integer> qty) {
        return evaluate(eq, qty, equipmentCriteria.root);
    }

    /**
     * Recursive helper function for evaluating an ExpressionTree on a
     * collection of equipment names and quantities.
     *
     * @param eq    A collection of equipment names
     * @param qty   The number of occurrences of each piece of equipment
     * @param n     The current node in the ExpressionTree
     * @return      True if the tree evaluates successfully, else false
     */
    private boolean evaluate(List<String> eq, List<Integer> qty, ExpNode n) {
        //Base Case: See if any of the equipment matches the leaf node in
        // sufficient quantity
        if (n.children.isEmpty()) {
            if (n.weaponClass != null) {
                // Since weapon classes can match across different types of equipment, we have to sum up
                // all equipment that matches the weaponClass value.
                // First, convert the two separate lists into a map of name->quantity.
                List<Map.Entry<String, Integer>> nameQtyPairs = IntStream.range(0, Math.min(eq.size(), qty.size()))
                    .mapToObj(i -> Map.entry(eq.get(i), qty.get(i)))
                    .collect(Collectors.toList());

                // Now, stream that map, filtering on a match with the WeaponClass, then extract the quantities and sum them up.
                Integer total = nameQtyPairs.stream()
                    .filter(p -> n.weaponClass.matches(p.getKey()))
                    .map(e -> e.getValue())
                    .reduce(0, (a, b) -> a + b);

                // If the requested quantity is 0, then we match if and only if the total number of matching equipment is also 0.
                // Otherwise, we match if the total equals or exceeds the requested amount.
                if (n.qty == 0)
                {
                    return total == 0;
                }
                else
                {
                    return total >= n.qty;
                }

            } else {
                Iterator<String> eqIter = eq.iterator();
                Iterator<Integer> qtyIter = qty.iterator();

                while (eqIter.hasNext()) {
                    String currEq = eqIter.next();

                    int currQty = qtyIter.next();

                    if (null == currEq) {
                        LogManager.getLogger().debug("List<String> currEq is null");
                        return false;
                    }

                    if (null == n) {
                        LogManager.getLogger().debug("ExpNode n is null");
                        return false;
                    }

                    // If the name matches, that means this is the weapon/equipment we are checking for.
                    // If the requested quantity is greater than 0, then the unit quantity must equal or exceed it.
                    // However, if the requested quantity is 0, then the simple fact that the weapon/equipment matches
                    // means that the unit isn't a match for the filter, as it has a weapon/equipment that is required to
                    // NOT be there.
                    if (currEq.equals(n.name) && n.qty > 0 && currQty >= n.qty) {
                        return true;
                    } else if (currEq.equals(n.name) && n.qty == 0) {
                        return false;
                    }
                    
                }

                // If we reach this point. It means that the MechSummary didn't have a weapon/equipment that matched the leaf node. 
                // If the leaf quantity is 0, that means that the mech is a match. If the leaf quantity is non-zero, that means the mech isn't
                // a match.
                if (n.qty == 0) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        // Otherwise, recurse on all the children and either AND the results
        // or OR them, based upon the operation in this node
        boolean retVal = n.operation == BoolOp.AND;
        // If we set the proper default starting value of retVal, we can take
        // advantage of logical short-circuiting.
        Iterator<ExpNode> childIter = n.children.iterator();
        while (childIter.hasNext()) {
            ExpNode child = childIter.next();
            if (n.operation == BoolOp.AND) {
                retVal = retVal && evaluate(eq, qty, child);
            } else {
                retVal = retVal || evaluate(eq, qty, child);
            }
        }
        return retVal;
    }


    /**
     * This class allows to create a tree where the leaf nodes contain names
     * and quantities of pieces of equipment while the non-leaf nodes contain
     * boolean operations (AND and OR).
     *
     * @author Arlith
     */
    public class ExpressionTree {
        private ExpNode root;

        public ExpressionTree() {
            root = new ExpNode();
        }

        /**
         * Deep copy constructor. New instantiations of all state variables
         * are created.
         *
         * @param et The <code>ExpressionTree</code> to create a copy of.
         */
        public ExpressionTree(ExpressionTree et) {
            root = new ExpNode(et.root);
        }

        public ExpressionTree(String n, int q) {
            root = new ExpNode(n, q);
        }

        @Override
        public String toString() {
            return root.children.isEmpty() ? "" : root.toString();
        }
    }

    public class ExpNode {

        public ExpNode parent;
        public BoolOp operation;
        public String name;
        public TWAdvancedSearchPanel.WeaponClass weaponClass;
        public int qty;
        public List<ExpNode> children;

        public ExpNode() {
            operation = BoolOp.NOP;
            children = new LinkedList<>();
        }

        /**
         * Deep copy constructor. New instantiations of all state variables
         * are created.
         *
         * @param e  The <code>ExpressionTree</code> to create a copy of.
         */
        public ExpNode(ExpNode e) {
            parent = null;
            this.operation = e.operation;
            this.qty = e.qty;
            //if (e.name != null) {
                this.name = e.name;
           // }
           this.weaponClass = e.weaponClass;
            Iterator<ExpNode> nodeIter = e.children.iterator();
            this.children = new LinkedList<>();
            while (nodeIter.hasNext()) {
                children.add(new ExpNode(nodeIter.next()));
            }
        }

        public ExpNode(String n, int q) {
            parent = null;
            name = n;
            weaponClass = null;
            qty = q;
            operation = BoolOp.NOP;
            children = new LinkedList<>();
        }

        public ExpNode(TWAdvancedSearchPanel.WeaponClass n, int q) {
            parent = null;
            name = null;
            weaponClass = n;
            qty = q;
            operation = BoolOp.NOP;
            children = new LinkedList<>();
        }

        @Override
        public String toString() {
            // Base Case: this is a leaf-node
            if (children.isEmpty()) {
                if (name != null) {
                    if (qty == 1) {
                        return qty + " " + name;
                    } else {
                        return qty + " " + name + "s";
                    }
                }
                else if (weaponClass != null) {
                    if (qty == 1) {
                        return qty + " " + weaponClass.toString();
                    } else {
                        return qty + " " + weaponClass.toString() + "s";
                    }
                }
            }

            // Recursive Case
            StringBuilder result = new StringBuilder("(");
            Iterator<ExpNode> nodeIter = children.iterator();
            int count = 0;
            while (nodeIter.hasNext()) {
                ExpNode child = nodeIter.next();
                if (operation == BoolOp.AND) {
                    if (count == children.size() - 1) {
                        result.append(child.toString());
                    } else {
                        result.append(child.toString() + " AND ");
                    }
                } else if (count == children.size() - 1) {
                    result.append(child.toString());
                } else {
                    result.append(child.toString() + " OR ");
                }
                count++;
            }
            result.append(" )");
            return result.toString();
        }

    }

    public class FilterParsingException extends Exception {
        public String msg;

        private static final long serialVersionUID = 1L;

        FilterParsingException(String m) {
            msg = m;
        }
    }
}

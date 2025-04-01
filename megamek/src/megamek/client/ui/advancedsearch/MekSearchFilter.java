/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.advancedsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import megamek.common.Entity;
import megamek.common.MekSummary;
import megamek.common.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * Class to perform filtering on units. This class stores a list of constraints and for a given <code>MekSummary</code>
 * it can tell whether that <code>MekSummary</code> meets the constraints or not.
 *
 * @author JSmyrloglou
 * @author Arlith
 */
public class MekSearchFilter {
    private static final MMLogger LOGGER = MMLogger.create(MekSearchFilter.class);

    enum BoolOp {
        AND, OR, NOP
    }

    public String sStartWalk;
    public String sEndWalk;
    public String sStartJump;
    public String sEndJump;
    public int iArmor;
    public int iOmni;
    public int iMilitary;
    public int iIndustrial;
    public int iMountedInfantry;
    public int iWaterOnly;
    public int iDoomedOnGround;
    public int iDoomedInAtmosphere;
    public int iDoomedInSpace;
    public int iDoomedInExtremeTemp;
    public int iDoomedInVacuum;
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
    public String mulid;
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
    public String sStartMekBays;
    public String sEndMekBays;
    public String sStartMekDoors;
    public String sEndMekDoors;
    public String sStartMekUnits;
    public String sEndMekUnits;
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
    public String sStartProtomekBays;
    public String sEndProtomekBays;
    public String sStartProtomekDoors;
    public String sEndProtomekDoors;
    public String sStartProtomekUnits;
    public String sEndProtomekUnits;
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
    public List<Integer> engineType = new ArrayList<>();
    public List<Integer> engineTypeExclude = new ArrayList<>();
    public List<Integer> gyroType = new ArrayList<>();
    public List<Integer> gyroTypeExclude = new ArrayList<>();
    public List<Integer> armorType = new ArrayList<>();
    public List<Integer> armorTypeExclude = new ArrayList<>();
    public List<Integer> internalsType = new ArrayList<>();
    public List<Integer> internalsTypeExclude = new ArrayList<>();
    public List<String> movemodes = new ArrayList<>();
    public List<String> movemodeExclude = new ArrayList<>();

    public List<Integer> cockpitType = new ArrayList<>();
    public List<Integer> cockpitTypeExclude = new ArrayList<>();

    public List<Integer> techLevel = new ArrayList<>();
    public List<Integer> techLevelExclude = new ArrayList<>();

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
    public int filterMek;
    public int filterBipedMek;
    public int filterProtomek;
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

    public MekSearchFilter() {
        isDisabled = true;
        checkEquipment = false;
        equipmentCriteria = new ExpressionTree();
    }

    /**
     * Deep copy constructor. New instantiations of all state variables are created.
     *
     * @param sf The <code>MekSearchFilter</code> to create a copy of.
     */
    public MekSearchFilter(MekSearchFilter sf) {
        if (sf == null) {
            isDisabled = true;
            checkEquipment = false;
            equipmentCriteria = new ExpressionTree();
            return;
        }

        sStartWalk = sf.sStartWalk;
        sEndWalk = sf.sEndWalk;
        sStartJump = sf.sStartJump;
        sEndJump = sf.sEndJump;

        iArmor = sf.iArmor;
        iOmni = sf.iOmni;
        iMilitary = sf.iMilitary;
        iIndustrial = sf.iIndustrial;
        iMountedInfantry = sf.iMountedInfantry;
        iWaterOnly = sf.iWaterOnly;
        iDoomedOnGround = sf.iDoomedOnGround;
        iDoomedInAtmosphere = sf.iDoomedInAtmosphere;
        iDoomedInSpace = sf.iDoomedInSpace;
        iDoomedInExtremeTemp = sf.iDoomedInExtremeTemp;
        iDoomedInVacuum = sf.iDoomedInVacuum;
        iSupportVehicle = sf.iSupportVehicle;
        iAerospaceFighter = sf.iAerospaceFighter;
        sStartTankTurrets = sf.sStartTankTurrets;
        sEndTankTurrets = sf.sEndTankTurrets;
        sStartLowerArms = sf.sStartLowerArms;
        sEndLowerArms = sf.sEndLowerArms;
        sStartHands = sf.sStartHands;
        sEndHands = sf.sEndHands;
        iClanEngine = sf.iClanEngine;
        iOfficial = sf.iOfficial;
        iCanon = sf.iCanon;
        iPatchwork = sf.iPatchwork;
        source = sf.source;
        mulid = sf.mulid;
        iInvalid = sf.iInvalid;
        iFailedToLoadEquipment = sf.iFailedToLoadEquipment;
        sStartTroopSpace = sf.sStartTroopSpace;
        sEndTroopSpace = sf.sEndTroopSpace;
        sStartASFBays = sf.sStartASFBays;
        sEndASFBays = sf.sEndASFBays;
        sStartASFDoors = sf.sStartASFDoors;
        sEndASFDoors = sf.sEndASFDoors;
        sStartASFUnits = sf.sStartASFUnits;
        sEndASFUnits = sf.sEndASFUnits;
        sStartSmallCraftBays = sf.sStartSmallCraftBays;
        sEndSmallCraftBays = sf.sEndSmallCraftBays;
        sStartSmallCraftDoors = sf.sStartSmallCraftDoors;
        sEndSmallCraftDoors = sf.sEndSmallCraftDoors;
        sStartSmallCraftUnits = sf.sStartSmallCraftUnits;
        sEndSmallCraftUnits = sf.sEndSmallCraftUnits;
        sStartMekBays = sf.sStartMekBays;
        sEndMekBays = sf.sEndMekBays;
        sStartMekDoors = sf.sStartMekDoors;
        sEndMekDoors = sf.sEndMekDoors;
        sStartMekUnits = sf.sStartMekUnits;
        sEndMekUnits = sf.sEndMekUnits;
        sStartHeavyVehicleBays = sf.sStartHeavyVehicleBays;
        sEndHeavyVehicleBays = sf.sEndHeavyVehicleBays;
        sStartHeavyVehicleDoors = sf.sStartHeavyVehicleDoors;
        sEndHeavyVehicleDoors = sf.sEndHeavyVehicleDoors;
        sStartHeavyVehicleUnits = sf.sStartHeavyVehicleUnits;
        sEndHeavyVehicleUnits = sf.sEndHeavyVehicleUnits;
        sStartLightVehicleBays = sf.sStartLightVehicleBays;
        sEndLightVehicleBays = sf.sEndLightVehicleBays;
        sStartLightVehicleDoors = sf.sStartLightVehicleDoors;
        sEndLightVehicleDoors = sf.sEndLightVehicleDoors;
        sStartLightVehicleUnits = sf.sStartLightVehicleUnits;
        sEndLightVehicleUnits = sf.sEndLightVehicleUnits;
        sStartProtomekBays = sf.sStartProtomekBays;
        sEndProtomekBays = sf.sEndProtomekBays;
        sStartProtomekDoors = sf.sStartProtomekDoors;
        sEndProtomekDoors = sf.sEndProtomekDoors;
        sStartProtomekUnits = sf.sStartProtomekUnits;
        sEndProtomekUnits = sf.sEndProtomekUnits;
        sStartBattleArmorBays = sf.sStartBattleArmorBays;
        sEndBattleArmorBays = sf.sEndBattleArmorBays;
        sStartBattleArmorDoors = sf.sStartBattleArmorDoors;
        sEndBattleArmorDoors = sf.sEndBattleArmorDoors;
        sStartBattleArmorUnits = sf.sStartBattleArmorUnits;
        sEndBattleArmorUnits = sf.sEndBattleArmorUnits;
        sStartInfantryBays = sf.sStartInfantryBays;
        sEndInfantryBays = sf.sEndInfantryBays;
        sStartInfantryDoors = sf.sStartInfantryDoors;
        sEndInfantryDoors = sf.sEndInfantryDoors;
        sStartInfantryUnits = sf.sStartInfantryUnits;
        sEndInfantryUnits = sf.sEndInfantryUnits;
        sStartSuperHeavyVehicleBays = sf.sStartSuperHeavyVehicleBays;
        sEndSuperHeavyVehicleBays = sf.sEndSuperHeavyVehicleBays;
        sStartSuperHeavyVehicleDoors = sf.sStartSuperHeavyVehicleDoors;
        sEndSuperHeavyVehicleDoors = sf.sEndSuperHeavyVehicleDoors;
        sStartSuperHeavyVehicleUnits = sf.sStartSuperHeavyVehicleUnits;
        sEndSuperHeavyVehicleUnits = sf.sEndSuperHeavyVehicleUnits;
        sStartDropshuttleBays = sf.sStartDropshuttleBays;
        sEndDropshuttleBays = sf.sEndDropshuttleBays;
        sStartDropshuttleDoors = sf.sStartDropshuttleDoors;
        sEndDropshuttleDoors = sf.sEndDropshuttleDoors;
        sStartDropshuttleUnits = sf.sStartDropshuttleUnits;
        sEndDropshuttleUnits = sf.sEndDropshuttleUnits;
        sStartDockingCollars = sf.sStartDockingCollars;
        sEndDockingCollars = sf.sEndDockingCollars;
        sStartBattleArmorHandles = sf.sStartBattleArmorHandles;
        sEndBattleArmorHandles = sf.sEndBattleArmorHandles;
        sStartCargoBayUnits = sf.sStartCargoBayUnits;
        sEndCargoBayUnits = sf.sEndCargoBayUnits;
        sStartNavalRepairFacilities = sf.sStartNavalRepairFacilities;
        sEndNavalRepairFacilities = sf.sEndNavalRepairFacilities;
        sStartYear = sf.sStartYear;
        sEndYear = sf.sEndYear;
        sStartTons = sf.sStartTons;
        sEndTons = sf.sEndTons;
        sStartBV = sf.sStartBV;
        sEndBV = sf.sEndBV;
        isDisabled = sf.isDisabled;
        engineType = List.copyOf(sf.engineType);
        engineTypeExclude = List.copyOf(sf.engineTypeExclude);
        gyroType = List.copyOf(sf.gyroType);
        gyroTypeExclude = List.copyOf(sf.gyroTypeExclude);
        armorType = List.copyOf(sf.armorType);
        armorTypeExclude = List.copyOf(sf.armorTypeExclude);
        internalsType = List.copyOf(sf.internalsType);
        internalsTypeExclude = List.copyOf(sf.internalsTypeExclude);
        movemodes = List.copyOf(sf.movemodes);
        movemodeExclude = List.copyOf(sf.movemodeExclude);

        cockpitType = List.copyOf(sf.cockpitType);
        cockpitTypeExclude = List.copyOf(sf.cockpitTypeExclude);

        techLevel = List.copyOf(sf.techLevel);
        techLevelExclude = List.copyOf(sf.techLevelExclude);

        techBase = List.copyOf(sf.techBase);
        techBaseExclude = List.copyOf(sf.techBaseExclude);
        quirkInclude = sf.quirkInclude;
        quirkExclude = sf.quirkExclude;
        quirkType = List.copyOf(sf.quirkType);
        quirkTypeExclude = List.copyOf(sf.quirkTypeExclude);
        weaponQuirkInclude = sf.weaponQuirkInclude;
        weaponQuirkExclude = sf.weaponQuirkExclude;
        weaponQuirkType = List.copyOf(sf.weaponQuirkType);
        weaponQuirkTypeExclude = List.copyOf(sf.weaponQuirkTypeExclude);
        checkEquipment = sf.checkEquipment;
        filterMek = sf.filterMek;
        filterBipedMek = sf.filterBipedMek;
        filterProtomek = sf.filterProtomek;
        filterLAM = sf.filterLAM;
        filterTripod = sf.filterTripod;
        filterQuad = sf.filterQuad;
        filterQuadVee = sf.filterQuadVee;
        filterAero = sf.filterAero;
        filterFixedWingSupport = sf.filterFixedWingSupport;
        filterConvFighter = sf.filterConvFighter;
        filterSmallCraft = sf.filterSmallCraft;
        filterDropship = sf.filterDropship;
        filterJumpship = sf.filterJumpship;
        filterWarship = sf.filterWarship;
        filterSpaceStation = sf.filterSpaceStation;
        filterInfantry = sf.filterInfantry;
        filterBattleArmor = sf.filterBattleArmor;
        filterTank = sf.filterTank;
        filterVTOL = sf.filterVTOL;
        filterSupportVTOL = sf.filterSupportVTOL;
        filterGunEmplacement = sf.filterGunEmplacement;
        filterSupportTank = sf.filterSupportTank;
        filterLargeSupportTank = sf.filterLargeSupportTank;
        filterSuperHeavyTank = sf.filterSuperHeavyTank;
        equipmentCriteria = new ExpressionTree(sf.equipmentCriteria);
    }

    /**
     * Creates an {@link ExpressionTree} from a collection of tokens.
     */
    public void createFilterExpressionFromTokens(List<FilterToken> tokens) throws FilterParsingException {
        equipmentCriteria = new ExpressionTree();
        if (!tokens.isEmpty()) {
            equipmentCriteria.root = createFTFromTokensRecursively(tokens.iterator(), null);
            checkEquipment = true;
        } else {
            checkEquipment = false;
        }
    }

    private ExpNode createFTFromTokensRecursively(Iterator<FilterToken> tokens, ExpNode currNode) {
        // Base case. We're out of tokens, so we're done.
        if (!tokens.hasNext()) {
            return currNode;
        }

        FilterToken filterTok = tokens.next();

        // Parsing Parenthesis
        if (filterTok instanceof ParensFT parensFT) {
            if (parensFT.parens.equals("(")) {
                if (currNode == null) {
                    return createFTFromTokensRecursively(tokens, null);
                } else {
                    currNode.children.add(createFTFromTokensRecursively(tokens, null));
                    return currNode;
                }
            } else if (parensFT.parens.equals(")")) {
                ExpNode nextNode = createFTFromTokensRecursively(tokens, null);
                // This right paren is the end of the expression
                if (nextNode == null) {
                    return currNode;
                } else { // Otherwise, we make a new root
                    nextNode.children.add(currNode);
                    return nextNode;
                }
            }
        }

        // Parsing an Operation
        if (filterTok instanceof OperatorFT operatorFT) {
            ExpNode newNode = new ExpNode();
            // If currNode is null, we came from a right parent
            if (currNode == null) {
                newNode.operation = operatorFT.op;
                ExpNode nextNode = createFTFromTokensRecursively(tokens, null);

                if (nextNode == null) {
                    return null;
                }

                if ((nextNode.operation == newNode.operation) || (nextNode.operation == BoolOp.NOP)) {
                    newNode.children.addAll(nextNode.children);
                } else {
                    newNode.children.add(nextNode);
                }

                return newNode;
                // If we are already working on the same operation, keeping adding children to it
            } else if ((currNode.operation == operatorFT.op) || (currNode.operation == BoolOp.NOP)) {
                currNode.operation = operatorFT.op;
                // We're already parsing this operation, continue on
                return createFTFromTokensRecursively(tokens, currNode);
            } else { // Mismatching operation
                // In the case of an AND, since AND has a higher precedence,
                // take the last seen operand, then the results of further
                // parsing becomes a child of the current node
                if (operatorFT.op == BoolOp.AND) {
                    ExpNode leaf = currNode.children.remove(currNode.children.size() - 1);
                    newNode.operation = BoolOp.AND;
                    newNode.children.add(leaf);
                    ExpNode sibling = createFTFromTokensRecursively(tokens, newNode);

                    if (sibling == null) {
                        return currNode;
                    }

                    if (sibling.operation == currNode.operation) {
                        currNode.children.addAll(sibling.children);
                    } else {
                        currNode.children.add(sibling);
                    }

                    return currNode;
                } else { // BoolOp.OR
                    newNode.operation = BoolOp.OR;
                    newNode.children.add(currNode);
                    newNode.children.add(createFTFromTokensRecursively(tokens, null));
                    return newNode;
                }
            }
        }

        // Parsing an Operand
        if (filterTok instanceof EquipmentTypeFT ft) {
            if (currNode == null) {
                currNode = new ExpNode();
            }
            ExpNode newChild = new ExpNode(ft.internalName, ft.qty, ft.atleast);
            currNode.children.add(newChild);
            return createFTFromTokensRecursively(tokens, currNode);

        }

        if (filterTok instanceof WeaponClassFT ft) {
            if (currNode == null) {
                currNode = new ExpNode();
            }

            ExpNode newChild = new ExpNode(ft.equipmentClass, ft.qty, ft.atleast);
            currNode.children.add(newChild);
            return createFTFromTokensRecursively(tokens, currNode);
        }
        return null;
    }

    public String getEquipmentExpression() {
        return equipmentCriteria.toString();
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

    private static boolean anyMatch(List<Integer> list, int search) {
        return list.stream().anyMatch(i -> i == search);
    }

    private static boolean anyMatch(List<Integer> list, HashSet<Integer> search) {
        return list.stream().anyMatch(search::contains);
    }

    public static boolean isMatch(MekSummary mek, MekSearchFilter f) {
        if (f == null || f.isDisabled) {
            return true;
        }

        // Check armor criteria
        int sel = f.iArmor;
        if (sel > 0) {
            int armor = mek.getTotalArmor();
            int maxArmor = mek.getTotalInternal() * 2 + 3;
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

        List<String> eqNames = mek.getEquipmentNames();
        List<Integer> qty = mek.getEquipmentQuantities();
        // Evaluate the expression tree, if there's not a match, return false
        if (f.checkEquipment && !f.evaluate(eqNames, qty)) {
            return false;
        }

        if (!f.source.isEmpty() && !f.findTokenized(mek.getSource(), f.source)) {
            return false;
        }

        if ((!f.mulid.isEmpty()) && (mek.getMulId() != StringUtil.toInt(f.mulid, -2))) {
            return false;
        }

        if (!isMatch(f.iInvalid, mek.getInvalid())) {
            return false;
        }

        if (!isMatch(f.iFailedToLoadEquipment, mek.getFailedToLoadEquipment())) {
            return false;
        }

        if (!isMatch(f.iOmni, mek.getOmni())) {
            return false;
        }

        if (!isMatch(f.iMilitary, mek.getMilitary())) {
            return false;
        }

        if (!isMatch(f.iIndustrial, mek.isIndustrialMek())) {
            return false;
        }

        if (!isMatch(f.iMountedInfantry, mek.getMountedInfantry())) {
            return false;
        }

        if (!isMatch(f.iWaterOnly, (mek.hasWaterMovement() && !mek.hasAirMovement() && !mek.hasGroundMovement()))) {
            return false;
        }

        if (!isMatch(f.iDoomedOnGround, mek.isDoomedOnGround())) {
            return false;
        }

        if (!isMatch(f.iDoomedInAtmosphere, mek.isDoomedInAtmosphere())) {
            return false;
        }

        if (!isMatch(f.iDoomedInSpace, mek.isDoomedInSpace())) {
            return false;
        }

        if (!isMatch(f.iDoomedInExtremeTemp, mek.isDoomedInExtremeTemp())) {
            return false;
        }

        if (!isMatch(f.iDoomedInVacuum, mek.isDoomedInVacuum())) {
            return false;
        }

        if (!isMatch(f.iSupportVehicle, mek.isSupportVehicle())) {
            return false;
        }

        if (!isMatch(f.iOfficial, (mek.getMulId() != -1))) {
            return false;
        }

        if (!isMatch(f.iCanon, mek.isCanon())) {
            return false;
        }

        if (!isMatch(f.iPatchwork, mek.isPatchwork())) {
            return false;
        }

        String msg_clan = Messages.getString("Engine.Clan");
        if (!isMatch(f.iClanEngine, mek.getEngineName().contains(msg_clan))) {
            return false;
        }

        // Check walk criteria
        if (!StringUtil.isBetween(mek.getWalkMp(), f.sStartWalk, f.sEndWalk)) {
            return false;
        }

        // Check jump criteria
        if (!StringUtil.isBetween(mek.getJumpMp(), f.sStartJump, f.sEndJump)) {
            return false;
        }

        // Check year criteria
        if (!StringUtil.isBetween(mek.getYear(), f.sStartYear, f.sEndYear)) {
            return false;
        }

        // Check Tonnage criteria
        if (!StringUtil.isBetween((int) mek.getTons(), f.sStartTons, f.sEndTons)) {
            return false;
        }

        // Check BV criteria
        if (!StringUtil.isBetween(mek.getBV(), f.sStartBV, f.sEndBV)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getTankTurrets(), f.sStartTankTurrets, f.sEndTankTurrets)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getLowerArms(), f.sStartLowerArms, f.sEndLowerArms)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getHands(), f.sStartHands, f.sEndHands)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getTroopCarryingSpace(), f.sStartTroopSpace, f.sEndTroopSpace)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getASFBays(), f.sStartASFBays, f.sEndASFBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getASFDoors(), f.sStartASFDoors, f.sEndASFDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getASFUnits(), f.sStartASFUnits, f.sEndASFUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getSmallCraftBays(), f.sStartSmallCraftBays, f.sEndSmallCraftBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getSmallCraftDoors(), f.sStartSmallCraftDoors, f.sEndSmallCraftDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getSmallCraftUnits(), f.sStartSmallCraftUnits, f.sEndSmallCraftUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getMekBays(), f.sStartMekBays, f.sEndMekBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getMekDoors(), f.sStartMekDoors, f.sEndMekDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getMekUnits(), f.sStartMekUnits, f.sEndMekUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getHeavyVehicleBays(), f.sStartHeavyVehicleBays, f.sEndHeavyVehicleBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getHeavyVehicleDoors(), f.sStartHeavyVehicleDoors, f.sEndHeavyVehicleDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getHeavyVehicleUnits(), f.sStartHeavyVehicleUnits, f.sEndHeavyVehicleUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getLightVehicleBays(), f.sStartLightVehicleBays, f.sEndLightVehicleBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getLightVehicleDoors(), f.sStartLightVehicleDoors, f.sEndLightVehicleDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getLightVehicleUnits(), f.sStartLightVehicleUnits, f.sEndLightVehicleUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getProtoMekBays(), f.sStartProtomekBays, f.sEndProtomekBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getProtoMekDoors(), f.sStartProtomekDoors, f.sEndProtomekDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getProtoMekUnits(), f.sStartProtomekUnits, f.sEndProtomekUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getBattleArmorBays(), f.sStartBattleArmorBays, f.sEndBattleArmorBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getBattleArmorDoors(), f.sStartBattleArmorDoors, f.sEndBattleArmorDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getBattleArmorUnits(), f.sStartBattleArmorUnits, f.sEndBattleArmorUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getInfantryBays(), f.sStartInfantryBays, f.sEndInfantryBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getInfantryDoors(), f.sStartInfantryDoors, f.sEndInfantryDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getInfantryUnits(), f.sStartInfantryUnits, f.sEndInfantryUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getSuperHeavyVehicleBays(),
              f.sStartSuperHeavyVehicleBays,
              f.sEndSuperHeavyVehicleBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getSuperHeavyVehicleDoors(),
              f.sStartSuperHeavyVehicleDoors,
              f.sEndSuperHeavyVehicleDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getSuperHeavyVehicleUnits(),
              f.sStartSuperHeavyVehicleUnits,
              f.sEndSuperHeavyVehicleUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getDropshuttleBays(), f.sStartDropshuttleBays, f.sEndDropshuttleBays)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getDropshuttleDoors(), f.sStartDropshuttleDoors, f.sEndDropshuttleDoors)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getDropshuttelUnits(), f.sStartDropshuttleUnits, f.sEndDropshuttleUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getDockingCollars(), f.sStartDockingCollars, f.sEndDockingCollars)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getBattleArmorHandles(), f.sStartBattleArmorHandles, f.sEndBattleArmorHandles)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getCargoBayUnits(), f.sStartCargoBayUnits, f.sEndCargoBayUnits)) {
            return false;
        }

        if (!StringUtil.isBetween(mek.getNavalRepairFacilities(),
              f.sStartNavalRepairFacilities,
              f.sEndNavalRepairFacilities)) {
            return false;
        }

        if ((!f.internalsType.isEmpty()) && (!f.internalsType.contains(mek.getInternalsType()))) {
            return false;
        }

        if (f.internalsTypeExclude.contains(mek.getInternalsType())) {
            return false;
        }

        if ((!f.cockpitType.isEmpty()) && (!f.cockpitType.contains(mek.getCockpitType()))) {
            return false;
        }

        if (f.cockpitTypeExclude.contains(mek.getCockpitType())) {
            return false;
        }

        if ((!f.armorType.isEmpty()) && (!anyMatch(f.armorType, mek.getArmorType()))) {
            return false;
        }

        if ((!f.armorTypeExclude.isEmpty()) && (anyMatch(f.armorTypeExclude, mek.getArmorType()))) {
            return false;
        }

        if ((!f.engineType.isEmpty()) && (!anyMatch(f.engineType, mek.getEngineType()))) {
            return false;
        }

        if ((!f.engineTypeExclude.isEmpty()) && (anyMatch(f.engineTypeExclude, mek.getEngineType()))) {
            return false;
        }

        if ((!f.gyroType.isEmpty()) && (!anyMatch(f.gyroType, mek.getGyroType()))) {
            return false;
        }

        if ((!f.gyroTypeExclude.isEmpty()) && (anyMatch(f.gyroTypeExclude, mek.getGyroType()))) {
            return false;
        }

        if ((!f.techLevel.isEmpty()) && (!anyMatch(f.techLevel, mek.getTechLevelCode()))) {
            return false;
        }

        if ((!f.techLevelExclude.isEmpty()) && (anyMatch(f.techLevelExclude, mek.getTechLevelCode()))) {
            return false;
        }

        if ((!f.techBase.isEmpty()) && (!anyMatch(f.techBase, mek.getTechBase()))) {
            return false;
        }

        if ((!f.techBaseExclude.isEmpty()) && (anyMatch(f.techBaseExclude, mek.getTechBase()))) {
            return false;
        }

        if ((!f.movemodes.isEmpty()) && (!anyMatch(f.movemodes, mek.getMoveMode().toString()))) {
            return false;
        }

        if ((!f.movemodeExclude.isEmpty()) && (anyMatch(f.movemodeExclude, mek.getMoveMode().toString()))) {
            return false;
        }

        if (f.quirkInclude == 0) {
            if ((!f.quirkType.isEmpty()) && (!allMatch(f.quirkType, mek.getQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.quirkType.isEmpty()) && (!anyMatch(f.quirkType, mek.getQuirkNames()))) {
                return false;
            }
        }

        if (f.quirkExclude == 0) {
            if ((!f.quirkTypeExclude.isEmpty()) && (allMatch(f.quirkTypeExclude, mek.getQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.quirkTypeExclude.isEmpty()) && (anyMatch(f.quirkTypeExclude, mek.getQuirkNames()))) {
                return false;
            }
        }

        if (f.weaponQuirkInclude == 0) {
            if ((!f.weaponQuirkType.isEmpty()) && (!allMatch(f.weaponQuirkType, mek.getWeaponQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.weaponQuirkType.isEmpty()) && (!anyMatch(f.weaponQuirkType, mek.getWeaponQuirkNames()))) {
                return false;
            }
        }

        if (f.weaponQuirkInclude == 0) {
            if ((!f.weaponQuirkTypeExclude.isEmpty()) &&
                      (allMatch(f.weaponQuirkTypeExclude, mek.getWeaponQuirkNames()))) {
                return false;
            }
        } else {
            if ((!f.weaponQuirkTypeExclude.isEmpty()) &&
                      (anyMatch(f.weaponQuirkTypeExclude, mek.getWeaponQuirkNames()))) {
                return false;
            }
        }

        long entityType = mek.getEntityType();

        long entityTypes = 0;

        if (f.filterMek == 1) {
            entityTypes = entityTypes | Entity.ETYPE_MEK;
        }
        if (f.filterBipedMek == 1) {
            entityTypes = entityTypes | Entity.ETYPE_BIPED_MEK;
        }
        if (f.filterProtomek == 1) {
            entityTypes = entityTypes | Entity.ETYPE_PROTOMEK;
        }
        if (f.filterLAM == 1) {
            entityTypes = entityTypes | Entity.ETYPE_LAND_AIR_MEK;
        }
        if (f.filterTripod == 1) {
            entityTypes = entityTypes | Entity.ETYPE_TRIPOD_MEK;
        }
        if (f.filterQuad == 1) {
            entityTypes = entityTypes | Entity.ETYPE_QUAD_MEK;
        }
        if (f.filterQuadVee == 1) {
            entityTypes = entityTypes | Entity.ETYPE_QUADVEE;
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
            entityTypes = entityTypes | Entity.ETYPE_AEROSPACEFIGHTER;
        }

        if ((!((entityType & entityTypes) > 0) && (entityTypes != 0))) {
            return false;
        }

        entityTypes = 0;

        if (f.filterMek == 2) {
            entityTypes = entityTypes | Entity.ETYPE_MEK;
        }
        if (f.filterBipedMek == 2) {
            entityTypes = entityTypes | Entity.ETYPE_BIPED_MEK;
        }
        if (f.filterProtomek == 2) {
            entityTypes = entityTypes | Entity.ETYPE_PROTOMEK;
        }
        if (f.filterLAM == 2) {
            entityTypes = entityTypes | Entity.ETYPE_LAND_AIR_MEK;
        }
        if (f.filterTripod == 2) {
            entityTypes = entityTypes | Entity.ETYPE_TRIPOD_MEK;
        }
        if (f.filterQuad == 2) {
            entityTypes = entityTypes | Entity.ETYPE_QUAD_MEK;
        }
        if (f.filterQuadVee == 2) {
            entityTypes = entityTypes | Entity.ETYPE_QUADVEE;
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
            entityTypes = entityTypes | Entity.ETYPE_AEROSPACEFIGHTER;
        }

        return ((entityType & entityTypes) <= 0) || (entityTypes == 0);
    }

    /**
     * Evaluates the given list of equipment names and quantities against the expression tree in this filter.
     *
     * @param eq  Collection of equipment names
     * @param qty The number of each piece of equipment
     *
     * @return True if the provided lists satisfy the expression tree
     */
    public boolean evaluate(List<String> eq, List<Integer> qty) {
        return evaluate(eq, qty, equipmentCriteria.root);
    }

    /**
     * Recursive helper function for evaluating an ExpressionTree on a collection of equipment names and quantities.
     *
     * @param eq  A collection of equipment names
     * @param qty The number of occurrences of each piece of equipment
     * @param n   The current node in the ExpressionTree
     *
     * @return True if the tree evaluates successfully, else false
     */
    private boolean evaluate(List<String> eq, List<Integer> qty, ExpNode n) {
        // Base Case: See if any of the equipment matches the leaf node in sufficient quantity
        if (n.children.isEmpty()) {
            if (n.equipmentClass != null) {
                // Since weapon classes can match across different types of equipment, we have to sum up all
                // equipment that matches the weaponClass value. First, convert the two separate lists into a map of
                // name->quantity.
                List<Map.Entry<String, Integer>> nameQtyPairs = IntStream.range(0, Math.min(eq.size(), qty.size()))
                                                                      .mapToObj(i -> Map.entry(eq.get(i), qty.get(i)))
                                                                      .toList();

                // Now, stream that map, filtering on a match with the WeaponClass, then extract the quantities and
                // sum them up.
                int total = nameQtyPairs.stream()
                                  .filter(p -> n.equipmentClass.matches(p.getKey()))
                                  .map(Map.Entry::getValue)
                                  .reduce(0, Integer::sum);

                // If the requested quantity is 0, then we match if and only if the total number of matching
                // equipment is also 0. Otherwise, we match if the total equals or exceeds the requested amount.
                if (n.atleast) {
                    return total >= n.qty;
                } else {
                    return total < n.qty;
                }

            } else {
                Iterator<String> eqIter = eq.iterator();
                Iterator<Integer> qtyIter = qty.iterator();

                while (eqIter.hasNext()) {
                    String currEq = eqIter.next();

                    int currQty = qtyIter.next();

                    if (null == currEq) {
                        LOGGER.debug("List<String> currEq is null");
                        return false;
                    }

                    // If the name matches, that means this is the weapon/equipment we are checking for. If the
                    // requested quantity is greater than 0, then the unit quantity must equal or exceed it. However,
                    // if the requested quantity is 0, then the simple fact that the weapon/equipment matches means
                    // that the unit isn't a match for the filter, as it has a weapon/equipment that is required to
                    // NOT be there.
                    if (currEq.equals(n.name) && n.atleast && (currQty >= n.qty)) {
                        return true;
                    } else if (currEq.equals(n.name) && !n.atleast && (currQty >= n.qty)) {
                        return false;
                    }
                }

                // If we reach this point. It means that the MekSummary didn't have a weapon/equipment that matched
                // the leaf node. If the leaf quantity is 0, that means that the mek is a match. If the leaf quantity
                // is non-zero, that means the mek isn't a match.
                return !n.atleast;
            }
        }
        // Otherwise, recurse on all the children and either AND the results or <code>OR</code> them, based upon the
        // operation in this node
        boolean retVal = n.operation == BoolOp.AND;
        // If we set the proper default starting value of retVal, we can take advantage of logical short-circuiting.
        for (ExpNode child : n.children) {
            if (n.operation == BoolOp.AND) {
                retVal = retVal && evaluate(eq, qty, child);
            } else {
                retVal = retVal || evaluate(eq, qty, child);
            }
        }
        return retVal;
    }

    /**
     * This class allows to create a tree where the leaf nodes contain names and quantities of pieces of equipment while
     * the non-leaf nodes contain boolean operations (<code>AND</code> and <code>OR</code>).
     *
     * @author Arlith
     */
    public static class ExpressionTree {
        private ExpNode root;

        public ExpressionTree() {
            root = new ExpNode();
        }

        /**
         * Deep copy constructor. New instantiations of all state variables are created.
         *
         * @param et The <code>ExpressionTree</code> to create a copy of.
         */
        public ExpressionTree(ExpressionTree et) {
            root = new ExpNode(et.root);
        }

        @Override
        public String toString() {
            return root.children.isEmpty() ? "" : root.toString();
        }
    }

    public static class ExpNode {

        public ExpNode parent;
        public BoolOp operation;
        public String name;
        public AdvancedSearchEquipmentClass equipmentClass;
        public int qty;
        public List<ExpNode> children;
        public boolean atleast;

        public ExpNode() {
            operation = BoolOp.NOP;
            children = new LinkedList<>();
        }

        /**
         * Deep copy constructor. New instantiations of all state variables are created.
         *
         * @param e The <code>ExpressionTree</code> to create a copy of.
         */
        public ExpNode(ExpNode e) {
            parent = null;
            operation = e.operation;
            qty = e.qty;
            // if (e.name != null) {
            name = e.name;
            // }
            equipmentClass = e.equipmentClass;
            Iterator<ExpNode> nodeIter = e.children.iterator();
            children = new LinkedList<>();
            while (nodeIter.hasNext()) {
                children.add(new ExpNode(nodeIter.next()));
            }
        }

        public ExpNode(String n, int q, boolean atLeast) {
            parent = null;
            name = n;
            equipmentClass = null;
            qty = q;
            operation = BoolOp.NOP;
            children = new LinkedList<>();
            this.atleast = atLeast;
        }

        public ExpNode(AdvancedSearchEquipmentClass n, int q, boolean atLeast) {
            parent = null;
            name = null;
            equipmentClass = n;
            qty = q;
            operation = BoolOp.NOP;
            children = new LinkedList<>();
            this.atleast = atLeast;
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
                } else if (equipmentClass != null) {
                    if (qty == 1) {
                        return qty + " " + equipmentClass;
                    } else {
                        return qty + " " + equipmentClass + "s";
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
                        result.append(child.toString()).append(" AND ");
                    }
                } else if (count == children.size() - 1) {
                    result.append(child.toString());
                } else {
                    result.append(child.toString()).append(" OR ");
                }
                count++;
            }
            result.append(" )");
            return result.toString();
        }

    }

    public static class FilterParsingException extends Exception {
        public String msg;

        FilterParsingException(String m) {
            msg = m;
        }
    }

    /**
     * Returns true if the given searchTarget contains all the tokens (separated by space) given in the searchTokens
     * String. Comparisons are done ignoring case. Returns false when any of the strings is null or the search tokens
     * are empty.
     *
     * @param searchTarget The String that may contain the search tokens, such as "Shrapnel #9"
     * @param searchTokens The String that contains the search tokens, such as "shra 9"
     *
     * @return True if all search tokens are contained in the searchTarget
     */
    private boolean findTokenized(@Nullable String searchTarget, @Nullable String searchTokens) {
        if (searchTarget == null || searchTokens == null || searchTokens.isBlank()) {
            return false;
        } else {
            String searchTargetLowerCase = searchTarget.toLowerCase(Locale.ROOT);
            String[] tokens = searchTokens.toLowerCase(Locale.ROOT).split(" ");
            return Arrays.stream(tokens).allMatch(searchTargetLowerCase::contains);
        }
    }
}

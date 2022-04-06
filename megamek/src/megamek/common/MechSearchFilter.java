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

import megamek.client.ui.swing.AdvancedSearchDialog;
import org.apache.logging.log4j.LogManager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

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
    public String sWalk;
    public String sJump;
    public int iWalk;
    public int iJump;
    public int iArmor;
    public String sStartYear;
    public String sEndYear;
    public boolean isDisabled;

    public boolean checkArmorType;
    public int armorType;
    public boolean checkInternalsType;
    public int internalsType;
    public boolean checkCockpitType;
    public int cockpitType;

    public boolean checkEquipment;
    public ExpressionTree equipmentCriteria;


    public MechSearchFilter()
    {
        isDisabled = true;
        checkArmorType = checkInternalsType = checkCockpitType = false;
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
    public void createFilterExpressionFromTokens(Vector<AdvancedSearchDialog.FilterTokens> toks)
            throws FilterParsingException {
        equipmentCriteria = new ExpressionTree();
        if (!toks.isEmpty()) {
            equipmentCriteria.root = createFTFromTokensRecursively(toks.iterator(), null);
            checkEquipment = true;
        } else {
            checkEquipment = false;
        }
    }

    private ExpNode createFTFromTokensRecursively(Iterator<AdvancedSearchDialog.FilterTokens> toks,
                                                  ExpNode currNode) {
        // Base case. We're out of tokens, so we're done.
        if (!toks.hasNext()) {
            return currNode;
        }

        AdvancedSearchDialog.FilterTokens filterTok = toks.next();

        // Parsing Parenthesis
        if (filterTok instanceof AdvancedSearchDialog.ParensFT) {
            if (((AdvancedSearchDialog.ParensFT) filterTok).parens.equals("(")) {
                if (currNode == null) {
                    return createFTFromTokensRecursively(toks, null);
                } else {
                    currNode.children.add(createFTFromTokensRecursively(toks, null));
                    return currNode;
                }
            } else if (((AdvancedSearchDialog.ParensFT) filterTok).parens.equals(")")) {
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
        if (filterTok instanceof AdvancedSearchDialog.OperationFT) {
            AdvancedSearchDialog.OperationFT ft = (AdvancedSearchDialog.OperationFT) filterTok;
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
        if (filterTok instanceof AdvancedSearchDialog.EquipmentFT) {
          if (currNode == null) {
              currNode = new ExpNode();
          }
          AdvancedSearchDialog.EquipmentFT ft = (AdvancedSearchDialog.EquipmentFT) filterTok;
          ExpNode newChild = new ExpNode(ft.internalName, ft.qty);
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

    public static boolean isMatch(MechSummary mech, MechSearchFilter f) {
        if (f == null || f.isDisabled) {
            return true;
        }

        //Check walk criteria
        int walk = -1;
        try {
            walk = Integer.parseInt(f.sWalk);
        } catch (NumberFormatException ne) {
            //ignore
        }
        if (walk > -1) {
            if (f.iWalk == 0) { // at least
                if (mech.getWalkMp() < walk) {
                    return false;
                }
            } else if (f.iWalk == 1) { // equal to
                if (walk != mech.getWalkMp()) {
                    return false;
                }
            } else if (f.iWalk == 2) { // not more than
                if (mech.getWalkMp() > walk) {
                    return false;
                }
            }
        }

        //Check jump criteria
        int jump = -1;
        try {
            jump = Integer.parseInt(f.sJump);
        } catch (Exception ignored) {

        }

        if (jump > -1) {
            if (f.iJump == 0) { // at least
                if (mech.getJumpMp() < jump) {
                    return false;
                }
            } else if (f.iJump == 1) { // equal to
                if (jump != mech.getJumpMp()) {
                    return false;
                }
            } else if (f.iJump == 2) { // not more than
                if (mech.getJumpMp() > jump) {
                    return false;
                }
            }
        }

        if (f.checkInternalsType) {
            if (f.internalsType != mech.getInternalsType())
                return false;
        }

        if (f.checkArmorType) {
            if (!mech.getArmorType().contains(f.armorType))
                return false;
        }

        if (f.checkCockpitType) {
            if (f.cockpitType != mech.getCockpitType())
                return false;
        }

        //Check armor criteria
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

        // Check year criteria
        int startYear = Integer.MIN_VALUE;
        int endYear = Integer.MAX_VALUE;
        try {
            startYear = Integer.parseInt(f.sStartYear);
        } catch (Exception ignored) {

        }

        try {
            endYear = Integer.parseInt(f.sEndYear);
        } catch (Exception ignored) {

        }

        if ((mech.getYear() < startYear) || (mech.getYear() > endYear)) {
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
            Iterator<String> eqIter = eq.iterator();
            Iterator<Integer> qtyIter = qty.iterator();
            while (eqIter.hasNext()) {
                String currEq = eqIter.next();

                int currQty = qtyIter.next();

                if (null == currEq) {
                    LogManager.getLogger().debug("List<String> currEq is null");
                }

                if (null == n) {
                    LogManager.getLogger().debug("ExpNode n is null");
                }

                if (currEq.equals(n.name) && currQty >= n.qty)
                    return true;
            }
            return false;
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
            if (e.name != null) {
                this.name = e.name;
            }
            Iterator<ExpNode> nodeIter = e.children.iterator();
            this.children = new LinkedList<>();
            while (nodeIter.hasNext())
                children.add(new ExpNode(nodeIter.next()));
        }

        public ExpNode(String n, int q) {
            parent = null;
            name = n;
            qty = q;
            operation = BoolOp.NOP;
            children = new LinkedList<>();
        }

        @Override
        public String toString() {
            // Base Case: this is a leaf-node
            if (children.isEmpty()) {
                if (qty == 1) {
                    return qty + " " + name;
                } else {
                    return qty + " " + name + "s";
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

/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import megamek.client.ui.swing.AdvancedSearchDialog;


/**
 * Class to perform filtering on units.  This class stores a list of
 * constraints and for a given <code>MechSummary</code> it can tell whether
 * that <code>MechSummary</codes> meets the constraints or not.
 * 
 * @author JSmyrloglou
 * @author Arlith
 */
public class MechSearchFilter {

    public enum BoolOp { AND, OR, NOP };
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
     * Deep copy constructor.  New instantiations of all state variables are 
     * created.
     * 
     * @param sf  The <code>MechSearchFilter</code> to create a copy of.
     */
    public MechSearchFilter(MechSearchFilter sf)
    {
        
        if (sf != null)
        {            
            isDisabled = sf.isDisabled;
            checkEquipment = sf.checkEquipment;
            equipmentCriteria = new ExpressionTree(sf.equipmentCriteria);
        }
        else{
            isDisabled = true;
            checkEquipment = false;
            equipmentCriteria = new ExpressionTree();  
        }
            
    }    
    
    /**
     * Creates an Expressiontree from a collection of tokens.
     */
    public void createFilterExpressionFromTokens1(Vector<AdvancedSearchDialog.FilterTokens> toks)  throws FilterParsingException{
            equipmentCriteria = new ExpressionTree();
            equipmentCriteria.root = createFTFromTokensRecursively(toks.iterator());
            checkEquipment = true;
    }
    
    /**
     * Binary tree implementation.
     * 
     * @param toks
     * @return
     * @throws FilterParsingException
     */
    private ExpNode createFTFromTokensRecursively(Iterator<AdvancedSearchDialog.FilterTokens> toks) 
        throws FilterParsingException{
        
        // Base case.  We're out of tokens, so we're done.
        if (!toks.hasNext())
          return null;
        
        AdvancedSearchDialog.FilterTokens filterTok = toks.next();
        
        // Parsing Parenthesis
        if (filterTok instanceof AdvancedSearchDialog.ParensFT) {
            if (((AdvancedSearchDialog.ParensFT) filterTok).parens.equals("(")){
                return createFTFromTokensRecursively(toks);
            } else if (((AdvancedSearchDialog.ParensFT) filterTok).parens
                    .equals(")")){
                return null;
            }
        }
        
        //Parsing an Operation
        if (filterTok instanceof AdvancedSearchDialog.OperationFT){
            //Setup this Expression Node
            ExpNode newNode = new ExpNode();
            newNode.operation = 
                ((AdvancedSearchDialog.OperationFT)filterTok).op;
            
            //Make sure we can create a child, then set it up
            if (!toks.hasNext()){
                throw new FilterParsingException(
                "Filter Expression Parsing Error: unexpected end of expression");
            }            
            AdvancedSearchDialog.FilterTokens nextTok = toks.next();
            if (!(nextTok instanceof AdvancedSearchDialog.EquipmentFT)){
                throw new FilterParsingException(
                "Filter Expression Parsing Error: unexpected operator");
            }
            AdvancedSearchDialog.EquipmentFT eqTok = 
                (AdvancedSearchDialog.EquipmentFT)nextTok;
            ExpNode childNode = new ExpNode(eqTok.name,eqTok.qty);            
            newNode.children.add(childNode);
            
            //See if there's anything left to the expression
            ExpNode parentNode = createFTFromTokensRecursively(toks);
            if (parentNode != null){ 
                newNode.parent = parentNode;
                parentNode.children.add(newNode);
                return parentNode;
            }
            return newNode;
        }
        
        //Parsing Equipment
        if (filterTok instanceof AdvancedSearchDialog.EquipmentFT){
            ExpNode childNode = 
              new ExpNode(((AdvancedSearchDialog.EquipmentFT)filterTok).name,
                          ((AdvancedSearchDialog.EquipmentFT)filterTok).qty);
            ExpNode parentNode = createFTFromTokensRecursively(toks);
            //This node should be an operation           
            if (parentNode != null && parentNode.operation == BoolOp.NOP){
                throw new FilterParsingException("Filter Expression Parsing Error: unexpected operand");
            }
            if (parentNode != null){
                parentNode.children.add(childNode);
                childNode.parent = parentNode;
                return parentNode;
            }
            return childNode;            
        }
        return null;
    }
    
    /**
     * Creates an Expressiontree from a collection of tokens.
     */
    public void createFilterExpressionFromTokens(Vector<AdvancedSearchDialog.FilterTokens> toks)  throws FilterParsingException{
            equipmentCriteria = new ExpressionTree();
            equipmentCriteria.root = createFTFromTokensRecursively2(toks.iterator(),null);
            checkEquipment = true;
    }    
    
    private ExpNode createFTFromTokensRecursively2(
            Iterator<AdvancedSearchDialog.FilterTokens> toks, 
            ExpNode currNode) throws FilterParsingException{
        
        // Base case.  We're out of tokens, so we're done.
        if (!toks.hasNext())
          return currNode;
        
        AdvancedSearchDialog.FilterTokens filterTok = toks.next();
        
        // Parsing Parenthesis
        if (filterTok instanceof AdvancedSearchDialog.ParensFT) {
            if (((AdvancedSearchDialog.ParensFT) filterTok).parens.equals("(")){
                return createFTFromTokensRecursively2(toks,null);
            } else if (((AdvancedSearchDialog.ParensFT) filterTok).parens
                    .equals(")")){
                
                return createFTFromTokensRecursively2(toks,currNode);
            }
        }
        
        //Parsing an Operation
        if (filterTok instanceof AdvancedSearchDialog.OperationFT){
            AdvancedSearchDialog.OperationFT ft = 
                (AdvancedSearchDialog.OperationFT) filterTok;
            if (currNode == null){
                throw new FilterParsingException(
                        "Filter Expression Parsing Error: unexpected operator");
            } else if (currNode.operation == ft.op || 
                    currNode.operation == BoolOp.NOP){
                currNode.operation = ft.op;
                //We're already parsing this opreation, continue on
                return createFTFromTokensRecursively2(toks,currNode);
            }else{ //Mismatching operation
                ExpNode newParent = new ExpNode();
                newParent.operation = ft.op;
                newParent.children.add(currNode);
                currNode = newParent;
                return createFTFromTokensRecursively2(toks,currNode);
            }
        }
        
        //Parsing an Operand
        if (filterTok instanceof AdvancedSearchDialog.EquipmentFT){
          if (currNode == null){
              currNode = new ExpNode();
          }
          AdvancedSearchDialog.EquipmentFT ft = 
              (AdvancedSearchDialog.EquipmentFT)filterTok;
          ExpNode newChild = new ExpNode(ft.name,ft.qty);
          currNode.children.add(newChild);
          return createFTFromTokensRecursively2(toks,currNode);
          
        }
        return null;             
    }
// N-ary tree implementation.  This is kludgey as hell and not finished
//    private ExpNode createFTFromTokensRecursively(Iterator<AdvancedSearchDialog.FilterTokens> toks) throws FilterParsingException{
//        if (!toks.hasNext())
//            throw new FilterParsingException();
//        AdvancedSearchDialog.FilterTokens filterTok = toks.next();
//        //Parsing Parenthesis
//        if (filterTok instanceof AdvancedSearchDialog.ParensFT){
//            if (((AdvancedSearchDialog.ParensFT)filterTok).parens.equals("(")){
//                return createFTFromTokensRecursively(toks);
//            } else if (((AdvancedSearchDialog.ParensFT)filterTok).parens.equals(")"))
//                return null;
//        }
//        //Parsing an Operation
//        if (filterTok instanceof AdvancedSearchDialog.OperationFT){
//            ExpNode newNode = new ExpNode();
//            newNode.operation = 
//                ((AdvancedSearchDialog.OperationFT)filterTok).op;
//            if (!toks.hasNext()){
//                throw new FilterParsingException();            
//            }else{
//                ExpNode childNode = createFTFromTokensRecursively(toks);
//                newNode.children.add(childNode);
//                childNode.parent = newNode;
//            }
//            return newNode;                
//        }
//        //Parsing Equipment
//        //  This is where most of the work is done.  
//        if (filterTok instanceof AdvancedSearchDialog.EquipmentFT){            
//            ExpNode newNode = 
//                new ExpNode(((AdvancedSearchDialog.EquipmentFT)filterTok).name,
//                            ((AdvancedSearchDialog.EquipmentFT)filterTok).qty);
//            ExpNode returnedNode = newNode;
//            if (toks.hasNext()){
//                ExpNode retVal = createFTFromTokensRecursively(toks);
//                while (retVal != null){
//                    //Check to see if we parsed an operation
//                    if (retVal.operation != BoolOp.NOP ){
//                        if (newNode.parent == null){
//                            newNode.parent = retVal;
//                            returnedNode = retVal;
//                        }else{
//                            //Check to see if the operation is different.
//                            //  If it's not different, we can just continue
//                            if (newNode.parent.operation != retVal.operation){
//                                
//                            }                            
//                        }
//                        
//                    }else{                        
//                        newNode.children.add(retVal);
//                    }
//                    retVal = createFTFromTokensRecursively(toks);
//                }
//            }
//            return returnedNode;               
//        }
//
//        return null;
//    }
    
    public void addEquipmentCriteria(String wpnName, int qty, BoolOp op){
        checkEquipment = true;
        ExpNode root = equipmentCriteria.root;
        //If the root has no children, just add a child
        if (root.children.size() == 0){
            ExpNode newChild = new ExpNode(wpnName, qty);
            root.operation = op;
            root.children.add(newChild);
        //If root has our op, we can just add a child
        } else if (root.operation == op || root.operation == BoolOp.NOP){
            ExpNode newChild = new ExpNode(wpnName,qty); 
            root.children.add(newChild);
            root.operation = op;
        //If root isn't our op, but it only has 1 child, its op doesn't matter
        } else if (root.children.size() == 1){
            ExpNode newChild = new ExpNode(wpnName,qty); 
            root.children.add(newChild);
            root.operation = op;
        //If all else fails, we'll make a new root 
        } else {
            ExpNode newRoot  = new ExpNode(); 
            ExpNode newChild = new ExpNode(wpnName,qty);
            newRoot.operation = op;
            newRoot.children.add(root);
            newRoot.children.add(newChild);
            equipmentCriteria.root = newRoot;
        }           
    }
    
    
    public void clearEquipmentCriteria()
    {
        checkEquipment = false;
        equipmentCriteria = new ExpressionTree();
    }
    
    public String getEquipmentExpression(){
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
        } catch (NumberFormatException ne) {
            //ignore
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
        
        if (f.checkInternalsType){
            if (f.internalsType != mech.getInternalsType())
                return false;
        }
        
        if (f.checkArmorType){
            if (!mech.getArmorType().contains(f.armorType))
                return false;
        }
        
        if (f.checkCockpitType){
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
        if (f.checkEquipment && !f.evaluate(eqNames,qty))
            return false;

        //Check year criteria
        int startYear = Integer.MIN_VALUE;
        int endYear = Integer.MAX_VALUE;
        try {
            startYear = Integer.parseInt(f.sStartYear);
        } catch (NumberFormatException ne) {
            //ignore
        }
        try {
            endYear = Integer.parseInt(f.sEndYear);
        } catch (NumberFormatException ne) {
            //ignore
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
    public boolean evaluate(List<String> eq, List<Integer> qty)
    {
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
    private boolean evaluate(List<String> eq, List<Integer> qty, ExpNode n)
    {
        //Base Case: See if any of the equipment matches the leaf node in 
        // sufficient quantity
        if (n.children.size() == 0)
        {
            Iterator<String> eqIter = eq.iterator();
            Iterator<Integer> qtyIter = qty.iterator();
            while (eqIter.hasNext())
            {
                String currEq = eqIter.next();
                int currQty = qtyIter.next();
                if (currEq.equals(n.name) && currQty >= n.qty)
                    return true;
            }
            return false;                                
        }
        //Otherwise, recurse on all of the children and either AND the results
        // or OR them, baesd upon the operation in this node
        boolean retVal;
        //If we set the proper default starting value of retVal, we can take 
        // advantage of logical short-circuiting.
        if (n.operation == BoolOp.AND)
            retVal = true;
        else
            retVal = false;
        Iterator<ExpNode> childIter = n.children.iterator();
        while (childIter.hasNext())
        {
            ExpNode child = childIter.next();
            if (n.operation == BoolOp.AND)
                retVal = retVal && evaluate(eq,qty,child);
            else
                retVal = retVal || evaluate(eq,qty,child);
                
        }        
        return retVal;
    }
    
    
    /**
     * This class allows to create a tree where the leaf nodes contain names 
     * and quantities of pieces of equipment while the non-leaf nodes contain
     * boolean operations (AND and OR).   
     * 
     * @author Arlith
     *
     */
    public class ExpressionTree {

        private ExpNode root;

        public ExpressionTree() {
            root = new ExpNode();
        }

        /**
         * Deep copy constructor.  New instantiations of all state variables 
         * are created.
         * 
         * @param et  The <code>ExpressionTree</code> to create a copy of.
         */
        public ExpressionTree(ExpressionTree et) {
            root = new ExpNode(et.root);
        }

        public ExpressionTree(String n, int q) {
            root = new ExpNode(n, q);
        }

        public String toString() {
            if (root.children.size() == 0)
                return "";
            else
                return root.toString();
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
            children = new LinkedList<ExpNode>();
        }
        
        /**
         * Deep copy constructor.  New instantiations of all state variables 
         * are created.
         * 
         * @param et  The <code>ExpressionTree</code> to create a copy of.
         */
        public ExpNode(ExpNode e){
            parent = null;
            this.operation = e.operation;
            this.qty = e.qty;
            if (e.name != null)
                this.name = new String(e.name);
            Iterator<ExpNode> nodeIter = e.children.iterator();
            this.children = new LinkedList<ExpNode>();
            while (nodeIter.hasNext())
                children.add(new ExpNode(nodeIter.next()));                
        }

        public ExpNode(String n, int q) {
            parent = null;
            name = n;
            qty = q;
            operation = BoolOp.NOP;
            children = new LinkedList<ExpNode>();
        }

        public String toString() {
            // Base Case: this is a leaf-node
            if (children.size() == 0)
                if (qty == 1)
                    return qty + " " + name;
                else
                    return qty + " " + name + "s";

            // Recursive Case
            StringBuilder result = new StringBuilder("(");
            Iterator<ExpNode> nodeIter = children.iterator();
            int count = 0;
            while (nodeIter.hasNext()) {
                ExpNode child = nodeIter.next();
                if (operation == BoolOp.AND)
                    if (count == children.size() - 1)
                        result.append(child.toString());
                    else
                        result.append(child.toString() + " AND ");
                else if (count == children.size() - 1)
                    result.append(child.toString());
                else
                    result.append(child.toString() + " OR ");
                count++;
            }
            result.append(" )");
            return result.toString();
        }

    }
    
    public class FilterParsingException extends Exception{
        
        public String msg;
        
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        
        FilterParsingException(String m){
            msg = m;            
        }
           
    }
}

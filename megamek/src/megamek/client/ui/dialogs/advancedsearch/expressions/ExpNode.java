/*
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.advancedsearch.expressions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchEquipmentClass;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;

public class ExpNode {

    public ExpNode parent;
    public MekSearchFilter.BoolOp operation;
    public String name;
    public AdvancedSearchEquipmentClass equipmentClass;
    public int qty;
    public List<ExpNode> children;
    public boolean atLeast;

    public ExpNode() {
        operation = MekSearchFilter.BoolOp.NOP;
        children = new LinkedList<>();
    }

    /**
     * Deep copy constructor. New instantiations of all state variables are created.
     *
     * @param expNode The <code>ExpressionTree</code> to create a copy of.
     */
    public ExpNode(ExpNode expNode) {
        parent = null;
        operation = expNode.operation;
        qty = expNode.qty;
        name = expNode.name;
        equipmentClass = expNode.equipmentClass;
        Iterator<ExpNode> nodeIter = expNode.children.iterator();
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
        operation = MekSearchFilter.BoolOp.NOP;
        children = new LinkedList<>();
        this.atLeast = atLeast;
    }

    public ExpNode(AdvancedSearchEquipmentClass n, int q, boolean atLeast) {
        parent = null;
        name = null;
        equipmentClass = n;
        qty = q;
        operation = MekSearchFilter.BoolOp.NOP;
        children = new LinkedList<>();
        this.atLeast = atLeast;
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
            if (operation == MekSearchFilter.BoolOp.AND) {
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

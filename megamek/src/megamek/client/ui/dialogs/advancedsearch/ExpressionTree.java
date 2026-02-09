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
package megamek.client.ui.dialogs.advancedsearch;

/**
 * This class allows to create a tree where the leaf nodes contain names and quantities of pieces of equipment while the
 * non-leaf nodes contain boolean operations (AND and OR).
 *
 * @author Arlith
 */
class ExpressionTree {
    private final ExpNode root;

    public ExpressionTree() {
        root = new ExpNode();
    }

    /**
     * Deep copy constructor. New instantiations of all state variables are created.
     *
     * @param expressionTree The <code>ExpressionTree</code> to create a copy of.
     */
    public ExpressionTree(ExpressionTree expressionTree) {
        root = new ExpNode(expressionTree.root);
    }

    public ExpressionTree(ExpNode rootNode) {
        root = rootNode;
    }

    @Override
    public String toString() {
        return root.children.isEmpty() ? "" : root.toString();
    }

    public ExpNode getRoot() {
        return root;
    }
}

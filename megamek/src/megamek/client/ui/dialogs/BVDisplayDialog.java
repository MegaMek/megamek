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
package megamek.client.ui.dialogs;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class BVDisplayDialog extends AbstractDialog {
    //region Variable Declarations
    private final Entity entity;
    //endregion Variable Declarations

    //region Constructors
    public BVDisplayDialog(final JFrame frame, final Entity entity) {
        this(frame, false, entity);
    }

    public BVDisplayDialog(final JFrame frame, final boolean modal, final Entity entity) {
        super(frame, modal, "BVDisplayDialog", "BVDisplayDialog.title");
        this.entity = Objects.requireNonNull(entity);
        initialize();
    }
    //endregion Constructors

    //region Getters
    public Entity getEntity() {
        return entity;
    }
    //endregion Getters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        final JEditorPane editorPane = new JEditorPane("text/html", getEntity().getBVText());
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);
        editorPane.setFont(UIUtil.getScaledFont());

        final JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(550, 300));
        return scrollPane;
    }
    //endregion Initialization
}

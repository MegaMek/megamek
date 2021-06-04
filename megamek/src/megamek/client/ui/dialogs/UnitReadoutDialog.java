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

import java.awt.Container;
import java.awt.Dimension;
import java.util.Objects;
import javax.swing.*;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;

/** A dialog showing the unit readout for a given unit. */
public class UnitReadoutDialog extends AbstractDialog {
    
    private final Entity entity;

    /** Constructs a non-modal dialog showing the readout (TRO) of the given entity. */ 
    public UnitReadoutDialog(final JFrame frame, final Entity entity) {
        this(frame, false, entity);
    }

    /** Constructs a dialog showing the readout (TRO) of the given entity with the given modality. */
    public UnitReadoutDialog(final JFrame frame, final boolean modal, final Entity entity) {
        super(frame, modal, "BVDisplayDialog", "BVDisplayDialog.title");
        this.entity = Objects.requireNonNull(entity);
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        MechView mv = new MechView(entity, false);
        // The label must want a fixed width to enforce linebreaks on fluff text
        JLabel mechSummary = new JLabel("<HTML>" + mv.getMechReadoutHead()
        + mv.getMechReadoutBasic() + mv.getMechReadoutLoadout()
        + mv.getMechReadoutFluff()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(500, super.getPreferredSize().height);
            }
        };
        mechSummary.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane tScroll = new JScrollPane(mechSummary,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tScroll.getVerticalScrollBar().setUnitIncrement(16);
//        mechSummary.setFont(UIUtil.);
        return tScroll;
    }

}

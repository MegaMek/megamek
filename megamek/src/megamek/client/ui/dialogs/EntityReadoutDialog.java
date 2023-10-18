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
import java.util.Objects;
import javax.swing.*;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.panes.EntityViewPane;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;

/**
 * A dialog showing the unit readout for a given unit. It shows an {@link EntityViewPane} with the entity summary,
 * TRO and AS card panels within a TabbedPane.
 */
public class EntityReadoutDialog extends AbstractDialog {

    private final Entity entity;
    private EntityViewPane entityView;

    /** Constructs a non-modal dialog showing the readout (TRO) of the given entity. */ 
    public EntityReadoutDialog(final JFrame frame, final Entity entity) {
        this(frame, false, entity);
    }

    /** Constructs a dialog showing the readout (TRO) of the given entity with the given modality. */
    public EntityReadoutDialog(final JFrame frame, final boolean modal, final Entity entity) {
        super(frame, modal, "EntityReadoutDialog", "EntityReadoutDialog.title");
        setTitle(getTitle() + entity.getShortNameRaw());
        this.entity = Objects.requireNonNull(entity);
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        entityView = new EntityViewPane(getFrame(), entity);
        return entityView;
    }
    
    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(entityView));
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
        }

        super.setVisible(visible);
    }
}
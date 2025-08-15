/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.Container;
import java.util.Objects;
import javax.swing.JFrame;

import megamek.client.ui.dialogs.abstractDialogs.AbstractDialog;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.Entity;

/**
 * A dialog showing the unit readout for a given unit. It shows an {@link EntityViewPane} with the entity summary, TRO
 * and AS card panels within a TabbedPane.
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
    protected void cancelAction() {
        dispose();
    }
}

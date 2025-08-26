/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.equipment.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

/**
 * This class loads the default quirks list from the mmconf/cannonUnitQuirks.xml file.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2012-03-05
 */
public class QuirksPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = -8360885055638738148L;
    private final Entity entity;
    private List<DialogOptionComponentYPanel> quirkComps;
    private final HashMap<Integer, ArrayList<DialogOptionComponentYPanel>> h_wpnQuirkComps = new HashMap<>();
    private final HashMap<Integer, WeaponQuirks> h_wpnQuirks;
    private final Quirks quirks;
    private final boolean editable;
    private final DialogOptionListener parent;

    public QuirksPanel(Entity entity, Quirks quirks, boolean editable, DialogOptionListener parent,
          HashMap<Integer, WeaponQuirks> h_wpnQuirks) {
        this.entity = entity;
        this.quirks = quirks;
        this.editable = editable;
        this.parent = parent;
        this.h_wpnQuirks = h_wpnQuirks;
        setLayout(new GridBagLayout());
        refreshQuirks();
    }

    public void refreshQuirks() {
        removeAll();
        quirkComps = new ArrayList<>();
        for (Integer eqNum : h_wpnQuirks.keySet()) {
            h_wpnQuirkComps.put(eqNum, new ArrayList<>());
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            add(new JLabel(group.getDisplayableName()), GBC.eol());

            for (Enumeration<IOption> j = group.getSortedOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (null == option || Quirks.isQuirkDisallowed(option, entity)) {
                    continue;
                }

                addQuirk(option, editable);
            }
        }

        // now for weapon quirks
        Set<Integer> set = h_wpnQuirks.keySet();
        for (int key : set) {
            Mounted<?> m = entity.getEquipment(key);
            WeaponQuirks wpnQuirks = h_wpnQuirks.get(key);
            JLabel labWpn = new JLabel(m.getName() + " ("
                  + entity.getLocationName(m.getLocation()) + ")");
            add(labWpn, GBC.eol());
            for (Enumeration<IOptionGroup> i = wpnQuirks.getGroups(); i.hasMoreElements(); ) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getSortedOptions(); j.hasMoreElements(); ) {
                    IOption option = j.nextElement();
                    if (WeaponQuirks.isQuirkDisallowed(option, entity, m.getType())) {
                        continue;
                    }
                    addWeaponQuirk(key, option, editable);
                }
            }
        }

        validate();
    }

    private void addQuirk(IOption option, boolean editable) {
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(parent, option, editable);
        add(optionComp, GBC.eol());
        quirkComps.add(optionComp);
    }

    private void addWeaponQuirk(int key, IOption option, boolean editable) {
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(parent, option, editable);
        add(optionComp, GBC.eol());
        h_wpnQuirkComps.get(key).add(optionComp);
    }

    public void setQuirks() {
        IOption option;
        for (final DialogOptionComponentYPanel newVar : quirkComps) {
            option = newVar.getOption();
            if ((newVar.getValue() == Messages.getString("CustomMekDialog.None"))) {
                entity.getQuirks().getOption(option.getName()).setValue("None");
            } else if (option.getName().equals("internal_bomb")) {
                // Need to set the quirk, and only then force re-computing bomb bay space for
                // Aero-derived units
                entity.getQuirks().getOption(option.getName()).setValue(newVar.getValue());
                if (entity.isAero()) {
                    ((Aero) entity).autoSetMaxBombPoints();
                }
            } else {
                entity.getQuirks().getOption(option.getName()).setValue(newVar.getValue());
            }
        }
        // now for weapon quirks
        Set<Integer> set = h_wpnQuirkComps.keySet();
        for (Integer key : set) {
            Mounted<?> m = entity.getEquipment(key);
            ArrayList<DialogOptionComponentYPanel> wpnQuirkComps = h_wpnQuirkComps.get(key);
            for (final DialogOptionComponentYPanel newVar : wpnQuirkComps) {
                option = newVar.getOption();
                if ((newVar.getValue() == Messages.getString("CustomMekDialog.None"))) {
                    m.getQuirks().getOption(option.getName()).setValue("None");
                } else {
                    m.getQuirks().getOption(option.getName()).setValue(newVar.getValue());
                }
            }
        }
    }
}

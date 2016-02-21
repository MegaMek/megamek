/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

/**
 * This class loads the default quirks list from the mmconf/cannonUnitQuirks.xml file.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 2012-03-05
 */
public class QuirksPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -8360885055638738148L;
    private Entity entity;
    private List<DialogOptionComponent> quirkComps;
    private HashMap<Integer, ArrayList<DialogOptionComponent>> h_wpnQuirkComps = new HashMap<Integer,
            ArrayList<DialogOptionComponent>>();
    private HashMap<Integer, WeaponQuirks> h_wpnQuirks;
    private Quirks quirks;
    private boolean editable;
    private DialogOptionListener parent;

    public QuirksPanel(Entity entity, Quirks quirks, boolean editable, DialogOptionListener parent, HashMap<Integer,
            WeaponQuirks> h_wpnQuirks) {
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
        quirkComps = new ArrayList<DialogOptionComponent>();
        for (Integer eqNum : h_wpnQuirks.keySet()) {
            h_wpnQuirkComps.put(eqNum, new ArrayList<DialogOptionComponent>());
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            add(new JLabel(group.getDisplayableName()), GBC.eol());

            for (Enumeration<IOption> j = group.getSortedOptions(); j
                    .hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (!Quirks.isQuirkLegalFor(option, entity)) {
                    continue;
                }

                addQuirk(option, editable);
            }
        }

        // now for weapon quirks
        Set<Integer> set = h_wpnQuirks.keySet();
        Iterator<Integer> iter = set.iterator();
        while (iter.hasNext()) {
            int key = iter.next();
            Mounted m = entity.getEquipment(key);
            WeaponQuirks wpnQuirks = h_wpnQuirks.get(key);
            JLabel labWpn = new JLabel(m.getName() + " ("
                                       + entity.getLocationName(m.getLocation()) + ")");
            add(labWpn, GBC.eol());
            for (Enumeration<IOptionGroup> i = wpnQuirks.getGroups(); i
                    .hasMoreElements(); ) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getSortedOptions(); j
                        .hasMoreElements(); ) {
                    IOption option = j.nextElement();
                    if (!WeaponQuirks.isQuirkLegalFor(option, entity,
                            m.getType())) {
                        continue;
                    }
                    addWeaponQuirk(key, option, editable);
                }
            }
        }

        validate();
    }

    private void addQuirk(IOption option, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(parent,
                                                                     option, editable);
        add(optionComp, GBC.eol());

        quirkComps.add(optionComp);
    }

    private void addWeaponQuirk(int key, IOption option, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(parent,
                option, editable);

        add(optionComp, GBC.eol());
        h_wpnQuirkComps.get(key).add(optionComp);
    }

    public void setQuirks() {
        IOption option;
        for (final Object newVar : quirkComps) {
            DialogOptionComponent comp = (DialogOptionComponent) newVar;
            option = comp.getOption();
            if ((comp.getValue() == Messages.getString("CustomMechDialog.None"))) { // NON-NLS-$1
                entity.getQuirks().getOption(option.getName()).setValue("None"); // NON-NLS-$1
            } else {
                entity.getQuirks().getOption(option.getName())
                      .setValue(comp.getValue());
            }
        }
        // now for weapon quirks
        Set<Integer> set = h_wpnQuirkComps.keySet();
        for (Integer key : set) {
            Mounted m = entity.getEquipment(key);
            ArrayList<DialogOptionComponent> wpnQuirkComps = h_wpnQuirkComps
                    .get(key);
            for (final Object newVar : wpnQuirkComps) {
                DialogOptionComponent comp = (DialogOptionComponent) newVar;
                option = comp.getOption();
                if ((comp.getValue() == Messages
                        .getString("CustomMechDialog.None"))) { // NON-NLS-$1
                    m.getQuirks().getOption(option.getName()).setValue("None"); // NON-NLS-$1
                } else {
                    m.getQuirks().getOption(option.getName())
                     .setValue(comp.getValue());
                }
            }
        }
    }
}

/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

/*
 * CustomMechDialog.java
 *
 * Created on March 18, 2002, 2:56 PM
 */

package megamek.client;

import java.awt.*;
import megamek.common.*;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import megamek.common.options.OptionGroup;
import megamek.common.options.GameOption;

/**
 * A dialog that a player can use to customize his mech before battle.  
 * Currently, only changing pilots is supported.
 *
 * @author  Ben
 * @version 
 */
public class CustomMechDialog 
extends Dialog implements ActionListener, DialogOptionListener { 
    
    private Label labName = new Label("Name: ", Label.RIGHT);
    private TextField fldName = new TextField(20);
    private Label labGunnery = new Label("Gunnery: ", Label.RIGHT);;
    private TextField fldGunnery = new TextField(3);
    private Label labPiloting = new Label("Piloting: ", Label.RIGHT);;
    private TextField fldPiloting = new TextField(3);
    private Label labC3 = new Label("C3 Network: ", Label.RIGHT);;
    private Choice choC3 = new Choice();
    private int[] entityCorrespondance;
    private Label labDeployment = new Label("Deployment Round: ", Label.RIGHT);
    private Choice choDeployment = new Choice();
    
    private Panel panButtons = new Panel();
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");

    private Vector m_vMunitions = new Vector();
    private Panel panMunitions = new Panel();
    
    private Entity entity;
    private boolean okay = false;
    private Client client;

    private PilotOptions options;
    
    private Vector optionComps = new Vector();
    
    private Panel panOptions = new Panel();
    private ScrollPane scrOptions = new ScrollPane();

    private TextArea texDesc = new TextArea("Mouse over an option to see a description.", 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);

    private boolean editable;
    
    /** Creates new CustomMechDialog */
    public CustomMechDialog(Client client, Entity entity, boolean editable) {
        super(client.frame, "Customize pilot/mech stats...", true);
        
        this.entity = entity;
        this.client = client;
        this.options = entity.getCrew().getOptions();
        this.editable = editable;
        
        texDesc.setEditable(false);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
            
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 5, 5, 5);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labName, c);
        add(labName);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldName, c);
        add(fldName);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labGunnery, c);
        add(labGunnery);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldGunnery, c);
        add(fldGunnery);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labPiloting, c);
        add(labPiloting);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldPiloting, c);
        add(fldPiloting);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labDeployment, c);
        add(labDeployment);
      
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(choDeployment, c);
        add(choDeployment);
        refreshDeployment();

        if ( client.game.getOptions().booleanOption("pilot_advantages") ) {
          scrOptions.add(panOptions);
        
          c.weightx = 1.0;    c.weighty = 1.0;
          c.fill = GridBagConstraints.BOTH;
          c.gridwidth = GridBagConstraints.REMAINDER;
          gridbag.setConstraints(scrOptions, c);
          add(scrOptions);
  
          c.weightx = 1.0;    c.weighty = 0.0;
          gridbag.setConstraints(texDesc, c);
          add(texDesc);
        }
        
        if(entity.hasC3() || entity.hasC3i())
        {        
          c.gridwidth = 1;
          c.anchor = GridBagConstraints.EAST;
          gridbag.setConstraints(labC3, c);
          add(labC3);
        
          c.gridwidth = GridBagConstraints.REMAINDER;
          c.anchor = GridBagConstraints.WEST;
          gridbag.setConstraints(choC3, c);
          add(choC3);
          refreshC3();
        }

        // Can't set up munitions on infantry.
        if ( !(entity instanceof Infantry) ) {
            setupMunitions();
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(panMunitions, c);
            add(panMunitions);
        }

        setupButtons();
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panButtons, c);
        add(panButtons);
        
        fldName.setText(entity.getCrew().getName());
        fldName.addActionListener(this);
        fldGunnery.setText(new Integer(entity.getCrew().getGunnery()).toString());
        fldGunnery.addActionListener(this);
        fldPiloting.setText(new Integer(entity.getCrew().getPiloting()).toString());
        fldPiloting.addActionListener(this);
        
        if (!editable) {
            fldName.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldPiloting.setEnabled(false);
            choC3.setEnabled(false);
            choDeployment.setEnabled(false);
        }
        
        addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { setVisible(false); }
  });

        pack();
        setResizable(false);
        setLocation(client.frame.getLocation().x + client.frame.getSize().width/2 - getSize().width/2,
                    client.frame.getLocation().y + client.frame.getSize().height/2 - getSize().height/2);
    }
    
    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }
    
    private void setupMunitions() {
        
        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        
        int row = 0;
        for (Enumeration e = entity.getAmmo(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();
            AmmoType at = (AmmoType)m.getType();
            Vector vTypes = new Vector();
            Vector vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
            if (vAllTypes == null || vAllTypes.size() < 2) {
                continue;
            }
            
            for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                AmmoType atCheck = (AmmoType)vAllTypes.elementAt(x);
                boolean bTechMatch = (entity.getTechLevel() == atCheck.getTechType());
                if (!bTechMatch && entity.getTechLevel() == TechConstants.T_IS_LEVEL_2 && 
                        atCheck.getTechType() == TechConstants.T_IS_LEVEL_1) {
                    bTechMatch = true;
                }
                
                // if is_eq_limits is unchecked allow l1 guys to use l2 stuff
                if (!client.game.getOptions().booleanOption("is_eq_limits")
                    && entity.getTechLevel() == TechConstants.T_IS_LEVEL_1
                    && atCheck.getTechType() == TechConstants.T_IS_LEVEL_2) {
                    bTechMatch = true;
                }

		// If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                //      to be combined to othter munition types.
                int muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY;
                if ( !client.game.getOptions().booleanOption("clan_ignore_eq_limits")
                     && entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2
                     && ( muniType == AmmoType.M_SEMIGUIDED ||
                          muniType == AmmoType.M_THUNDER_AUGMENTED ||
                          muniType == AmmoType.M_THUNDER_INFERNO   ||
                          muniType == AmmoType.M_THUNDER_VIBRABOMB ||
                          muniType == AmmoType.M_THUNDER_ACTIVE)) {
                    bTechMatch = false;
		}

                if ( !client.game.getOptions().booleanOption("minefields") &&
                	AmmoType.canDeliverMinefield(atCheck) ) {
                    continue;
                }

                // Battle Armor ammo can't be selected at all.
                // All other ammo types need to match on rack size and tech.
                if ( bTechMatch &&
                     atCheck.getRackSize() == at.getRackSize() &&
                     !atCheck.hasFlag(AmmoType.F_BATTLEARMOR) &&
                     atCheck.getTonnage(entity) == at.getTonnage(entity) ) {
                    vTypes.addElement(atCheck);
                }
            }
            
            if (vTypes.size() < 2) {
                continue;
            }
            
            gbc.gridy = row++;
            // Protomechs need special choice panels.
            MunitionChoicePanel mcp = null;
            if ( entity instanceof Protomech ) {
                mcp = new ProtomechMunitionChoicePanel(m, vTypes);
            } else {
                mcp = new MunitionChoicePanel(m, vTypes);
            }
            gbl.setConstraints(mcp, gbc);
            panMunitions.add(mcp);
            m_vMunitions.addElement(mcp);
        }
    }
    
    class MunitionChoicePanel extends Panel {
        private Vector m_vTypes;
        private Choice m_choice;
        private Mounted m_mounted;
        
        public MunitionChoicePanel(Mounted m, Vector vTypes) {
            m_vTypes = vTypes;
            m_mounted = m;
            AmmoType curType = (AmmoType)m.getType();
            m_choice = new Choice();
            Enumeration e = m_vTypes.elements();
            for (int x = 0; e.hasMoreElements(); x++) {
                AmmoType at = (AmmoType)e.nextElement();
                m_choice.add(at.getName());
                if (at.getMunitionType() == curType.getMunitionType()) {
                    m_choice.select(x);
                }
            }
            setLayout(new BorderLayout());
            String sDesc = "(" + entity.getLocationAbbr(m.getLocation()) + ")";
            add(new Label(sDesc), BorderLayout.WEST);
            add(m_choice, BorderLayout.CENTER);
        }
        
        public void applyChoice() {
            int n = m_choice.getSelectedIndex();
            AmmoType at = (AmmoType)m_vTypes.elementAt(n);
            m_mounted.changeAmmoType(at);
        }

        /**
         * Get the number of shots in the mount.
         *
         * @return      the <code>int</code> number of shots in the mount.
         */
        /* package */ int getShotsLeft() {
            return m_mounted.getShotsLeft();
        }

        /**
         * Set the number of shots in the mount.
         *
         * @param shots the <code>int</code> number of shots for the mount.
         */
        /* package */ void setShotsLeft( int shots ) {
            m_mounted.setShotsLeft( shots );
        }
    }

    /**
     * When a Protomech selects ammo, you need to adjust the shots on the
     * unit for the weight of the selected munition.
     */
    class ProtomechMunitionChoicePanel extends MunitionChoicePanel {
        private final float m_origShotsLeft;
        private final AmmoType m_origAmmo;

        public ProtomechMunitionChoicePanel(Mounted m, Vector vTypes) {
            super( m, vTypes );
            m_origAmmo = (AmmoType) m.getType();
            m_origShotsLeft = m.getShotsLeft();
        }

        /**
         * All ammo must be applied in ratios to the starting load.
         */
        public void applyChoice() {
            super.applyChoice();

            // Calculate the number of shots for the new ammo.
            // N.B. Some special ammos are twice as heavy as normal
            // so they have half the number of shots (rounded down).
            setShotsLeft( Math.round( getShotsLeft() * m_origShotsLeft /
                                      m_origAmmo.getShots() ) );
        }
    }

    public void setOptions() {
      GameOption option;
      
      for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
        
        option = comp.getOption();
        
        entity.getCrew().getOptions().getOption(option.getShortName()).setValue(comp.getValue());
      }
    }
    
    public void resetOptions() {
      GameOption option;
      
      for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
        
        option = comp.getOption();
        option.setValue(false);
                
        entity.getCrew().getOptions().getOption(option.getShortName()).setValue(comp.getValue());
      }
    }
    
    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new Vector();
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;    c.ipady = 0;
        
        for (Enumeration i = options.groups(); i.hasMoreElements();) {
            OptionGroup group = (OptionGroup)i.nextElement();
            
            addGroup(group, gridbag, c);
            
            for (Enumeration j = group.options(); j.hasMoreElements();) {
                GameOption option = (GameOption)j.nextElement();

                addOption(option, gridbag, c, editable);
            }
        }
        
        validate();
    }
    
    private void addGroup(OptionGroup group, GridBagLayout gridbag, GridBagConstraints c) {
        Label groupLabel = new Label(group.getName());
        
        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }
    
    private void addOption(GameOption option, GridBagLayout gridbag, GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, editable);

        if (option.getShortName().equals("weapon_specialist")) {
            optionComp.addValue("None");
            Hashtable uniqueWeapons = new Hashtable(entity.getWeaponList().size());
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted m = (Mounted)entity.getWeaponList().elementAt(i);
                uniqueWeapons.put(m.getName(),new Boolean(true));
            }
            String enumValue;
            for (Enumeration e = uniqueWeapons.keys(); e.hasMoreElements(); ) {
                optionComp.addValue((String)e.nextElement());
            }
            optionComp.setSelected(option.stringValue());
        }
        
        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);
        
        optionComps.addElement(optionComp);
    }
    
    public void showDescFor(GameOption option) {
        texDesc.setText(option.getDesc());
    }

    // TODO : implement me!!!
    public void optionClicked( DialogOptionComponent comp,
                               GameOption option, boolean state ) {
    }

    public boolean isOkay() {
        return okay;
    }

    private void refreshDeployment() {
      choDeployment.removeAll();
      choDeployment.add("Start of game");
      
      if ( entity.getDeployRound() < 1 )
        choDeployment.select(0);
        
      for ( int i = 1; i <= 15; i++ ) {
        choDeployment.add("After round " + i);
        
        if ( entity.getDeployRound() == i )
          choDeployment.select(i);
      }
    }
    
    private void refreshC3() {
        choC3.removeAll();
        int listIndex = 0;
        entityCorrespondance = new int[client.game.getNoOfEntities() + 2];

        if(entity.hasC3i()) {
            choC3.add("Create new network (6 free)");
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();
        }
        else if ( entity.hasC3MM() ) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3.add("Set as company-level master (" + mNodes + "M / " 
                      + sNodes + "S free)");
            if(entity.C3MasterIs(entity)) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.add("Set as independant master (" + sNodes + " free)");
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = -1;

        }

        else if(entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3.add("Set as company-level master (" + nodes + " free)");
            if(entity.C3MasterIs(entity)) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.add("Set as independant master (" + nodes + " free)");
            if(entity.getC3Master() == null) choC3.select(listIndex);
            entityCorrespondance[listIndex++] = -1;

        }
        for (Enumeration i = client.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();
            // ignore enemies or self
            if(entity.isEnemyOf(e) || entity.equals(e)) {
                continue;
            }
            // c3i only links with c3i
            if (entity.hasC3i() != e.hasC3i()) {
                continue;
            }
            int nodes = e.calculateFreeC3Nodes();
            if ( e.hasC3MM() && entity.hasC3M() && e.C3MasterIs(e) ) {
                nodes = e.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(e) && !entity.equals(e)) {
                nodes++;
            }
            if (entity.hasC3i() && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            if(e.hasC3i()) {
                if (entity.onSameC3NetworkAs(e)) {
                    choC3.add("Join " + e.getDisplayName() + " [netid " + e.getC3NetId() + ": " + (nodes - 1)  + " free]");
                    choC3.select(listIndex);
                }
                else {
                    choC3.add("Join " + e.getDisplayName() + " (netid " + e.getC3NetId() + ": " + nodes + " free)");
                }
                entityCorrespondance[listIndex++] = e.getId();
            }
            else if ( e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have
                // *both* sub-masters AND slave units.
                choC3.add( "Connect to " + e.getDisplayName() +
                           " (" + nodes + " free)" );
                entityCorrespondance[listIndex] = e.getId();
                if (entity.C3MasterIs(e)) {
                    choC3.select(listIndex);
                }
                listIndex++;
            }
            else if ( e.C3MasterIs(e) != entity.hasC3M() ) {
                // If we're a slave-unit, we can only connect to sub-masters,
                // not main masters likewise, if we're a master unit, we can
                // only connect to main master units, not sub-masters.
            }
            else if (entity.C3MasterIs(e)) {
                choC3.add("Connect to " + e.getDisplayName() + " [" + (nodes - 1) + " free]");
                choC3.select(listIndex);
                entityCorrespondance[listIndex++] = e.getId();
            }
            else {
                choC3.add("Connect to " + e.getDisplayName() + " (" + nodes + " free)");
                entityCorrespondance[listIndex++] = e.getId();
            }
        }
    }


    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        if (actionEvent.getSource() != butCancel) {
            // get values
            String name = fldName.getText();
            int gunnery;
            int piloting;
            try {
                gunnery = Integer.parseInt(fldGunnery.getText());
                piloting =  Integer.parseInt(fldPiloting.getText());
            } catch (NumberFormatException e) {
                new AlertDialog(client.frame, "Number Format Error", "Please enter valid numbers for the skill values.").show();
                return;
            }
            
            // keep these reasonable, please
            if (gunnery < 0 || gunnery > 7 || piloting < 0 || piloting > 7) {
                new AlertDialog(client.frame, "Number Format Error", "Please enter values between 0 and 7 for the skill values.").show();
                return;
            }
            
            // change entity
            entity.setCrew(new Pilot(name, gunnery, piloting));
            if(entity.hasC3() && choC3.getSelectedIndex() > -1) {
                Entity chosen = client.getEntity
                    ( entityCorrespondance[choC3.getSelectedIndex()] );
                int entC3nodeCount = 
                    client.game.getC3SubNetworkMembers( entity ).size();
                int choC3nodeCount = 
                    client.game.getC3NetworkMembers( chosen ).size();
                if ( entC3nodeCount + choC3nodeCount <= Entity.MAX_C3_NODES ) {
                    entity.setC3Master( chosen );
                }
                else {
                    StringBuffer message = new StringBuffer();
                    message.append( entity.getShortName() )
                        .append( " can't join the C3 network of\n" )
                        .append( chosen.getShortName() )
                        .append( " because that would add " )
                        .append( entC3nodeCount )
                        .append( "\nunits to a network that already has " )
                        .append( choC3nodeCount )
                        .append( ",\nwhich is more than the maximum of " )
                        .append( Entity.MAX_C3_NODES )
                        .append( "." );
                    client.doAlertDialog( "C3 Network Too Big",
                                          message.toString() );
                    refreshC3();
                }
            }
            else if(entity.hasC3i() && choC3.getSelectedIndex() > -1) {
                entity.setC3NetId(client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]));
            }
            
            entity.setDeployRound(choDeployment.getSelectedIndex());
            
            // update munitions selections
            for (Enumeration e = m_vMunitions.elements(); e.hasMoreElements(); ) {
                ((MunitionChoicePanel)e.nextElement()).applyChoice();
            }
            
            setOptions();
            
            okay = true;
        }
        
        this.setVisible(false);
    }
    
}

/*
 * MechSelectorDialog.java - Copyright (C) 2002 Josh Yockey
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
 
package megamek.client;
 
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Arrays;
import megamek.common.*;


/* 
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class MechSelectorDialog 
    extends Dialog implements ActionListener, ItemListener, KeyListener
{
    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;
     
    // these indices should match up with the static values in the MechSummaryComparator
    private String[] m_saSorts = { "Name", "Ref", "Weight", "BV", "Year" };
    
    private MechSummary[] m_mechsCurrent;
    private Client m_client;
        
    private StringBuffer m_sbSearch = new StringBuffer();
    private long m_nLastSearch = 0;
    
    private Label m_labelWeightClass = new Label("Weight Class: ", Label.RIGHT);
    private Choice m_chWeightClass = new Choice();
    private Label m_labelType = new Label("Type: ", Label.RIGHT);
    private Choice m_chType = new Choice();
    private Label m_labelSort = new Label("Sort: ", Label.RIGHT);
    private Choice m_chSort = new Choice();
    private Panel m_pParams = new Panel();
    List m_mechList = new List(10);
    private Button m_bPick = new Button("Select Mech");
    private Button m_bCancel = new Button("Cancel");
    private Panel m_pButtons = new Panel();
    
    public MechSelectorDialog(Client cl)
    {
        super(cl.frame, "Select Mech...", true);
        m_client = cl;
        
        for (int x = 0; x < m_saSorts.length; x++) {
            m_chSort.addItem(m_saSorts[x]);
        }
        m_pParams.setLayout(new GridLayout(3, 2));
        m_pParams.add(m_labelWeightClass);
        m_pParams.add(m_chWeightClass);
        m_pParams.add(m_labelType);
        m_pParams.add(m_chType);
        m_pParams.add(m_labelSort);
        m_pParams.add(m_chSort);
        
        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bPick);
        m_pButtons.add(m_bCancel);
        
        setLayout(new BorderLayout());
        add(m_pParams, BorderLayout.NORTH);
        m_mechList.setFont(new Font("Courier", Font.PLAIN, 12));
        m_mechList.addKeyListener(this);
        add(m_mechList, BorderLayout.CENTER);
        add(m_pButtons, BorderLayout.SOUTH);
        
        m_chWeightClass.addItemListener(this);
        m_chType.addItemListener(this);
        m_chSort.addItemListener(this);
        m_bPick.addActionListener(this);
        m_bCancel.addActionListener(this);
        setSize(360, 320);
        setLocation(m_client.frame.getLocation().x + m_client.frame.getSize().width/2 - getSize().width/2,
                    m_client.frame.getLocation().y + m_client.frame.getSize().height/2 - getSize().height/2);
        populateChoices(MechSummaryCache.getInstance().getAllMechs());
        filterMechs();
    }
    
    
    private void populateChoices(MechSummary[] msa) {
        
        
        m_chWeightClass.addItem("Light");
        m_chWeightClass.addItem("Medium");
        m_chWeightClass.addItem("Heavy");
        m_chWeightClass.addItem("Assault");
        
        for (int i = 0; i < TechConstants.T_NAMES.length; i++) {
            m_chType.addItem(TechConstants.T_NAMES[i]);
        }
    }
    
    
    private void filterMechs()
    {
        Vector vMechs = new Vector();
        String sClass = m_chWeightClass.getSelectedItem();
        int nWeight;
        if (sClass.equals("Light")) {
            nWeight = Entity.WEIGHT_LIGHT;
        }
        else if (sClass.equals("Medium")) {
            nWeight = Entity.WEIGHT_MEDIUM;
        }
        else if (sClass.equals("Heavy")) {
            nWeight = Entity.WEIGHT_HEAVY;
        }
        else {
            nWeight = Entity.WEIGHT_ASSAULT;
        }
        int nType = m_chType.getSelectedIndex();
        MechSummary[] mechs = MechSummaryCache.getInstance().getAllMechs();
        for (int x = 0; x < mechs.length; x++) {
            if (mechs[x].getWeightClass() == nWeight && 
                    mechs[x].getType() == nType) {
                vMechs.addElement(mechs[x]);
            }
        }
        m_mechsCurrent = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_mechsCurrent);
        sortMechs();
    }
    
    private void sortMechs()
    {
        Arrays.sort(m_mechsCurrent, new MechSummaryComparator(m_chSort.getSelectedIndex()));
        m_mechList.removeAll();
        for (int x = 0; x < m_mechsCurrent.length; x++) {
            m_mechList.add(formatMech(m_mechsCurrent[x]));
        }
        repaint();
    }
    
    private void searchFor(String search) {
        for (int i = 0; i < m_mechsCurrent.length; i++) {
            if (m_mechsCurrent[i].getName().toLowerCase().startsWith(search)) {
                m_mechList.select(i);
                break;
            }
        }
    }
    
    public void show() {
        setLocation(m_client.frame.getLocation().x + m_client.frame.getSize().width/2 - getSize().width/2,
                    m_client.frame.getLocation().y + m_client.frame.getSize().height/2 - getSize().height/2);
        super.show();
    }
    
    private String formatMech(MechSummary ms)
    {
        return makeLength(ms.getModel(), 10) + " " + 
                makeLength(ms.getChassis(), 20) + " " + 
                makeLength("" + ms.getTons(), 3) + " " + 
                makeLength("" + ms.getBV(),5)+""+
		    ms.getYear();
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == m_bCancel) {
            this.setVisible(false);
        }
        else if (ae.getSource() == m_bPick) {
            int x = m_mechList.getSelectedIndex();
            if (x == -1) {
                return;
            }
            MechSummary ms = m_mechsCurrent[m_mechList.getSelectedIndex()];
            try {
                Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                e.setOwner(m_client.getLocalPlayer());
                m_client.sendAddEntity(e);
            } catch (EntityLoadingException ex) {
                System.out.println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName());
                ex.printStackTrace();
                return;
            }
            this.setVisible(false);
        }
    }
    
    public void itemStateChanged(ItemEvent ie)
    {
        if (ie.getSource() == m_chSort) {
            sortMechs();
        }
        else if (ie.getSource() == m_chWeightClass || ie.getSource() == m_chType) {
            filterMechs();
        }
    }
    
    private static final String SPACES = "                        ";
    private String makeLength(String s, int nLength)
    {
        if (s.length() == nLength) {
            return s;
        }
        else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + "..";
        }
        else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }
        
    public void keyReleased(java.awt.event.KeyEvent ke) {
    }
    
    public void keyPressed(java.awt.event.KeyEvent ke) {
        long curTime = System.currentTimeMillis();
        if (curTime - m_nLastSearch > KEY_TIMEOUT) {
            m_sbSearch = new StringBuffer();
        }
        m_nLastSearch = curTime;
        m_sbSearch.append(ke.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }
    
    public void keyTyped(java.awt.event.KeyEvent ke) {
    }
        
}
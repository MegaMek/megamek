/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Ken Nguyen (kenn)
 */
public class Ruler extends JDialog implements BoardViewListener, IPreferenceChangeListener {
    private static final long serialVersionUID = -4820402626782115601L;
    public static Color color1 = Color.cyan;
    public static Color color2 = Color.magenta;

    private Coords start;
    private Coords end;
    private Color startColor;
    private Color endColor;
    private int distance;
    private Client client;
    private BoardView bv;
    private Game game;
    private boolean flip;

    private JPanel buttonPanel;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JButton butFlip = new JButton();
    private JLabel jLabel1;
    private JTextField tf_start = new JTextField();
    private JLabel jLabel2;
    private JTextField tf_end = new JTextField();
    private JLabel jLabel3;
    private JTextField tf_distance = new JTextField();
    private JLabel jLabel4;
    private JTextField tf_los1 = new JTextField();
    private JLabel jLabel5;
    private JTextField tf_los2 = new JTextField();
    private JButton butClose = new JButton();
    private JLabel heightLabel1;
    private JTextField height1 = new JTextField();
    private JLabel heightLabel2;
    private JTextField height2 = new JTextField();
    
    private JCheckBox cboIsMech1 = 
        new JCheckBox(Messages.getString("Ruler.isMech"));
    private JCheckBox cboIsMech2 = 
        new JCheckBox(Messages.getString("Ruler.isMech"));

    public Ruler(JFrame f, Client c, BoardView b, Game g) {
        super(f, Messages.getString("Ruler.title"), false);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        start = null;
        end = null;
        flip = true;
        startColor = color1;
        endColor = color2;

        bv = b;
        client = c;
        game = g;
        b.addBoardViewListener(this);

        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    private void jbInit() {
        buttonPanel = new JPanel();
        butFlip.setText(Messages.getString("Ruler.flip"));
        butFlip.addActionListener(e -> butFlip_actionPerformed());
        JPanel panelMain = new JPanel(gridBagLayout1);
        jLabel1 = new JLabel(Messages.getString("Ruler.Start"), SwingConstants.RIGHT);
        tf_start.setEditable(false);
        tf_start.setColumns(16);
        jLabel2 = new JLabel(Messages.getString("Ruler.End"), SwingConstants.RIGHT);
        tf_end.setEditable(false);
        tf_end.setColumns(16);
        jLabel3 = new JLabel(Messages.getString("Ruler.Distance"), SwingConstants.RIGHT);
        tf_distance.setEditable(false);
        tf_distance.setColumns(5);
        jLabel4 = new JLabel(Messages.getString("Ruler.POV") + ":", SwingConstants.RIGHT);
        jLabel4.setForeground(startColor);
        tf_los1.setEditable(false);
        tf_los1.setColumns(30);
        jLabel5 = new JLabel(Messages.getString("Ruler.POV") + ":", SwingConstants.RIGHT);
        jLabel5.setForeground(endColor);
        tf_los2.setEditable(false);
        tf_los2.setColumns(30);
        butClose.setText(Messages.getString("Ruler.Close"));
        butClose.addActionListener(e -> butClose_actionPerformed());
        heightLabel1 = new JLabel(Messages.getString("Ruler.Height1"), SwingConstants.RIGHT);
        heightLabel1.setForeground(startColor);
        height1.setText("1");
        height1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                height1_keyReleased();
            }
        });
        height1.setColumns(5);
        cboIsMech1.addItemListener(e -> checkBoxSelectionChanged());
        
        heightLabel2 = new JLabel(Messages.getString("Ruler.Height2"), SwingConstants.RIGHT);
        heightLabel2.setForeground(endColor);
        height2.setText("1");
        height2.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                height2_keyReleased();
            }
        });
        height2.setColumns(5);
        cboIsMech2.addItemListener(e -> checkBoxSelectionChanged());
        
        GridBagConstraints c = new GridBagConstraints();
        
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        gridBagLayout1.setConstraints(heightLabel1, c);
        panelMain.add(heightLabel1);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        gridBagLayout1.setConstraints(height1, c);
        panelMain.add(height1);
        c.gridx = 2;
        gridBagLayout1.setConstraints(cboIsMech1, c);
        panelMain.add(cboIsMech1);

        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(heightLabel2, c);
        panelMain.add(heightLabel2);
        c.anchor = GridBagConstraints.WEST;   
        c.gridx = 1;
        gridBagLayout1.setConstraints(height2, c);
        panelMain.add(height2);
        c.gridx = 2;
        gridBagLayout1.setConstraints(cboIsMech2, c);
        panelMain.add(cboIsMech2);
        
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel1, c);
        panelMain.add(jLabel1);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.gridwidth = 2;
        gridBagLayout1.setConstraints(tf_start, c);
        c.gridwidth = 1;
        panelMain.add(tf_start);

        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel2, c);
        panelMain.add(jLabel2);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        c.gridx = 1;        
        gridBagLayout1.setConstraints(tf_end, c);
        c.gridwidth = 1;
        panelMain.add(tf_end);

        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel3, c);
        panelMain.add(jLabel3);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        gridBagLayout1.setConstraints(tf_distance, c);
        panelMain.add(tf_distance);

        c.gridx = 0;
        c.gridy = 5;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel4, c);
        panelMain.add(jLabel4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        gridBagLayout1.setConstraints(tf_los1, c);
        c.gridwidth = 1;
        panelMain.add(tf_los1);

        c.gridx = 0;
        c.gridy = 6;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel5, c);
        panelMain.add(jLabel5);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        gridBagLayout1.setConstraints(tf_los2, c);
        c.gridwidth = 1;
        panelMain.add(tf_los2);

        buttonPanel.add(butFlip);
        buttonPanel.add(butClose);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        gridBagLayout1.setConstraints(buttonPanel, c);
        panelMain.add(buttonPanel);

        JScrollPane sp = new JScrollPane(panelMain);
        setLayout(new BorderLayout());
        add(sp);

        validate();

        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);

        setVisible(false);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    private void cancel() {
        dispose();
        butClose_actionPerformed();
    }

    private void clear() {
        start = null;
        end = null;
    }

    private void addPoint(Coords c) {
        int absHeight = Integer.MIN_VALUE;
        boolean isMech = false;
        boolean entFound = false;
        for (Entity ent : game.getEntitiesVector(c)) {
            int trAbsheight = ent.relHeight();
            if (trAbsheight > absHeight) {
                absHeight = trAbsheight;
                isMech = ent instanceof Mech;
                entFound = true;
            }
        }
        if (start == null) {
            start = c;
            if (entFound) {
                height1.setText(absHeight+"");
                cboIsMech1.setSelected(isMech);
            }
        } else if (start.equals(c)) {
            clear();
            setVisible(false);
        } else {
            end = c;
            distance = start.distance(end);
            if (entFound) {
                height2.setText(absHeight+"");
                cboIsMech2.setSelected(isMech);
            }
            setText();
            setVisible(true);
        }
    }

    private void setText() {
        int h1 = 1, h2 = 1;
        try {
            h1 = Integer.parseInt(height1.getText());
        } catch (NumberFormatException e) {
            // leave at default value
        }

        try {
            h2 = Integer.parseInt(height2.getText());
        } catch (NumberFormatException e) {
            // leave at default value
        }

        if (!game.getBoard().contains(start) || !game.getBoard().contains(end)) {
            return;
        }
        
        String toHit1 = "", toHit2 = "";
        ToHitData thd;
        if (flip) {
            thd = LosEffects.calculateLos(game,
                    buildAttackInfo(start, end, h1, h2, cboIsMech1.isSelected(),
                            cboIsMech2.isSelected())).losModifiers(game);
        } else {
            thd = LosEffects.calculateLos(game,
                    buildAttackInfo(end, start, h2, h1, cboIsMech2.isSelected(),
                            cboIsMech1.isSelected())).losModifiers(game);
        }
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            toHit1 = thd.getValue() + " = ";
        }
        toHit1 += thd.getDesc();

        if (flip) {
            thd = LosEffects.calculateLos(game,
                    buildAttackInfo(end, start, h2, h1, cboIsMech2.isSelected(),
                            cboIsMech1.isSelected())).losModifiers(game);
        } else {
            thd = LosEffects.calculateLos(game,
                    buildAttackInfo(start, end, h1, h2, cboIsMech1.isSelected(),
                            cboIsMech2.isSelected())).losModifiers(game);
        }
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            toHit2 = thd.getValue() + " = ";
        }
        toHit2 += thd.getDesc();

        tf_start.setText(start.toString());
        tf_end.setText(end.toString());
        tf_distance.setText("" + distance);
        tf_los1.setText(toHit1);
        // tf_los1.setCaretPosition(0);
        tf_los2.setText(toHit2);
        // tf_los2.setCaretPosition(0);
    }

    /**
     * Ignores determining if the attack is on land or under water.
     * 
     * @param c1
     * @param c2
     * @param h1
     * @param h2
     * @return
     */
    private LosEffects.AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1,
            int h2, boolean attackerIsMech, boolean targetIsMech) {
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = c1;
        ai.targetPos = c2;
        ai.attackHeight = h1;
        ai.targetHeight = h2;
        ai.attackerIsMech = attackerIsMech;
        ai.targetIsMech = targetIsMech;
        ai.attackAbsHeight = game.getBoard().getHex(c1).floor() + h1;
        ai.targetAbsHeight = game.getBoard().getHex(c2).floor() + h2;
        return ai;
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        if ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                addPoint(b.getCoords());
            }
        }

        bv.drawRuler(start, end, startColor, endColor);
    }

    @Override
    public void hexCursor(BoardViewEvent b) {
        //ignored
    }

    @Override
    public void boardHexHighlighted(BoardViewEvent b) {
        //ignored
    }

    @Override
    public void hexSelected(BoardViewEvent b) {
        //ignored
    }

    @Override
    public void firstLOSHex(BoardViewEvent b) {
        //ignored
    }

    @Override
    public void secondLOSHex(BoardViewEvent b) {
        //ignored
    }

    void butFlip_actionPerformed() {
        flip = !flip;

        if (startColor.equals(color1)) {
            startColor = color2;
            endColor = color1;
        } else {
            startColor = color1;
            endColor = color2;
        }

        heightLabel1.setForeground(startColor);
        heightLabel2.setForeground(endColor);

        setText();
        setVisible(true);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void butClose_actionPerformed() {
        clear();
        setVisible(false);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void height1_keyReleased() {
        setText();
        setVisible(true);
    }

    void height2_keyReleased() {
        setText();
        setVisible(true);
    }
    
    void checkBoxSelectionChanged() {
        setText();
        setVisible(true);
    }

    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        //ignored
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        //ignored
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }
}

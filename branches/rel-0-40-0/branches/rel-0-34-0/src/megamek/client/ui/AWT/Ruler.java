/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IBoardView;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.LosEffects;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;

/**
 * <p>
 * Title: Ruler
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author Ken Nguyen (kenn)
 * @version 1.0
 */
public class Ruler extends Dialog implements BoardViewListener {
    /**
     * 
     */
    private static final long serialVersionUID = 704013239167655138L;
    public static Color color1 = Color.cyan;
    public static Color color2 = Color.magenta;

    private Coords start;
    private Coords end;
    private Color startColor;
    private Color endColor;
    private int distance;
    private Client client;
    private IBoardView bv;
    private boolean flip;

    Panel panel1 = new Panel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    Button butFlip = new Button();
    Label jLabel1 = new Label();
    TextField tf_start = new TextField();
    Label jLabel2 = new Label();
    TextField tf_end = new TextField();
    Label jLabel3 = new Label();
    TextField tf_distance = new TextField();
    Label jLabel4 = new Label();
    TextField tf_los1 = new TextField();
    Label jLabel5 = new Label();
    TextField tf_los2 = new TextField();
    Button butClose = new Button();
    Label heightLabel1 = new Label();
    TextField height1 = new TextField();
    Label heightLabel2 = new Label();
    TextField height2 = new TextField();

    public Ruler(Frame f, Client c, IBoardView b) {
        super(f, Messages.getString("Ruler.title"), false); //$NON-NLS-1$
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        start = null;
        end = null;
        flip = true;
        startColor = color1;
        endColor = color2;

        bv = b;
        client = c;
        b.addBoardViewListener(this);

        try {
            jbInit();
            add(panel1);
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        butFlip.setLabel(Messages.getString("Ruler.flip")); //$NON-NLS-1$
        butFlip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                butFlip_actionPerformed(e);
            }
        });
        panel1.setLayout(gridBagLayout1);
        jLabel1.setText(Messages.getString("Ruler.Start")); //$NON-NLS-1$
        tf_start.setEditable(false);
        tf_start.setColumns(16);
        jLabel2.setText(Messages.getString("Ruler.End")); //$NON-NLS-1$
        tf_end.setEditable(false);
        tf_end.setColumns(16);
        jLabel3.setText(Messages.getString("Ruler.Distance")); //$NON-NLS-1$
        tf_distance.setEditable(false);
        tf_distance.setColumns(5);
        jLabel4.setText(Messages.getString("Ruler.POV") + ": "); //$NON-NLS-1$ //$NON-NLS-2$
        jLabel4.setForeground(startColor);
        tf_los1.setEditable(false);
        tf_los1.setColumns(30);
        jLabel5.setText(Messages.getString("Ruler.POV")); //$NON-NLS-1$
        jLabel5.setForeground(endColor);
        tf_los2.setEditable(false);
        tf_los2.setColumns(30);
        butClose.setLabel(Messages.getString("Ruler.Close")); //$NON-NLS-1$
        butClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                butClose_actionPerformed(e);
            }
        });
        heightLabel1.setText(Messages.getString("Ruler.Height1")); //$NON-NLS-1$
        heightLabel1.setForeground(startColor);
        height1.setText("1"); //$NON-NLS-1$
        height1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                height1_keyReleased(e);
            }
        });
        height1.setColumns(5);
        heightLabel2.setText(Messages.getString("Ruler.Height2")); //$NON-NLS-1$
        heightLabel2.setForeground(endColor);
        height2.setText("1"); //$NON-NLS-1$
        height2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                height2_keyReleased(e);
            }
        });
        height2.setColumns(5);

        GridBagConstraints c = new GridBagConstraints();
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 5, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        c.insets = new Insets(0, 0, 0, 0);
        gridBagLayout1.setConstraints(butFlip, c);
        panel1.add(butFlip);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(heightLabel1, c);
        panel1.add(heightLabel1);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(height1, c);
        panel1.add(height1);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(heightLabel2, c);
        panel1.add(heightLabel2);

        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(height2, c);
        panel1.add(height2);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(jLabel1, c);
        panel1.add(jLabel1);

        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 3;
        gridBagLayout1.setConstraints(tf_start, c);
        panel1.add(tf_start);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(jLabel2, c);
        panel1.add(jLabel2);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 3;
        gridBagLayout1.setConstraints(tf_end, c);
        panel1.add(tf_end);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(jLabel3, c);
        panel1.add(jLabel3);

        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 3;
        gridBagLayout1.setConstraints(tf_distance, c);
        panel1.add(tf_distance);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(jLabel4, c);
        panel1.add(jLabel4);

        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 3;
        gridBagLayout1.setConstraints(tf_los1, c);
        panel1.add(tf_los1);

        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        gridBagLayout1.setConstraints(jLabel5, c);
        panel1.add(jLabel5);

        c.gridx = 1;
        c.gridy = 6;
        c.gridwidth = 3;
        gridBagLayout1.setConstraints(tf_los2, c);
        panel1.add(tf_los2);

        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 5;
        gridBagLayout1.setConstraints(butClose, c);
        panel1.add(butClose);

        validate();

        setVisible(false);
    }

    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    void cancel() {
        dispose();
        butClose_actionPerformed(null);
    }

    public Coords getStart() {
        return start;
    }

    public void setStart(Coords start) {
        this.start = start;
    }

    public void setEnd(Coords end) {
        this.end = end;
    }

    public Coords getEnd() {
        return end;
    }

    public void clear() {
        start = null;
        end = null;
    }

    public void addPoint(Coords c) {
        if (start == null) {
            start = c;

        } else if (start.equals(c)) {
            clear();

            setVisible(false);
        } else {
            end = c;

            distance = start.distance(end);

            setText();
            setVisible(true);
        }
    }

    public void setText() {
        int h1 = 1, h2 = 1;
        try {
            h1 = Integer.parseInt(height1.getText());
        } catch (NumberFormatException e) {
        }

        try {
            h2 = Integer.parseInt(height2.getText());
        } catch (NumberFormatException e) {
        }

        String toHit1 = "", toHit2 = ""; //$NON-NLS-1$ //$NON-NLS-2$
        ToHitData thd;
        if (flip) {
            thd = LosEffects.calculateLos(client.game,
                    buildAttackInfo(start, end, h1, h2)).losModifiers(
                    client.game);
        } else {
            thd = LosEffects.calculateLos(client.game,
                    buildAttackInfo(end, start, h2, h1)).losModifiers(
                    client.game);
        }
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            toHit1 = thd.getValue() + " = "; //$NON-NLS-1$
        }
        toHit1 += thd.getDesc();

        if (flip) {
            thd = LosEffects.calculateLos(client.game,
                    buildAttackInfo(end, start, h2, h1)).losModifiers(
                    client.game);
        } else {
            thd = LosEffects.calculateLos(client.game,
                    buildAttackInfo(start, end, h1, h2)).losModifiers(
                    client.game);
        }
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            toHit2 = thd.getValue() + " = "; //$NON-NLS-1$
        }
        toHit2 += thd.getDesc();

        tf_start.setText(start.toString());
        tf_end.setText(end.toString());
        tf_distance.setText("" + distance); //$NON-NLS-1$
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
    LosEffects.AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1, int h2) {
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = c1;
        ai.targetPos = c2;
        ai.attackHeight = h1;
        ai.targetHeight = h2;
        ai.attackAbsHeight = client.game.getBoard().getHex(c1).floor() + h1;
        ai.targetAbsHeight = client.game.getBoard().getHex(c2).floor() + h2;
        return ai;
    }

    public boolean valid() {
        return (start != null) && (end != null);
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public void hexMoused(BoardViewEvent b) {
        if ((b.getModifiers() & InputEvent.ALT_MASK) != 0) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                addPoint(b.getCoords());
            }
        }

        bv.drawRuler(start, end, startColor, endColor);
    }

    public void hexCursor(BoardViewEvent b) {

    }

    public void boardHexHighlighted(BoardViewEvent b) {

    }

    public void hexSelected(BoardViewEvent b) {

    }

    public void firstLOSHex(BoardViewEvent b) {

    }

    public void secondLOSHex(BoardViewEvent b, Coords c) {

    }

    void butFlip_actionPerformed(ActionEvent e) {
        flip = !flip;

        if (startColor == color1) {
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

    void butClose_actionPerformed(ActionEvent e) {
        clear();
        setVisible(false);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void height1_keyReleased(KeyEvent e) {
        setText();
        setVisible(true);
    }

    void height2_keyReleased(KeyEvent e) {
        setText();
        setVisible(true);
    }

    public void boardChangedEntity(BoardViewEvent b) {
    }

    public void boardNewAttack(BoardViewEvent a) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.client.BoardViewListener#finishedMovingUnits(megamek.client.BoardViewEvent)
     */
    public void finishedMovingUnits(BoardViewEvent b) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.client.BoardViewListener#selectUnit(megamek.client.BoardViewEvent)
     */
    public void unitSelected(BoardViewEvent b) {
    }
}

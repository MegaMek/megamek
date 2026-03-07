/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.panels.LOSElevationDiagramPanel;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.losDiagram.DiagramUnitType;
import megamek.common.losDiagram.LOSDiagramData;
import megamek.common.losDiagram.LOSDiagramDataBuilder;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.SmokeCloud;

/**
 * @author Ken Nguyen (kenn)
 */
public class RulerDialog extends JDialog implements BoardViewListener {
    private static final MMLogger logger = MMLogger.create(RulerDialog.class);

    @Serial
    private static final long serialVersionUID = -4820402626782115601L;
    public static Color color1 = Color.cyan;
    public static Color color2 = Color.magenta;

    private Coords start;
    private Coords end;
    private Color startColor;
    private Color endColor;
    private int distance;
    private final BoardView bv;
    private final Game game;
    private boolean flip;

    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private final JButton butFlip = new JButton();
    private final JTextField tf_start = new JTextField();
    private final JTextField tf_end = new JTextField();
    private final JTextField tf_distance = new JTextField();
    private final JTextField tf_los1 = new JTextField();
    private final JTextField tf_los2 = new JTextField();
    private final JButton butClose = new JButton();
    private JLabel heightLabel1;
    private final JTextField height1 = new JTextField();
    private JLabel heightLabel2;
    private final JTextField height2 = new JTextField();

    private final JCheckBox cboIsMek1 = new JCheckBox(Messages.getString("Ruler.isMek"));
    private final JCheckBox cboIsMek2 = new JCheckBox(Messages.getString("Ruler.isMek"));
    private String entityName1 = "";
    private String entityName2 = "";
    private DiagramUnitType unitType1 = DiagramUnitType.OTHER;
    private DiagramUnitType unitType2 = DiagramUnitType.OTHER;
    private Entity entity1;
    private Entity entity2;

    private final JButton butDiagram = new JButton();
    private final LOSElevationDiagramPanel diagramPanel = new LOSElevationDiagramPanel();
    private final JScrollPane diagramScrollPane = new JScrollPane(diagramPanel);
    private boolean diagramExpanded;

    public RulerDialog(JFrame frame, Client client, BoardView boardView, Game game) {
        super(frame, getRulerTitle(game), false);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        start = null;
        end = null;
        flip = true;
        startColor = color1;
        endColor = color2;

        bv = boardView;
        this.game = game;
        boardView.addBoardViewListener(this);

        try {
            jbInit();
            restoreSavedBounds();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    saveBounds();
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    saveBounds();
                }
            });
        } catch (Exception ex) {
            logger.error(ex, "");
        }
    }

    private void jbInit() {
        JPanel buttonPanel = new JPanel();
        butFlip.setText(Messages.getString("Ruler.flip"));
        butFlip.addActionListener(e -> butFlip_actionPerformed());
        JPanel panelMain = new JPanel(gridBagLayout1);
        JLabel jLabel1 = new JLabel(Messages.getString("Ruler.Start"), SwingConstants.RIGHT);
        tf_start.setEditable(false);
        tf_start.setColumns(16);
        JLabel jLabel2 = new JLabel(Messages.getString("Ruler.End"), SwingConstants.RIGHT);
        tf_end.setEditable(false);
        tf_end.setColumns(16);
        JLabel jLabel3 = new JLabel(Messages.getString("Ruler.Distance"), SwingConstants.RIGHT);
        tf_distance.setEditable(false);
        tf_distance.setColumns(5);
        JLabel jLabel4 = new JLabel(Messages.getString("Ruler.attackerPOV") + ":", SwingConstants.RIGHT);
        jLabel4.setForeground(startColor);
        tf_los1.setEditable(false);
        tf_los1.setColumns(30);
        JLabel jLabel5 = new JLabel(Messages.getString("Ruler.targetPOV") + ":", SwingConstants.RIGHT);
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
        cboIsMek1.setToolTipText(Messages.getString("Ruler.isMekTooltip"));
        cboIsMek1.addItemListener(e -> checkBoxSelectionChanged());

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
        cboIsMek2.setToolTipText(Messages.getString("Ruler.isMekTooltip"));
        cboIsMek2.addItemListener(e -> checkBoxSelectionChanged());

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
        gridBagLayout1.setConstraints(cboIsMek1, c);
        panelMain.add(cboIsMek1);

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
        gridBagLayout1.setConstraints(cboIsMek2, c);
        panelMain.add(cboIsMek2);

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
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        gridBagLayout1.setConstraints(buttonPanel, c);
        panelMain.add(buttonPanel);

        // Diagram controls panel (toggle button + all terrain checkbox)
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        diagramExpanded = guiPreferences.getRulerDiagramExpanded();
        updateDiagramButtonText();
        butDiagram.addActionListener(e -> toggleDiagram());

        JPanel diagramControlsPanel = new JPanel();
        diagramControlsPanel.add(butDiagram);
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(4, 0, 4, 0);
        gridBagLayout1.setConstraints(diagramControlsPanel, c);
        panelMain.add(diagramControlsPanel);

        // Diagram panel (collapsible)
        diagramScrollPane.setVisible(diagramExpanded);
        diagramScrollPane.setMinimumSize(UIUtil.scaleForGUI(200, 150));
        diagramScrollPane.setPreferredSize(UIUtil.scaleForGUI(500, 200));
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        gridBagLayout1.setConstraints(diagramScrollPane, c);
        panelMain.add(diagramScrollPane);

        JScrollPane sp = new JScrollPane(panelMain);
        setLayout(new BorderLayout());
        add(sp);

        setResizable(true);
        setMinimumSize(UIUtil.scaleForGUI(350, 250));
        validate();
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
        entityName1 = "";
        entityName2 = "";
        unitType1 = DiagramUnitType.OTHER;
        unitType2 = DiagramUnitType.OTHER;
        entity1 = null;
        entity2 = null;
    }

    private void addPoint(Coords c) {
        int absHeight = Integer.MIN_VALUE;
        boolean isMek = false;
        boolean entFound = false;
        String entityName = "";
        DiagramUnitType unitType = DiagramUnitType.OTHER;
        Entity tallestEntity = null;
        for (Entity ent : game.getEntitiesVector(c)) {
            // Convert to TW height: relHeight + 1 (code 0-indexed -> TW 1-indexed)
            // Hull-down Mek: 1 TW level instead of 2
            int twHeight = ent.relHeight() + 1;
            if ((ent instanceof Mek) && ent.isHullDown()) {
                twHeight -= 1;
            }
            if (twHeight > absHeight) {
                absHeight = twHeight;
                isMek = ent instanceof Mek;
                entityName = ent.getDisplayName();
                unitType = DiagramUnitType.fromEntity(ent);
                tallestEntity = ent;
                entFound = true;
            }
        }
        if (start == null) {
            start = c;
            if (entFound) {
                height1.setText(absHeight + "");
                cboIsMek1.setSelected(isMek);
                entityName1 = entityName;
                unitType1 = unitType;
                entity1 = tallestEntity;
            }
        } else if (start.equals(c)) {
            clear();
            setVisible(false);
        } else {
            end = c;
            distance = start.distance(end);
            if (entFound) {
                height2.setText(absHeight + "");
                cboIsMek2.setSelected(isMek);
                entityName2 = entityName;
                unitType2 = unitType;
                entity2 = tallestEntity;
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

        boolean isMek1 = cboIsMek1.isSelected();
        boolean isMek2 = cboIsMek2.isSelected();

        // Attacker POV: attacker shoots at target
        Coords attackerPos = flip ? start : end;
        Coords targetPos = flip ? end : start;
        int attackerHeight = flip ? h1 : h2;
        int targetHeight = flip ? h2 : h1;
        boolean attackerIsMek = flip ? isMek1 : isMek2;
        boolean targetIsMek = flip ? isMek2 : isMek1;

        String toHit1 = computeFullModifiers(attackerPos, targetPos,
              attackerHeight, targetHeight, attackerIsMek, targetIsMek);

        // Target POV: target shoots back at attacker (swap roles)
        String toHit2 = computeFullModifiers(targetPos, attackerPos,
              targetHeight, attackerHeight, targetIsMek, attackerIsMek);

        tf_start.setText(start.toString());
        tf_end.setText(end.toString());
        tf_distance.setText("" + distance);
        tf_los1.setText(toHit1);
        tf_los2.setText(toHit2);

        updateDiagram();
    }

    /**
     * Computes the combined to-hit modifiers for a hypothetical attack, including LOS modifiers (intervening terrain),
     * attacker hex terrain, target hex terrain, and water partial cover. Mirrors the fire phase calculation from
     * {@code ComputeTerrainMods} but without requiring actual Entity objects. Movement TMMs are excluded.
     *
     * <p>Heights are dynamically adjusted for entity state (hull-down, prone) by checking actual
     * entities at each hex, so the calculation stays accurate even if state changed after the Ruler was opened.</p>
     */
    private String computeFullModifiers(Coords attackerPos, Coords targetPos,
          int attackerHeight, int targetHeight, boolean attackerIsMek, boolean targetIsMek) {
        // LosEffects needs the physical (non-hull-down) heights to correctly detect partial
        // cover, matching the real game where Mek.height() doesn't change for hull-down.
        // The hull-down modifier (+2) is applied separately via addTargetEntityStateModifiers.
        int losAttackerHeight = attackerHeight;
        int losTargetHeight = targetHeight;
        if (attackerIsMek && isMekHullDownAt(attackerPos)) {
            losAttackerHeight += 1;
        }
        if (targetIsMek && isMekHullDownAt(targetPos)) {
            losTargetHeight += 1;
        }

        LosEffects.AttackInfo attackInfo = buildAttackInfo(attackerPos, targetPos,
              losAttackerHeight, losTargetHeight, attackerIsMek, targetIsMek);
        LosEffects losEffects = LosEffects.calculateLos(game, attackInfo);
        ToHitData thd = losEffects.losModifiers(game);

        // If LOS is blocked, no point adding terrain modifiers
        if (thd.getValue() == TargetRoll.IMPOSSIBLE) {
            return thd.getDesc();
        }

        // Attacker hex terrain modifiers (matching Compute.getAttackerTerrainModifier)
        Hex attackerHex = game.getBoard().getHex(attackerPos);
        if (attackerHex != null) {
            addAttackerTerrainModifiers(thd, attackerHex);
        }

        // Target hex terrain modifiers (matching Compute.getTargetTerrainModifier)
        Hex targetHex = game.getBoard().getHex(targetPos);
        if (targetHex != null) {
            addTargetTerrainModifiers(thd, targetHex, targetHeight, targetIsMek);
        }

        // Water partial cover (matching ComputeTerrainMods lines 167-180)
        if ((targetHex != null) && targetIsMek) {
            addWaterPartialCover(thd, losEffects, targetHex, targetHeight);
        }

        // Target entity state modifiers (prone, immobile, hull down) from actual
        // entities on the board at the target hex
        int hexDistance = attackerPos.distance(targetPos);
        addTargetEntityStateModifiers(thd, losEffects, targetPos, hexDistance);

        String result = "";
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            result = thd.getValue() + " = ";
        }
        result += thd.getDesc();
        return result;
    }

    /**
     * Adds attacker hex terrain modifiers. Mirrors {@code Compute.getAttackerTerrainModifier()}.
     */
    private void addAttackerTerrainModifiers(ToHitData thd, Hex attackerHex) {
        int screenLevel = attackerHex.terrainLevel(Terrains.SCREEN);
        if (screenLevel > 0) {
            thd.addModifier(screenLevel + 1, "attacker in screen(s)");
        }
    }

    /**
     * Adds target hex terrain modifiers. Mirrors {@code Compute.getTargetTerrainModifier()} for the subset of modifiers
     * computable without an Entity object.
     *
     * @param thd             the to-hit data to append modifiers to
     * @param targetHex       the target's hex
     * @param targetRelHeight the target's relative height (elevation + unit height)
     * @param targetIsMek     whether the target is a Mek
     */
    private void addTargetTerrainModifiers(ToHitData thd, Hex targetHex,
          int targetRelHeight, boolean targetIsMek) {
        // Woods/Jungle in target hex
        boolean hasWoods = targetHex.containsTerrain(Terrains.WOODS)
              || targetHex.containsTerrain(Terrains.JUNGLE);
        int foliageElev = targetHex.terrainLevel(Terrains.FOLIAGE_ELEV);
        if (foliageElev == Terrain.LEVEL_NONE) {
            foliageElev = 0;
        }

        // Target is above woods if relHeight + 1 > foliage_elev
        boolean isAboveWoods = !hasWoods || (targetRelHeight + 1 > foliageElev);

        if (!isAboveWoods
              && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER)) {
            int woodsLevel = targetHex.terrainLevel(Terrains.WOODS);
            int jungleLevel = targetHex.terrainLevel(Terrains.JUNGLE);
            String foliageType = "woods";
            int effectiveLevel = woodsLevel;
            if (jungleLevel > woodsLevel) {
                effectiveLevel = jungleLevel;
                foliageType = "jungle";
            }
            if (effectiveLevel == 1) {
                thd.addModifier(1, "target in light " + foliageType);
            } else if (effectiveLevel >= 2) {
                thd.addModifier(effectiveLevel, "target in heavy " + foliageType);
            }
        }

        // Smoke in target hex
        boolean isAboveSmoke = (targetRelHeight + 1 > 2)
              || !targetHex.containsTerrain(Terrains.SMOKE);
        if (!isAboveSmoke) {
            int smokeLevel = targetHex.terrainLevel(Terrains.SMOKE);
            switch (smokeLevel) {
                case SmokeCloud.SMOKE_LIGHT:
                case SmokeCloud.SMOKE_LI_LIGHT:
                case SmokeCloud.SMOKE_LI_HEAVY:
                case SmokeCloud.SMOKE_CHAFF_LIGHT:
                case SmokeCloud.SMOKE_GREEN:
                    thd.addModifier(1, "target in light smoke");
                    break;
                case SmokeCloud.SMOKE_HEAVY:
                    thd.addModifier(2, "target in heavy smoke");
                    break;
                default:
                    break;
            }
        }

        // Erupting geyser
        if (targetHex.terrainLevel(Terrains.GEYSER) == 2) {
            thd.addModifier(2, "target in erupting geyser");
        }

        // Heavy industrial zone (target not above structures)
        if (targetHex.containsTerrain(Terrains.INDUSTRIAL)) {
            int ceiling = targetHex.ceiling();
            if (targetRelHeight <= ceiling) {
                thd.addModifier(1, "target in heavy industrial zone");
            }
        }

        // Screen in target hex
        int screenLevel = targetHex.terrainLevel(Terrains.SCREEN);
        if (screenLevel > 0) {
            thd.addModifier(screenLevel + 1, "target in screen(s)");
        }
    }

    /**
     * Adds water partial cover for a Mek target standing in depth 1 water. Mirrors {@code ComputeTerrainMods} lines
     * 167-180. In the fire phase, water partial cover is OR'd into existing target cover. The +1 modifier comes from
     * {@code losModifiers()} if any partial cover is already set from terrain. We only add it here when water is the
     * sole source of partial cover.
     *
     * @param thd             the to-hit data to append modifiers to
     * @param losEffects      the LOS effects (checked for existing terrain partial cover)
     * @param targetHex       the target's hex
     * @param targetRelHeight the target's relative height (elevation + unit height)
     */
    private void addWaterPartialCover(ToHitData thd, LosEffects losEffects,
          Hex targetHex, int targetRelHeight) {
        if (!targetHex.containsTerrain(Terrains.WATER)) {
            return;
        }

        int waterDepth = targetHex.terrainLevel(Terrains.WATER);
        if (waterDepth == Terrain.LEVEL_NONE) {
            return;
        }

        // ComputeTerrainMods checks: waterLevel == 1, targEl == 0, height > 0
        // targEl = entity.relHeight() = elevation + height
        // For a Mek (height=1) in depth 1 water: elevation=-1, relHeight=0
        // targetIsMek is guaranteed by the caller's guard (Mek has height > 0)
        if ((waterDepth == 1) && (targetRelHeight == 0)) {
            boolean terrainCoverAlreadyApplied = losEffects.getTargetCover() != LosEffects.COVER_NONE;
            if (!terrainCoverAlreadyApplied) {
                thd.addModifier(1, "target has partial cover (water)");
            }
        }
    }

    /**
     * Checks if a Mek at the given hex is hull-down.
     *
     * @param hexPos the hex coordinates to check
     *
     * @return true if a Mek at the hex is hull-down
     */
    private boolean isMekHullDownAt(Coords hexPos) {
        List<Entity> entities = game.getEntitiesVector(hexPos);
        for (Entity entity : entities) {
            if ((entity instanceof Mek) && entity.isHullDown()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds target entity state modifiers (prone, immobile, hull down, stuck) from actual entities present on the board
     * at the target hex. If multiple entities are present, applies the state of the first one found. These mirror the
     * fire phase modifiers from {@code ComputeTargetToHitMods} and {@code ComputeTerrainMods}.
     *
     * @param thd        the to-hit data to append modifiers to
     * @param losEffects the LOS effects (used for hull down partial cover check)
     * @param targetPos  the target hex coordinates
     * @param distance   the hex distance between attacker and target
     */
    private void addTargetEntityStateModifiers(ToHitData thd, LosEffects losEffects,
          Coords targetPos, int distance) {
        List<Entity> entitiesAtTarget = game.getEntitiesVector(targetPos);
        if (entitiesAtTarget.isEmpty()) {
            return;
        }

        // Use the first entity at the target hex for state checks
        Entity targetEntity = entitiesAtTarget.get(0);

        // Prone: -2 if adjacent (distance <= 1), +1 at range (distance > 1)
        if (targetEntity.isProne()) {
            if (distance <= 1) {
                thd.addModifier(-2, "target prone (adjacent)");
            } else {
                thd.addModifier(1, "target prone (range)");
            }
        }

        // Immobile: -4
        if (targetEntity.isImmobile()) {
            thd.addModifier(-4, "target immobile");
        }

        // Hull Down: +2 for Meks with partial cover
        if (targetEntity.isHullDown() && (targetEntity instanceof Mek)) {
            if (losEffects.getTargetCover() > LosEffects.COVER_NONE) {
                thd.addModifier(2, "target hull down");
            }
        }

        // Stuck in swamp: -2
        if (targetEntity.isStuck()) {
            thd.addModifier(-2, "target stuck in swamp");
        }
    }

    /**
     * Updates the elevation diagram panel with current LOS data.
     */
    private void updateDiagram() {
        if (!diagramExpanded || start == null || end == null) {
            return;
        }

        int h1 = 1;
        int h2 = 1;
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

        LosEffects.AttackInfo attackInfo;
        if (flip) {
            attackInfo = buildAttackInfo(start, end, h1, h2,
                  cboIsMek1.isSelected(), cboIsMek2.isSelected());
        } else {
            attackInfo = buildAttackInfo(end, start, h2, h1,
                  cboIsMek2.isSelected(), cboIsMek1.isSelected());
        }

        Coords attackerPos = flip ? start : end;
        Coords targetPos = flip ? end : start;
        boolean attackerHullDown = isMekHullDownAt(attackerPos);
        boolean targetHullDown = isMekHullDownAt(targetPos);

        String attackerName = flip ? entityName1 : entityName2;
        String targetName = flip ? entityName2 : entityName1;
        DiagramUnitType attackerType = flip ? unitType1 : unitType2;
        DiagramUnitType targetType = flip ? unitType2 : unitType1;
        LOSDiagramData diagramData = LOSDiagramDataBuilder.build(game, attackInfo,
              attackerHullDown, targetHullDown, attackerType, targetType,
              attackerName, targetName);

        Entity attackerEntity = flip ? entity1 : entity2;
        Entity targetEntity = flip ? entity2 : entity1;
        Image attackerSprite = getEntitySprite(attackerEntity);
        Image targetSprite = getEntitySprite(targetEntity);
        diagramPanel.setData(diagramData, attackerSprite, targetSprite);
    }

    /**
     * Gets the sprite image for an entity from the tileset manager, or null if unavailable.
     */
    private Image getEntitySprite(Entity entity) {
        if (entity == null) {
            return null;
        }
        return bv.getTilesetManager().imageFor(entity);
    }

    private void toggleDiagram() {
        diagramExpanded = !diagramExpanded;
        updateDiagramButtonText();
        diagramScrollPane.setVisible(diagramExpanded);
        GUIPreferences.getInstance().setRulerDiagramExpanded(diagramExpanded);

        if (diagramExpanded) {
            updateDiagram();
        }

        revalidate();
        repaint();
    }

    private void updateDiagramButtonText() {
        butDiagram.setText(diagramExpanded
              ? Messages.getString("Ruler.hideDiagram")
              : Messages.getString("Ruler.showDiagram"));
    }

    private void restoreSavedBounds() {
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        int savedWidth = guiPreferences.getRulerSizeWidth();
        int savedHeight = guiPreferences.getRulerSizeHeight();
        int savedX = guiPreferences.getRulerPosX();
        int savedY = guiPreferences.getRulerPosY();

        if ((savedWidth > 0) && (savedHeight > 0)) {
            setSize(savedWidth, savedHeight);
        } else {
            setSize(UIUtil.scaleForGUI(600, 350));
        }
        if ((savedX >= 0) && (savedY >= 0)) {
            setLocation(savedX, savedY);
        } else {
            setLocationRelativeTo(getOwner());
        }
        UIUtil.updateWindowBounds(this);
    }

    private void saveBounds() {
        if (!isVisible()) {
            return;
        }
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        guiPreferences.setRulerSizeWidth(getWidth());
        guiPreferences.setRulerSizeHeight(getHeight());
        guiPreferences.setRulerPosX(getX());
        guiPreferences.setRulerPosY(getY());
    }

    /**
     * Returns the ruler dialog title based on which optional LOS rules are active.
     */
    private static String getRulerTitle(Game game) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)) {
            return Messages.getString("Ruler.titleDiagrammedLOS");
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)) {
            return Messages.getString("Ruler.titleDeadZone");
        }
        return Messages.getString("Ruler.title");
    }

    /**
     * Builds an AttackInfo for the Ruler tool, matching the calculation in
     * {@code LosEffects.calculateLOS()}.
     *
     * <p>The height parameters (h1, h2) are TW unit heights from the height text fields,
     * auto-populated from entity state (e.g., Mek = 2, hull-down Mek = 1, VTOL at elev 5 = 6).
     * These are converted to code-internal absHeight by subtracting 1 (TW to code conversion)
     * and adding the hex ground level.</p>
     */
    private LosEffects.AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1, int h2, boolean attackerIsMek,
          boolean targetIsMek) {
        LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
        attackInfo.attackPos = c1;
        attackInfo.targetPos = c2;
        attackInfo.attackerIsMek = attackerIsMek;
        attackInfo.targetIsMek = targetIsMek;

        // attackHeight/targetHeight = intrinsic unit height (how tall, not position)
        // Mek.height() = 1 (code 0-indexed; TW = 2 levels), non-Mek = 0 (TW = 1 level)
        attackInfo.attackHeight = attackerIsMek ? 1 : 0;
        attackInfo.targetHeight = targetIsMek ? 1 : 0;

        Hex attackerHex = game.getBoard().getHex(c1);
        Hex targetHex = game.getBoard().getHex(c2);

        // h1/h2 are TW heights (1-indexed). Subtract 1 to convert to code units (0-indexed),
        // then add hexLevel to get absolute height matching LosEffects formula.
        attackInfo.attackAbsHeight = (h1 - 1) + attackerHex.getLevel();
        attackInfo.targetAbsHeight = (h2 - 1) + targetHex.getLevel();

        // Set water state flags (matching LosEffects.calculateLOS entity-based logic)
        boolean attackerHasWater = attackerHex.containsTerrain(Terrains.WATER)
              && (attackerHex.depth() > 0);
        boolean targetHasWater = targetHex.containsTerrain(Terrains.WATER)
              && (targetHex.depth() > 0);

        attackInfo.attUnderWater = attackerHasWater
              && (attackInfo.attackAbsHeight < attackerHex.getLevel());
        attackInfo.attInWater = attackerHasWater
              && (attackInfo.attackAbsHeight == attackerHex.getLevel());
        attackInfo.attOnLand = !(attackInfo.attUnderWater || attackInfo.attInWater);

        attackInfo.targetUnderWater = targetHasWater
              && (attackInfo.targetAbsHeight < targetHex.getLevel());
        attackInfo.targetInWater = targetHasWater
              && (attackInfo.targetAbsHeight == targetHex.getLevel());
        attackInfo.targetOnLand = !(attackInfo.targetUnderWater || attackInfo.targetInWater);

        return attackInfo;
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        // ALT+click triggers the ruler/LOS tool via BOARD_HEX_CLICKED.
        // CTRL+click is intercepted by BoardView.checkLOS() and arrives
        // via firstLOSHex()/secondLOSHex() events instead.
        if ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                addPoint(b.getCoords());
            }
        }

        bv.drawRuler(start, end, startColor, endColor);
    }

    @Override
    public void hexCursor(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void boardHexHighlighted(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void hexSelected(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void firstLOSHex(BoardViewEvent b) {
        addPoint(b.getCoords());
        bv.drawRuler(start, end, startColor, endColor);
    }

    @Override
    public void secondLOSHex(BoardViewEvent b) {
        addPoint(b.getCoords());
        bv.drawRuler(start, end, startColor, endColor);
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
        // ignored
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // ignored
    }
}

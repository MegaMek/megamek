/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.LabelDisplayStyle;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.units.*;

/**
 * Sprite for an entity. Changes whenever the entity changes. Consists of an image, drawn from the Tile Manager; facing
 * and possibly secondary facing arrows; armor and internal bars; and an identification label.
 */
public class EntitySprite extends Sprite {

    // Statics
    private static final int SMALL = 0;
    private static final int STATUS_BAR_LENGTH = 24;
    private static final int STATUS_BAR_X = 55;
    private static final int MAX_TMM_PIPS = 6;
    private static final int BIGGER_PIP_SCALE = 2;
    private static final int BIGGER_PIP_OFFSET = 1;
    private static final int TMM_PIP_SIZE = STATUS_BAR_LENGTH / MAX_TMM_PIPS;
    private static final boolean DIRECT = true;
    private static final Color LABEL_CRITICAL_BACK = new Color(200, 0, 0, 200);
    private static final Color LABEL_SPACE_BACK = new Color(0, 0, 200, 200);
    private static final Color LABEL_GROUND_BACK = new Color(50, 50, 50, 200);

    enum Positioning {
        LEFT, RIGHT
    }

    // Individuals
    final Entity entity;

    private final Image radarBlipImage;
    private final int secondaryPos;

    private Rectangle entityRect;
    private Rectangle labelRect;
    private Font labelFont;
    private Point hexOrigin;
    private boolean criticalStatus;
    private Positioning labelPos;
    private final Color labelBack;
    /** Used to color the label when this unit is selected for movement etc. */
    private boolean isSelected;

    // Keep track of ECM state, as it's too expensive to compute on the fly.
    private boolean isAffectedByECM = false;

    /**
     * Generic terms that can be removed from the end of vehicle names to create a chassis name.
     */
    private static final Set<String> REMOVABLE_NAME_PARTS = Set.of("Defense",
          "Heavy",
          "Medium",
          "Light",
          "Artillery",
          "Tank",
          "Wheeled",
          "Command",
          "Standard",
          "Hover",
          "Hovercraft",
          "Mechanized",
          "(Standard)",
          "Platoon",
          "Transport",
          "Vehicle",
          "Air",
          "Assault",
          "Mobile",
          "Platform",
          "Battle Armor",
          "Vessel",
          "Infantry",
          "Fighting",
          "Fire",
          "Support",
          "Reconnaissance",
          "Fast");

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public EntitySprite(BoardView boardView1, final Entity entity, int secondaryPos, Image radarBlipImage) {
        super(boardView1);
        this.entity = entity;
        this.radarBlipImage = radarBlipImage;
        this.secondaryPos = secondaryPos;
        if (bv.getBoard().isSpace()) {
            labelBack = LABEL_SPACE_BACK;
        } else {
            labelBack = LABEL_GROUND_BACK;
        }
        getBounds();
    }

    public Entity getEntity() {
        return entity;
    }

    private String getAdjShortName() {
        Coords position = entity.getPosition();
        boolean multipleUnits = bv.game.getEntitiesVector(position, entity.getBoardId(), true).size() > 4;

        if (onlyDetectedBySensors()) {
            return Messages.getString("BoardView1.sensorReturn");
        } else if (multipleUnits) {
            if (GUIP.getUnitLabelStyle() == LabelDisplayStyle.ONLY_STATUS) {
                return "";
            } else {
                return Messages.getString("BoardView1.multipleUnits");
            }
        } else {
            switch (GUIP.getUnitLabelStyle()) {
                case FULL:
                    return standardLabelName();
                case ABBREV:
                    return (entity instanceof Mek) ? entity.getModel() : abbreviateUnitName(standardLabelName());
                case CHASSIS:
                    return reduceVehicleName(entity);
                case NICKNAME:
                    if (!pilotNick().isBlank()) {
                        return "\"" + pilotNick().toUpperCase() + "\"";
                    } else if (!unitNick().isBlank()) {
                        return "'" + unitNick() + "'";
                    } else {
                        return reduceVehicleName(entity);
                    }
                case ONLY_NICKNAME:
                    if (!pilotNick().isBlank()) {
                        return "\"" + pilotNick().toUpperCase() + "\"";
                    } else if (!unitNick().isBlank()) {
                        return "'" + unitNick() + "'";
                    } else {
                        return "";
                    }
                default: // ONLY_STATUS
                    return "";
            }
        }
    }

    /**
     * Returns a shortened unit name string, mostly for vehicles. Words contained in the removableNameStrings list are
     * taken away from the end of the name until something is encountered that is not contained in that list. On Mek
     * names this will typically have no effect.
     */
    private static String reduceVehicleName(Entity entity) {
        if (!entity.isVehicle()) {
            return entity.getChassis();
        } else {
            String[] tokens = entity.getChassis().split(" ");
            int i = tokens.length - 1;
            for (;
                  i > 0;
                  i--) {
                if (!REMOVABLE_NAME_PARTS.contains(tokens[i])) {
                    break;
                }
            }
            return String.join(" ", Arrays.copyOfRange(tokens, 0, i + 1));
        }
    }

    /** Returns the string with some content shortened like Battle Armor -> BA */
    private static String abbreviateUnitName(String unitName) {
        return unitName.replace("(Standard)", "")
              .replace("Battle Armor", "BA")
              .replace("Standard", "Std.")
              .replace("Vehicle", "Veh.")
              .replace("Medium", "Med.")
              .replace("Support", "Spt.")
              .replace("Heavy", "Hvy.")
              .replace("Light", "Lgt.")
              .replace("Assault", "Asslt.")
              .replace("Transport", "Trnsp.")
              .replace("Command", "Cmd.")
              .replace("Mechanized", "Mechn.")
              .replace("Wheeled", "Whee.")
              .replace("Platoon", "Plt.")
              .replace("Artillery", "Arty.")
              .replace("Defense", "Def.")
              .replace("Hovercraft", "Hov.")
              .replace("Platoon", "Plt.")
              .replace("Reconnaissance", "Rcn.")
              .replace("Recon", "Rcn.")
              .replace("Tank", "Tk.")
              .replace("Hover ", "Hov. ");
    }

    private String pilotNick() {
        if ((entity.getCrew().getSize() >= 1) && !entity.getCrew().getNickname().isBlank()) {
            return entity.getCrew().getNickname();
        } else {
            return "";
        }
    }

    private String unitNick() {
        String name = entity.getShortName();
        int firstApo = name.indexOf('\'');
        int secondApo = name.indexOf('\'', name.indexOf('\'') + 1);
        if ((firstApo >= 0) && (secondApo >= 0)) {
            return name.substring(firstApo + 1, secondApo);
        } else {
            return "";
        }
    }

    private String standardLabelName() {
        return entity.getShortName();
    }

    @Override
    public Rectangle getBounds() {
        // Start with the hex and add the label
        bounds = new Rectangle(0, 0, bv.getHexSize().width, bv.getHexSize().height);
        updateLabel();
        bounds.add(labelRect);
        // Add space for 4 little status boxes
        if (labelPos == Positioning.RIGHT) {
            bounds.add(-4 * (labelRect.height + 2) + labelRect.x, labelRect.y);
        } else {
            bounds.add(4 * (labelRect.height + 2) + labelRect.x + labelRect.width, labelRect.y);
        }

        // Move to the board position, save this origin for correct drawing
        hexOrigin = bounds.getLocation();
        Point ePos;
        if (secondaryPos == -1) {
            ePos = bv.getHexLocation(entity.getPosition());
        } else {
            ePos = bv.getHexLocation(entity.getSecondaryPositions().get(secondaryPos));
        }

        if (ePos != null) {
            bounds.setLocation(hexOrigin.x + ePos.x, hexOrigin.y + ePos.y);
        }

        entityRect = new Rectangle(bounds.x + (int) (20 * bv.getScale()),
              bounds.y + (int) (14 * bv.getScale()),
              (int) (44 * bv.getScale()),
              (int) (44 * bv.getScale()));

        return bounds;
    }

    private void updateLabel() {
        Rectangle oldRect = new Rectangle();
        if (labelRect != null) {
            oldRect = new Rectangle(labelRect);
        }

        int face = (entity.isCommander() && !onlyDetectedBySensors()) ? Font.ITALIC : Font.PLAIN;
        labelFont = new Font(MMConstants.FONT_SANS_SERIF, face, (int) (10 * Math.max(bv.getScale(), 0.9)));

        // Check the hexes in directions 2, 5, 1, 4 if they are free of entities
        // and place the label in the direction of the first free hex
        // if none are free, the label will be centered in the current hex
        labelRect = new Rectangle(bv.getPanel().getFontMetrics(labelFont).stringWidth(getAdjShortName()) + 4,
              bv.getPanel().getFontMetrics(labelFont).getAscent() + 2);

        Coords position = entity.getPosition();

        if (position != null) {
            if (bv.game.getEntitiesVector(position.translated("SE"), entity.getBoardId(), true).isEmpty()) {
                labelRect.setLocation((int) (bv.getHexSize().width * 0.55), (int) (0.75 * bv.getHexSize().height));
                labelPos = Positioning.RIGHT;
            } else if (bv.game.getEntitiesVector(position.translated("NW"), entity.getBoardId(), true).isEmpty()) {
                labelRect.setLocation((int) (bv.getHexSize().width * 0.45) - labelRect.width,
                      (int) (0.25 * bv.getHexSize().height) - labelRect.height);
                labelPos = Positioning.LEFT;
            } else if (bv.game.getEntitiesVector(position.translated("NE"), entity.getBoardId(), true).isEmpty()) {
                labelRect.setLocation((int) (bv.getHexSize().width * 0.55),
                      (int) (0.25 * bv.getHexSize().height) - labelRect.height);
                labelPos = Positioning.RIGHT;
            } else if (bv.game.getEntitiesVector(position.translated("SW"), entity.getBoardId(), true).isEmpty()) {
                labelRect.setLocation((int) (bv.getHexSize().width * 0.45) - labelRect.width,
                      (int) (0.75 * bv.getHexSize().height));
                labelPos = Positioning.LEFT;
            } else {
                labelRect.setLocation(bv.getHexSize().width / 2 - labelRect.width / 2,
                      (int) (0.75 * bv.getHexSize().height));
                labelPos = Positioning.RIGHT;
            }
        }

        // If multiple units are present in a hex, fan out the labels
        // In the deployment phase; indexOf returns -1 for the current unit
        int indexEntity = bv.game.getEntitiesVector(position, entity.getBoardId(), true).indexOf(entity);
        int numEntity = bv.game.getEntitiesVector(position, entity.getBoardId(), true).size();

        if ((indexEntity != -1) && (numEntity <= 4)) {
            labelRect.y += (bv.getPanel().getFontMetrics(labelFont).getAscent() + 4) * indexEntity;
        } else if (indexEntity == -1) {
            labelRect.y += (bv.getPanel().getFontMetrics(labelFont).getAscent() + 4) * numEntity;
        } else {
            labelRect.y += (bv.getPanel().getFontMetrics(labelFont).getAscent() + 4);
        }

        // If the label has changed, force redrawing. Necessary for the Deployment phase
        if (!labelRect.equals(oldRect)) {
            image = null;
        }
    }

    // Happy little class to hold status info until it gets drawn
    private class Status {
        final Color color;
        final String status;
        final boolean small;

        Status(Color color, String status) {
            this.color = color;
            this.status = Messages.getString("BoardView1." + status);
            small = false;

            if (this.color.equals(GUIP.getWarningColor())) {
                criticalStatus = true;
            }
        }

        Status(Color color, String status, Object... objs) {
            this.color = color;
            this.status = Messages.getString("BoardView1." + status, objs);
            small = false;
            if (this.color.equals(GUIP.getWarningColor())) {
                criticalStatus = true;
            }
        }

        Status(Color color, String status, boolean direct) {
            this.color = color;
            this.status = status;
            small = false;
            if (this.color.equals(GUIP.getWarningColor())) {
                criticalStatus = true;
            }
        }

        Status(Color color, String status, int t) {
            this.color = color;
            this.status = status;
            small = true;
        }

        Status(Color color, int b, int t) {
            this.color = color;
            status = null;
            small = true;
        }
    }

    private void drawStatusStrings(Graphics2D graphics2D, ArrayList<Status> statusStrings) {
        if (statusStrings.isEmpty()) {
            return;
        }

        // We pull out just the height as that is the size we want for the Rectangle. We use this for all width/height
        // variables to ensure equal size and remove the warnings from IDEA about wrong values.
        int squareEdge = labelRect.height;

        // The small info blobs
        graphics2D.setFont(labelFont);

        Rectangle rectangle = new Rectangle(labelRect.x, labelRect.y, squareEdge, squareEdge);
        if (labelPos == Positioning.LEFT) {
            rectangle.translate(labelRect.width - labelRect.height, 0);
        }

        for (Status curStatus : statusStrings) {
            if (curStatus.small) {
                if (labelPos == Positioning.RIGHT) {
                    rectangle.translate(-labelRect.height - 2, 0);
                } else {
                    rectangle.translate(labelRect.height + 2, 0);
                }
                graphics2D.setColor(labelBack);
                graphics2D.fillRoundRect(rectangle.x, rectangle.y, squareEdge, squareEdge, 5, 5);
                if (curStatus.status == null) {
                    Color damageColor = getDamageColor();
                    if (damageColor != null) {
                        graphics2D.setColor(damageColor);
                        graphics2D.fillRoundRect(rectangle.x + 2,
                              rectangle.y + 2,
                              rectangle.width - 4,
                              rectangle.height - 4,
                              5,
                              5);
                    }

                } else {
                    new StringDrawer(curStatus.status).center()
                          .color(curStatus.color)
                          .at(rectangle.x + rectangle.height / 2, rectangle.y + rectangle.height / 2)
                          .draw(graphics2D);
                }
            }
        }

        if ((bv.getScale() < 0.55) && criticalStatus) {
            // When zoomed far out, status wouldn't be readable, therefore, draw a big "!" (and the label is red)
            Font bigFont = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, (int) (42 * bv.getScale()));
            new StringDrawer("!").at(bv.getHexSize().width / 2, bv.getHexSize().height / 2)
                  .color(GUIP.getWarningColor())
                  .outline(Color.WHITE, 1)
                  .center()
                  .font(bigFont)
                  .draw(graphics2D);
        } else {
            // Critical status texts
            Font boldFont = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, (int) (12 * bv.getScale()));
            int y = (int) (bv.getHexSize().height / 2.0f + bv.getScale() * (statusStrings.size() - 1) * 7);
            for (Status curStatus : statusStrings) {
                if (!curStatus.small) { // Critical status
                    new StringDrawer(curStatus.status).at(bv.getHexSize().width / 2, y)
                          .color(curStatus.color)
                          .outline(Color.BLACK, 1)
                          .center()
                          .font(boldFont)
                          .draw(graphics2D);
                    y -= (int) (14 * bv.getScale());
                }
            }
        }
    }

    /**
     * Creates the sprite for this entity. Fortunately, it is no longer an extra pain to create transparent images in
     * AWT.
     */
    @Override
    public void prepare() {
        final Board board = bv.getBoard();
        // recalculate bounds & label
        getBounds();

        // create image for buffer
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
              .getDefaultScreenDevice()
              .getDefaultConfiguration();
        image = config.createCompatibleImage(bounds.width, bounds.height, Transparency.TRANSLUCENT);
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // translate everything (=correction for label placement)
        graph.translate(-hexOrigin.x, -hexOrigin.y);

        // scale the following draws according to board zoom
        graph.scale(bv.getScale(), bv.getScale());

        boolean isTank = (entity instanceof Tank);
        boolean isInfantry = (entity instanceof Infantry);
        boolean isAero = entity.isAero();
        boolean isStaticEntity = entity.isBuildingEntityOrGunEmplacement()
              || entity instanceof HandheldWeapon
              || entity instanceof AbstractBuildingEntity;
        boolean isSquadron = entity instanceof FighterSquadron;

        if ((isAero && ((IAero) entity).isSpheroid() && !board.isSpace()) && (secondaryPos == 1)) {
            graph.setColor(Color.WHITE);
            graph.draw(bv.getFacingPolys()[entity.getFacing()]);
        }

        if ((secondaryPos == -1) || (secondaryPos == 6)) {
            // Gather unit conditions
            ArrayList<Status> stStr = new ArrayList<>();
            criticalStatus = false;

            // Determine if the entity has a locked turret and if it is a gun emplacement
            boolean turretLocked = false;
            int crewStunned = 0;
            if (entity instanceof Tank tankEntity) {
                turretLocked = !tankEntity.hasNoTurret() && !tankEntity.canChangeSecondaryFacing();
                crewStunned = tankEntity.getStunnedTurns();
            }

            // draw elevation/altitude if non-zero
            if (entity.isAirborne()) {
                if (!board.isSpace()) {
                    stStr.add(new Status(Color.CYAN, "A", SMALL));
                    stStr.add(new Status(Color.CYAN, Integer.toString(entity.getAltitude()), SMALL));
                }
            } else if (entity.getElevation() != 0) {
                stStr.add(new Status(Color.CYAN, Integer.toString(entity.getElevation()), SMALL));
            }

            // Shutdown
            if (entity.isManualShutdown()) {
                stStr.add(new Status(GUIP.getCautionColor(), "SHUTDOWN"));
            } else if (entity.isShutDown()) {
                stStr.add(new Status(GUIP.getWarningColor(), "SHUTDOWN"));
            }

            // Prone, Hull down, Stuck, Immobile, Jammed
            if (entity.isProne()) {
                stStr.add(new Status(GUIP.getCautionColor(), "PRONE"));
            }

            if (!entity.getHiddenActivationPhase().isUnknown()) {
                stStr.add(new Status(GUIP.getPrecautionColor(), "ACTIVATING"));
            }

            if (entity.isHidden()) {
                stStr.add(new Status(GUIP.getPrecautionColor(), "HIDDEN"));
            }

            if (entity.isGyroDestroyed()) {
                stStr.add(new Status(GUIP.getWarningColor(), "NO_GYRO"));
            }

            if (entity.isHullDown()) {
                stStr.add(new Status(GUIP.getPrecautionColor(), "HULLDOWN"));
            }

            if (entity.isStuck()) {
                stStr.add(new Status(GUIP.getCautionColor(), "STUCK"));
            }

            if (!isStaticEntity && entity.isImmobile()) {
                stStr.add(new Status(GUIP.getWarningColor(), "IMMOBILE"));
            }

            if (entity.isBracing()) {
                stStr.add(new Status(GUIP.getPrecautionColor(), "BRACING"));
            }

            if (isAffectedByECM()) {
                stStr.add(new Status(GUIP.getCautionColor(), "Jammed"));
            }

            // Turret Lock
            if (turretLocked) {
                stStr.add(new Status(GUIP.getCautionColor(), "LOCKED"));
            }

            // Grappling & Swarming
            if (entity.getGrappled() != Entity.NONE) {
                if (entity.isGrappleAttacker()) {
                    stStr.add(new Status(GUIP.getCautionColor(), "GRAPPLER"));
                } else {
                    stStr.add(new Status(GUIP.getWarningColor(), "GRAPPLED"));
                }
            }
            if (entity.getSwarmAttackerId() != Entity.NONE) {
                stStr.add(new Status(GUIP.getWarningColor(), "SWARMED"));
            }

            // Transporting (but not Squadrons that are obviously composed of subunits)
            if (!entity.getLoadedUnits().isEmpty() && !isSquadron) {
                stStr.add(new Status(GUIP.getCautionColor(), "T", SMALL));
            }

            if (!entity.getAllTowedUnits().isEmpty()) {
                stStr.add(new Status(GUIP.getCautionColor(), "TOWING"));
            }

            // Hidden, Unseen Unit
            if (trackThisEntitiesVisibilityInfo(entity)) {
                if (!entity.isEverSeenByEnemy()) {
                    stStr.add(new Status(Color.GREEN, "U", SMALL));
                } else if (!entity.isVisibleToEnemy()) {
                    stStr.add(new Status(Color.GREEN, "H", SMALL));
                }
            }

            if (entity.hasAnyTypeNarcPodsAttached()) {
                stStr.add(new Status(GUIP.getWarningColor(), "N", SMALL));
            }

            // Large Craft Ejecting
            if (entity instanceof Aero aero) {
                if (aero.isEjecting()) {
                    stStr.add(new Status(GUIP.getCautionColor(), "EJECTING"));
                }
            }

            // Crew
            if (entity.getCrew().isDead()) {
                stStr.add(new Status(GUIP.getWarningColor(), "CrewDead"));
            }

            if (crewStunned > 0) {
                stStr.add(new Status(GUIP.getCautionColor(), "STUNNED", new Object[] { crewStunned }));
            }

            // Infantry
            if (isInfantry && entity instanceof Infantry inf) {
                int dig = inf.getDugIn();
                if (dig == Infantry.DUG_IN_COMPLETE) {
                    stStr.add(new Status(Color.PINK, "D", SMALL));
                } else if (dig != Infantry.DUG_IN_NONE) {
                    stStr.add(new Status(GUIP.getPrecautionColor(), "Working", DIRECT));
                    stStr.add(new Status(Color.PINK, "D", SMALL));
                } else if (inf.isTakingCover()) {
                    stStr.add(new Status(GUIP.getPrecautionColor(), "TakingCover"));
                }

                if (inf.turnsLayingExplosives >= 0) {
                    stStr.add(new Status(GUIP.getPrecautionColor(), "Working", DIRECT));
                    stStr.add(new Status(Color.PINK, "E", SMALL));
                }
            }

            // Tank
            if (isTank && entity instanceof Tank tank) {
                int dig = tank.getDugIn();
                if ((dig >= Tank.DUG_IN_FORTIFYING1) && (dig <= Tank.DUG_IN_FORTIFYING3)) {
                    stStr.add(new Status(GUIP.getPrecautionColor(), "Working", DIRECT));
                    stStr.add(new Status(Color.PINK, "D", SMALL));
                }
            }

            // Aero
            if (isAero) {
                IAero a = (IAero) entity;
                if (a.isRolled()) {
                    stStr.add(new Status(GUIP.getCautionColor(), "ROLLED"));
                }

                if ((a.getCurrentFuel() <= 0) && a.requiresFuel()) {
                    stStr.add(new Status(GUIP.getWarningColor(), "FUEL"));
                }

                if (entity.isEvading()) {
                    stStr.add(new Status(Color.GREEN, "EVADE"));
                }

                if (a.isOutControlTotal() && a.isRandomMove()) {
                    stStr.add(new Status(GUIP.getWarningColor(), "RANDOM"));
                } else if (a.isOutControlTotal()) {
                    stStr.add(new Status(GUIP.getWarningColor(), "CONTROL"));
                }
            }

            if (GUIP.getShowDamageLevel()) {
                Color damageColor = getDamageColor();
                if (damageColor != null) {
                    stStr.add(new Status(damageColor, 0, SMALL));
                }
            }

            // Unit Label
            // no scaling for the label, its size is changed by varying the font size directly => better control
            graph.scale(1 / bv.getScale(), 1 / bv.getScale());

            // Label background
            if (!getAdjShortName().isBlank()) {
                if (criticalStatus) {
                    graph.setColor(LABEL_CRITICAL_BACK);
                } else {
                    graph.setColor(labelBack);
                }
                graph.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 5, 10);

                // Draw a label border with player colors or team coloring
                if (GUIP.getUnitLabelBorder()) {
                    if (GUIP.getTeamColoring()) {
                        boolean isLocalTeam = entity.getOwner().getTeam() == bv.getClientgui()
                              .getClient()
                              .getLocalPlayer()
                              .getTeam();
                        boolean isLocalPlayer = entity.getOwner()
                              .equals(bv.getClientgui().getClient().getLocalPlayer());
                        if (isLocalPlayer) {
                            graph.setColor(GUIP.getMyUnitColor());
                        } else if (isLocalTeam) {
                            graph.setColor(GUIP.getAllyUnitColor());
                        } else {
                            graph.setColor(GUIP.getEnemyUnitColor());
                        }
                    } else {
                        graph.setColor(entity.getOwner().getColour().getColour(false));
                    }
                    Stroke oldStroke = graph.getStroke();
                    graph.setStroke(new BasicStroke(3));
                    graph.drawRoundRect(labelRect.x - 1,
                          labelRect.y - 1,
                          labelRect.width + 1,
                          labelRect.height + 1,
                          5,
                          10);
                    graph.setStroke(oldStroke);
                }

                // Label text
                graph.setFont(labelFont);
                Color textColor = GUIP.getUnitTextColor();
                if (!entity.isDone() && !onlyDetectedBySensors()) {
                    textColor = GUIP.getUnitValidColor();
                }
                if (isSelected) {
                    textColor = GUIP.getUnitSelectedColor();
                }
                if (entity.isDone() && !onlyDetectedBySensors()) {
                    textColor = UIUtil.addAlpha(textColor, 100);
                }
                new StringDrawer(getAdjShortName()).center()
                      .at(labelRect.x + labelRect.width / 2, labelRect.y + labelRect.height / 2)
                      .color(textColor)
                      .draw(graph);
            }

            // Past here, everything is drawing status that shouldn't be seen on a sensor return, so we'll just quit
            // here
            if (onlyDetectedBySensors()) {
                graph.dispose();
                return;
            }

            // Draw all the status information now
            drawStatusStrings(graph, stStr);

            // from here, scale the following draws according to board zoom
            graph.scale(bv.getScale(), bv.getScale());

            // draw facing
            graph.setColor(Color.white);
            if ((entity.getFacing() != -1) && !((entity instanceof Infantry)
                  && !((Infantry) entity).hasFieldWeapon()
                  && !((Infantry) entity).isTakingCover()) && !(
                  (entity instanceof IAero)
                        && ((IAero) entity).isSpheroid()
                        && !board.isSpace())) {
                // Indicate a stacked unit with the same facing that can still move
                if (shouldIndicateNotDone() && bv.game.getPhase().isMovement()) {
                    var tr = graph.getTransform();
                    // rotate the arrow slightly
                    graph.scale(1 / bv.getScale(), 1 / bv.getScale());
                    graph.rotate(Math.PI / 24, bv.getHexSize().width / 2.0, bv.getHexSize().height / 2.0);
                    graph.scale(bv.getScale(), bv.getScale());
                    graph.setColor(GUIP.getWarningColor());
                    graph.fill(bv.getFacingPolys()[entity.getFacing()]);
                    graph.setColor(Color.LIGHT_GRAY);
                    graph.draw(bv.getFacingPolys()[entity.getFacing()]);
                    graph.setTransform(tr);
                }

                if (!entity.isDone() && bv.game.getPhase().isMovement()) {
                    graph.setColor(GUIP.getWarningColor());
                    graph.fill(bv.getFacingPolys()[entity.getFacing()]);
                    graph.setColor(Color.WHITE);
                    graph.draw(bv.getFacingPolys()[entity.getFacing()]);
                } else {
                    graph.setColor(Color.GRAY);
                    graph.fill(bv.getFacingPolys()[entity.getFacing()]);
                    graph.setColor(Color.LIGHT_GRAY);
                    graph.draw(bv.getFacingPolys()[entity.getFacing()]);
                }
            }

            // determine secondary facing for non-meks & flipped arms
            int secFacing = entity.getFacing();
            if (!((entity instanceof Mek) || (entity instanceof ProtoMek)) || (entity instanceof QuadVee)) {
                secFacing = entity.getSecondaryFacing();
            } else if (entity.getArmsFlipped()) {
                secFacing = (entity.getFacing() + 3) % 6;
            }
            // draw secondary facing arrow if necessary
            if ((secFacing != -1) && (secFacing != entity.getFacing())) {
                graph.setColor(Color.GREEN);
                graph.draw(bv.getFacingPolys()[secFacing]);
            }
            if (entity.isAero() && this.bv.game.useVectorMove()) {
                for (int head : entity.getHeading()) {
                    graph.setColor(Color.GREEN);
                    graph.draw(bv.getFacingPolys()[head]);
                }
            }

            // armor, internal and TMM status bars
            double percentRemaining = entity.getArmorRemainingPercent();
            int barLength = (int) (STATUS_BAR_LENGTH * percentRemaining);

            graph.setColor(Color.darkGray);
            graph.fillRect(STATUS_BAR_X + 1, 7, STATUS_BAR_LENGTH, 3);
            graph.setColor(Color.lightGray);
            graph.fillRect(STATUS_BAR_X, 6, STATUS_BAR_LENGTH, 3);
            graph.setColor(getStatusBarColor(percentRemaining));
            graph.fillRect(STATUS_BAR_X, 6, barLength, 3);

            if (!isStaticEntity && !isSquadron) {
                // Gun emplacements and squadrons don't use internal structure nor SI damage
                percentRemaining = entity.getInternalRemainingPercent();
                barLength = (int) (STATUS_BAR_LENGTH * percentRemaining);

                graph.setColor(Color.darkGray);
                graph.fillRect(STATUS_BAR_X + 1, 11, STATUS_BAR_LENGTH, 3);
                graph.setColor(Color.lightGray);
                graph.fillRect(STATUS_BAR_X, 10, STATUS_BAR_LENGTH, 3);
                graph.setColor(getStatusBarColor(percentRemaining));
                graph.fillRect(STATUS_BAR_X, 10, barLength, 3);
            }

            // TMM pips show if done in movement, or on all units during firing
            int pipOption = GUIP.getTMMPipMode();
            int pipScaleFactor = 1;
            int pipOffset = 0;
            if (pipOption == 3 || pipOption == 4) { // bigger pips
                pipScaleFactor = BIGGER_PIP_SCALE;
                pipOffset = BIGGER_PIP_OFFSET;
            }
            if ((pipOption != 0) && !isStaticEntity && !entity.isAero() && ((entity.isDone() && bv.game.getPhase()
                  .isMovement()) || bv.game.getPhase().isFiring())) {
                int tmm = Compute.getTargetMovementModifier(bv.game, entity.getId()).getValue();
                Color tmmColor = (pipOption == 1 || pipOption == 3) ? Color.WHITE :
                      GUIP.getColorForMovement(entity.moved);
                graph.setColor(Color.darkGray);
                graph.fillRect(STATUS_BAR_X - (pipOffset * (MAX_TMM_PIPS - 1)),
                      12 + TMM_PIP_SIZE,
                      STATUS_BAR_LENGTH + (pipOffset * MAX_TMM_PIPS),
                      TMM_PIP_SIZE * pipScaleFactor + 1);
                if (tmm >= 0) {
                    // draw left to right for positive TMM
                    for (int i = 0;
                          i < MAX_TMM_PIPS;
                          i++) {
                        graph.setColor(Color.DARK_GRAY);
                        graph.setColor(i < tmm ? tmmColor : Color.BLACK);
                        graph.fillRect(STATUS_BAR_X - (pipOffset * (MAX_TMM_PIPS - 1)) + 1 + i * (TMM_PIP_SIZE
                                    + pipOffset),
                              13 + TMM_PIP_SIZE,
                              TMM_PIP_SIZE - pipScaleFactor + pipOffset,
                              TMM_PIP_SIZE * pipScaleFactor - 1);
                    }
                } else {
                    // draw pips right to left for negative TMM
                    for (int i = 0;
                          i < MAX_TMM_PIPS;
                          i++) {
                        graph.setColor(Color.DARK_GRAY);
                        graph.setColor(i >= (MAX_TMM_PIPS + tmm) ? tmmColor : Color.BLACK);
                        graph.fillRect(STATUS_BAR_X - (pipOffset * MAX_TMM_PIPS - 1) + 1 + i * (TMM_PIP_SIZE
                                    + pipOffset),
                              13 + TMM_PIP_SIZE,
                              TMM_PIP_SIZE - pipScaleFactor + pipOffset,
                              TMM_PIP_SIZE * pipScaleFactor - 1);
                    }
                }
            }
        }

        graph.dispose();
    }

    /**
     * Returns true when an indicator should be shown that a unit with the same facing as this unit is stacked below it
     * and can still move.
     */
    private boolean shouldIndicateNotDone() {
        var hexEntities = bv.game.getEntitiesVector(entity.getPosition(), entity.getBoardId());
        return hexEntities.stream()
              .filter(e -> hexEntities.indexOf(entity) > hexEntities.indexOf(e))
              .filter(e -> e.getFacing() == entity.getFacing())
              .anyMatch(e -> !e.isDone());
    }

    private @Nullable Color getDamageColor() {
        return switch (entity.getDamageLevel()) {
            case Entity.DMG_CRIPPLED -> Color.black;
            case Entity.DMG_HEAVY -> GUIP.getWarningColor();
            case Entity.DMG_MODERATE -> GUIP.getCautionColor();
            case Entity.DMG_LIGHT -> Color.green;
            default -> null;
        };
    }

    /**
     * We only want to show double-blind visibility indicators on our own meks and teammates' meks (assuming the `team
     * vision` option is on).
     */
    private boolean trackThisEntitiesVisibilityInfo(Entity e) {
        return EntityVisibilityUtils.trackThisEntitiesVisibilityInfo(bv.getLocalPlayer(), e);
    }

    /**
     * @return determine if this {@link EntitySprite} is only detected by an enemies sensor and hence should only be a
     *       sensor return.
     */
    public boolean onlyDetectedBySensors() {
        return EntityVisibilityUtils.onlyDetectedBySensors(bv.getLocalPlayer(), entity);
    }

    private Color getStatusBarColor(double percentRemaining) {
        if (percentRemaining <= .25) {
            return GUIP.getWarningColor();
        } else if (percentRemaining <= .75) {
            return GUIP.getCautionColor();
        } else {
            return new Color(16, 196, 16);
        }
    }

    /**
     * Overrides to provide for a smaller sensitive area.
     */
    @Override
    public boolean isInside(Point point) {
        return entityRect.contains(point.x, point.y);
    }

    public Coords getPosition() {
        return entity.getPosition();
    }

    /**
     * @deprecated No indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String getPlayerColor() {
        if (onlyDetectedBySensors()) {
            // TODO : Make me customizable
            return "C0C0C0";
        } else {
            return entity.getOwner().getColour().getHexString();
        }
    }

    public boolean isAffectedByECM() {
        return isAffectedByECM;
    }

    public void setAffectedByECM(boolean isAffectedByECM) {
        boolean changed = isAffectedByECM != this.isAffectedByECM;
        this.isAffectedByECM = isAffectedByECM;
        // We need to prepare the icon again if the value changed
        if (changed) {
            prepare();
        }
    }

    /** Marks the entity as selected for movement etc., recoloring the label */
    public void setSelected(boolean status) {
        if (isSelected != status) {
            isSelected = status;
            prepare();
        }
    }

    /**
     * Returns if the entity is marked as selected for movement etc., recoloring the label
     */
    public boolean getSelected() {
        return isSelected;
    }

    @Override
    protected int getSpritePriority() {
        return entity.getSpriteDrawPriority();
    }
}

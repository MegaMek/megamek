/*
 * Copyright (C) 2015-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitDisplay;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import java.io.Serial;
import java.util.Enumeration;

import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.mapset.*;
import megamek.client.ui.widget.picmap.LocationSelectListener;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.CriticalSlot;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.*;
import megamek.logging.MMLogger;

/**
 * This panel contains the armor readout display.
 */
public class ArmorPanel extends PicMap {
    private static final MMLogger logger = MMLogger.create(ArmorPanel.class);

    @Serial
    private static final long serialVersionUID = -3612396252172441104L;
    private TankMapSet tank;
    private MekMapSet mek;
    private InfantryMapSet infantry;
    private BattleArmorMapSet battleArmor;
    private ProtoMekMapSet proto;
    private VTOLMapSet vtol;
    private QuadMapSet quad;
    private TripodMekMapSet tripod;
    private SimpleUnitMapSet simpleUnit;
    private LargeSupportTankMapSet largeSupportTank;
    private SuperHeavyTankMapSet superHeavyTank;
    private AeroMapSet aero;
    private CapitalFighterMapSet capFighter;
    private SquadronMapSet squad;
    private JumpshipMapSet jump;
    private SpheroidMapSet sphere;
    private WarshipMapSet warship;
    private int minTopMargin;
    private int minLeftMargin;
    private int minBottomMargin;
    private int minRightMargin;

    private final LocationSelectListener locationSelectListener;

    private static final int minTankTopMargin = 8;
    private static final int minTankLeftMargin = 8;
    private static final int minVTOLTopMargin = 8;
    private static final int minVTOLLeftMargin = 8;
    private static final int minMekTopMargin = 18;
    private static final int minMekLeftMargin = 7;
    private static final int minMekBottomMargin = 0;
    private static final int minMekRightMargin = 0;
    private static final int minInfTopMargin = 8;
    private static final int minInfLeftMargin = 8;
    private static final int minAeroTopMargin = 8;
    private static final int minAeroLeftMargin = 8;

    private final Game game;

    /** Locations shaded as having taken a critical hit. */
    private Set<Integer> criticalLocations = Set.of();

    /**
     * Whether a caller set the shaded locations itself. The damage editor does, to show the crits it has staged but
     * not yet applied. When no caller sets them, the panel shades the locations the unit itself has crits in, so the
     * unit display stripes damaged locations the same way the editor does.
     */
    private boolean criticalLocationsSetByCaller = false;

    /** Whether the diagram enlarges itself to fill the space it is given, rather than staying at its drawn size. */
    private boolean fitToWindow = false;

    /** How far the diagram may be enlarged to fill its space, past which its bitmap frames turn blocky. */
    private static final double MAX_FIT_SCALE = 2.5;

    public ArmorPanel(@Nullable Game g, @Nullable LocationSelectListener locationSelectListener) {
        game = g;
        this.locationSelectListener = locationSelectListener;
    }

    /**
     * Sets whether the diagram enlarges itself to fill the space it is given. The unit display turns this on so the
     * diagram uses the panel instead of sitting small in a large empty area. It is drawn at its natural size
     * otherwise, as in the damage editor, which sizes the diagram itself.
     */
    public void setFitToWindow(boolean fitToWindow) {
        this.fitToWindow = fitToWindow;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        tank = new TankMapSet(this, locationSelectListener);
        mek = new MekMapSet(this, locationSelectListener);
        infantry = new InfantryMapSet(this);
        battleArmor = new BattleArmorMapSet(this);
        proto = new ProtoMekMapSet(this, locationSelectListener);
        vtol = new VTOLMapSet(this, locationSelectListener);
        quad = new QuadMapSet(this, locationSelectListener);
        tripod = new TripodMekMapSet(this, locationSelectListener);
        simpleUnit = new SimpleUnitMapSet(this, locationSelectListener);
        largeSupportTank = new LargeSupportTankMapSet(this, locationSelectListener);
        superHeavyTank = new SuperHeavyTankMapSet(this, locationSelectListener);
        aero = new AeroMapSet(this, locationSelectListener);
        capFighter = new CapitalFighterMapSet(this);
        sphere = new SpheroidMapSet(this, locationSelectListener);
        jump = new JumpshipMapSet(this, locationSelectListener);
        warship = new WarshipMapSet(this, locationSelectListener);
        squad = new SquadronMapSet(this, game);
    }

    @Override
    public void onResize() {
        if (fitToWindow) {
            fitScaleToWindow();
        }
        Rectangle r = getContentBounds();
        if (r != null) {
            // Center the content within the scaled-back size. The frame behind it - the labeled boxes for rear
            // armor, internal structure and heat - is drawn centered by the map set, so centering the values and
            // figures the same way keeps the two lined up. Top-aligning only the content would split them apart.
            Dimension contentSize = getContentSize();
            int w = (contentSize.width - r.width) / 2;
            int h = (contentSize.height - r.height) / 2;
            int dx = Math.max(w, minLeftMargin);
            int dy = Math.max(h, minTopMargin);
            setContentMargins(dx, dy, minRightMargin, minBottomMargin);
        }
    }

    /**
     * Enlarges the diagram to fill the space it has been given, leaving a little room around it, but never shrinking
     * it below its natural size and never past {@link #MAX_FIT_SCALE}, where its bitmap frames turn blocky. The
     * content size is in unscaled coordinates, so the scale it works out does not depend on the current scale, and
     * re-applying the same scale is a no-op that stops this from looping through {@link #setDisplayScale}.
     */
    private void fitScaleToWindow() {
        Rectangle content = getContentBounds();
        Dimension available = getSize();
        if ((content == null) || (content.width <= 0) || (content.height <= 0)
              || (available.width <= 0) || (available.height <= 0)) {
            return;
        }
        double widthScale = available.width / (double) content.width;
        double heightScale = available.height / (double) content.height;
        // leave a little breathing room so the diagram does not touch the panel edges
        double scale = 0.9 * Math.min(widthScale, heightScale);
        scale = Math.max(1.0, Math.min(scale, MAX_FIT_SCALE));
        if (Math.abs(scale - getDisplayScale()) > 0.01) {
            setDisplayScale(scale);
        }
    }

    /**
     * updates fields for the specified mek
     */
    public void displayMek(Entity en) {
        // Look out for a race condition.
        if (en == null) {
            return;
        }
        DisplayMapSet ams = null;
        removeAll();
        switch (en) {
            case QuadMek ignored -> {
                ams = quad;
                minLeftMargin = minMekLeftMargin;
                minTopMargin = minMekTopMargin;
                minBottomMargin = minMekBottomMargin;
                minRightMargin = minMekRightMargin;
            }
            case TripodMek ignored -> {
                ams = tripod;
                minLeftMargin = minMekLeftMargin;
                minTopMargin = minMekTopMargin;
                minBottomMargin = minMekBottomMargin;
                minRightMargin = minMekRightMargin;
            }
            case Mek ignored -> {
                ams = mek;
                minLeftMargin = minMekLeftMargin;
                minTopMargin = minMekTopMargin;
                minBottomMargin = minMekBottomMargin;
                minRightMargin = minMekRightMargin;
            }
            case GunEmplacement ignored -> {
                ams = simpleUnit;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            }
            case VTOL ignored -> {
                ams = vtol;
                minLeftMargin = minVTOLLeftMargin;
                minTopMargin = minVTOLTopMargin;
                minBottomMargin = minVTOLTopMargin;
                minRightMargin = minVTOLLeftMargin;
            }
            case LargeSupportTank ignored -> {
                ams = largeSupportTank;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            }
            case SuperHeavyTank ignored -> {
                ams = superHeavyTank;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            }
            case Tank ignored -> {
                ams = tank;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            }
            case BattleArmor ignored -> {
                ams = battleArmor;
                minLeftMargin = minInfLeftMargin;
                minTopMargin = minInfTopMargin;
                minBottomMargin = minInfTopMargin;
                minRightMargin = minInfLeftMargin;
            }
            case Infantry ignored -> {
                ams = infantry;
                minLeftMargin = minInfLeftMargin;
                minTopMargin = minInfTopMargin;
                minBottomMargin = minInfTopMargin;
                minRightMargin = minInfLeftMargin;
            }
            case ProtoMek ignored -> {
                ams = proto;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            }
            case Warship ignored -> {
                ams = warship;
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            }
            case Jumpship ignored -> {
                ams = jump;
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            }
            case FighterSquadron ignored -> {
                ams = squad;
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            }
            case Aero ignored -> {
                ams = aero;
                if (en instanceof SmallCraft sc) {
                    if (sc.isSpheroid()) {
                        ams = sphere;
                    }
                }
                if (en.isCapitalFighter()) {
                    ams = capFighter;
                }
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            }
            // Units with no drawn figure of their own get the plain box-per-location diagram.
            default -> {
                if ((en instanceof HandheldWeapon) || (en instanceof AbstractBuildingEntity)) {
                    ams = simpleUnit;
                    minLeftMargin = minInfLeftMargin;
                    minTopMargin = minInfTopMargin;
                    minBottomMargin = minInfTopMargin;
                    minRightMargin = minInfLeftMargin;
                }
            }
        }

        if (ams == null) {
            logger.info("The armor panel is null");
            return;
        }
        ams.setEntity(en);
        // shading the locations that took a crit has to follow setEntity, which colors them by their damage. The
        // editor sets the locations itself to preview staged crits; otherwise shade the ones the unit really has.
        ams.setCriticalLocations(criticalLocationsSetByCaller ? criticalLocations : unitCriticalLocations(en));
        addElement(ams.getContentGroup());
        Enumeration<BackGroundDrawer> iter = ams.getBackgroundDrawers().elements();
        while (iter.hasMoreElements()) {
            addBgDrawer(iter.nextElement());
        }
        onResize();
        update();
    }

    /**
     * Sets the locations to shade as having taken a critical hit, shown the next time the unit is drawn. The damage
     * editor uses this to show the crits the user has set but not yet applied, which the unit itself does not carry
     * yet. Once a caller sets them, the panel stops shading the unit's own crits and shows only what it is given.
     *
     * @param criticalLocations the locations that have taken a critical hit
     */
    public void setCriticalLocations(Set<Integer> criticalLocations) {
        this.criticalLocations = Set.copyOf(criticalLocations);
        criticalLocationsSetByCaller = true;
    }

    /** The locations the unit itself carries a critical hit in: any location with a damaged critical slot. */
    private static Set<Integer> unitCriticalLocations(Entity entity) {
        Set<Integer> locations = new HashSet<>();
        for (int location = 0; location < entity.locations(); location++) {
            for (CriticalSlot slot : entity.getCriticalSlots(location)) {
                if ((slot != null) && slot.isDamaged()) {
                    locations.add(location);
                    break;
                }
            }
        }
        return locations;
    }
}

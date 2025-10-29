/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Rectangle;
import java.io.Serial;
import java.util.Enumeration;

import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.mapset.*;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.Game;
import megamek.common.units.*;
import megamek.logging.MMLogger;

/**
 * This panel contains the armor readout display.
 */
class ArmorPanel extends PicMap {
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
    private GunEmplacementMapSet gunEmplacement;
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

    private final UnitDisplayPanel unitDisplayPanel;

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

    ArmorPanel(Game g, UnitDisplayPanel unitDisplayPanel) {
        game = g;
        this.unitDisplayPanel = unitDisplayPanel;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        tank = new TankMapSet(this, unitDisplayPanel);
        mek = new MekMapSet(this, unitDisplayPanel);
        infantry = new InfantryMapSet(this);
        battleArmor = new BattleArmorMapSet(this);
        proto = new ProtoMekMapSet(this, unitDisplayPanel);
        vtol = new VTOLMapSet(this, unitDisplayPanel);
        quad = new QuadMapSet(this, unitDisplayPanel);
        tripod = new TripodMekMapSet(this, unitDisplayPanel);
        gunEmplacement = new GunEmplacementMapSet(this);
        largeSupportTank = new LargeSupportTankMapSet(this, unitDisplayPanel);
        superHeavyTank = new SuperHeavyTankMapSet(this, unitDisplayPanel);
        aero = new AeroMapSet(this, unitDisplayPanel);
        capFighter = new CapitalFighterMapSet(this);
        sphere = new SpheroidMapSet(this, unitDisplayPanel);
        jump = new JumpshipMapSet(this, unitDisplayPanel);
        warship = new WarshipMapSet(this, unitDisplayPanel);
        squad = new SquadronMapSet(this, game);
    }

    @Override
    public void onResize() {
        Rectangle r = getContentBounds();
        if (r != null) {
            int w = (getSize().width - r.width) / 2;
            int h = (getSize().height - r.height) / 2;
            int dx = Math.max(w, minLeftMargin);
            int dy = Math.max(h, minTopMargin);
            setContentMargins(dx, dy, minRightMargin, minBottomMargin);
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
        if (en instanceof QuadMek) {
            ams = quad;
            minLeftMargin = minMekLeftMargin;
            minTopMargin = minMekTopMargin;
            minBottomMargin = minMekBottomMargin;
            minRightMargin = minMekRightMargin;
        } else if (en instanceof TripodMek) {
            ams = tripod;
            minLeftMargin = minMekLeftMargin;
            minTopMargin = minMekTopMargin;
            minBottomMargin = minMekBottomMargin;
            minRightMargin = minMekRightMargin;
        } else if (en instanceof Mek) {
            ams = mek;
            minLeftMargin = minMekLeftMargin;
            minTopMargin = minMekTopMargin;
            minBottomMargin = minMekBottomMargin;
            minRightMargin = minMekRightMargin;
        } else if (en instanceof GunEmplacement) {
            ams = gunEmplacement;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof VTOL) {
            ams = vtol;
            minLeftMargin = minVTOLLeftMargin;
            minTopMargin = minVTOLTopMargin;
            minBottomMargin = minVTOLTopMargin;
            minRightMargin = minVTOLLeftMargin;
        } else if (en instanceof LargeSupportTank) {
            ams = largeSupportTank;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof SuperHeavyTank) {
            ams = superHeavyTank;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof Tank) {
            ams = tank;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof BattleArmor) {
            ams = battleArmor;
            minLeftMargin = minInfLeftMargin;
            minTopMargin = minInfTopMargin;
            minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;
        } else if (en instanceof Infantry) {
            ams = infantry;
            minLeftMargin = minInfLeftMargin;
            minTopMargin = minInfTopMargin;
            minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;
        } else if (en instanceof ProtoMek) {
            ams = proto;
            minLeftMargin = minTankLeftMargin;
            minTopMargin = minTankTopMargin;
            minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (en instanceof Warship) {
            ams = warship;
            minLeftMargin = minAeroLeftMargin;
            minTopMargin = minAeroTopMargin;
            minBottomMargin = minAeroTopMargin;
            minRightMargin = minAeroLeftMargin;
        } else if (en instanceof Jumpship) {
            ams = jump;
            minLeftMargin = minAeroLeftMargin;
            minTopMargin = minAeroTopMargin;
            minBottomMargin = minAeroTopMargin;
            minRightMargin = minAeroLeftMargin;
        } else if (en instanceof FighterSquadron) {
            ams = squad;
            minLeftMargin = minAeroLeftMargin;
            minTopMargin = minAeroTopMargin;
            minBottomMargin = minAeroTopMargin;
            minRightMargin = minAeroLeftMargin;
        } else if (en instanceof Aero) {
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

        if (ams == null) {
            logger.info("The armor panel is null");
            return;
        }
        ams.setEntity(en);
        addElement(ams.getContentGroup());
        Enumeration<BackGroundDrawer> iter = ams.getBackgroundDrawers().elements();
        while (iter.hasMoreElements()) {
            addBgDrawer(iter.nextElement());
        }
        onResize();
        update();
    }
}

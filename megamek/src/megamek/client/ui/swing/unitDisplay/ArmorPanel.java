package megamek.client.ui.swing.unitDisplay;

import java.awt.Rectangle;
import java.util.Enumeration;

import megamek.client.ui.swing.widget.AeroMapSet;
import megamek.client.ui.swing.widget.ArmlessMechMapSet;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.BattleArmorMapSet;
import megamek.client.ui.swing.widget.CapitalFighterMapSet;
import megamek.client.ui.swing.widget.DisplayMapSet;
import megamek.client.ui.swing.widget.GunEmplacementMapSet;
import megamek.client.ui.swing.widget.InfantryMapSet;
import megamek.client.ui.swing.widget.JumpshipMapSet;
import megamek.client.ui.swing.widget.LargeSupportTankMapSet;
import megamek.client.ui.swing.widget.MechMapSet;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.ProtomechMapSet;
import megamek.client.ui.swing.widget.QuadMapSet;
import megamek.client.ui.swing.widget.SpheroidMapSet;
import megamek.client.ui.swing.widget.SquadronMapSet;
import megamek.client.ui.swing.widget.SuperHeavyTankMapSet;
import megamek.client.ui.swing.widget.TankMapSet;
import megamek.client.ui.swing.widget.TripodMechMapSet;
import megamek.client.ui.swing.widget.VTOLMapSet;
import megamek.client.ui.swing.widget.WarshipMapSet;
import megamek.common.Aero;
import megamek.common.ArmlessMech;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LargeSupportTank;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.SmallCraft;
import megamek.common.SuperHeavyTank;
import megamek.common.Tank;
import megamek.common.TripodMech;
import megamek.common.VTOL;
import megamek.common.Warship;
import org.apache.logging.log4j.LogManager;

/**
 * This panel contains the armor readout display.
 */
class ArmorPanel extends PicMap {
    private static final long serialVersionUID = -3612396252172441104L;
    private TankMapSet tank;
    private MechMapSet mech;
    private InfantryMapSet infantry;
    private BattleArmorMapSet battleArmor;
    private ProtomechMapSet proto;
    private VTOLMapSet vtol;
    private QuadMapSet quad;
    private TripodMechMapSet tripod;
    private GunEmplacementMapSet gunEmplacement;
    private ArmlessMechMapSet armless;
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
    
    private UnitDisplay unitDisplay;

    private static final int minTankTopMargin = 8;
    private static final int minTankLeftMargin = 8;
    private static final int minVTOLTopMargin = 8;
    private static final int minVTOLLeftMargin = 8;
    private static final int minMechTopMargin = 18;
    private static final int minMechLeftMargin = 7;
    private static final int minMechBottomMargin = 0;
    private static final int minMechRightMargin = 0;
    private static final int minInfTopMargin = 8;
    private static final int minInfLeftMargin = 8;
    private static final int minAeroTopMargin = 8;
    private static final int minAeroLeftMargin = 8;

    private Game game;

    ArmorPanel(Game g, UnitDisplay unitDisplay) {
        game = g;
        this.unitDisplay = unitDisplay;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        tank = new TankMapSet(this, unitDisplay);
        mech = new MechMapSet(this, unitDisplay);
        infantry = new InfantryMapSet(this);
        battleArmor = new BattleArmorMapSet(this);
        proto = new ProtomechMapSet(this, unitDisplay);
        vtol = new VTOLMapSet(this, unitDisplay);
        quad = new QuadMapSet(this, unitDisplay);
        tripod = new TripodMechMapSet(this, unitDisplay);
        gunEmplacement = new GunEmplacementMapSet(this);
        armless = new ArmlessMechMapSet(this, unitDisplay);
        largeSupportTank = new LargeSupportTankMapSet(this, unitDisplay);
        superHeavyTank = new SuperHeavyTankMapSet(this, unitDisplay);
        aero = new AeroMapSet(this, unitDisplay);
        capFighter = new CapitalFighterMapSet(this);
        sphere = new SpheroidMapSet(this, unitDisplay);
        jump = new JumpshipMapSet(this, unitDisplay);
        warship = new WarshipMapSet(this, unitDisplay);
        squad = new SquadronMapSet(this, game);
    }

    @Override
    public void onResize() {
        Rectangle r = getContentBounds();
        if (r == null) {
            return;
        }
        int w = Math.round(((getSize().width - r.width) / 2));
        int h = Math.round(((getSize().height - r.height) / 2));
        int dx = w < minLeftMargin ? minLeftMargin : w;
        int dy = h < minTopMargin ? minTopMargin : h;
        setContentMargins(dx, dy, minRightMargin, minBottomMargin);
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        // Look out for a race condition.
        if (en == null) {
            return;
        }
        DisplayMapSet ams = mech;
        removeAll();
        if (en instanceof QuadMech) {
            ams = quad;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof TripodMech) {
            ams = tripod;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof ArmlessMech) {
            ams = armless;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof Mech) {
            ams = mech;
            minLeftMargin = minMechLeftMargin;
            minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
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
        } else if (en instanceof Protomech) {
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
            if (en instanceof SmallCraft) {
                SmallCraft sc = (SmallCraft) en;
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
            LogManager.getLogger().error("The armor panel is null");
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
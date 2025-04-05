package megamek.server.totalwarfare.Target;  // Ajout du package

import java.util.Vector;
import megamek.common.*;
import megamek.server.totalwarfare.TWGameManager;

public class ArtilleryHexHandler implements TargetHandler {
    private final TWGameManager gameManager;

    public ArtilleryHexHandler(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public Vector<Report> handle(Targetable t, Game game, Entity ae, int missiles, int attId) {
        Vector<Report> vPhaseReport = new Vector<>();
        Hex hex = game.getBoard().getHex(t.getPosition());
        Report r;

        // Traitement des entités
        for (Entity e : game.getEntitiesVector(t.getPosition())) {
            if (e.getElevation() > hex.terrainLevel(Terrains.BLDG_ELEV)) {
                r = new Report(6685);
                r.subject = e.getId();
                r.addDesc(e);
                vPhaseReport.add(r);
                vPhaseReport.addAll(gameManager.deliverInfernoMissiles(ae, e, missiles, false));
            } else {
                Roll diceRoll = Compute.rollD6(1);
                r = new Report(3570);
                r.subject = e.getId();
                r.addDesc(e);
                r.add(diceRoll);
                vPhaseReport.add(r);

                if (diceRoll.getIntValue() >= 5) {
                    vPhaseReport.addAll(gameManager.deliverInfernoMissiles(ae, e, missiles, false));
                }
            }
        }

        // Traitement des bâtiments
        Building building = game.getBoard().getBuildingAt(t.getPosition());
        if (building != null) {
            Vector<Report> buildingReports = gameManager.damageBuilding(
                building, 
                2 * missiles, 
                t.getPosition()
            );
            buildingReports.forEach(report -> report.subject = attId);
            vPhaseReport.addAll(buildingReports);
        }

        return vPhaseReport;
    }
}
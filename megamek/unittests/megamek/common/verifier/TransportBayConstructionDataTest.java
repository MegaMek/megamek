package megamek.common.verifier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import megamek.common.*;

public class TransportBayConstructionDataTest {
    
    private Entity createEntity(long etype) {
        Entity entity = mock(Entity.class);
        when(entity.hasETypeFlag(anyLong())).thenAnswer(inv ->
            (((Long) inv.getArguments()[0]).longValue() & etype) != 0);
        return entity;
    }

    @Test
    public void testCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = TransportBayConstructionData.CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), TransportBayConstructionData.CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testLiquidCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = TransportBayConstructionData.LIQUID_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), TransportBayConstructionData.LIQUID_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testRefrigeratedCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = TransportBayConstructionData.REFRIGERATED_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), TransportBayConstructionData.REFRIGERATED_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testInsulatedCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = TransportBayConstructionData.INSULATED_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), TransportBayConstructionData.INSULATED_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testLivestockCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = TransportBayConstructionData.LIVESTOCK_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), TransportBayConstructionData.LIVESTOCK_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void identifyMechBay() {
        Bay bay = new MechBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.MECH);
    }

    @Test
    public void identifyProtomechBay() {
        Bay bay = new ProtomechBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.PROTOMECH);
    }

    @Test
    public void identifyHeavyVehicleBay() {
        Bay bay = new HeavyVehicleBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.VEHICLE_HEAVY);
    }

    @Test
    public void identifyLightVehcleay() {
        Bay bay = new LightVehicleBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.VEHICLE_LIGHT);
    }

    @Test
    public void identifySuperHeavyVehicleBay() {
        Bay bay = new SuperHeavyVehicleBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.VEHICLE_SH);
    }

    @Test
    public void identifyFootInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.FOOT);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.INFANTRY_FOOT);
    }

    @Test
    public void identifyJumpInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.JUMP);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.INFANTRY_JUMP);
    }

    @Test
    public void identifyMotorizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.MOTORIZED);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.INFANTRY_MOTORIZED);
    }

    @Test
    public void identifyMechanizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.MECHANIZED);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.INFANTRY_MECHANIZED);
    }

    @Test
    public void identifyISBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, false);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.IS_BATTLE_ARMOR);
    }

    @Test
    public void identifyClanBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, true, false);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.CLAN_BATTLE_ARMOR);
    }

    @Test
    public void identifyCSBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, true);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.CS_BATTLE_ARMOR);
    }

    @Test
    public void identifyFighterBay() {
        Bay bay = new ASFBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.FIGHTER);
    }

    @Test
    public void identifySmallCraftBay() {
        Bay bay = new SmallCraftBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.SMALL_CRAFT);
    }

    @Test
    public void identifyCargoBay() {
        Bay bay = new CargoBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.CARGO);
    }

    @Test
    public void identifyLiquidCargoBay() {
        Bay bay = new LiquidCargoBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.LIQUID_CARGO);
    }

    @Test
    public void identifyRefrigeratedCargoBay() {
        Bay bay = new RefrigeratedCargoBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.REFRIGERATED_CARGO);
    }

    @Test
    public void identifyInsulatedCargoBay() {
        Bay bay = new InsulatedCargoBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.INSULATED_CARGO);
    }

    @Test
    public void identifyLivestockCargoBay() {
        Bay bay = new LivestockCargoBay(1, 1, 0);
        
        assertEquals(TransportBayConstructionData.getBayType(bay), TransportBayConstructionData.LIVESTOCK_CARGO);
    }

    @Test
    public void cargoBayLegalForMech() {
        Entity entity = createEntity(Entity.ETYPE_MECH);
        
        assertTrue(TransportBayConstructionData.CARGO.isLegalFor(entity));
    }

    @Test
    public void livestockBayIllegalForMech() {
        Entity entity = createEntity(Entity.ETYPE_MECH);
        
        assertFalse(TransportBayConstructionData.LIVESTOCK_CARGO.isLegalFor(entity));
    }

    @Test
    public void cargoBayLegalForTank() {
        Entity entity = createEntity(Entity.ETYPE_TANK);
        
        assertTrue(TransportBayConstructionData.CARGO.isLegalFor(entity));
    }

    @Test
    public void livestockBayLegalForTank() {
        Entity entity = createEntity(Entity.ETYPE_TANK);
        
        assertTrue(TransportBayConstructionData.LIVESTOCK_CARGO.isLegalFor(entity));
    }

    @Test
    public void bayIllegalForInfantry() {
        Entity entity = createEntity(Entity.ETYPE_INFANTRY);
        
        assertFalse(TransportBayConstructionData.CARGO.isLegalFor(entity));
    }
}

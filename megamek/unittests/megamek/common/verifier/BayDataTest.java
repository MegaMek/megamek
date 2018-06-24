package megamek.common.verifier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import megamek.common.*;

public class BayDataTest {
    
    private Entity createEntity(long etype) {
        Entity entity = mock(Entity.class);
        when(entity.hasETypeFlag(anyLong())).thenAnswer(inv ->
            (((Long) inv.getArguments()[0]).longValue() & etype) != 0);
        return entity;
    }

    @Test
    public void testCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = BayData.CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), BayData.CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testLiquidCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = BayData.LIQUID_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), BayData.LIQUID_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testRefrigeratedCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = BayData.REFRIGERATED_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), BayData.REFRIGERATED_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testInsulatedCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = BayData.INSULATED_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), BayData.INSULATED_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void testLivestockCargoBayMultiplier() {
        final double size = 2.0;
        Bay cargoBay = BayData.LIVESTOCK_CARGO.newBay(size, 0);
        
        assertEquals(cargoBay.getWeight(), BayData.LIVESTOCK_CARGO.getWeight() * size, 0.01);
    }

    @Test
    public void identifyMechBay() {
        Bay bay = new MechBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.MECH);
    }

    @Test
    public void identifyProtomechBay() {
        Bay bay = new ProtomechBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.PROTOMECH);
    }

    @Test
    public void identifyHeavyVehicleBay() {
        Bay bay = new HeavyVehicleBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.VEHICLE_HEAVY);
    }

    @Test
    public void identifyLightVehcleay() {
        Bay bay = new LightVehicleBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.VEHICLE_LIGHT);
    }

    @Test
    public void identifySuperHeavyVehicleBay() {
        Bay bay = new SuperHeavyVehicleBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.VEHICLE_SH);
    }

    @Test
    public void identifyFootInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.FOOT);
        
        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_FOOT);
    }

    @Test
    public void identifyJumpInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.JUMP);
        
        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_JUMP);
    }

    @Test
    public void identifyMotorizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.MOTORIZED);
        
        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_MOTORIZED);
    }

    @Test
    public void identifyMechanizedInfantryBay() {
        Bay bay = new InfantryBay(1, 1, 0, InfantryBay.PlatoonType.MECHANIZED);
        
        assertEquals(BayData.getBayType(bay), BayData.INFANTRY_MECHANIZED);
    }

    @Test
    public void identifyISBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, false);
        
        assertEquals(BayData.getBayType(bay), BayData.IS_BATTLE_ARMOR);
    }

    @Test
    public void identifyClanBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, true, false);
        
        assertEquals(BayData.getBayType(bay), BayData.CLAN_BATTLE_ARMOR);
    }

    @Test
    public void identifyCSBABay() {
        Bay bay = new BattleArmorBay(1, 1, 0, false, true);
        
        assertEquals(BayData.getBayType(bay), BayData.CS_BATTLE_ARMOR);
    }

    @Test
    public void identifyFighterBay() {
        Bay bay = new ASFBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.FIGHTER);
    }

    @Test
    public void identifySmallCraftBay() {
        Bay bay = new SmallCraftBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.SMALL_CRAFT);
    }

    @Test
    public void identifyCargoBay() {
        Bay bay = new CargoBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.CARGO);
    }

    @Test
    public void identifyLiquidCargoBay() {
        Bay bay = new LiquidCargoBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.LIQUID_CARGO);
    }

    @Test
    public void identifyRefrigeratedCargoBay() {
        Bay bay = new RefrigeratedCargoBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.REFRIGERATED_CARGO);
    }

    @Test
    public void identifyInsulatedCargoBay() {
        Bay bay = new InsulatedCargoBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.INSULATED_CARGO);
    }

    @Test
    public void identifyLivestockCargoBay() {
        Bay bay = new LivestockCargoBay(1, 1, 0);
        
        assertEquals(BayData.getBayType(bay), BayData.LIVESTOCK_CARGO);
    }

    @Test
    public void cargoBayLegalForMech() {
        Entity entity = createEntity(Entity.ETYPE_MECH);
        
        assertTrue(BayData.CARGO.isLegalFor(entity));
    }

    @Test
    public void livestockBayIllegalForMech() {
        Entity entity = createEntity(Entity.ETYPE_MECH);
        
        assertFalse(BayData.LIVESTOCK_CARGO.isLegalFor(entity));
    }

    @Test
    public void cargoBayLegalForTank() {
        Entity entity = createEntity(Entity.ETYPE_TANK);
        
        assertTrue(BayData.CARGO.isLegalFor(entity));
    }

    @Test
    public void livestockBayLegalForTank() {
        Entity entity = createEntity(Entity.ETYPE_TANK);
        
        assertTrue(BayData.LIVESTOCK_CARGO.isLegalFor(entity));
    }

    @Test
    public void bayIllegalForInfantry() {
        Entity entity = createEntity(Entity.ETYPE_INFANTRY);
        
        assertFalse(BayData.CARGO.isLegalFor(entity));
    }
}

package megamek.client.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.ratgenerator.AvailabilityRating;
import megamek.client.ratgenerator.ChassisRecord;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.CompositeTechLevel;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.ITechnology;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitType;

public class RATToRatGeneratorConverter {
    private static String[] ratingLevels = new String[] { "A", "B", "C", "D", "F" }; 
    
    public void convertTurretRATs() {
        /*for(int year = 2750; year <= 3145; year++) {
            convertSingleTurretRAT(year);
        }*/
        RATGenerator.getInstance().initRemainingUnits();
        
        Set<String> addedChassis = new HashSet<>();
        
        MechSummary ms = MechSummaryCache.getInstance().getMech("Blazer Cannon Turret (Single)");
        //MechSummary ms = MechSummaryCache.getInstance().getMech("Blazer Cannon Turret (Dual)");
        try {
            GunEmplacement turret = (GunEmplacement) new MechFileParser(ms.getSourceFile(), 
                ms.getEntryName()).getEntity();

            //turret.getPrototypeDate()
            int availabilityLevel = calculateAvailabilityRating(turret, ITechnology.F_NONE, 3025, turret.isClan());
        }
        catch (Exception e) {
            return;
        }
        
        /*for(ModelRecord mr : RATGenerator.getInstance().getModelList()) {
            if(mr.getUnitType() != UnitType.GUN_EMPLACEMENT && !mr.getChassis().contains("LRM 10")) {
                continue;
            }
            
            try {
                GunEmplacement turret = (GunEmplacement) new MechFileParser(mr.getMechSummary().getSourceFile(), 
                        mr.getMechSummary().getEntryName()).getEntity();
                
                int techLevel = turret.getTechLevel(3025);
                int alpha = 1;
                if(!addedChassis.contains(mr.getChassisKey())) {
                    //int nextDate = Math.max(turret.getProductionDate(), turret.getExtinctionDate());
                    //CompositeTechLevel.DATE_NONE;
                    
                    
                    //insertAvailabilityRecords(mr.getChassisKey(), turret.getPrototypeDate(), nextDate)
                }
            } catch (Exception e) {
                int alpha = 1;
            }
            
        }*/
        
        //RATGenerator.getInstance().exportRATGen(Configuration.forceGeneratorDir());
    }
    
    private int calculateAvailabilityRating(Entity entity, int faction, int year, boolean clan) {
        // availability level is plays into how often we generate this unit
        int era = ITechnology.getTechEra(year);
        int inverseAvailabilityLevel = 0;//ITechnology.RATING_X - entity.getTechLevel(era);
        
        // units in experimental or advanced stage will be less common 
        int advancementFactor = 0;
        
        if(isExperimental(entity, faction, year, clan)) {
            advancementFactor = 1;
        } else if (isAdvanced(entity, faction, year, clan)) {
            advancementFactor = 3;
        } else if (isCommon(entity, faction, year, clan)) {
            advancementFactor = 5;
        }
        
        if(advancementFactor == 0) {
            return 0;
        } else {
            return advancementFactor + inverseAvailabilityLevel;
        }
    }
    
    /**
     * Whether the given entity is experimental
     * @param entity
     * @param faction
     * @param year
     * @return
     */
    private boolean isExperimental(Entity entity, int faction, int year, boolean clan) {
        return entity.getPrototypeDate(clan, faction) != ITechnology.DATE_NONE &&
                entity.getPrototypeDate(clan, faction) <= year &&
                !isAdvanced(entity, faction, year, clan) && 
                !isCommon(entity, faction, year, clan) &&
                !entity.isExtinct(year);
    }
    
    private boolean isAdvanced(Entity entity, int faction, int year, boolean clan) {
        return entity.getProductionDate(entity.isClan(), faction) != ITechnology.DATE_NONE &&
                entity.getProductionDate(entity.isClan(), faction) <= year &&
                !isCommon(entity, faction, year, clan) &&
                !entity.isExtinct(year, clan, faction);
    }
    
    private boolean isCommon(Entity entity, int faction, int year, boolean clan) {
        return entity.getCommonDate() != ITechnology.DATE_NONE &&
                entity.getCommonDate() <= year &&
                !entity.isExtinct(year, clan, faction);
    }
    
    private List<Integer> calculateDateSequence(Entity entity) {
        return null;
    }
    
    private void insertAvailabilityRecords(String chassisKey, int startYear, int endYear, String exclusiveFaction, int rating) {
        
    }
    
    public void convertSingleTurretRAT(int year) {
        // start with F. These are availability 5-.
        // D is availability 4-
        // C is availability 3-
        // B is availability 2-
        // A is availability 1-
        
        // for each unit:
        // get mech summary cache
        // if chassis name not in dictionary, add to dictionary and set chassisfaction rating
        // if model name not in dictionary, add to dictionary and set modelfactionrating
        
        /*Map<String, Set<String>> addedUnits = new HashMap<>();
        
        RATGenerator.getInstance().initRemainingUnits();
        
        for(int ratingIndex = ratingLevels.length - 1; ratingIndex >= 0; ratingIndex--) {
            String ratName = String.format("Turrets %d %s", year, ratingLevels[ratingIndex]);
        
            // there's no turret rat for this year, move on
            if(!RandomUnitGenerator.getInstance().getRatMap().containsKey(ratName)) {
                return;
            }
            
            int actualYear = RATGenerator.getInstance().getEraSet().floor(year);
            RATGenerator.getInstance().loadYear(actualYear);
            for(ChassisRecord cr : RATGenerator.getInstance().getChassisList()) {
                if(cr.getUnitType() == UnitType.GUN_EMPLACEMENT) {
                    int alpha = 1;
                }
            }
            
            for(String unit : RandomUnitGenerator.getInstance().getRatMap().get(ratName).getUnits()) {
                MechSummary ms = MechSummaryCache.getInstance().getMech(unit);
                
                if(!addedUnits.containsKey(ms.getChassis())) {
                    addedUnits.put(ms.getChassis(), new HashSet<>());
                    setChassisAvailabilityRating(ms, actualYear, ratingIndex + 1);
                }
                
                if(!addedUnits.get(ms.getChassis()).contains(ms.getModel())) {
                    addedUnits.get(ms.getChassis()).add(ms.getModel());
                    setModelAvailabilityRating(ms, actualYear);
                }
            }
        }*/
    }
    
    private void setChassisAvailabilityRating(MechSummary ms, int year, int availability) {
        ModelRecord mr = new ModelRecord(ms);
        AvailabilityRating ar = new AvailabilityRating(mr.getChassisKey(), year, String.format("General:%d-", availability));
        RATGenerator.getInstance().setChassisFactionRating(year, mr.getChassisKey(), ar);
    }
    
    private void setModelAvailabilityRating(MechSummary ms, int year) {
        ModelRecord mr = new ModelRecord(ms);
        AvailabilityRating ar = new AvailabilityRating(mr.getKey(), year, "General:5");
        RATGenerator.getInstance().setModelFactionRating(year, mr.getKey(), ar);
    }
}

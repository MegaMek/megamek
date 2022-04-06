${fullName}
<#if includeFluff>
<#include "fluff.ftlh">
</#if>
  
Type: ${chassis}
Chassis Type: ${moveType} (${weightClass})
Technology Base: ${techBase} 
Mass: ${mass} ${weightStandard}
Battle Value: ${battleValue}
	
${formatBasicDataRow("Equipment", "", "Mass (" + weightStandard + ")")}
${formatBasicDataRow("Chassis/Controls", "", chassisControlMass)}
${formatBasicDataRow("Engine/Trans.", "", engineMass)}
    Cruise MP:${walkMP}
	Flank MP:${runMP}
${formatBasicDataRow("Heat Sinks", hsCount, hsMass)}
${formatBasicDataRow("Fuel", fuelRange, fuelMass)}
<#if amplifierMass gt 0>
${formatBasicDataRow("Power Amplifier", "", amplifierMass)}
</#if>
<#if hasTurret2>
${formatBasicDataRow("Rear Turret", "", turretMass)}
${formatBasicDataRow("Front Turret", "", turretMass2)}
<#elseif hasTurret>
${formatBasicDataRow("Turret", "", turretMass)}
</#if>
${formatBasicDataRow("Armor Factor (" + barRating + ")", armorFactor, armorMass)}

     ${formatArmorRow("", "Internal", "Armor")}
     ${formatArmorRow("", "Structure", "Value")}
     ${formatArmorRow("Front", structureValues.FR, armorValues.FR)}<#if patchworkByLoc??> ${patchworkByLoc.FR}</#if>
<#if armorValues.FRRS??>
     ${formatArmorRow("Front R/L Side", structureValues.FRRS, armorValues.FRRS)}<#if patchworkByLoc??> ${patchworkByLoc.FRRS}</#if>
     ${formatArmorRow("Rear R/L Side", structureValues.RRRS, armorValues.RRRS)}<#if patchworkByLoc??> ${patchworkByLoc.RRRS}</#if>
<#else>     
     ${formatArmorRow("R/L Side", structureValues.RS, armorValues.RS)}<#if patchworkByLoc??> ${patchworkByLoc.RS}</#if>
</#if>
     ${formatArmorRow("Rear", structureValues.RR, armorValues.RR)}<#if patchworkByLoc??> ${patchworkByLoc.RR}</#if>
<#if hasTurret2>
     ${formatArmorRow("Rear Turret", structureValues.TU, armorValues.TU)}<#if patchworkByLoc??> ${patchworkByLoc.TU}</#if>
     ${formatArmorRow("Front Turret", structureValues.FT, armorValues.FT)}<#if patchworkByLoc??> ${patchworkByLoc.FT}</#if>
<#elseif hasTurret>
     ${formatArmorRow("Turret", structureValues.TU, armorValues.TU)}<#if patchworkByLoc??> ${patchworkByLoc.TU}</#if>
</#if>
<#if isVTOL>
     ${formatArmorRow("Rotor", structureValues.RO, armorValues.RO)}<#if patchworkByLoc??> ${patchworkByLoc.RO}</#if>
</#if>
	
<#if isOmni>
Fixed Equipment
	<#if fixedTonnage gt 0>
${formatBasicDataRow("Location", "Fixed", "Tonnage")}	
	<#list fixedEquipment as row>
		<#if row.equipment != "None">
${formatBasicDataRow(row.location, row.equipment, row.tonnage)}
		</#if>
	</#list>
	<#else>
None
	</#if>
</#if>

Weapons
${formatEquipmentRow("and Ammo", "Location", "Tonnage")}	
<#list weaponList as eq>
${formatEquipmentRow(eq.name, eq.location, eq.tonnage)}
<#else>
None
</#list>
	
Cargo
<#list bays>
    <#items as bay>
    ${formatBayRow("Bay " + bay?counter + ":", bay.name + " (" + bay.size + ")", bay.doors + (bay.doors == 1) ? string(" Door", " Doors"))}
	</#items>
<#else>
    None
</#list>

<#if chassisMods?size + miscEquipment?size gt 0>
Notes: 
<#if chassisMods?size gt 0>
Features ${chassisMods?join(", ")} Chassis and Controls Modification<#if chassisMods?size gt 1>s</#if>
</#if>
<#list miscEquipment as misc>
${misc}
</#list>
</#if>
	
<#if quirks??>
Features the following design quirks: ${quirks}
</#if>
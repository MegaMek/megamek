${fullName}

<#if includeFluff>
<#include "fluff.ftl">
</#if>

Type: ${chassis}
Technology Base: ${techBase} 
Movement Type: ${moveType}
Tonnage: ${tonnage}
Battle Value: ${battleValue}

${formatBasicDataRow("Equipment", "", "Mass")}
${formatBasicDataRow("Internal Structure", "", isMass)}
${formatBasicDataRow("Engine", engineName, engineMass)}
	Cruising MP: ${walkMP}
	Flank MP: ${runMP}
<#if jumpMP??>
	Jumping MP: ${jumpMP}
</#if>
${formatBasicDataRow("Heat Sinks:", hsCount, hsMass)}
${formatBasicDataRow("Control Equipment:", "", controlMass)}
<#if liftMass gt 0>
${formatBasicDataRow("Lift Equipment:", "", liftMass)}
</#if>
${formatBasicDataRow("Power Amplifier:", "", amplifierMass)}
<#if hasTurret2>
${formatBasicDataRow("Rear Turret:", "", turretMass)}
${formatBasicDataRow("Front Turret:", "", turretMass2)}
<#elseif hasTurret>
${formatBasicDataRow("Turret:", "", turretMass)}
</#if>
${formatBasicDataRow("Armor Factor" + armorType, armorFactor, armorMass)}

     ${formatArmorRow("", "Internal", "Armor")}
     ${formatArmorRow("", "Structure", "Value")}
     ${formatArmorRow("Front", structureValues.FR, armorValues.FR)}<#if patchworkByLoc??> ${patchworkByLoc.FR}</#if>
<#if isSuperheavy && !isVTOL>
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
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.tonnage)}
</#list>
	
<#if quirks??>
Features the following design quirks: ${quirks}
</#if>
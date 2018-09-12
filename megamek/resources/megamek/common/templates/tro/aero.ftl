${fullName}

<#if includeFluff>
<#include "fluff.ftl">
</#if>

Type: ${chassis}
Technology Base: ${techBase} 
Tonnage: ${tonnage}
Battle Value: ${battleValue}

${formatBasicDataRow("Equipment", "", "Mass")}
${formatBasicDataRow("Engine", engineName, engineMass)}
	Safe Thrust: ${safeThrust}
	Max Thrust: ${maxThrust}
${formatBasicDataRow("Structural Integrity:", si, "")}
${formatBasicDataRow("Heat Sinks:", hsCount, hsMass)}
${formatBasicDataRow("Fuel:", fuelPoints, fuelMass)}
${formatBasicDataRow(cockpitType, "", cockpitMass)}
${formatBasicDataRow("Armor Factor" + armorType, armorFactor, armorMass)}

     ${formatArmorRow("", "Armor")}
     ${formatArmorRow("", "Value")}
     ${formatArmorRow("Nose", armorValues.NOS)}<#if patchworkByLoc??> ${patchworkByLoc.NOS}</#if>
     ${formatArmorRow("Wings", armorValues.RWG)}<#if patchworkByLoc??> ${patchworkByLoc.RWG}</#if>
     ${formatArmorRow("Aft", armorValues.AFT)}<#if patchworkByLoc??> ${patchworkByLoc.AFT}</#if>

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
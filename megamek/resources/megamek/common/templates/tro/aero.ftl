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
<#if isVSTOL && isConventional && !isSupportVehicle>
${formatBasicDataRow("VSTOL Equipment:", "", vstolMass)}
</#if>
${formatBasicDataRow("Heat Sinks:", hsCount, hsMass)}
${formatBasicDataRow("Fuel:", fuelPoints, fuelMass)}
${formatBasicDataRow(cockpitType, "", cockpitMass)}
${formatBasicDataRow("Armor Factor" + armorType, armorFactor, armorMass)}

     ${formatArmorRow("", "Armor")}
     ${formatArmorRow("", "Value")}
     ${formatArmorRow("Nose", armorValues.NOS)}<#if patchworkByLoc??> ${patchworkByLoc.NOS}</#if>
     ${formatArmorRow("Wings", armorValues.RWG)}<#if patchworkByLoc??> ${patchworkByLoc.RWG}</#if>
     ${formatArmorRow("Aft", armorValues.AFT)}<#if patchworkByLoc??> ${patchworkByLoc.AFT}</#if>

<#if isOmni && fixedTonnage gt 0>
${formatBasicDataRow("Fixed Equipment", "Location", "Tonnage")}
	<#list fixedEquipment as row>
		<#if row.equipment != "None">
${formatBasicDataRow(row.equipment, row.location, row.tonnage)}
		</#if>
	</#list>
</#if>

Weapons
${formatEquipmentRow("and Ammo", "Location", "Tonnage", "Heat", "SRV", "MRV", "LRV", "ERV")}
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.tonnage, eq.heat, eq.srv, eq.mrv, eq.lrv, eq.erv)}
</#list>

<#if quirks??>
Features the following design quirks: ${quirks}
</#if>

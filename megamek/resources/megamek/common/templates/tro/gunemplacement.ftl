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
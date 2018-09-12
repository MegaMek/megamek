${fullName}

<#if includeFluff>
<#include "fluff.ftl">
</#if>

Type: ${chassis}
Technology Base: ${techBase} 
Tonnage: ${tonnage}
Battle Value: ${battleValue}

${formatBasicDataRow("Equipment", "", "Mass")}
${formatBasicDataRow("Internal Structure", structureName, isMass)}
<#if lamConversionMass??>
${formatBasicDataRow("LAM Conversion Equipment", "", lamConversionMass)}
<#elseif qvConversionMass??>
${formatBasicDataRow("QuadVee Conversion Equipment", "", qvConversionMass)}
</#if>
${formatBasicDataRow("Engine", engineName, engineMass)}
	Walking MP: ${walkMP}
	Running MP: ${runMP}
	Jumping MP: ${jumpMP}
<#if airmechCruise??>
	AirMech Cruising MP: ${airmechCruise}
	AirMech Flanking MP: ${airmechFlank}
	Safe Thrust: ${safeThrust}
	Max Thrust: ${maxThrust}
<#elseif qvType??>
	${qvType} Cruising MP: ${qvCruise}
	${qvType} Flanking MP: ${qvFlank}
</#if>
${formatBasicDataRow(hsType, hsCount, hsMass)}
${formatBasicDataRow(gyroType, "", gyroMass)}
${formatBasicDataRow(cockpitType, "", cockpitMass)}
${formatBasicDataRow("Armor Factor" + armorType, armorFactor, armorMass)}

     ${formatArmorRow("", "Internal", "Armor")}
     ${formatArmorRow("", "Structure", "Value")}
     ${formatArmorRow("Head", structureValues.HD, armorValues.HD)}<#if patchworkByLoc??> ${patchworkByLoc.HD}</#if>
     ${formatArmorRow("Center Torso", structureValues.CT, armorValues.CT)}<#if patchworkByLoc??> ${patchworkByLoc.CT}</#if>
     ${formatArmorRow("Center Torso (rear)", "", rearArmorValues.CT)}
     ${formatArmorRow("R/L Torso", structureValues.RT, armorValues.RT)}<#if patchworkByLoc??> ${patchworkByLoc.RT}</#if>
     ${formatArmorRow("R/L Torso (rear)", "", rearArmorValues.RT)}
<#if isQuad>
     ${formatArmorRow("FR/L Leg", structureValues.FRL, armorValues.FRL)}<#if patchworkByLoc??> ${patchworkByLoc.FRL}</#if>
<#else>
     ${formatArmorRow("R/L Arm", structureValues.RA, armorValues.RA)}<#if patchworkByLoc??> ${patchworkByLoc.RA}</#if>
</#if>
<#if isQuad>
     ${formatArmorRow("RR/L Leg", structureValues.RRL, armorValues.RRL)}<#if patchworkByLoc??> ${patchworkByLoc.RRL}</#if>
<#elseif isTripod>
     ${formatArmorRow("R/C/L Leg", structureValues.RL, armorValues.RL)}<#if patchworkByLoc??> ${patchworkByLoc.RL}</#if>
<#else>
     ${formatArmorRow("R/L Leg", structureValues.RL, armorValues.RL)}<#if patchworkByLoc??> ${patchworkByLoc.RL}</#if>
</#if>

<#if isOmni>
Weight and Space Allocation
${formatBasicDataRow("Location", "Fixed", "Space Remaining")}	
<#list fixedEquipment as row>
${formatBasicDataRow(row.location, row.equipment, row.remaining)}
</#list>
</#if>
<#if !isQuad>

Right Arm Actuators: ${rightArmActuators}
Left Arm Actuators: ${leftArmActuators}

</#if>
Weapons
${formatEquipmentRow("and Ammo", "Location", "Critical", "Heat", "Tonnage")}	
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.slots, eq.heat, eq.tonnage)}
</#list>
	
<#if quirks??>
Features the following design quirks: ${quirks}
</#if>
${fullName}

Mass: ${massDesc}
Chassis: ${chassisDesc}
Power Plant: ${engineDesc}
Cruising Speed: ${cruisingSpeed} kph
Maximum Speed: ${maxSpeed} kph
Jump Jets: ${jjDesc}
     Jump Capacity: ${jumpCapacity} meters
Armor: ${armorDesc}
Armament:
<#list armamentList as armament>
     ${armament}
</#list>
Manufacturer: <#if manufacturerDesc??>${manufacturerDesc}<#else>Unknown</#if>
     Primary Factory: <#if factoryDesc??>${factoryDesc}<#else>Unknown</#if>
Communication System: <#if communicationDesc??>${communicationDesc}<#else>Unknown</#if>
Targeting & Tracking System: <#if targetingDesc??>${targetingDesc}<#else>Unknown</#if>
Introduction Year: ${year}
Tech Rating/Availability: ${techRating}
Cost: ${cost} C-bills
<#if fluffOverview??>

Overview
${fluffOverview}
</#if>
<#if fluffCapabilities??>

Capabilities
${fluffCapabilities}
</#if>
<#if fluffDeployment??>

Deployment
${fluffDeployment}
</#if>
<#if fluffHistory??>

History
${fluffHistory}
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
     ${formatArmorRow("Head", structureValues.HD, armorValues.HD)}
     ${formatArmorRow("Center Torso", structureValues.CT, armorValues.CT)}
     ${formatArmorRow("Center Torso (rear)", "", rearArmorValues.CT)}
     ${formatArmorRow("R/L Torso", structureValues.RT, armorValues.RT)}
     ${formatArmorRow("R/L Torso (rear)", "", rearArmorValues.RT)}
<#if isQuad>
     ${formatArmorRow("FR/L Leg", structureValues.FRL, armorValues.RFL)}
<#else>
     ${formatArmorRow("R/L Arm", structureValues.RA, armorValues.RA)}
</#if>
<#if isQuad>
     ${formatArmorRow("RR/L Leg", structureValues.RRL, armorValues.RRL)}
<#elseif isTripod>
     ${formatArmorRow("R/C/L Leg", structureValues.RL, armorValues.RL)}
<#else>
     ${formatArmorRow("R/L Leg", structureValues.RL, armorValues.RL)}
</#if>

<#if isOmni>
Weight and Space Allocation
${formatBasicDataRow("Location", "Fixed", "Space Remaining")}	
<#list fixedEquipment as row>
${formatBasicDataRow(row.location, row.equipment, row.remaining)}
</#list>
</#if>

Weapons
${formatEquipmentRow("and Ammo", "Location", "Critical", "Heat", "Tonnage")}	
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.slots, eq.heat, eq.tonnage)}
</#list>
	
<#if quirks??>
Features the following design quirks: ${quirks}
</#if>
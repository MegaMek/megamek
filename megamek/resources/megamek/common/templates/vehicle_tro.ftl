${fullName}

Mass: ${massDesc}
Movement Type: ${moveType}
Power Plant: ${engineDesc}
Cruising Speed: ${cruisingSpeed} kph
Flank Speed: ${maxSpeed} kph
<#if jumpMP??>
Jump Jets: ${jjDesc}
     Jump Capacity: ${jumpCapacity} meters
</#if>
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
${formatBasicDataRow("Turret 1:", "", turretMass)}
${formatBasicDataRow("Turret 2:", "", turretMass2)}
<#elseif hasTurret>
${formatBasicDataRow("Turret:", "", turretMass)}
</#if>
${formatBasicDataRow("Armor Factor" + armorType, armorFactor, armorMass)}

     ${formatArmorRow("", "Internal", "Armor")}
     ${formatArmorRow("", "Structure", "Value")}
     ${formatArmorRow("Front", structureValues.FR, armorValues.FR)}
<#if isSuperheavy && !isVTOL>
     ${formatArmorRow("FR/FL Side", structureValues.FRRS, armorValues.FRRS)}
     ${formatArmorRow("RR/RL Side", structureValues.RRRS, armorValues.RRRS)}
<#else>     
     ${formatArmorRow("R/L Side", structureValues.RS, armorValues.RS)}
</#if>
     ${formatArmorRow("Rear", structureValues.RR, armorValues.RR)}
<#if hasTurret2>
     ${formatArmorRow("Turret 1", structureValues.TU, armorValues.TU)}
     ${formatArmorRow("Turret 2", structureValues.FT, armorValues.FT)}
<#elseif hasTurret>
     ${formatArmorRow("Turret", structureValues.TU, armorValues.TU)}
</#if>
<#if isVTOL>
     ${formatArmorRow("Rotor", structureValues.RO, armorValues.RO)}
</#if>

<#if isOmni>
Weight and Space Allocation
${formatBasicDataRow("Location", "Fixed", "Tonnage")}	
<#list fixedEquipment as row>
<#if row.equipment != "None">
${formatBasicDataRow(row.location, row.equipment, row.tonnage)}
</#if>
</#list>
</#if>

Weapons
${formatEquipmentRow("and Ammo", "Location", "Tonnage")}	
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.tonnage)}
</#list>
	
<#if quirks??>
Features the following design quirks: ${quirks}
</#if>
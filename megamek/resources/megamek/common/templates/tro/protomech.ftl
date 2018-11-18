${fullName}
<#if includeFluff>
<#include "fluff.ftl">
</#if>

Type: ${chassis} <#if configurationDesc??>(${configurationDesc})</#if>
Technology Base: ${techBase} 
Tonnage: ${tonnage}
Battle Value: ${battleValue}
	
${formatBasicDataRow("Equipment", "", "Mass (kg)")}
${formatBasicDataRow("Internal Structure", "", isMass)}
${formatBasicDataRow("Engine", engineRating, engineMass)}
<#if isGlider>
    Walking MP: 1
    Running MP: 1
    WiGE Cruise MP: ${walkMP}
    WiGE Flank MP: ${runMP}
<#else>
    Walking MP: ${walkMP}
    Running MP: ${runMP}
<#if umuMP??>
${formatBasicDataRow("    UMU MP" + umuMP, "", umuMass)}
<#elseif jumpMass gt 0>
${formatBasicDataRow("    Jumping MP" + jumpMP, "", jumpMass)}
<#else>
    Jumping MP: 0
</#if>
</#if>
${formatBasicDataRow("Heat Sinks:", hsCount, hsMass)}
${formatBasicDataRow("Cockpit:", "", cockpitMass)}
${formatBasicDataRow("Armor Factor" + armorType + ":", armorFactor, armorMass)}

     ${formatArmorRow("", "Internal", "Armor")}
     ${formatArmorRow("", "Structure", "Value")}
     ${formatArmorRow("Head", structureValues.HD, armorValues.HD)}
     ${formatArmorRow("Torso", structureValues.T, armorValues.T)}
<#if !isQuad>
     ${formatArmorRow("R/L Arm", structureValues.RA, armorValues.RA)}
</#if>
     ${formatArmorRow("Legs", structureValues.L, armorValues.L)}
<#if structureValues.MG??>
     ${formatArmorRow("Main Gun", structureValues.MG, armorValues.MG)}
<#else>
     ${formatArmorRow("Main Gun", "-", "-")}
</#if>
	
Weapons
${formatEquipmentRow("and Ammo", "Location", "Mass")}	
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.mass)}
</#list>

<#if quirks??>
Features the following design quirks: ${quirks}
</#if>

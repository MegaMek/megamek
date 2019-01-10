${fullName}
<#if includeFluff>
Type: ${chassis}
Manufacturer: <#if manufacturerDesc??>${manufacturerDesc}<#else>Unknown</#if>
    Primary Factory: <#if factoryDesc??>${factoryDesc}<#else>Unknown</#if>

</#if>
Tech Base: ${techBase} 
Chassis Type: ${chassisType}
Weight Class: ${weightClass}
Maximum Weight: ${weight} kg
Battle Value: ${battleValue}
Swarm/Leg Attack/Mechanized/AP: ${swarmAttack}/${legAttack}/${mechanized}/${antiPersonnel}
<#if legAttack == "Yes" && umuMP??>
Note: Leg attacks can only be made in water of depth 1+
</#if>
</p>

${formatBasicDataRow("Equipment", "", "Slots", "Mass")}	
${formatBasicDataRow("Chassis:", "", "", massChassis + " kg")} 	
${formatBasicDataRow("Motive System:", "", "", "")} 	
${formatBasicDataRow("     Ground MP:", groundMP, "", groundMass + " kg")} 	
<#if jumpMP??>
${formatBasicDataRow("     Jump MP:", jumpMP, "", jumpMass + " kg")} 	
</#if>
<#if umuMP??>	
${formatBasicDataRow("     UMU MP:", umuMP, "", umuMass + " kg")} 	
</#if>	
<#if vtolMP??>	
${formatBasicDataRow("     VTOL MP:", vtolMP, "", vtolMass + " kg")} 	
</#if>
${formatBasicDataRow("Manipulators:", "", "", "")} 	
<#list manipulators as arm>
${formatBasicDataRow("    " + arm.locName + ":", arm.eqName, "", arm.eqMass + " kg")}
</#list>
${formatBasicDataRow("Armor:", armorType, armorSlots, armorMass + " kg")} 	
${formatBasicDataRow("    Armor Value:", armorValue + internal + " (Trooper)", "", "")} 	

${formatEquipmentRow("", "", "Slots", "")}
${formatEquipmentRow("Weapons and Equipment", "Location", "(Capacity)", "Mass")}
<#list equipment as row>
${formatEquipmentRow(row.name, row.location, row.slots, row.mass + " kg")}
</#list>
<#if modularMount??>
${formatEquipmentRow(modularMount.name, modularMount.location, modularMount.slots, modularMount.mass + " kg")}
<#list modularEquipment as row>
${formatEquipmentRow("    " + row.name, "-", row.slots, row.mass + "kg")}
</#list>
</#if>
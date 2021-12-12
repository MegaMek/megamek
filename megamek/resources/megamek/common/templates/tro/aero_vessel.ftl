${fullName}
<#if typeDesc??>
Type: ${typeDesc}
</#if>
Mass: ${massDesc} tons
<#if use??>  	
Use: ${use}
</#if>
Technology Base: ${techBase} 
Introduced: ${year}
Mass: ${tonnage}
Battle Value: ${battleValue}
Tech Rating/Availability: ${techRating}
Cost: ${cost} C-bills

<#if dimensions??>
Dimensions
<#if dimensions.length??>
    Length: ${dimensions.length}
</#if>
<#if dimensions.width??>
    Width: ${dimensions.width}
</#if>
<#if dimensions.height??>
    Height: ${dimensions.height}
</#if>

</#if>	
Fuel: ${fuelMass} tons (${fuelPoints})
Safe Thrust: ${safeThrust}
Maximum Thrust: ${maxThrust}
<#if sailIntegrity??>
Sail Integrity: ${sailIntegrity}
</#if>
<#if kfIntegrity??>
KF Drive Integrity: ${kfIntegrity}
</#if>
<#if jumpRange??>
Jump Range: ${jumpRange}
</#if>
Heat Sinks: ${hsCount}
Structural Integrity: ${si}

Armor
    Nose: ${armorValues.NOS}
<#if armorValues.RS??>
    Sides: ${armorValues.RS}
<#else>
    Fore Sides: ${armorValues.FRS}
    Aft Sides: ${armorValues.ARS}
</#if>
    Aft: ${armorValues.AFT}

Cargo
<#list bays>
    <#items as bay>
    ${formatBayRow("Bay " + bay?counter + ":", bay.name + " (" + bay.size + ")", bay.doors + (bay.doors == 1) ? string(" Door", " Doors"))}
	</#items>
<#else>
    None
</#list>

Ammunition:
<#list ammo as row>
    ${row.shots} rounds of ${row.name} ammunition (${row.tonnage} tons)<#if row?has_next>, </#if>
<#else>
	None
</#list>		

<#if dropshipCapacity??>
Dropship Capacity: ${dropshipCapacity}
</#if>
<#if gravDecks??>
Grav Decks: ${gravDecks?size}<#if gravDecks?size gt 0> (${gravDecks?join(", ")})</#if>
</#if>
Escape Pods: ${escapePods}
Life Boats: ${lifeBoats}
Crew: <#if crew?size gt 0> ${crew?join(", ")}<#else>None</#if>		

<#if miscEquipment?? && miscEquipment?size gt 0>
Notes: Equipped with 
	<#list miscEquipment as eq>
    ${eq}
	</#list>
	${armorMass} tons of ${armorType} armor.
<#else>
Notes: <#if armorMass gt 0>Mounts ${armorMass} tons of ${armorType} armor.</#if>
</#if>

<#if usesWeaponBays>
${formatWeaponBayRow("Weapons:", "", "Capital Attack Values (Standard)")}
${formatWeaponBayRow("Arc (Heat)", "Heat", "SRV", "MRV", "LRV", "ERV", "Class")}
<#list weaponBayArcs as arc>
${arc} (${weaponBayHeat[arc]} Heat)
<#list weaponBays[arc] as bay>
${formatWeaponBayRow(bay.weapons[0], bay.heat, bay.srv, bay.mrv, bay.lrv, bay.erv, bay.class)}
<#list bay.weapons[1..] as wpn>
    ${wpn}
</#list>
</#list>
<#else>
None
</#list>
<#else>
Weapons
${formatEquipmentRow("and Ammo", "Location", "Tonnage", "Heat", "SRV", "MRV", "LRV", "ERV")}	
<#list equipment as eq>
${formatEquipmentRow(eq.name, eq.location, eq.tonnage, eq.heat, eq.srv, eq.mrv, eq.lrv, eq.erv)}
<#else>
None
</#list>
</#if>
	
<#if quirks??>
Features the following design quirks: ${quirks}
</#if>
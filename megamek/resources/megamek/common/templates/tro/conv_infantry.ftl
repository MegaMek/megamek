${fullName}
<#if notableUnit??>
Notable Unit:  ${notableUnit}
</#if>
Tech Base: ${techBase} 
Tech Rating/Availability: ${techRating}
Transport Weight:  ${transportWeight}
Equipment:
    Primary Weapon: ${weaponPrimary}
    Secondary Weapon: ${weaponSecondary}
<#if armorKit??>
    Armor:  ${armorKit}
</#if>
Battle Value: ${battleValue}
Introduction Year: ${year}
Cost: ${cost} C-bills
Notes: ${notes}
	
Platoon Type (Specialty): ${motiveType} (${specialty})
<#if groundMP??>
    Ground MP: ${groundMP}<#if groundMP == 0>*</#if>
</#if>
<#if jumpMP??>
    Jump MP: ${jumpMP}
</#if>
<#if vtolMP??>
    VTOL MP: ${vtolMP}
</#if>
<#if umuMP??>
    Underwater MP: ${umuMP}
</#if>
Platoon Size (Squad/Platoon): ${squadSize * squadCount} (${squadSize}/${squadCount})
Armor Divisor: ${armorDivisor}
To-Hit Modifier (Range in Hexes):
    ${toHitModifiers}
Maximum Weapon Damage (# of Troopers):
    ${maxDamage}
<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>
  	<p>
<#if typeDesc??>
  	<b>Type: </b>${typeDesc}<br/>
</#if>
  	<b>Mass: </b>${massDesc} tons<br/>
<#if use??>  	
  	<b>Use:</b> ${use}<br/>
</#if>
	<b>Technology Base:</b> ${techBase}<br/> 
  	<b>Introduced:</b> ${year}<br/>
	<b>Mass:</b> ${tonnage}<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
  	<b>Tech Rating/Availability:</b> ${techRating}<br/>
  	<b>Cost:</b> ${cost} C-bills<br/>
	</p>
<#if dimensions??>
	<p>
	<b>Dimensions</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Length:</b> ${dimensions.length}<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Width:</b> ${dimensions.width}<br/>
	</p>
</#if>	
	<p>
	<b>Fuel:</b> ${fuelMass} tons (${fuelPoints})<br/>
	<b>Safe Thrust:</b> ${safeThrust}<br/>
	<b>Maximum Thrust:</b> ${maxThrust}<br/>
	<#if sailIntegrity??>
	<b>Sail Integrity:</b> ${sailIntegrity}<br/>
	</#if>
	<#if kfIntegrity??>
	<b>KF Drive Integrity:</b> ${kfIntegrity}<br/>
	</#if>
	<#if jumpRange??>
	<b>Jump Range:</b> ${jumpRange}<br/>
	</#if>
	<b>Heat Sinks:</b> ${hsCount}<br/>
	<b>Structural Integrity:</b> ${si}<br/>
	</p>
	<p>
	<b>Armor</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Nose:</b> ${armorValues.NOS}<br/>
<#if armorValues.RS??>	
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Sides:</b> ${armorValues.RS}<br/>
<#else>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Fore Sides:</b> ${armorValues.FRS}<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Aft Sides:</b> ${armorValues.ARS}<br/>
</#if>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Aft:</b> ${armorValues.AFT}<br/>
	</p>
	<p>
	<b>Cargo</b><br/>
<#list bays>
	<table>
	<#items as bay>
		<tr><td>Bay ${bay?counter}:</td><td>${bay.name} (${bay.size})</td><td>${bay.doors} <#if bay.doors == 1>Door<#else>Doors</#if></td></tr>
	</#items>
	</table>
<#else>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;None<br/>
</#list>
	</p>
	<p>
<#if dropshipCapacity??>
	<b>Dropship Capacity:</b> ${dropshipCapacity}<br/>
</#if>
<#if gravDecks??>
	<b>Grav Decks:</b> ${gravDecks?size}<#if gravDecks?size gt 0> (${gravDecks?join(", ")})</#if><br/>
</#if>
	<b>Escape Pods:</b> ${escapePods}<br/>
	<b>Life Boats:</b> ${lifeBoats}<br/>
	<b>Crew:</b> <#if crew?size gt 0> ${crew?join(", ")}<#else>None</#if><br/>
	</p>
	<p>
	<b>Ammunition:</b>
<#list ammo as row>
	${row.shots} rounds of ${row.name} ammunition (${row.tonnage} tons)<#if row?has_next>, </#if>
<#else>
	None
</#list>		
	<br/>
	</p>
	<p>
<#if miscEquipment?? && miscEquipment?size gt 0>
	<b>Notes: </b> Equipped with 
	<#list miscEquipment as eq>
		${eq}<#if miscEquipment?size gt 1>,</#if>
	</#list>
	and ${armorMass} tons of ${armorType} armor.<br/>
<#else>
	<b>Notes: </b><#if armorMass gt 0>Mounts ${armorMass} tons of ${armorType} armor.</#if><br/>
</#if>
	</p>

<#if usesWeaponBays>
	<table>
		<tr><th align="left">Weapons:</th><th></th><th colspan="4">Capital Attack Values (Standard)</th><th></th></tr>
		<tr><th align="left">Arc (Heat)</th><th>Heat</th><th>SRV</th><th>MRV</th><th>LRV</th><th>ERV</th><th>Class</th></tr>
		<#list weaponBayArcs as arc>
		<tr><td><b>${arc} (${weaponBayHeat[arc]} Heat)</td></tr>
			<#list weaponBays[arc] as bay>
				<tr><td>${bay.weapons?join(", ")}</td><td align="center">${bay.heat}</td><td align="center">${bay.srv}</td><td align="center">${bay.mrv}</td><td align="center">${bay.lrv}</td><td align="center">${bay.erv}</td><td>${bay.class}</td></tr>
			</#list>
		<#else>
		None
		</#list>
	</table>
<#else>
	<table>
		<tr><th align="left">Weapons<br/>and Ammo</th><th>Location</th><th>Tonnage</th><th>Heat</th><th>SRV</th><th>MRV</th><th>LRV</th><th>ERV</th></tr>
		<#list equipment as eq>
			<tr><td>${eq.name}</td><td align="center">${eq.location}</td><td align="center">${eq.tonnage}</td><td>${eq.heat}</td><td>${eq.srv}</td><td>${eq.mrv}</td><td>${eq.lrv}</td><td>${eq.erv}</td></tr>
		<#else>
		None			
		</#list>
	</table>
</#if>
	
	<#if quirks??>
		<p>
		Features the following design quirks: ${quirks}
		</p>
	</#if>
	</div>
</body>
</html>
<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>

<#if includeFluff>
	<#noautoesc>
	<#if fluffOverview??>
	<p>
	<b>Overview</b><br/>
	${fluffOverview}
	</p>
	</#if>
	
	<#if fluffCapabilities??>
	<p>
	<b>Capabilities</b><br/>
	${fluffCapabilities}
	</p>
	</#if>
	
	<#if fluffDeployment??>
	<p>
	<b>Deployment</b><br/>
	${fluffDeployment}
	</p>
	</#if>
	
	<#if fluffHistory??>
	<p>
	<b>History</b><br/>
	${fluffHistory}
	</p>
	</#if>
	</#noautoesc>
</#if>

	<p>
<#if notableUnit??>
	<b>Notable Unit: </b> ${notableUnit}<br/>
</#if>
	<b>Tech Base:</b> ${techBase}<br/> 
	<b>Tech Rating/Availability:</b> ${techRating}<br/>
	<b>Transport Weight: </b> ${transportWeight}<br/>
	<b>Equipment:</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Primary Weapon:</b> ${weaponPrimary}<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Secondary Weapon:</b> ${weaponSecondary}<br/>
<#if armorKit??>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Armor: </b> ${armorKit}<br/>
</#if>
	<b>Battle Value:</b> ${battleValue}<br/>
	<b>Introduction Year:</b> ${year}<br/>
	<b>Cost:</b> ${cost} C-bills<br/>
	<b>Notes:</b> ${notes}<br/>
	</p>
	
	<p>
	<b>Platoon Type (Specialty):</b> ${motiveType} (${specialty})<br/>
<#if groundMP??>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Ground MP:</b> ${groundMP}<#if groundMP == 0>*</#if><br/>
</#if>
<#if jumpMP??>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Jump MP:</b> ${jumpMP}<br/>
</#if>
<#if vtolMP??>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>VTOL MP:</b> ${vtolMP}<br/>
</#if>
<#if umuMP??>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Underwater MP:</b> ${umuMP}<br/>
</#if>
	<b>Platoon Size (Squad/Platoon):</b> ${squadSize * squadCount} (${squadSize}/${squadCount})<br/>
	<b>Armor Divisor:</b> ${armorDivisor}<br/>
	<b>To-Hit Modifier (Range in Hexes):</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${toHitModifiers}<br/>
	<b>Maximum Weapon Damage (# of Troopers):</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${maxDamage}<br/>
	</p>
</div>
</body>
</html>
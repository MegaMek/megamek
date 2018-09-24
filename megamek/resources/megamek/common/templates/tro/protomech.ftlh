<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>
<#if includeFluff>
<#include "fluff.ftlh">
</#if>
	<p>
	<b>Type:</b> ${chassis} <#if configurationDesc??>(${configurationDesc})</#if><br/>
	<b>Technology Base:</b> ${techBase}<br/> 
	<b>Tonnage:</b> ${tonnage}<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
	</p>
	
	<table>
	<tr><th>Equipment</th><th/><th>Mass (kg)</th></tr>
	<tr><td>Internal Structure</td><td></td><td align="center">${isMass}</td></tr>
	<tr><td>Engine</td><td align="center">${engineRating}</td><td align="center">${engineMass}</td></tr>
	<#if isGlider>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Walking MP:</td><td align="center">1</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Running MP:</td><td align="center">1</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;WiGE Cruise MP:</td><td align="center">${walkMP}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;WiGE Flank MP:</td><td align="center">${runMP}</td><td></td></tr>
	<#else>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Walking MP:</td><td align="center">${walkMP}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Running MP:</td><td align="center">${runMP}</td><td></td></tr>
		<#if umuMP??>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;UMU MP:</td><td align="center">${umuMP}</td><td align="center">${umuMass}</td></tr>
		<#else>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Jumping MP:</td><td align="center">${jumpMP}</td><td align="center"><#if jumpMass gt 0>${jumpMass}</#if></td></tr>
		</#if>
	</#if>
	<tr><td>Heat Sinks:</td><td align="center">${hsCount}</td><td align="center">${hsMass}</td></tr>
	<tr><td>Cockpit:</td><td></td><td align="center">${cockpitMass}</td></tr>
	<tr><td>Armor Factor${armorType}:</td><td>${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table width="80%">
		<tr><th></th><th>Internal<br/>Structure</th><th>Armor<br/>Value</th></tr>
		<tr>
			<td>Head</td>
			<td align="center">${structureValues.HD}</td>
			<td align="center">${armorValues.HD}</td>
		</tr>
		<tr>
			<td>Torso</td>
			<td align="center">${structureValues.T}</td>
			<td align="center">${armorValues.T}</td>
		</tr>
		<tr>
			<td>R/L Arm</td>
			<td align="center">${structureValues.RA}</td>
			<td align="center">${armorValues.RA}</td>
		</tr>
		<tr>
			<td>Legs</td>
			<td align="center">${structureValues.L}</td>
			<td align="center">${armorValues.L}</td>
		</tr>
		<tr>
			<td>Main Gun</td>
			<#if structureValues.MG??>
				<td align="center">${structureValues.MG}</td>
				<td align="center">${armorValues.MG}</td>
			<#else>
				<td align="center">-</td>
				<td align="center">-</td>
			</#if>
		</tr>
	</table>
	
	<table>
		<tr><th align="left">Weapons<br/>and Ammo</th><th>Location</th><th>Mass (kg)</th></tr>
		<#list equipment as eq>
			<tr><td>${eq.name}</td><td align="center">${eq.location}</td><td align="center">${eq.mass}</td></tr>
		</#list>
	</table>
	
	<#if quirks??>
		<p>
		Features the following design quirks: ${quirks}
		</p>
	</#if>
	</div>
</body>
</html>
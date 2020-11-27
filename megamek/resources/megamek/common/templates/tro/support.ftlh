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
	<b>Type:</b> ${chassis}<br/>
	<b>Chassis Type:</b> ${moveType} (${weightClass})<br/>
	<b>Technology Base:</b> ${techBase}<br/> 
	<b>Mass:</b> ${mass} ${weightStandard}<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
	</p>
	
	<table>
	<tr><th>Equipment</th><th/><th>Mass (${weightStandard})</th></tr>
	<tr><td>Chassis/Controls</td><td></td><td align="center">${chassisControlMass}</td></tr>
	<tr><td>Engine/Trans.</td><td align="center">${engineName}</td><td align="center">${engineMass}</td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Cruise MP:</td><td align="center">${walkMP}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Flank MP:</td><td align="center">${runMP}</td><td></td></tr>
	<tr><td>Heat Sinks:</td><td align="center">${hsCount}</td><td align="center">${hsMass}</td></tr>
	<tr><td>Fuel:</td><td align="center">${fuelRange}</td><td align="center">${fuelMass}</td></tr>
<#if amplifierMass gt 0>
	<tr><td>Power Amplifier:</td><td></td><td align="center">${amplifierMass}</td</tr>
</#if>
<#if hasTurret2>
	<tr><td>Rear Turret:</td><td></td><td align="center">${turretMass}</td</tr>
	<tr><td>Front Turret:</td><td></td><td align="center">${turretMass2}</td</tr>
<#elseif hasTurret>
	<tr><td>Turret:</td><td></td><td align="center">${turretMass}</td</tr>
</#if>
	<tr><td>Armor Factor (${barRating}):</td><td>${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table width="80%">
		<tr><th></th><th>Internal<br/>Structure</th><th>Armor<br/>Value</th></tr>
		<tr>
			<td>Front</td>
			<td align="center">${structureValues.FR}</td>
			<td align="center">${armorValues.FR}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.FR}</td></#if>
		</tr>
<#if armorValues.FRRS??>
		<tr>
			<td>Front R/L Side</td>
			<td align="center">${structureValues.FRRS}</td>
			<td align="center">${armorValues.FRRS}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.FRRS}</td></#if>
		</tr>
		<tr>
			<td>Rear R/L Side</td>
			<td align="center">${structureValues.RRRS}</td>
			<td align="center">${armorValues.RRRS}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.RRRS}</td></#if>
		</tr>
<#else>
		<tr>
			<td>R/L Side</td>
			<td align="center">${structureValues.RS}</td>
			<td align="center">${armorValues.RS}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.RS}</td></#if>
		</tr>
</#if>
		<tr>
			<td>Rear</td>
			<td align="center">${structureValues.RR}</td>
			<td align="center">${armorValues.RR}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.RR}</td></#if>
		</tr>
<#if isVTOL>
		<tr>
			<td>Rotor</td>
			<td align="center">${structureValues.RO}</td>
			<td align="center">${armorValues.RO}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.RO}</td></#if>
		</tr>
</#if>
<#if hasTurret2>
		<tr>
			<td>Rear Turret</td>
			<td align="center">${structureValues.TU}</td>
			<td align="center">${armorValues.TU}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.TU}</td></#if>
		</tr>
		<tr>
			<td>Front Turret</td>
			<td align="center">${structureValues.FT}</td>
			<td align="center">${armorValues.FT}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.FT}</td></#if>
		</tr>
<#elseif hasTurret>
		<tr>
			<td>Turret</td>
			<td align="center">${structureValues.TU}</td>
			<td align="center">${armorValues.TU}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.TU}</td></#if>
		</tr>
</#if>
	</table>
	
	<#if isOmni>
	<b>Fixed Equipment</b><br/>
		<#if fixedTonnage gt 0>
			<table>
			<tr><td><i>Location</i></td><td align="center"><i>Fixed</i></td><td align="center"><i>Mass (${weightStandard})</i></td></tr>
			<#list fixedEquipment as row>
				<#if row.equipment != "None">
			<tr><td>${row.location}</td><td align="center">${row.equipment}</td><td align="center">${row.tonnage}</td></tr>
				</#if>
			</#list>
			</table>
		<#else>
			None
		</#if>
	</#if>
	
	<table>
		<tr><th align="left">Weapons<br/>and Ammo</th><th>Location</th><th>Critical</th><th>Mass (${weightStandard})</th></tr>
		<#list weaponList as eq>
			<tr><td>${eq.name}</td><td align="center">${eq.location}</td><td align="center">${eq.slots}</td><td align="center">${eq.tonnage}</td></tr>
		<#else>
			<tr><td>None</td></tr>
		</#list>
	</table>

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
	<b>Notes: </b>
	<#if chassisMods?size + miscEquipment?size gt 0>Features
		<#if chassisMods?size gt 0>
		${chassisMods?join(", ")} Chassis and Controls Modification<#if chassisMods?size gt 1>s</#if><#if miscEquipment?size gt 0>, </#if>
		</#if>	
		<#if miscEquipment?size gt 0>
		${miscEquipment?join(", ")}<br/>
		</#if>
	<#else>
	None
	</#if><br/>
	
	<#if quirks??>
		<p>
		Features the following design quirks: ${quirks}
		</p>
	</#if>
	</div>
</body>
</html>
<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>
  	<p>
  	<b>Mass: </b>${massDesc}<br/>
  	<b>Movement Type: </b>${moveType}<br/>
  	<b>Power Plant: </b>${engineDesc}<br/>
  	<b>Cruising Speed: </b>${cruisingSpeed} kph<br/>
  	<b>Maximum Speed: </b>${maxSpeed} kph<br/>
<#if jumpMP??>  	
  	<b>Jump Jets: </b>${jjDesc}<br/>
  	<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Jump Capacity: </b>${jumpCapacity} meters<br/>
</#if>
  	<b>Armor: </b>${armorDesc}</b><br/>
  	<b>Armament:</b><br/>
  		<#list armamentList as armament>
  			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${armament}<br/>
  		</#list>
	<#if manufacturerDesc??><b>Manufacturer:</b> ${manufacturerDesc}<br/></#if>
  	<#if factoryDesc??><b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Primary Factory: </b>${factoryDesc}</b></#if>
  	<#if communicationDesc??><b>Communication System: </b>${communicationDesc}<br/></#if>
  	<#if targetingDesc??><b>Targeting & Tracking System: </b>${targetingDesc}<br/></#if>
  	<b>Introduction Year:</b> ${year}<br/>
  	<b>Tech Rating/Availability:</b> ${techRating}<br/>
  	<b>Cost:</b> ${cost} C-bills<br/>
  	</p>
	
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
  
	<p>
	<b>Type:</b> ${chassis}<br/>
	<b>Technology Base:</b> ${techBase}<br/> 
	<b>Tonnage:</b> ${tonnage}<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
	</p>
	
	<table>
	<tr><th>Equipment</th><th/><th>Mass</th></tr>
	<tr><td>Internal Structure</td><td></td><td align="center">${isMass}</td></tr>
	<tr><td>Engine</td><td align="center">${engineName}</td><td align="center">${engineMass}</td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Walking MP:</td><td align="center">${walkMP}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Running MP:</td><td align="center">${runMP}</td><td></td></tr>
<#if jumpMP??>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Jumping MP:</td><td align="center">${jumpMP}</td><td></td></tr>
</#if>	
	<tr><td>Heat Sinks:</td><td align="center">${hsCount}</td><td align="center">${hsMass}</td></tr>
	<tr><td>Control Equipment:</td><td></td><td align="center">${controlMass}</td</tr>
<#if liftMass gt 0>
	<tr><td>Lift Equipment:</td><td></td><td align="center">${liftMass}</td</tr>
</#if>
	<tr><td>Power Amplifier:</td><td></td><td align="center">${amplifierMass}</td</tr>
<#if hasTurret2>
	<tr><td>Rear Turret:</td><td></td><td align="center">${turretMass}</td</tr>
	<tr><td>Front Turret:</td><td></td><td align="center">${turretMass2}</td</tr>
<#elseif hasTurret>
	<tr><td>Turret:</td><td></td><td align="center">${turretMass}</td</tr>
</#if>
	<tr><td>Armor Factor${armorType}:</td><td>${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table width="80%">
		<tr><th></th><th>Internal<br/>Structure</th><th>Armor<br/>Value</th></tr>
		<tr>
			<td>Front</td>
			<td align="center">${structureValues.FR}</td>
			<td align="center">${armorValues.FR}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.FR}</td></#if>
		</tr>
<#if isSuperheavy && !isVTOL>
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
			<tr><td><i>Location</i></td><td align="center"><i>Fixed</i></td><td align="center"><i>Tonnage</i></td></tr>
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
		<tr><th align="left">Weapons<br/>and Ammo</th><th>Location</th><th>Critical</th><th>Tonnage</th></tr>
		<#list equipment as eq>
			<tr><td>${eq.name}</td><td align="center">${eq.location}</td><td align="center">${eq.slots}</td><td align="center">${eq.tonnage}</td></tr>
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
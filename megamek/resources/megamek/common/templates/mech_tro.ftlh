<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>
  	<p>
  	<b>Mass: </b>${massDesc}<br/>
  	<b>Chassis: </b>${chassisDesc}<br/>
  	<b>Power Plant: </b>${engineDesc}<br/>
  	<b>Cruising Speed: </b>${cruisingSpeed} kph<br/>
  	<b>Maximum Speed: </b>${maxSpeed} kph<br/>
  	<b>Jump Jets: </b>${jjDesc}<br/>
  	<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Jump Capacity: </b>${jumpCapacity} meters<br/>
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
	<tr><td>Internal Structure</td><td align="center">${structureName}</td><td align="center">${isMass}</td></tr>
	<#if lamConversionMass??>
	<tr><td>LAM Conversion Equipment</td><td></td><td align="center">${lamConversionMass}</td></tr>
	<#elseif qvConversionMass??>
	<tr><td>QuadVee Conversion Equipment</td><td></td><td align="center">${qvConversionMass}</td></tr>
	</#if>
	<tr><td>Engine</td><td align="center">${engineName}</td><td align="center">${engineMass}</td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Walking MP:</td><td align="center">${walkMP}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Running MP:</td><td align="center">${runMP}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Jumping MP:</td><td align="center">${jumpMP}</td><td></td></tr>
	<#if airmechCruise??>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;AirMech Cruising MP:</td><td align="center">${airmechCruise}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;AirMech Flanking MP:</td><td align="center">${airmechFlank}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Safe Thrust:</td><td align="center">${safeThrust}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Max Thrust:</td><td align="center">${maxThrust}</td><td></td></tr>
	<#elseif qvType??>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${qvType} Cruising MP:</td><td align="center">${qvCruise}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${qvType} Flanking MP:</td><td align="center">${qvFlank}</td><td></td></tr>
	</#if>
	<tr><td>${hsType}:</td><td align="center">${hsCount}</td><td align="center">${hsMass}</td></tr>
	<tr><td>${gyroType}:</td><td></td><td align="center">${gyroMass}</td></tr>
	<tr><td>${cockpitType}:</td><td></td><td align="center">${cockpitMass}</td></tr>
	<tr><td>Armor Factor${armorType}:</td><td>${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table width="80%">
		<tr><th></th><th>Internal<br/>Structure</th><th>Armor<br/>Value</th></tr>
		<tr>
			<td>Head</td>
			<td align="center">${structureValues.HD}</td>
			<td align="center">${armorValues.HD}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.HD}</td></#if>
		</tr>
		<tr>
			<td>Center Torso</td>
			<td align="center">${structureValues.CT}</td>
			<td align="center">${armorValues.CT}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.CT}</td></#if>
		</tr>
		<tr>
			<td>Center Torso (rear)</td>
			<td></td>
			<td align="center">${rearArmorValues.CT}</td>
		</tr>
		<tr>
			<td>R/L Torso</td>
			<td align="center">${structureValues.RT}</td>
			<td align="center">${armorValues.RT}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.RT}</td></#if>
		</tr>
		<tr>
			<td>R/L Torso (rear)</td>
			<td></td>
			<td align="center">${rearArmorValues.RT}</td>
		</tr>
		<#if isQuad>
			<tr>
				<td>FR/L Leg</td>
				<td align="center">${structureValues.FRL}</td>
				<td align="center">${armorValues.FRL}</td>
				<#if patchworkByLoc??><td>${patchworkByLoc.FRL}</td></#if>
			</tr>
		<#else>
			<tr>
				<td>R/L Arm</td>
				<td align="center">${structureValues.RA}</td>
				<td align="center">${armorValues.RA}</td>
				<#if patchworkByLoc??><td>${patchworkByLoc.RA}</td></#if>
			</tr>
		</#if>
		<#if isQuad>
			<tr>
				<td>RR/L Leg</td>
				<td align="center">${structureValues.RRL}</td>
				<td align="center">${armorValues.RRL}</td>
				<#if patchworkByLoc??><td>${patchworkByLoc.RRL}</td></#if>
			</tr>
		<#elseif isTripod>
			<tr>
				<td>R/C/L Leg</td>
				<td align="center">${structureValues.RL}</td>
				<td align="center">${armorValues.RL}</td>
				<#if patchworkByLoc??><td>${patchworkByLoc.RL}</td></#if>
			</tr>
		<#else>
			<tr>
				<td>R/L Leg</td>
				<td align="center">${structureValues.RL}</td>
				<td align="center">${armorValues.RL}</td>
				<#if patchworkByLoc??><td>${patchworkByLoc.RL}</td></#if>
			</tr>
		</#if>
	</table>
	
	<#if isOmni>
	<b>Weight and Space Allocation</b>
	<table>
	<tr><td><i>Location</i></td><td align="center"><i>Fixed</i></td><td align="center"><i>Space Remaining</i></td></tr>
	<#list fixedEquipment as row>
	<tr><td>${row.location}</td><td align="center">${row.equipment}</td><td align="center">${row.remaining}</td></tr>
	</#list>
	</table>
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
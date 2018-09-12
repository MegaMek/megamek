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
  	<b>Mass: </b>${massDesc}<br/>
<#if frameDesc??>  	
  	<b>Frame:</b> ${frameDesc}<br/>
<#else>
  	<b>Frame:</b> Unknown<br/>
</#if>
  	<b>Power Plant: </b>${engineDesc}<br/>
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
	<tr><td>Engine</td><td align="center">${engineName}</td><td align="center">${engineMass}</td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Safe Thrust:</td><td align="center">${safeThrust}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Max Thrust:</td><td align="center">${maxThrust}</td><td></td></tr>
	<tr><td>Structural Integrity:</td><td align="center">${si}</td><td></td></tr>
	<tr><td>Heat Sinks:</td><td align="center">${hsCount}</td><td align="center">${hsMass}</td></tr>
	<tr><td>Fuel:</td><td align="center">${fuelPoints}</td><td align="center">${fuelMass}</td></tr>
	<tr><td>${cockpitType}:</td><td></td><td align="center">${cockpitMass}</td></tr>
	<tr><td>Armor Factor${armorType}:</td><td align="center">${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table width="80%">
		<tr><th></th><th>Armor<br/>Value</th></tr>
		<tr>
			<td>Nose</td>
			<td align="center">${armorValues.NOS}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.NOS}</td></#if>
		</tr>
		<tr>
			<td>Wings</td>
			<td align="center">${armorValues.RWG}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.RWG}</td></#if>
		</tr>
		<tr>
			<td>Aft</td>
			<td align="center">${armorValues.AFT}</td>
			<#if patchworkByLoc??><td>${patchworkByLoc.AFT}</td></#if>
		</tr>
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
		<tr><th align="left">Weapons<br/>and Ammo</th><th>Location</th><th>Tonnage</th></tr>
		<#list equipment as eq>
			<tr><td>${eq.name}</td><td align="center">${eq.location}</td><td align="center">${eq.tonnage}</td></tr>
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
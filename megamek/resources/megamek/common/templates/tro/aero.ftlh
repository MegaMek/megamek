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
	<b>Technology Base:</b> ${techBase}<br/>
	<b>Tonnage:</b> ${tonnage}<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
	</p>
	<br/>
	<table>
	<tr><th align="left">Equipment</th><th/><th>Mass</th></tr>
	<#if !isFighterSquadron>
	<tr><td>Engine</td><td align="center">${engineName!'(None)'}</td><td align="center">${engineMass!''}</td></tr>
	</#if>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Safe Thrust:</td><td align="center">${safeThrust}</td><td></td></tr>
	<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Max Thrust:</td><td align="center">${maxThrust}</td><td></td></tr>
	<tr><td>Structural Integrity:</td><td align="center">${si}</td><td></td></tr>
	<#if isVSTOL && isConventional && !isSupportVehicle>
	<tr><td>VSTOL Equipment:</td><td align="center"></td><td align="center">${vstolMass}</td></tr>
	</#if>
	<#if !isFighterSquadron>
	<tr><td>Heat Sinks:</td><td align="center">${hsCount}</td><td align="center">${hsMass}</td></tr>
	</#if>
	<tr><td>Fuel:</td><td align="center">${fuelPoints}</td><td align="center">${fuelMass}</td></tr>
	<tr><td>${cockpitType}:</td><td></td><td align="center">${cockpitMass}</td></tr>
	<tr><td>Armor Factor${armorType}:</td><td align="center">${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table>
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
<#if isOmni && fixedTonnage gt 0>
    <br/>
    <table>
        <tr><th align="left">Fixed Equipment</th><th>Location</th><th>Tonnage</th>
        <#list fixedEquipment as row>
            <#if row.equipment != "None">
        <tr><td>${row.equipment}</td><td align="center">${row.location}</td><td align="center">${row.tonnage}</td></tr>
            </#if>
        </#list>
    </table>
</#if>
	<br/>
	<table>
		<tr><th align="left">Weapons<br/>and Ammo</th><th>Location</th><th>Tonnage</th><th>Heat</th><th>SRV</th><th>MRV</th><th>LRV</th><th>ERV</th></tr>
		<#list equipment as eq>
			<tr><td>${eq.name}</td><td align="center">${eq.location}</td><td align="center">${eq.tonnage}</td><td>${eq.heat}</td><td>${eq.srv}</td><td>${eq.mrv}</td><td>${eq.lrv}</td><td>${eq.erv}</td></tr>
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

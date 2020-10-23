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
	
	<table>
	<tr><th>Equipment</th><th/><th>Mass</th></tr>
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
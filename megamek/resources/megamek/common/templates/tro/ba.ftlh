<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>

	<p>
	<b>Tech Base:</b> ${techBase}<br/> 
	<b>Chassis Type:</b> ${chassisType}<br/>
	<b>Weight Class:</b> ${weightClass}<br/>
	<b>Maximum Weight:</b> ${weight} kg<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
	<b>Swarm/Leg Attack/Mechanized/AP:</b> ${swarmAttack}/${legAttack}/${mechanized}/${antiPersonnel}<br/>
	<#if legAttack == "Yes" && umuMP??>
	<b>Note:</b> Leg attacks can only be made in water of depth 1+<br/>
	</#if>
	</p>
	
	<table>
		<tr><th>Equipment</th><th></th><th>Slots</th><th>Mass</th></tr>
		<tr><td>Chassis:</td><td></td><td></td><td align="right">${massChassis} kg</td></tr>	
		<tr><td>Motive System:</td><td></td><td></td><td></td></tr>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;Ground MP:</td><td>${groundMP}</td><td></td><td align="right">${groundMass} kg</td></tr>
		<#if jumpMP??>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;Jump MP:</td><td>${jumpMP}</td><td></td><td align="right">${jumpMass} kg</td></tr>
		</#if>
		<#if umuMP??>	
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;UMU MP:</td><td>${umuMP}</td><td></td><td align="right">${umuMass} kg</td></tr>	
		</#if>	
		<#if vtolMP??>	
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;VTOL MP:</td><td>${vtolMP}</td><td></td><td align="right">${vtolMass} kg</td></tr>	
		</#if>
		<tr><td>Manipulators:</td><td></td><td></td><td></td></tr>
		<#list manipulators as arm>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;${arm.locName}:</td><td>${arm.eqName}</td><td></td><td align="right">${arm.eqMass} kg</td></tr>
		</#list>
		<tr><td>Armor:</td><td>${armorType}</td><td align="center">${armorSlots}</td><td align="right">${armorMass} kg</td></tr>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;Armor Value:</td><td>${armorValue} + ${internal} (Trooper)</td><td></td><td></td></tr>
	</table>
		
	<table>
		<tr><th>Weapons and Equipment</th><th>Location</th><th>Slots (Capacity)</th><th>Mass</th></tr>
		<#list equipment as row>
		<tr><td>${row.name}</td><td align="center">${row.location}</td><td align="center">${row.slots}</td><td align="right">${row.mass} kg</td></tr>
		</#list>
		<#if modularMount??>
		<tr><td>${modularMount.name}</td><td align="center">${modularMount.location}</td><td align="center">${modularMount.slots}</td><td align="right">${modularMount.mass} kg</td></tr>
		<#list modularEquipment as row>
		<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${row.name}</td><td align="center">-</td><td align="center">${row.slots}</td><td align="right">${row.mass} kg</td></tr>
		</#list>
		</#if>
	</table>
</div>
</body>
</html>
<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <h1>${fullName}</h1>
	<p>
	Type: ${chassis}<br/>
	Technology Base: ${techBase}<br/> 
	Tonnage: ${tonnage}<br/>
	Battle Value: ${battleValue}<br/>
	</p>
	
	<table>
	<tr><th>Equipment</th><th/><th>Mass</th></tr>
	<tr><td>Internal Structure</td align="center"><td align="center">${structureName}</td><td align="center">${isMass}</td></tr>
	<#if lamConversionMass??>
	<tr><td>LAM Conversion Equipment</td align="center"><td></td><td align="center">${lamConversionMass}</td></tr>
	<#elseif qvConversionMass??>
	<tr><td>QuadVee Conversion Equipment</td align="center"><td></td><td align="center">${qvConversionMass}</td></tr>
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
	<tr><td>Armor Factor:</td><td>${armorFactor}</td><td align="center">${armorMass}</td></tr>
	</table>

	<table width="80%">
		<tr><td></td><td>Internal<br/>Structure</td><td>Armor<br/>Value</td></tr>
		<tr><td>Head</td><td align="center">${structureValues.HD}</td><td align="center">${armorValues.HD}</td></tr>
		<tr><td>Center Torso</td><td align="center">${structureValues.CT}</td><td align="center">${armorValues.CT}</td></tr>
		<tr><td>Center Torso (rear)</td><td></td><td align="center">${rearArmorValues.CT}</td></tr>
		
		<tr><td>R/L Torso</td>
			<td align="center">${structureValues.RT}
				<#if structureValues.LT != structureValues.RT>
					/${structureValues.LT}
				</#if></td>
			<td align="center">${armorValues.RT}
				<#if armorValues.LT != armorValues.RT>
					/${armorValues.LT}
				</#if></td></td></tr>
		<tr><td>R/L Torso (rear)</td><td></td>
			<td align="center">${rearArmorValues.RT}
				<#if rearArmorValues.LT != rearArmorValues.RT>
					/${rearArmorValues.LT}
				</#if></td></td></tr>
				
		<#if isQuad>
			<tr><td>FR/L Leg</td>
				<td align="center">${structureValues.FRL}
					<#if structureValues.FLL != structureValues.FRL>
						/${structureValues.FLL}
					</#if></td>
				<td align="center">${armorValues.FRL}
					<#if armorValues.FLL != armorValues.FRL>
						/${armorValues.FLL}
					</#if></td></td></tr>
		<#else>
			<tr><td>R/L Arm</td>
				<td align="center">${structureValues.RA}
					<#if structureValues.LA != structureValues.RA>
						/${structureValues.LA}
					</#if></td>
				<td align="center">${armorValues.RA}
					<#if armorValues.LA != armorValues.RA>
						/${armorValues.LA}
					</#if></td></td></tr>
		</#if>
		<#if isQuad>
			<tr><td>RR/L Leg</td>
				<td align="center">${structureValues.RRL}
					<#if structureValues.RLL != structureValues.RRL>
						/${structureValues.RLL}
					</#if></td>
				<td align="center">${armorValues.RRL}
					<#if armorValues.RLL != armorValues.RRL>
						/${armorValues.RLL}
					</#if></td></td></tr>
		<#elseif isTripod>
			<tr><td>R/C/L Leg</td>
				<td align="center">${structureValues.RL}
					<#if structureValues.LL != structureValues.RL || structureValues.CL != structureValues.RL>
						/${structureValues.CL}/${structureValues.LL}
					</#if></td>
				<td align="center">${armorValues.RL}
					<#if armorValues.LL != armorValues.RL || armorValues.CL != armorValues.RL>
						/${armorValues.CL}/${armorValues.LL}
					</#if></td></td></tr>
		<#else>
			<tr><td>R/L Leg</td>
				<td align="center">${structureValues.RL}
					<#if structureValues.LL != structureValues.RL>
						/${structureValues.LL}
					</#if></td>
				<td align="center">${armorValues.RL}
					<#if armorValues.LL != armorValues.RL>
						/${armorValues.LL}
					</#if></td></td></tr>
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
</body>
</html>
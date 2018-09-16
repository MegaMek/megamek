<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>
  	<p>
  	<b>Type: </b>${typeDesc}<br/>
  	<b>Mass: </b>${massDesc} tons<br/>
<#if use??>  	
  	<b>Use:</b> ${use}<br/>
</#if>
	<b>Technology Base:</b> ${techBase}<br/> 
  	<b>Introduced:</b> ${year}<br/>
	<b>Mass:</b> ${tonnage}<br/>
	<b>Battle Value:</b> ${battleValue}<br/>
  	<b>Tech Rating/Availability:</b> ${techRating}<br/>
  	<b>Cost:</b> ${cost} C-bills<br/>
	</p>
<#if dimensions??>
	<p>
	<b>Dimensions</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Length:</b> ${dimensions.length}<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Width:</b> ${dimensions.width}<br/>
	</p>
</#if>	
	<p>
	<b>Fuel:</b> ${fuelMass} tons (${fuelPoints})<br/>
	<b>Safe Thrust:</b> ${safeThrust}<br/>
	<b>Maximum Thrust:</b> ${maxThrust}<br/>
	<b>Heat Sinks:</b> ${hsCount}<br/>
	<b>Structural Integrity:</b> ${si}<br/>
	</p>
	<p>
	<b>Armor</b><br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Nose:</b> ${armorValues.NOS}<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Sides:</b> ${armorValues.RS}<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>Aft:</b> ${armorValues.AFT}<br/>
	</p>
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
	<b>Ammunition:</b>
<#list ammo as row>
	${row.shots} rounds of ${row.name} ammunition (${row.tonnage} tons)<#if row?has_next>, </#if>
<#else>
	None
</#list>		
	<br/>
	</p>
	<p>
	<b>Crew:</b> <#if crew?size gt 0> ${crew?join(", ")}<#else>None</#if>		
	<br/>
	</p>
	<p>
	<b>Notes: </p> Mounts ${armorMass} tons of ${armorType} armor.<br/>
	</p>

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
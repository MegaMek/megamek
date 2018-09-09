${fullName}

Mass: ${massDesc}
Chassis: ${chassisDesc}
Power Plant: ${engineDesc}
Cruising Speed: ${cruisingSpeed} kph
Maximum Speed: ${maxSpeed} kph
Jump Jets: ${jjDesc}
     Jump Capacity: ${jumpCapacity} meters
Armor: ${armorDesc}
Armament:
<#list armamentList as armament>
     ${armament}
</#list>
<#if manufacturerDesc??>Manufacturer: ${manufacturerDesc}</#if>
<#if factoryDesc??>Primary Factory: ${factoryDesc}</#if>
<#if communicationDesc??>Communication System: ${communicationDesc}</#if>
<#if targetingDesc??>Targeting & Tracking System: ${targetingDesc}</#if>
Introduction Year: ${year}
Tech Rating/Availability: ${techRating}
Cost: ${cost} C-bills
	
<#if fluffOverview??>
	Overview
	${fluffOverview}
	
</#if>
<#if fluffCapabilities??>
	Capabilities
	${fluffCapabilities}
	
</#if>
<#if fluffDeployment??>
	Deployment
	${fluffDeployment}
	
</#if>
<#if fluffHistory??>
	History
	${fluffHistory}
	
</#if>
Type: ${chassis}
Technology Base: ${techBase} 
Tonnage: ${tonnage}
Battle Value: ${battleValue}

${formatBasicDataRow("Equipment", "", "Mass")}
${formatBasicDataRow("Internal Structure", structureName, isMass)}
<#if lamConversionMass??>
${formatBasicDataRow("LAM Conversion Equipment", "", lamConversionMass)}
<#elseif qvConversionMass??>
${formatBasicDataRow("QuadVee Conversion Equipment", "", qvConversionMass)}
</#if>
${formatBasicDataRow("Engine", engineName, engineMass)}
	Walking MP: ${walkMP}
	Running MP: ${runMP}
	Jumping MP: ${jumpMP}
<#if airmechCruise??>
	AirMech Cruising MP: ${airmechCruise}
	AirMech Flanking MP: ${airmechFlank}
	Safe Thrust: ${safeThrust}
	Max Thrust: ${maxThrust}
<#elseif qvType??>
	${qvType} Cruising MP: ${qvCruise}
	${qvType} Flanking MP: ${qvFlank}
</#if>
${formatBasicDataRow(hsType, hsCount, hsMass)}
${formatBasicDataRow(gyroType, "", gyroMass)}
${formatBasicDataRow(cockpitType, "", cockpitMass)}
${formatBasicDataRow("Armor Factor" + armorType, armorFactor, armorMass)}

		                     InternalStructure        ArmorValue
		Head${structureValues.HD}${armorValues.HD}
		Center Torso${structureValues.CT}${armorValues.CT}
		Center Torso (rear)${rearArmorValues.CT}
		
		R/L Torso
			${structureValues.RT}
				<#if structureValues.LT != structureValues.RT>
					/${structureValues.LT}
				</#if>
			${armorValues.RT}
				<#if armorValues.LT != armorValues.RT>
					/${armorValues.LT}
				</#if>
		R/L Torso (rear)
			${rearArmorValues.RT}
				<#if rearArmorValues.LT != rearArmorValues.RT>
					/${rearArmorValues.LT}
				</#if>
				
		<#if isQuad>
			FR/L Leg
				${structureValues.FRL}
					<#if structureValues.FLL != structureValues.FRL>
						/${structureValues.FLL}
					</#if>
				${armorValues.FRL}
					<#if armorValues.FLL != armorValues.FRL>
						/${armorValues.FLL}
					</#if>
		<#else>
			R/L Arm
				${structureValues.RA}
					<#if structureValues.LA != structureValues.RA>
						/${structureValues.LA}
					</#if>
				${armorValues.RA}
					<#if armorValues.LA != armorValues.RA>
						/${armorValues.LA}
					</#if>
		</#if>
		<#if isQuad>
			RR/L Leg
				${structureValues.RRL}
					<#if structureValues.RLL != structureValues.RRL>
						/${structureValues.RLL}
					</#if>
				${armorValues.RRL}
					<#if armorValues.RLL != armorValues.RRL>
						/${armorValues.RLL}
					</#if>
		<#elseif isTripod>
			R/C/L Leg
				${structureValues.RL}
					<#if structureValues.LL != structureValues.RL || structureValues.CL != structureValues.RL>
						/${structureValues.CL}/${structureValues.LL}
					</#if>
				${armorValues.RL}
					<#if armorValues.LL != armorValues.RL || armorValues.CL != armorValues.RL>
						/${armorValues.CL}/${armorValues.LL}
					</#if>
		<#else>
			R/L Leg
				${structureValues.RL}
					<#if structureValues.LL != structureValues.RL>
						/${structureValues.LL}
					</#if>
				${armorValues.RL}
					<#if armorValues.LL != armorValues.RL>
						/${armorValues.LL}
					</#if>
		</#if>
	
	
	<#if isOmni>
	Weight and Space Allocation
	
	<i>Location</i><i>Fixed</i><i>Space Remaining</i>
	<#list fixedEquipment as row>
	${row.location}${row.equipment}${row.remaining}
	</#list>
	
	</#if>
	
	
		<th align="left">Weaponsand AmmoLocationCriticalTonnage
		<#list equipment as eq>
			${eq.name}${eq.location}${eq.slots}${eq.tonnage}
		</#list>
	
	
	<#if quirks??>
		
		Features the following design quirks: ${quirks}
		
	</#if>
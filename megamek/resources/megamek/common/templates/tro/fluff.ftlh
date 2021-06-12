<p>
<b>Mass: </b>${massDesc}<br/>
<#if chassisDesc??><b>Chassis: </b>${chassisDesc}<br/></#if>
<#if moveType??><b>Movement Type: </b>${moveType}<br/></#if>
<#if frameDesc??><b>Chassis: </b>${frameDesc}<br/></#if>
<b>Power Plant: </b>${engineDesc}<br/>
<#if cruisingSpeed??><b>Cruising Speed: </b>${cruisingSpeed} kph<br/></#if>
<#if cruisingSpeed??><b>Maximum Speed: </b>${maxSpeed} kph<br/></#if>
<#if jumpDesc??>
<b>Jump Jets: </b>${jjDesc}<br/>
<b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Jump Capacity: </b>${jumpCapacity} meters<br/>
</#if>
<b>Armor: </b>${armorDesc}</b><br/>
<b>Armament:</b><br/>
    <#list armamentList as armament>
  		&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${armament}<br/>
	</#list>
<#if manufacturerDesc??><b>Manufacturer:</b> ${manufacturerDesc}<br/></#if>
<#if factoryDesc??><b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Primary Factory: </b>${factoryDesc}<br/></#if>
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
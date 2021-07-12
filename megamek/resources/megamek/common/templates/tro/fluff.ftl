Mass: ${massDesc}
<#if chassisDesc??>
Chassis: ${chassisDesc}
</#if>
<#if moveType??>
Movement Type: ${moveType}
</#if>
<#if frameDesc??>
Frame: ${frameDesc}
</#if>
Power Plant: ${engineDesc}
<#if cruisingSpeed??>
Cruising Speed: ${cruisingSpeed} kph
</#if>
<#if maxSpeed??>
Maximum Speed: ${maxSpeed} kph
</#if>
<#if jumpMP?? && jjDesc??>
Jump Jets: ${jjDesc}
     Jump Capacity: ${jumpCapacity} meters
</#if>
Armor: ${armorDesc}
Armament:
<#list armamentList as armament>
     ${armament}
</#list>
Manufacturer: <#if manufacturerDesc??>${manufacturerDesc}<#else>Unknown</#if>
     Primary Factory: <#if factoryDesc??>${factoryDesc}<#else>Unknown</#if>
Communication System: <#if communicationDesc??>${communicationDesc}<#else>Unknown</#if>
Targeting & Tracking System: <#if targetingDesc??>${targetingDesc}<#else>Unknown</#if>
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

${fullName}
<#if includeFluff>
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
</#if>

Asset Type: ${assetType}
Tech Base: ${techBase}
Tech Rating/Availability: ${techRating}
Introduction Year: ${year}
Source: ${source}
Crew Grade: ${crewGrade}
BSP Cost: ${bsp}
BFS BV: ${bfsBv}

Movement: ${movement}
TMM: ${tmm}
Range: ${range}
Skill: ${skill}
Damage: ${damage}
Destroy Check: <#if damaged>${originalDestroyCheck} -> </#if>${destroyCheck}
Threshold: ${threshold}
Specials: ${specials}

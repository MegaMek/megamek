<html>
<head>
  <title>${fullName}</title>
</head>
<body>
  <div style="font:12pt monospaced">
  <font size="+1"><b>${fullName}</b></font>
<#if includeFluff>
  <#noautoesc>
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
  </#noautoesc>
</#if>

  <p>
  <b>Asset Type:</b> ${assetType}<br/>
  <b>Tech Base:</b> ${techBase}<br/>
  <b>Tech Rating/Availability:</b> ${techRating}<br/>
  <b>Introduction Year:</b> ${year}<br/>
  <b>Source:</b> ${source}<br/>
  <b>Crew Grade:</b> ${crewGrade}<br/>
  <b>BSP Cost:</b> ${bsp}<br/>
  <b>BFS BV:</b> ${bfsBv}<br/>
  </p>

  <table>
  <tr><th align="left">Characteristic</th><th>Value</th></tr>
  <tr><td>Movement:</td><td align="center">${movement}</td></tr>
  <tr><td>TMM:</td><td align="center">${tmm}</td></tr>
  <tr><td>Range:</td><td align="center">${range}</td></tr>
  <tr><td>Skill:</td><td align="center">${skill}</td></tr>
  <tr><td>Damage:</td><td align="center">${damage}</td></tr>
  <tr><td>Destroy Check:</td><td align="center"><#if damaged>${originalDestroyCheck} -&gt; </#if>${destroyCheck}</td></tr>
  <tr><td>Threshold:</td><td align="center">${threshold}</td></tr>
  <tr><td>Specials:</td><td align="center">${specials}</td></tr>
  </table>
  </div>
</body>
</html>

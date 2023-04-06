<#macro dash dmg><#if dmg == "0">-<#else>${dmg}</#if></#macro>

<#macro arcText arc><@arcDmgText arc.STD/>, CAP<@arcDmgText arc.CAP/>, SCAP<@arcDmgText arc.SCAP/>, MSL<@arcDmgText arc.MSL/><#if (arc.specials?length > 0)>, ${arc.specials}</#if></#macro>

<#macro arcDmgText arcDmg><@dash arcDmg.dmgS/>/<@dash arcDmg.dmgM/>/<@dash arcDmg.dmgL/>/<@dash arcDmg.dmgE/></#macro>
${chassis} ${model}
<#assign n = chassis?length + model?length + 1>
<#list 0..<n as i>-</#list>
Point Value (PV): ${PV}
TP: ${TP},  SZ: ${SZ}<#if !usesE>,  TMM: ${TMM}</#if>,  <#if usesTh>THR<#else>MV</#if>: ${MV}
<#if !usesArcs>
Damage: (S) ${dmg.dmgS} / (M) ${dmg.dmgM} / (L) ${dmg.dmgL}<#if usesE> / (E) ${dmg.dmgE}</#if><#if usesOV>,  OV: ${OV}</#if>
</#if>
Armor (A): ${Arm}<#if usesTh>, Threshold (TH): ${Th}</#if>, Structure (S): ${Str}
Specials: ${specials}
<#if usesArcs>
Front Arc: <@arcText frontArc/>
Left/Right Arc: <@arcText leftArc/>
Rear Arc: <@arcText rearArc/>
</#if>
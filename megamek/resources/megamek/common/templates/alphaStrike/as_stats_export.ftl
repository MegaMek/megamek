${chassis} ${model}
<#assign n = chassis?length + model?length + 1>
<#list 0..<n as i>-</#list>
Point Value (PV): ${PV}
TP: ${TP},  SZ: ${SZ}<#if !usesE>,  TMM: ${TMM}</#if>,  <#if usesTh>THR<#else>MV</#if>: ${MV}
<#if !usesArcs>
Damage: (S) ${dmgS} / (M) ${dmgM} / (L) ${dmgL}<#if usesE> / (E) ${dmgE}</#if><#if usesOV>,  OV: ${OV}</#if>
</#if>
Armor (A): ${Arm}<#if usesTh>, Threshold (TH): ${Th}</#if>, Structure (S): ${Str}
Specials: ${specials}
<#if usesArcs>
Front Arc: ${frontArc}
Left/Right Arc: ${leftArc}
Rear Arc: ${rearArc}
</#if>
<#assign ogDescription="Custom map packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Map Packs</a>
			/</span> ${game.name}
	</@heading>

	<@tline timeline=timeline game=game></@tline>

	<@content class="biglist">
		<ul>
		<#list game.groups as k, gametype>
			<li style='background-image: url("${staticPath()}/images/gametypes/${game.name}/t_${gametype.name}.png")'>
				<span class="meta">${gametype.count}</span>
				<a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
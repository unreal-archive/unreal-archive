<#assign ogDescription="Custom map packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<a href="${relPath(sectionPath + "/index.html")}">Map Packs</a>
		/ ${game.name}
	</@heading>

	<@content class="biglist">
		<ul>
		<#list game.gametypes as k, gametype>
			<li style='background-image: url("${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png")'>
				<span class="meta">${gametype.packs}</span>
				<a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
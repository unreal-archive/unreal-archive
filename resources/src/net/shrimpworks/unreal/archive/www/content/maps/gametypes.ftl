<#assign ogDescription="Custom maps for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath()}/images/games/${game.name}.png"]>
		<a href="${relPath(sectionPath + "/index.html")}">Maps</a>
		/ ${game.name}
	</@heading>

	<@content class="biglist">
		<ul>
		<#list game.gametypes as k, gametype>
			<li style='background-image: url("${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png")'>
				<span class="meta">${gametype.maps}</span>
				<a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
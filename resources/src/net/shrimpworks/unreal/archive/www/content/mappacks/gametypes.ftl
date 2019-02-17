<#assign ogDescription="Custom map packs for ${game.name}">
<#assign ogImage="${staticPath(static)}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
		<a href="${relUrl(siteRoot, "index.html")}">Map Packs</a>
		/ ${game.name}
	</@heading>

	<@content class="biglist">
		<ul>
		<#list game.gametypes as k, v>
			<li style='background-image: url("${staticPath(static)}/images/gametypes/${game.name}/${v.name}.png")'>
				<span class="meta">${v.packs}</span>
				<a href="${relUrl(game.path, v.path + "/index.html")}">${v.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
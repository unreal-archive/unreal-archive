<#include "../../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=["${static}/images/games/${game.name}.png"]>
		<a href="${relUrl(siteRoot, "index.html")}">Map Packs</a>
		/ ${game.name}
	</@heading>

	<@content class="biglist">
		<ul>
		<#list game.gametypes as k, v>
			<li style='background-image: url("${static}/images/gametypes/${game.name}/${v.name}.png")'>
				<span class="meta">${v.packs}</span>
				<a href="${relUrl(game.path, v.path + "/index.html")}">${v.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
<#assign ogDescription="Custom map packs for Unreal, Unreal Tournament, and Unreal Tournament 2004 and their mods">
<#assign ogImage="${staticPath(static)}/images/contents/mappacks.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/contents/mappacks.png"]>
		Map Packs
	</@heading>

	<@content class="biglist">
		<ul>
		<#list games.games as k, game>
			<li style='background-image: url("${staticPath(static)}/images/games/${game.name}.png")'>
				<span class="meta">${game.packs}</span>
				<a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
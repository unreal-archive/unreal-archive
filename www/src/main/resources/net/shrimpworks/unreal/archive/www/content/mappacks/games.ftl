<#assign ogDescription="Custom map packs for Unreal, Unreal Tournament, and Unreal Tournament 2004 and their mods">
<#assign ogImage="${staticPath()}/images/contents/mappacks.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		Map Packs
	</@heading>

	<@content class="biglist">
		<ul>
		<#list games.games as k, game>
			<li style='background-image: url("${staticPath()}/images/games/t_${game.name}.png")'>
				<span class="meta">${game.count}</span>
				<a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
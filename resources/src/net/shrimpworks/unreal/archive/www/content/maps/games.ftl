<#assign ogDescription="Custom maps for Unreal, Unreal Tournament, and Unreal Tournament 2004 and their mods">
<#assign ogImage="${staticPath()}/images/contents/maps.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		Maps
	</@heading>

	<@content class="biglist">
		<ul>
		<#list games.games as k, game>
			<li style='background-image: url("${staticPath()}/images/games/${game.name}.png")'>
				<span class="meta">${game.maps}</span>
				<a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
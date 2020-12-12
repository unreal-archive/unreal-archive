<#assign ogDescription="GameTypes and mods for Unreal, Unreal Tournament, and Unreal Tournament 2004">
<#assign ogImage="${staticPath()}/images/contents/mappacks.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		Game Types &amp; Mods
	</@heading>

	<@content class="biglist">
		<ul>
		<#list games as k, game>
			<li style='background-image: url("${staticPath()}/images/games/${game.name}.png")'>
				<span class="meta">${game.gametypes?size}</span>
				<a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
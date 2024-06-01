<#assign ogDescription="Custom maps for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/</span> Maps
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
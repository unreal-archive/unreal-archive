<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading bg=["${staticPath()}/images/games/All.png"]>Contents</@heading>

<@content class="biglist bigger">
	<ul>
	<#list games as k, game>
		<li style='background-image: url("${staticPath()}/images/games/t_${game.name}.png")'>
			<span class="meta">${game.gametypes?size}</span>
			<a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		</li>
	</#list>
	</ul>
</@content>

<#include "../_footer.ftl">
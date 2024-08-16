<#assign ogDescription="Custom map packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/</span> Map Packs
	</@heading>

	<@tline timeline=timeline game=game></@tline>

	<@content class="biglist">
		<ul>
		<#list game.groups as k, gametype>
      <@bigitem link="${relPath(gametype.path + '/index.html')}" meta="${gametype.count}" bg="${staticPath()}/images/gametypes/${game.name}/t_${gametype.name}.png">${gametype.name}</@bigitem>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
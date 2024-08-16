<#assign ogDescription="Custom gametypes and mods for ${game.name}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/</span>Game Types &amp; Mods
	</@heading>

	<@content class="biglist bigger">
		<ul>
		<#list game.gametypes as gametype>
			<#outputformat "plainText"><#assign pic>
				<#if gametype.gametype.titleImage?? && gametype.gametype.titleImage?length gt 0>
					${relPath(gametype.path + "/" + gametype.gametype.titleImage)}
				<#elseif gametype.gametype.bannerImage?? && gametype.gametype.bannerImage?length gt 0>
					${relPath(gametype.path + "/" + gametype.gametype.bannerImage)}
				<#elseif gametype.fallbackTitle?? && gametype.fallbackTitle?length gt 0>
					${gametype.fallbackTitle}
				</#if>
			</#assign></#outputformat>
			<@bigitem link="${relPath(gametype.path + '/index.html')}" meta="" bg="${pic?trim}">${gametype.gametype.name}</@bigitem>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
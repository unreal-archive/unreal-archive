<#assign ogDescription="Custom gametypes and mods for ${game.name}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<a href="${relPath(sectionPath + "/index.html")}">Game Types &amp; Mods</a>
		/ ${game.name}
	</@heading>

	<@content class="biglist">
		<ul>
		<#list game.gametypes as gametype>
			<li
				<#if gametype.gametype.titleImage?? && gametype.gametype.titleImage?length gt 0>
					style='background-image: url("${relPath(gametype.path + "/" + gametype.gametype.titleImage)}"); box-shadow: none'
				</#if>
			>
				<a href="${relPath(gametype.path + "/index.html")}" title="${gametype.gametype.name}">
				  <#if gametype.gametype.titleImage?? && gametype.gametype.titleImage?length gt 0>&nbsp;<#else>${gametype.gametype.name}</#if>
				</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
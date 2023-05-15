<#assign ogDescription="Custom player skins for ${game.game.bigName} released in ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Skins</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/</span> ${year?c}
	</@heading>


	<@tline timeline=timeline game=game activeYear=year></@tline>

	<@content class="biglist">
		<ul>
			<#list months as m, count>
				<li <#if count == 0>class="disabled"</#if>>
					<span class="meta">${count}</span>
					<#if count gt 0>
						<a href="${relPath(game.path + "/releases/${year?c}/${m}/index.html")}">${monthNames[m-1]}</a>
					<#else>
						${monthNames[m-1]}
					</#if>
				</li>
			</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
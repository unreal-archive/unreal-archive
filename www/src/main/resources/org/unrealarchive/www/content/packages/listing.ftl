<#assign game=page.letter.game>

<#assign ogDescription="All files for ${game.game.bigName}">
<#assign ogImage="${staticPath(static)}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
			<span class="crumbs"><a href="${siteRoot}/index.html">Package Browser</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/</span> ${page.letter.letter}
	</@heading>

	<@content class="list">

		<nav class="letters">
			<#list game.letters as k, letter><a href="${relUrl(root, letter.path + "/index.html")}"<#if letter.letter == page.letter.letter>class="active"</#if>>${letter.letter}</a></#list>
		</nav>

		<#if page.letter.pages?size gt 1>
			<nav class="pages">
				<#list page.letter.pages as pg><a href="${relUrl(root, pg.path + "/index.html")}" <#if pg.number == page.number>class="active"</#if>>${pg.number}</a></#list>
			</nav>
		</#if>

		<table class="skins">
			<thead>
			<tr>
				<th>Package</th>
				<th>Variations</th>
				<th>In Packages</th>
			</tr>
			</thead>
			<tbody>
				<#list page.files as f>
				<tr>
					<td nowrap="nowrap"><a href="${relUrl(root, f.path + ".html")}">${f.name}</a></td>
					<td>${f.variations}</td>
					<td>${f.packages}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
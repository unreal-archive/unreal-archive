<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=page.letter.game>

	<@heading bg=["${static}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Models</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		/ ${page.letter.letter}
		/ pg ${page.number}
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

		<table class="models">
			<thead>
			<tr>
				<th>Model</th>
				<th>Author</th>
				<th>Info</th>
				<th> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.models as m>
				<tr class="${m?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, m.path + ".html")}">${m.model.name}</a></td>
					<td>${m.model.author}</td>
					<td>
						<#if m.model.models?size gt 0>
							${m.model.models?size} character<#if m.model.models?size gt 1>s</#if>
							<#if m.model.skins?size gt 0>,</#if>
						</#if>
						<#if m.model.skins?size gt 0>
							${m.model.skins?size} skin<#if m.model.skins?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta">
						<#if m.model.attachments?size gt 0>
							<img src="${static!"static"}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
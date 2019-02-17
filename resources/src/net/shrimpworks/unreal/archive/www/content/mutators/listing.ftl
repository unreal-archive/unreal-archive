<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=page.letter.game>

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
			<a href="${siteRoot}/index.html">Mutators</a>
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

		<table class="mutators">
			<thead>
			<tr>
				<th>Mutator</th>
				<th>Author</th>
				<th>Info</th>
				<th> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.mutators as m>
				<tr class="${m?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, m.path + ".html")}">${m.mutator.name}</a></td>
					<td>${m.mutator.author}</td>
					<td>
						<#if m.mutator.mutators?size gt 0>
							${m.mutator.mutators?size} mutator<#if m.mutator.mutators?size gt 1>s</#if>
							<#if m.mutator.weapons?size gt 0 || m.mutator.vehicles?size gt 0>,</#if>
						</#if>
						<#if m.mutator.weapons?size gt 0>
							${m.mutator.weapons?size} weapons<#if m.mutator.weapons?size gt 1>s</#if>
							<#if m.mutator.vehicles?size gt 0>,</#if>
						</#if>
						<#if m.mutator.vehicles?size gt 0>
							${m.mutator.vehicles?size} vehicle<#if m.mutator.vehicles?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta">
						<#if m.mutator.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
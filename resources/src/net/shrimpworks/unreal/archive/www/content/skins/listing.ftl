<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=page.letter.game>

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
			<a href="${siteRoot}/index.html">Skins</a>
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

		<table class="skins">
			<thead>
			<tr>
				<th>Skin</th>
				<th>Author</th>
				<th>Info</th>
				<th> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.skins as s>
				<tr class="${s?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, s.path + ".html")}">${s.skin.name}</a></td>
					<td>${s.skin.author}</td>
					<td>
						<#if s.skin.skins?size gt 0>
							${s.skin.skins?size} skin<#if s.skin.skins?size gt 1>s</#if>
							<#if s.skin.faces?size gt 0>,</#if>
						</#if>
						<#if s.skin.faces?size gt 0>
							${s.skin.faces?size} face<#if s.skin.faces?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta">
						<#if s.skin.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
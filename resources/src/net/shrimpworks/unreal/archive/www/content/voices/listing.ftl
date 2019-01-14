<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=page.letter.game>

	<@heading bg=["${static}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Voices</a>
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

		<table class="voices">
			<thead>
			<tr>
				<th>Voice</th>
				<th>Author</th>
				<th>Info</th>
			</tr>
			</thead>
			<tbody>
				<#list page.voices as v>
				<tr class="${v?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, v.path + ".html")}">${v.voice.name}</a></td>
					<td>${v.voice.author}</td>
					<td>
						<#if v.voice.voices?size gt 0>
							${v.voice.voices?size} voice<#if v.voice.voices?size gt 1>s</#if>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
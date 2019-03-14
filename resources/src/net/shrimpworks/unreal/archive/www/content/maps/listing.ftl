<#assign game=page.letter.gametype.game>
<#assign gametype=page.letter.gametype>

<#assign ogDescription="${gametype.name} maps for ${game.game.bigName}">
<#assign ogImage="${staticPath(static)}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/gametypes/${game.name}/${gametype.name}.png", "${staticPath(static)}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Maps</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		/ <a href="${relUrl(siteRoot, gametype.path)}/index.html">${gametype.name}</a>
		/ ${page.letter.letter}
		/ pg ${page.number}
	</@heading>

	<@content class="list">

		<nav class="letters">
			<#list gametype.letters as k, letter><a href="${relUrl(root, letter.path + "/index.html")}"<#if letter.letter == page.letter.letter>class="active"</#if>>${letter.letter}</a></#list>
		</nav>

		<#if page.letter.pages?size gt 1>
			<nav class="pages">
				<#list page.letter.pages as pg><a href="${relUrl(root, pg.path + "/index.html")}" <#if pg.number == page.number>class="active"</#if>>${pg.number}</a></#list>
			</nav>
		</#if>

		<table class="maps">
			<thead>
			<tr>
				<th>Map</th>
				<th class="nomobile">Title</th>
				<th>Author</th>
				<th>Players</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.maps as m>
				<tr class="${m?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, m.path + ".html")}">${m.map.name}</a></td>
					<td class="nomobile">${m.map.title}</td>
					<td>${m.map.author}</td>
					<td>${m.map.playerCount}</td>
					<td class="meta nomobile">
						<#if m.map.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
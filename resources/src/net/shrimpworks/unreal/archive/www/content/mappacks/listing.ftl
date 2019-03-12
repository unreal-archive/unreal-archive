<#assign game=page.gametype.game>
<#assign gametype=page.gametype>

<#assign ogDescription="${gametype.name} map packs for ${game.name}">
<#assign ogImage="${staticPath(static)}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/gametypes/${game.name}/${gametype.name}.png", "${staticPath(static)}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Map Packs</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		/ <a href="${relUrl(siteRoot, gametype.path)}/index.html">${gametype.name}</a>
		/ pg ${page.number}
	</@heading>

	<@content class="list">

		<nav class="pages">
			<#list gametype.pages as pg><a href="${relUrl(root, pg.path + "/index.html")}" <#if pg.number == page.number>class="active"</#if>>${pg.number}</a></#list>
		</nav>

		<table class="mappacks">
			<thead>
			<tr>
				<th>Name</th>
				<th>Author</th>
				<th>Maps</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.packs as p>
				<tr class="${p?item_parity}">
					<td><a href="${relUrl(root, p.path + ".html")}">${p.pack.name}</a></td>
					<td>${p.pack.author}</td>
					<td>${p.pack.maps?size}</td>
					<td class="meta nomobile">
						<#if p.pack.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
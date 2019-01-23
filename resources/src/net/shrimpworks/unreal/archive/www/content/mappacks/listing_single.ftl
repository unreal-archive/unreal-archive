<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=gametype.game>

	<@heading bg=["${static}/images/gametypes/${game.name}/${gametype.name}.png", "${static}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Map Packs</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		/ <a href="${relUrl(siteRoot, gametype.path)}/index.html">${gametype.name}</a>
	</@heading>

	<@content class="list">
		<table class="mappacks">
			<thead>
			<tr>
				<th>Name</th>
				<th>Author</th>
				<th>Maps</th>
				<th> </th>
			</tr>
			</thead>
			<tbody>
				<#list packs as p>
				<tr class="${p?item_parity}">
					<td><a href="${relUrl(gametype.path, p.path + ".html")}">${p.pack.name}</a></td>
					<td>${p.pack.author}</td>
					<td>${p.pack.maps?size}</td>
					<td class="meta">
						<#if p.pack.attachments?size gt 0>
							<img src="${static!"static"}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
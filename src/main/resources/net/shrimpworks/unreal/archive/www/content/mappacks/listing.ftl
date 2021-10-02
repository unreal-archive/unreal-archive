<#assign game=page.gametype.game>
<#assign gametype=page.gametype>

<#assign ogDescription="${gametype.name} map packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage, "${staticPath()}/images/games/${game.name}.png"]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Map Packs</a>
			/</span> <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
		<span class="crumbs">
			<#if gametype.pages?size gt 1>/ pg ${page.number}</#if>
		</span>
	</@heading>

	<@content class="list">

		<@paginator pages=gametype.pages currentPage=page />

		<table class="mappacks">
			<thead>
			<tr>
				<th>Name</th>
				<th>Author</th>
				<th class="nomobile">Maps</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.packs as p>
				<tr>
					<td><a href="${relPath(p.path + ".html")}">${p.pack.name}</a></td>
					<td><@authorLink p.pack.authorName /></td>
					<td class="nomobile">${p.pack.maps?size}</td>
					<td class="meta nomobile">
						<#if p.pack.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
						</#if>
						<@dependencyIcon p.pack.dependencies/>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>

		<@paginator pages=gametype.pages currentPage=page />
	</@content>

<#include "../../_footer.ftl">
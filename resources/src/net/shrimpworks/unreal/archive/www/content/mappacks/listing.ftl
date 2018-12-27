<#include "../../_header.ftl">
<#include "../macros.ftl">

	<#assign game=page.gametype.game>
	<#assign gametype=page.gametype>

	<@heading bg=["${static}/images/gametypes/${game.name}/${gametype.name}.png", "${static}/images/games/${game.name}.png"]>
		<h1>
			<a href="${siteRoot}/index.html">Map Packs</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ <a href="${relUrl(siteRoot, gametype.path)}/index.html">${gametype.name}</a>
			/ pg ${page.number}
		</h1>
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
			</tr>
			</thead>
			<tbody>
				<#list page.packs as p>
				<tr>
					<td><a href="${relUrl(root, p.path + ".html")}">${p.pack.name}</a></td>
					<td>${p.pack.author}</td>
					<td>${p.pack.maps?size}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
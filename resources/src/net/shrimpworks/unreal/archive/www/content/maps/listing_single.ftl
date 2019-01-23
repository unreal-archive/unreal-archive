<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=gametype.game>

	<@heading bg=["${static}/images/gametypes/${game.name}/${gametype.name}.png", "${static}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Maps</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		/ ${gametype.name}
	</@heading>

	<@content class="list">
		<table class="maps">
			<thead>
			<tr>
				<th>Map</th>
				<th>Title</th>
				<th>Author</th>
				<th>Players</th>
				<th> </th>
			</tr>
			</thead>
			<tbody>
				<#list maps as m>
				<tr>
					<td><a href="${relUrl(gametype.path, m.path + ".html")}">${m.map.name}</a></td>
					<td>${m.map.title}</td>
					<td>${m.map.author}</td>
					<td>${m.map.playerCount}</td>
					<td class="meta">
						<#if m.map.attachments?size gt 0>
							<img src="${static!"static"}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
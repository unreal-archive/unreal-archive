<#assign game=gametype.game>

<#assign ogDescription="${gametype.name} maps for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png", "${staticPath()}/images/games/${game.name}.png"]>
		<a href="${relPath(sectionPath + "/index.html")}">Maps</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ ${gametype.name}
	</@heading>

	<@content class="list">
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
				<#list maps as m>
				<tr>
					<td><a href="${relPath(m.path + ".html")}">${m.map.name}</a></td>
					<td class="nomobile">${m.map.title}</td>
					<td>${m.map.author}</td>
					<td>${m.map.playerCount}</td>
					<td class="meta nomobile">
						<#if m.map.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
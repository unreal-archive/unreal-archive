<#assign ogDescription="Custom maps for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Maps</a>
			/ <a href="${relPath(game.path + "/releases/${year?c}/index.html")}">${year?c}</a>
			/</span> ${monthNames[month-1]}
	</@heading>

	<@tline timeline=timeline game=game activeYear=year activeMonth=month></@tline>

	<@content class="list">

		<#assign gametypes = [] />
		<#list items as i>
			<#if !gametypes?seq_contains(i.item.gametype)>
				<#assign gametypes += [i.item.gametype] />
			</#if>
		</#list>

		<#list gametypes?sort as gametype>
			<section>
				<h2>${gametype}</h2>

				<table>
					<thead>
					<tr>
						<th>Map</th>
						<th class="nomobile">Title</th>
						<th>Author</th>
						<th class="nomobile">Players</th>
						<th class="nomobile"> </th>
					</tr>
					</thead>
					<tbody>
					<#list items as m>
						<#if m.item.gametype != gametype><#continue/></#if>
						<tr>
							<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.item.name}</a></td>
							<td class="nomobile">${m.item.title}</td>
							<td><@authorLink m.item.authorName /></td>
							<td class="nomobile">${m.item.playerCount}</td>
							<td class="meta nomobile">
								<#if m.item.bots>
									<img src="${staticPath()}/images/icons/bots.svg" alt="AI/Bot support" title="AI/Bot support" height="22"/>
								</#if>
								<#if m.item.attachments?size gt 0>
									<img src="${staticPath()}/images/icons/image.svg" alt="Has images" title="Has images" height="22"/>
								</#if>
								<@dependencyIcon m.item.dependencies/>
							</td>
						</tr>
					</#list>
					</tbody>
				</table>
			</section>
		</#list>

	</@content>

<#include "../../_footer.ftl">
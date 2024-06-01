<#assign ogDescription="Custom map packs for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Map Packs</a>
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
						<th>Name</th>
						<th>Author</th>
						<th class="nomobile">Maps</th>
						<th class="nomobile"> </th>
					</tr>
					</thead>
					<tbody>
          <#list items as p>
						<#if p.item.gametype != gametype><#continue/></#if>
						<tr>
							<td><a href="${relPath(p.path + ".html")}">${p.item.name}</a></td>
							<td><@authorLink p.item.authorName /></td>
							<td class="nomobile">${p.item.maps?size}</td>
							<td class="meta nomobile">
								<#if p.item.attachments?size gt 0>
									<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
								</#if>
								<@dependencyIcon p.item.dependencies/>
							</td>
						</tr>
          </#list>
					</tbody>
				</table>
			</section>
		</#list>

	</@content>

<#include "../../_footer.ftl">
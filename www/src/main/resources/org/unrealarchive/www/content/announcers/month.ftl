<#assign ogDescription="Custom announcer packs for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Announcers</a>
			/ <a href="${relPath(game.path + "/releases/${year?c}/index.html")}">${year?c}</a>
			/</span> ${monthNames[month-1]}
	</@heading>

	<@tline timeline=timeline game=game activeYear=year activeMonth=month></@tline>

	<@content class="list">

		<section>
			<table>
				<thead>
				<tr>
					<th>Announcer</th>
					<th>Author</th>
					<th class="nomobile">Info</th>
					<th class="nomobile"> </th>
				</tr>
				</thead>
				<tbody>
				<#list items as a>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(a.path + ".html")}">${a.item.name}</a></td>
						<td><@authorLink a.item.authorName /></td>
						<td class="nomobile">
							<#if a.item.announcers?size gt 0>
								${a.item.announcers?size} voice<#if a.item.announcers?size gt 1>s</#if>
							</#if>
						</td>
						<td class="meta nomobile">
							<#if a.item.attachments?size gt 0>
								<@icon name="image" title="Has images"/>
							</#if>
							<@dependencyIcon a.item.dependencies/>
						</td>
					</tr>
				</#list>
				</tbody>
			</table>
		</section>

	</@content>

<#include "../../_footer.ftl">
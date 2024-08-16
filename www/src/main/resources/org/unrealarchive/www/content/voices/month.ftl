<#assign ogDescription="Custom voice packs for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Voices</a>
			/ <a href="${relPath(game.path + "/releases/${year?c}/index.html")}">${year?c}</a>
			/</span> ${monthNames[month-1]}
	</@heading>

	<@tline timeline=timeline game=game activeYear=year activeMonth=month></@tline>

	<@content class="list">

		<section>
			<table>
				<thead>
				<tr>
					<th>Voice</th>
					<th>Author</th>
					<th class="nomobile">Info</th>
					<th class="nomobile"> </th>
				</tr>
				</thead>
				<tbody>
				<#list items as v>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(v.path + ".html")}">${v.item.name}</a></td>
						<td><@authorLink v.item /></td>
						<td class="nomobile">
							<#if v.item.voices?size gt 0>
								${v.item.voices?size} voice<#if v.item.voices?size gt 1>s</#if>
							</#if>
						</td>
						<td class="meta nomobile">
							<#if v.item.attachments?size gt 0>
								<@icon name="image" title="Has images"/>
							</#if>
							<@dependencyIcon v.item.dependencies/>
						</td>
					</tr>
				</#list>
				</tbody>
			</table>
		</section>

	</@content>

<#include "../../_footer.ftl">
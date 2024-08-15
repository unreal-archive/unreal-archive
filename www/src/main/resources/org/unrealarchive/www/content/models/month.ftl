<#assign ogDescription="Custom player models for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Models &amp; Characters</a>
			/ <a href="${relPath(game.path + "/releases/${year?c}/index.html")}">${year?c}</a>
			/</span> ${monthNames[month-1]}
	</@heading>

	<@tline timeline=timeline game=game activeYear=year activeMonth=month></@tline>

	<@content class="list">

		<section>
			<table>
				<thead>
				<tr>
					<th>Model</th>
					<th>Author</th>
					<th class="nomobile">Info</th>
					<th class="nomobile"> </th>
				</tr>
				</thead>
				<tbody>
        <#list items as m>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.item.name}</a></td>
						<td><@authorLink m.item /></td>
						<td class="nomobile">
							<#if m.item.models?size gt 0>
								${m.item.models?size} character<#if m.item.models?size gt 1>s</#if>
								<#if m.item.skins?size gt 0>,</#if>
							</#if>
							<#if m.item.skins?size gt 0>
								${m.item.skins?size} skin<#if m.item.skins?size gt 1>s</#if>
							</#if>
						</td>
						<td class="meta nomobile">
							<#if m.item.attachments?size gt 0>
								<@icon name="image" title="Has images"/>
							</#if>
							<@dependencyIcon m.item.dependencies/>
						</td>
					</tr>
        </#list>
				</tbody>
			</table>
		</section>

	</@content>

<#include "../../_footer.ftl">
<#assign ogDescription="Custom gameplay mutators for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Mutators</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/releases/${year?c}/index.html")}">${year?c}</a>
			/</span> ${monthNames[month-1]}
	</@heading>

	<@tline timeline=timeline game=game activeYear=year activeMonth=month></@tline>

	<@content class="list">

		<section>
			<table>
				<thead>
				<tr>
					<th>Mutator</th>
					<th>Author</th>
					<th class="nomobile">Info</th>
					<th class="nomobile"> </th>
				</tr>
				</thead>
				<tbody>
        <#list items as m>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.item.name}</a></td>
						<td><@authorLink m.item.authorName /></td>
						<td class="nomobile">
							<#if m.item.mutators?size gt 0>
								${m.item.mutators?size} mutator<#if m.item.mutators?size gt 1>s</#if>
								<#if m.item.weapons?size gt 0 || m.item.vehicles?size gt 0>,</#if>
							</#if>
							<#if m.item.weapons?size gt 0>
								${m.item.weapons?size} weapon<#if m.item.weapons?size gt 1>s</#if>
								<#if m.item.vehicles?size gt 0>,</#if>
							</#if>
							<#if m.item.vehicles?size gt 0>
								${m.item.vehicles?size} vehicle<#if m.item.vehicles?size gt 1>s</#if>
							</#if>
						</td>
						<td class="meta nomobile">
							<#if m.item.attachments?size gt 0>
								<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
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
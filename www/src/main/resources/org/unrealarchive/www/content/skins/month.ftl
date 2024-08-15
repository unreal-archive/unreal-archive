<#assign ogDescription="Custom player skins for ${game.game.bigName} released in ${monthNames[month-1]} ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Skins</a>
			/ <a href="${relPath(game.path + "/releases/${year?c}/index.html")}">${year?c}</a>
			/</span> ${monthNames[month-1]}
	</@heading>

	<@tline timeline=timeline game=game activeYear=year activeMonth=month></@tline>

	<@content class="list">

		<section>
			<table class="skins">
				<thead>
				<tr>
					<th>Skin</th>
					<th>Author</th>
					<th class="nomobile">Info</th>
					<th class="nomobile"> </th>
				</tr>
				</thead>
				<tbody>
        <#list items as s>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(s.path + ".html")}">${s.item.name}</a></td>
						<td><@authorLink s.item /></td>
						<td class="nomobile">
							<#if s.item.skins?size gt 0>
								${s.item.skins?size} skin<#if s.item.skins?size gt 1>s</#if>
								<#if s.item.faces?size gt 0>,</#if>
							</#if>
							<#if s.item.faces?size gt 0>
								${s.item.faces?size} face<#if s.item.faces?size gt 1>s</#if>
							</#if>
						</td>
						<td class="meta nomobile">
							<#if s.item.attachments?size gt 0>
								<@icon name="image" title="Has images"/>
							</#if>
							<@dependencyIcon s.item.dependencies/>
						</td>
					</tr>
        </#list>
				</tbody>
			</table>
		</section>

	</@content>

<#include "../../_footer.ftl">
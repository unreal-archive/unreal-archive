<#assign ogDescription="Custom gameplay mutators for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<a href="${relPath(sectionPath + "/index.html")}">Mutators</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
	</@heading>

	<@content class="list">
		<table class="mutators">
			<thead>
			<tr>
				<th>Mutator</th>
				<th>Author</th>
				<th>Info</th>
				<td class="nomobile"> </td>
			</tr>
			</thead>
			<tbody>
				<#list mutators as m>
				<tr>
					<td><a href="${relPath(m.path + ".html")}">${m.mutator.name}</a></td>
					<td>${m.mutator.author}</td>
					<td>
						<#if m.mutator.mutators?size gt 0>
							${m.mutator.mutators?size} mutator<#if m.mutator.mutators?size gt 1>s</#if>
							<#if m.mutator.weapons?size gt 0 || m.mutator.vehicles?size gt 0>,</#if>
						</#if>
						<#if m.mutator.weapons?size gt 0>
							${m.mutator.weapons?size} weapons<#if m.mutator.weapons?size gt 1>s</#if>
							<#if m.mutator.vehicles?size gt 0>,</#if>
						</#if>
						<#if m.mutator.vehicles?size gt 0>
							${m.mutator.vehicles?size} vehicle<#if m.mutator.vehicles?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta nomobile">
						<#if m.mutator.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
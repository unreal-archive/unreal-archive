<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${static}/images/games/${game.name}.png"]>
			<a href="${siteRoot}/index.html">Mutators</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
	</@heading>

	<@content class="list">
		<table class="mutators">
			<thead>
			<tr>
				<th>Mutator</th>
				<th>Author</th>
				<th>Info</th>
				<td> </td>
			</tr>
			</thead>
			<tbody>
				<#list mutators as m>
				<tr>
					<td><a href="${relUrl(game.path, m.path + ".html")}">${m.mutator.name}</a></td>
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
					<td class="meta">
						<#if m.mutator.attachments?size gt 0>
							<img src="${static!"static"}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
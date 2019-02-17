<#assign ogDescription="Custom player models for ${game.name}">
<#assign ogImage="${staticPath(static)}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Models</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
	</@heading>

	<@content class="list">
		<table class="models">
			<thead>
			<tr>
				<th>Model</th>
				<th>Author</th>
				<th>Info</th>
				<th> </th>
			</tr>
			</thead>
			<tbody>
				<#list models as m>
				<tr>
					<td><a href="${relUrl(game.path, m.path + ".html")}">${m.model.name}</a></td>
					<td>${m.model.author}</td>
					<td>
						<#if m.model.models?size gt 0>
							${m.model.models?size} character<#if m.model.models?size gt 1>s</#if>
							<#if m.model.skins?size gt 0>,</#if>
						</#if>
						<#if m.model.skins?size gt 0>
							${m.model.skins?size} skin<#if m.model.skins?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta">
						<#if m.model.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
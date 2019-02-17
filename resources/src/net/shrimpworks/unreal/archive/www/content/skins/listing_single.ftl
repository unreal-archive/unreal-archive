<#assign ogDescription="Custom character skins for ${game.name}">
<#assign ogImage="${staticPath(static)}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
			<a href="${siteRoot}/index.html">Skins</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
	</@heading>

	<@content class="list">
		<table class="skins">
			<thead>
			<tr>
				<th>Skin</th>
				<th>Author</th>
				<th>Info</th>
				<td> </td>
			</tr>
			</thead>
			<tbody>
				<#list skins as s>
				<tr>
					<td><a href="${relUrl(game.path, s.path + ".html")}">${s.skin.name}</a></td>
					<td>${s.skin.author}</td>
					<td>
						<#if s.skin.skins?size gt 0>
							${s.skin.skins?size} skin<#if s.skin.skins?size gt 1>s</#if>
							<#if s.skin.faces?size gt 0>,</#if>
						</#if>
						<#if s.skin.faces?size gt 0>
							${s.skin.faces?size} face<#if s.skin.faces?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta">
						<#if s.skin.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
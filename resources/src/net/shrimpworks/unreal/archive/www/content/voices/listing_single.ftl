<#assign ogDescription="Custom player voice packs for ${game.name}">
<#assign ogImage="${staticPath(static)}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Voices</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
	</@heading>

	<@content class="list">
		<table class="voices">
			<thead>
			<tr>
				<th>Voice</th>
				<th>Author</th>
				<th>Info</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list voices as v>
				<tr>
					<td><a href="${relUrl(game.path, v.path + ".html")}">${v.voice.name}</a></td>
					<td>${v.voice.author}</td>
					<td>
						<#if v.voice.voices?size gt 0>
							${v.voice.voices?size} voice<#if v.voice.voices?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta nomobile">
						<#if v.voice.attachments?size gt 0>
							<img src="${staticPath(static)}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
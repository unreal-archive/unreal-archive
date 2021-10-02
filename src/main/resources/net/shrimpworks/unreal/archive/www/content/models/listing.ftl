<#if page??>
  <#assign game=page.letter.game>
  <#assign models=page.models>
</#if>

<#assign ogDescription="Custom player models for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Models</a>
			/</span> <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		<span class="crumbs">
			<#if page?? && game.letters?size gt 1>/ ${page.letter.letter}</#if>
			<#if page?? && page.letter.pages?size gt 1>/ pg ${page.number}</#if>
		</span>
	</@heading>

	<@content class="list">

		<#if page??>
			<@letterPages letters=game.letters currentLetter=page.letter.letter pages=page.letter.pages currentPage=page />
		</#if>

		<table class="models">
			<thead>
			<tr>
				<th>Model</th>
				<th>Author</th>
				<th class="nomobile">Info</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list models as m>
				<tr>
					<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.model.name}</a></td>
					<td><@authorLink m.model.authorName /></td>
					<td class="nomobile">
						<#if m.model.models?size gt 0>
							${m.model.models?size} character<#if m.model.models?size gt 1>s</#if>
							<#if m.model.skins?size gt 0>,</#if>
						</#if>
						<#if m.model.skins?size gt 0>
							${m.model.skins?size} skin<#if m.model.skins?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta nomobile">
						<#if m.model.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
						</#if>
						<@dependencyIcon m.model.dependencies/>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>

		<#if page??>
			<@paginator pages=page.letter.pages currentPage=page />
		</#if>

	</@content>

<#include "../../_footer.ftl">
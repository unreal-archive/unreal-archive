<#if page??>
  <#assign game=page.letter.group.game>
  <#assign models=page.items>
</#if>

<#assign ogDescription="Custom player models for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Models &amp; Characters</a>
			/</span> <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		<span class="crumbs">
			<#if page?? && game.groups.all.letters?size gt 1>/ ${page.letter.letter}</#if>
			<#if page?? && page.letter.pages?size gt 1>/ pg ${page.number}</#if>
		</span>
	</@heading>

	<@content class="list">

		<@tline timeline=timeline game=game></@tline>

		<#if page??>
			<@letterPages letters=game.groups.all.letters currentLetter=page.letter.letter pages=page.letter.pages currentPage=page />
		</#if>

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
					<#list models as m>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.item.name}</a></td>
						<td><@authorLink m.item.authorName /></td>
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
								<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
							</#if>
							<@dependencyIcon m.item.dependencies/>
						</td>
					</tr>
					</#list>
				</tbody>
			</table>
		</section>

		<#if page??>
			<@paginator pages=page.letter.pages currentPage=page />
		</#if>

	</@content>

<#include "../../_footer.ftl">
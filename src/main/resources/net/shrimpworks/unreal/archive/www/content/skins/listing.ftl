<#if page??>
  <#assign game=page.letter.game>
  <#assign skins=page.skins>
</#if>

<#assign ogDescription="Custom player skins for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Skins</a>
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
				<#list skins as s>
				<tr>
					<td nowrap="nowrap"><a href="${relPath(s.path + ".html")}">${s.skin.name}</a></td>
					<td><@authorLink s.skin.authorName /></td>
					<td class="nomobile">
						<#if s.skin.skins?size gt 0>
							${s.skin.skins?size} skin<#if s.skin.skins?size gt 1>s</#if>
							<#if s.skin.faces?size gt 0>,</#if>
						</#if>
						<#if s.skin.faces?size gt 0>
							${s.skin.faces?size} face<#if s.skin.faces?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta nomobile">
						<#if s.skin.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
						</#if>
						<@dependencyIcon s.skin.dependencies/>
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
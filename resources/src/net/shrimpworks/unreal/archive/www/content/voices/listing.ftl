<#if page??>
	<#assign game=page.letter.game>
  <#assign voices=page.voices>
</#if>

<#assign ogDescription="Custom player voice packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<a href="${relPath(sectionPath + "/index.html")}">Voices</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		<#if page?? && game.letters?size gt 1>/ ${page.letter.letter}</#if>
		<#if page?? && page.letter.pages?size gt 1>/ pg ${page.number}</#if>
	</@heading>

	<@content class="list">

		<#if page??>
			<@letterPages letters=game.letters currentLetter=page.letter.letter pages=page.letter.pages currentPage=page />
    </#if>

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
				<tr class="${v?item_parity}">
					<td nowrap="nowrap"><a href="${relPath(v.path + ".html")}">${v.voice.name}</a></td>
					<td>${v.voice.author}</td>
					<td>
						<#if v.voice.voices?size gt 0>
							${v.voice.voices?size} voice<#if v.voice.voices?size gt 1>s</#if>
						</#if>
					</td>
					<td class="meta nomobile">
						<#if v.voice.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/black/px22/ico-images-grey.png" alt="Has images"/>
						</#if>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</@content>

<#include "../../_footer.ftl">
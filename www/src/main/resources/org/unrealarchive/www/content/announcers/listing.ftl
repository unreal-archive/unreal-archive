<#if page??>
	<#assign game=page.letter.group.game>
  <#assign announcers=page.items>
</#if>

<#assign ogDescription="Custom announcer packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/</span> <a href="${relPath(game.path + "/index.html")}">Announcers</a>
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
					<th>Announcer</th>
					<th>Author</th>
					<th class="nomobile">Info</th>
					<th class="nomobile"> </th>
				</tr>
				</thead>
				<tbody>
					<#list announcers as a>
					<tr>
						<td nowrap="nowrap"><a href="${relPath(a.path + ".html")}">${a.item.name}</a></td>
						<td><@authorLink a.item.authorName /></td>
						<td class="nomobile">
							<#if a.item.announcers?size gt 0>
								${a.item.announcers?size} announcer<#if a.item.announcers?size gt 1>s</#if>
							</#if>
						</td>
						<td class="meta nomobile">
							<#if a.item.attachments?size gt 0>
								<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
							</#if>
							<@dependencyIcon a.item.dependencies/>
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
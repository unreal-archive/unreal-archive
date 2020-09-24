<#if page??>
  <#assign gametype=page.letter.gametype>
  <#assign maps=page.maps>
</#if>

<#assign game=gametype.game>

<#assign ogDescription="${gametype.name} maps for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage, "${staticPath()}/images/games/${game.name}.png"]>
		<a href="${relPath(sectionPath + "/index.html")}">Maps</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
    <#if page?? && gametype.letters?size gt 1>/ ${page.letter.letter}</#if>
    <#if page?? && page.letter.pages?size gt 1>/ pg ${page.number}</#if>
	</@heading>

	<@content class="list">

    <#if page??>
      <@letterPages letters=gametype.letters currentLetter=page.letter.letter pages=page.letter.pages currentPage=page />
    </#if>

		<table class="maps">
			<thead>
			<tr>
				<th>Map</th>
				<th class="nomobile">Title</th>
				<th>Author</th>
				<th class="nomobile">Players</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list maps as m>
				<tr>
					<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.map.name}</a></td>
					<td class="nomobile">${m.map.title}</td>
					<td>${m.map.author}</td>
					<td class="nomobile">${m.map.playerCount}</td>
					<td class="meta nomobile">
						<#if m.map.bots>
							<img src="${staticPath()}/images/icons/bots.svg" alt="AI/Bot support" title="AI/Bot support" height="22"/>
						</#if>
						<#if m.map.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/image.svg" alt="Has images" title="Has images" height="22"/>
						</#if>
						<@dependencyIcon m.map.dependencies/>
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
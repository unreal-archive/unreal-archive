<#if page??>
  <#assign gametype=page.letter.group>
  <#assign maps=page.items>
</#if>

<#assign game=gametype.game>

<#assign ogDescription="${gametype.name} maps for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage, "${staticPath()}/images/games/${game.name}.png"]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Maps</a>
			/</span> <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
		<span class="crumbs">
			<#if page?? && gametype.letters?size gt 1>/ ${page.letter.letter}</#if>
			<#if page?? && page.letter.pages?size gt 1>/ pg ${page.number}</#if>
		</span>
	</@heading>

	<@content class="list">

    <#if page??>
      <@letterPages letters=gametype.letters currentLetter=page.letter.letter pages=page.letter.pages currentPage=page />
    </#if>

		<#if gameTypeInfo?? && gameTypeInfoPath??>
			<#assign gtBg="">
			<#if gameTypeInfo.leadImage?has_content>
				<#if gameTypeInfo.leadImage?contains("://")>
					<#assign gtBg=urlEncode(gameTypeInfo.leadImage)>
				<#else>
					<#assign gtBg=rootPath(gameTypeInfo.leadImage)?no_esc>
				</#if>
			</#if>
			<section class="sectionInfo">
				<h3>${gameTypeInfo.name}</h3>
				<p>
					${gameTypeInfo.description}
					<br/>
					<a href="${relPath(gameTypeInfoPath + "/index.html")}" class="infoButton">More about ${gameTypeInfo.name}...</a>
				</p>
				<#if gtBg?has_content>
					<img src="${gtBg}" alt="${gameTypeInfo.name}"/>
				</#if>
			</section>
		</#if>

		<section>
			<table>
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
						<td nowrap="nowrap"><a href="${relPath(m.path + ".html")}">${m.item.name}</a></td>
						<td class="nomobile">${m.item.title}</td>
						<td><@authorLink m.item /></td>
						<td class="nomobile">${m.item.playerCount}</td>
						<td class="meta nomobile">
							<#if m.item.bots>
								<@icon name="bots" title="AI/Bot support"/>
							</#if>
							<#if m.item.attachments?size gt 0>
								<@icon name="image" title="Has images"/>
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
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

	<#if gameTypeInfo?? && gameTypeInfoPath??>
		<div class="page contentpage">
			<section class="section-info">
				<h3>${gameTypeInfo.name}</h3>
				<#if gameTypeInfo.description?has_content>
					<blockquote>
						${gameTypeInfo.description}
					</blockquote>
				</#if>
				<p>
					These maps can be played with the <b>${gameTypeInfo.name}</b> game type. You will need to download and install the
					mod before you're able to play them.
				</p>
				<p>
					<a href="${relPath(gameTypeInfoPath + "/index.html")}" class="info-button">More about ${gameTypeInfo.name}</a>
				</p>
			</section>
		</div>
	</#if>

	<@content class="list">

    <#if page??>
      <@letterPages letters=gametype.letters currentLetter=page.letter.letter pages=page.letter.pages currentPage=page />
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
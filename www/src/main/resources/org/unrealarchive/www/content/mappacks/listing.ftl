<#assign game=gametype.game>

<#assign ogDescription="${gametype.name} map packs for ${game.game.bigName}">
<#assign ogImage="${staticPath()}/images/gametypes/${game.name}/${gametype.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage, "${staticPath()}/images/games/${game.name}.png"]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Map Packs</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/</span> <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
		<span class="crumbs">
			<#if pages?size gt 1>/ pg ${page.number}</#if>
		</span>
	</@heading>

	<@content class="list">

		<@paginator pages=pages currentPage=page />

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
					<a href="${relPath(gameTypeInfoPath + "/index.html")}" class="infoButton">${gameTypeInfo.name}</a>
				</p>
				<#if gtBg?has_content>
					<img src="${gtBg}" alt="${gameTypeInfo.name}"/>
				</#if>
			</section>
		</#if>

		<table>
			<thead>
			<tr>
				<th>Name</th>
				<th>Author</th>
				<th class="nomobile">Maps</th>
				<th class="nomobile"> </th>
			</tr>
			</thead>
			<tbody>
				<#list page.items as p>
				<tr>
					<td><a href="${relPath(p.path + ".html")}">${p.item.name}</a></td>
					<td><@authorLink p.item.authorName /></td>
					<td class="nomobile">${p.item.maps?size}</td>
					<td class="meta nomobile">
						<#if p.item.attachments?size gt 0>
							<img src="${staticPath()}/images/icons/image.svg" alt="Has images" height="22"/>
						</#if>
						<@dependencyIcon p.item.dependencies/>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>

		<@paginator pages=pages currentPage=page />
	</@content>

<#include "../../_footer.ftl">
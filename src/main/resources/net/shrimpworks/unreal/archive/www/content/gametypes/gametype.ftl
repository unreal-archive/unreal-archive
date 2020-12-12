<#if gametype.gametype.bannerImage?? && gametype.gametype.bannerImage?length gt 0>
	<#assign headerbg>${relPath(gametype.path + "/" + gametype.gametype.bannerImage)}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/games/${gametype.game.name}.png</#assign>
</#if>

<#assign ogDescription=gametype.gametype.description>
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${relPath(sectionPath + "/index.html")}">Game Types &amp; Mods</a>
			/ <a href="${relPath(gametype.game.path + "/index.html")}">${gametype.game.name}</a>
			/ ${gametype.gametype.name}
	</@heading>

	<@content class="info-side">
		<section class="info">
			<h2><img src="${staticPath()}/images/icons/info.svg" alt="About"/>About</h2>
			<div class="readable">${page?no_esc}</div>
		</section>

		<section class="side">
			<h2><img src="${staticPath()}/images/icons/info.svg" alt="Information"/>Information</h2>
			<div class="label-value">
				<label>Author</label><span>${gametype.gametype.author}</span>
			</div>
			<div class="label-value">
				<label>Summary</label><span>${gametype.gametype.description}</span>
			</div>
			<div class="label-value">
				<label>Release Date</label><span>${gametype.gametype.releaseDate!"-"}</span>
			</div>
			<#if gametype.gametype.links?size gt 0>
				<div class="label-value">
					<label>Links</label><span>
					<#list gametype.gametype.links as t, u>
						<div>
							<a href="${u}">${t}</a>
						</div>
					</#list>
				</span>
				</div>
			</#if>
<#--			<#if gametype.gametype.homepage??>-->
<#--				<div class="label-value">-->
<#--					<label> </label><span><a href="${gametype.gametype.homepage}">Homepage</a></span>-->
<#--				</div>-->
<#--			</#if>-->
<#--			<div class="label-value">-->
<#--				<label>Date Added</label><span>${gametype.gametype.createdDate}</span>-->
<#--			</div>-->
<#--			<div class="label-value">-->
<#--				<label>Last Updated</label><span>${gametype.gametype.updatedDate}</span>-->
<#--			</div>-->

<#--			<section class="downloads">-->
<#--				<h2><img src="${staticPath()}/images/icons/download.svg" alt="Downloads"/> Downloads</h2>-->
<#--				<#list gametype.gametype.downloads as d>-->
<#--					<#if !d.deleted && d.downloads?size gt 0>-->
<#--						<div class="download">-->
<#--							<div class="title">-->
<#--								<img src="${staticPath()}/images/icons/os-${d.platform?lower_case}.svg" title="${d.platform}" alt="${d.platform}"/>-->
<#--								${d.title} ver ${d.version}-->
<#--							</div>-->
<#--							<div class="info">-->
<#--								<div>${fileName(d.localFile)}</div>-->
<#--								<div>${fileSize(d.fileSize)}</div>-->
<#--								<div>${d.description}</div>-->
<#--							</div>-->
<#--							<div class="links">-->
<#--								<#list d.downloads as l>-->
<#--									<a href="${urlEncode(l)}">${urlHost(l)}</a>-->
<#--								</#list>-->
<#--							</div>-->
<#--						</div>-->
<#--					</#if>-->
<#--				</#list>-->
<#--			</section>-->
		</section>

	</@content>

<#include "../../_footer.ftl">
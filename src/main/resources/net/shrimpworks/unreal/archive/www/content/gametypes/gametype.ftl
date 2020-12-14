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

	<@content class="split split6040">
		<div class="left">
			<section>
				<h2><img src="${staticPath()}/images/icons/info.svg" alt="About"/>About</h2>
				<div class="readable">${page?no_esc}</div>
			</section>

			<#if gametype.gallery?size gt 0>
				<section class="gallery">
					<h2><img src="${staticPath()}/images/icons/image.svg" alt="Screenshots"/>Screenshots</h2>
					<div>
						<#list gametype.gallery as img>
							<img src="${img}" alt="screenshot" class="thumb"/>
						</#list>
					</div>
				</section>
      </#if>

			<#if gametype.gametype.credits?size gt 0>
				<section class="credits">
					<h2><img src="${staticPath()}/images/icons/list.svg" alt="Credits"/>Credits</h2>
					<div>
							<ul>
								<#list gametype.gametype.credits as t, l>
									<li class="group">${t}</li>
									<ul class="names">
										<#list l as n><li>${n}</li></#list>
									</ul>
								</#list>
							</ul>
					</div>
				</section>
			</#if>
		</div>

		<div class="right">
			<section>
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
			</section>

			<section class="releases">
				<h2><img src="${staticPath()}/images/icons/download.svg" alt="Releases"/>Releases</h2>
				<#list gametype.gametype.releases as r>
					<#if !r.deleted && r.files?size gt 0>
						<div class="release">
							<div class="title">
								${r.title} ver ${r.version}
							</div>
							<div class="info">
								<div>Released: ${r.releaseDate}</div>
								<div>${r.description}</div>
							</div>
							<div class="links">
								<a href="${slug(r.title)}/index.html">${r.files?size} Download<#if r.files?size gt 1>s</#if></a>
							</div>
						</div>
					</#if>
				</#list>
			</section>

			<@ghIssue
        text="Report a problem"
        repoUrl="${dataProjectUrl}"
        title="[GameType] ${gametype.gametype.name}"
        hash="None"
        name="${gametype.gametype.name}"/>
		</div>

	</@content>

<#include "../../_footer.ftl">
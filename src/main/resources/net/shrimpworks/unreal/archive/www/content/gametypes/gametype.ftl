<#if gametype.gametype.bannerImage?? && gametype.gametype.bannerImage?length gt 0>
	<#assign headerbg>${relPath(gametype.path + "/" + gametype.gametype.bannerImage)}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/games/${gametype.game.name}.png</#assign>
</#if>

<#assign ogDescription=gametype.gametype.description>
<#assign ogImage=headerbg>

<#assign schemaItemName="${gametype.gametype.name}">
<#assign schemaItemAuthor="${gametype.gametype.authorName}">
<#assign schemaItemDate="${gametype.gametype.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Game Types &amp; Mods</a>
				/ <a href="${relPath(gametype.game.path + "/index.html")}">${gametype.game.name}</a>
				<#if gametype.variationOf??>
					/ <a href="../index.html">${gametype.variationOf.name}</a>
				</#if>
				/</span> ${gametype.gametype.name}
	</@heading>

	<@content class="split split6040">
		<div class="left">
			<section>
				<h2><img src="${staticPath()}/images/icons/info.svg" alt="About"/> About</h2>
				<#if gametype.gametype.titleImage?? && gametype.gametype.titleImage?length gt 0>
					<img src="${relPath(gametype.path + "/" + gametype.gametype.titleImage)}" class="full" alt="${gametype.gametype.name}"/>
				</#if>
				<div class="readable">${page?no_esc}</div>
			</section>

			<#if gametype.gametype.gameTypes?size gt 0
			     || gametype.gametype.mutators?size gt 0
			     || gametype.gametype.maps?size gt 0>
				<section class="contents">
					<h2><img src="${staticPath()}/images/icons/package.svg" alt="Contents"/> Contents</h2>
					<#if gametype.gametype.gameTypes?size gt 0>
						<h3>Game Types (${gametype.gametype.gameTypes?size})</h3>
						<ul class="readable">
							<#list gametype.gametype.gameTypes as g>
								<li>
									${g.name}
									<#if g.description?? && g.description?length gt 0>
										<div class="meta">${g.description}</div>
									</#if>
								</li>
							</#list>
						</ul>
					</#if>

					<#if gametype.gametype.mutators?size gt 0>
						<h3>Mutators (${gametype.gametype.mutators?size})</h3>
						<ul class="readable">
							<#list gametype.gametype.mutators as m>
								<li>
									${m.name}
									<#if m.description?? && m.description?length gt 0>
										<div class="meta">${m.description}</div>
									</#if>
								</li>
							</#list>
						</ul>
					</#if>

					<#if gametype.gametype.maps?size gt 0>
						<h3>Maps (${gametype.gametype.maps?size})</h3>
						<div class="maps">
							<#list gametype.gametype.maps as m>
								<div class="map">
									<div class="image">
										<#if m.screenshot??>
											<img src="${m.screenshot.url}" alt="${m.name}" class="thumb lb"/>
										<#else>
											<img src="${staticPath()}/images/none.png" alt="no image"/>
                    </#if>
									</div>
									<div class="info">
										<div class="title">${m.name}</div>
										<div class="description">${m.title} by ${m.author}</div>
									</div>
								</div>
							</#list>
						</div>
					</#if>
				</section>
			</#if>

			<#if gametype.gallery?size gt 0>
				<section class="gallery">
					<h2><img src="${staticPath()}/images/icons/image.svg" alt="Screenshots"/> Screenshots</h2>
					<div>
							<#list gametype.gallery as img, thumb><a href="${img}"><img src="${thumb}" alt="screenshot" class="thumb"/></a></#list>
					</div>
				</section>
			</#if>

			<#if gametype.gametype.credits?size gt 0>
				<section class="credits">
					<h2><img src="${staticPath()}/images/icons/list.svg" alt="Credits"/> Credits</h2>
					<ul class="readable">
						<#list gametype.gametype.credits as t, l>
							<li class="group">${t}</li>
							<ul class="names">
								<#list l as n><li>${n}</li></#list>
							</ul>
						</#list>
					</ul>
				</section>
			</#if>
		</div>

		<div class="right">
		  <#if gametype.variationOf??>
				<section class="variations">
					<h2><img src="${staticPath()}/images/icons/variant.svg" alt="Variation"/> Variation</h2>
					<p>
						This is a variation of <a href="../index.html">${gametype.variationOf.name}</a>, which is not
						necessarily released or supported by the original authors.
					</p>
					<#if gametype.variationOf.titleImage?? && gametype.variationOf.titleImage?length gt 0>
						<a href="../index.html">
							<img src="${relPath(gametype.path + "/../" + gametype.variationOf.titleImage)}" class="full" alt="${gametype.variationOf.name}"/>
						</a>
					<#else>
					</#if>
				</section>
		  </#if>

			<section>
				<h2><img src="${staticPath()}/images/icons/info.svg" alt="Information"/> Information</h2>
				<div class="label-value">
					<label>Author</label><span><@authorLink gametype.gametype.authorName /></span>
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
				<h2><img src="${staticPath()}/images/icons/download.svg" alt="Releases"/> Releases</h2>
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

			<#if gametype.variations?size gt 0>
				<section class="variations">
					<h2><img src="${staticPath()}/images/icons/variant.svg" alt="Variations"/> Variations</h2>
					  <p class="blurb">
							Enhanced, updated or alternative versions of <b>${gametype.gametype.name}</b>, which
							are not necessarily released or	supported by the original authors.
						</p>
						<#list gametype.variations as v>
							<div class="variation">
								<img src="${staticPath()}/images/icons/variant.svg" alt="Variation"/>
								<a href="${slug(v.gametype.name)}/index.html">${v.gametype.name}</a>
							</div>
						</#list>
				</section>
      </#if>

			<@ghIssue
        text="Report a problem"
        repoUrl="${dataProjectUrl}"
        title="[GameType] ${gametype.gametype.name}"
        hash="None"
        name="${gametype.gametype.name}"/>
		</div>

	</@content>

<#include "../../_footer.ftl">
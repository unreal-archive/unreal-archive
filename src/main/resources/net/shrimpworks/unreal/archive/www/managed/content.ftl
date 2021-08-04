<#if managed.managed.titleImage??>
	<#assign headerbg>${managed.managed.titleImage}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/contents/patches.png</#assign>
</#if>

<#assign ogDescription=managed.managed.description>
<#assign ogImage=headerbg>

<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=[headerbg]>
		<#list groupPath as p>
			<a href="${relPath(p.path + "/index.html")}">${p.name}</a>
			<#if p?has_next>/</#if>
		</#list>
		/ ${managed.managed.title}
	</@heading>

	<@content class="managed document">
		<section class="meta">
			<h2><img src="${staticPath()}/images/icons/info.svg" alt="Information"/>Information</h2>
			<div class="label-value">
				<label>Author</label><span><@authorLink managed.managed.authorName /></span>
			</div>
			<#if managed.managed.homepage??>
				<div class="label-value">
					<label> </label><span><a href="${managed.managed.homepage}">Homepage</a></span>
				</div>
			</#if>
			<div class="label-value">
				<label>Release Date</label><span>${managed.managed.releaseDate!"-"}</span>
			</div>
<#--			<div class="label-value">-->
<#--				<label>Date Added</label><span>${managed.managed.createdDate}</span>-->
<#--			</div>-->
<#--			<div class="label-value">-->
<#--				<label>Last Updated</label><span>${managed.managed.updatedDate}</span>-->
<#--			</div>-->
			<div class="label-value">
				<label>Summary</label><span>${managed.managed.description}</span>
			</div>

			<section class="downloads">
				<h2><img src="${staticPath()}/images/icons/download.svg" alt="Downloads"/> Downloads</h2>
				<#list managed.managed.downloads as d>
					<#if !d.deleted && d.downloads?size gt 0>
						<div class="download">
							<div class="title">
								<img src="${staticPath()}/images/icons/os-${d.platform?lower_case}.svg" title="${d.platform}" alt="${d.platform}"/>
								${d.title} ver ${d.version}
							</div>
							<div class="info">
								<div>${d.originalFileName!fileName(d.localFile)}</div>
								<div>${fileSize(d.fileSize)}</div>
								<div>${d.description}</div>
							</div>
							<div class="links">
								<#list d.downloads as l>
										<#if l.state == 'OK'>
											<a href="${urlEncode(l.url)}" <#if l.main>class="main"</#if>>
													${urlHost(l.url)}
											</a>
										</#if>
								</#list>
							</div>
						</div>
					</#if>
				</#list>
			</section>
		</section>

		<section class="content readable">
			${page?no_esc}
		</section>
	</@content>

<#include "../_footer.ftl">
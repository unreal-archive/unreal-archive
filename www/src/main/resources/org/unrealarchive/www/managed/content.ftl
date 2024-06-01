<#if managed.managed.titleImage??>
	<#assign headerbg>${managed.managed.titleImage}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/contents/patches.png</#assign>
</#if>

<#assign group=managed.subGroup.parent>
<#assign subgroup=managed.subGroup>

<#assign ogDescription=managed.managed.description>
<#assign ogImage=headerbg>

<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(group.game.path + "/index.html")}">${group.game.name}</a>
			/ <a href="${relPath(group.path + "/index.html")}">${group.name}</a>
			/ <a href="${relPath(subgroup.path + "/index.html")}">${subgroup.name}</a>
			/</span> ${managed.managed.title}
	</@heading>

	<@content class="managed document split split7030">
		<div class="left">
			<section class="content readable">
					${page?no_esc}
			</section>
		</div>

		<div class="right">
			<section class="meta">
				<h2><img src="${staticPath()}/images/icons/info.svg" alt="Information"/>Information</h2>
				<div class="label-value">
					<label>Author</label><span><@authorLink managed.managed.authorName /></span>
				</div>
				<#if managed.managed.homepage?? && managed.managed.homepage?has_content>
					<div class="label-value">
						<label> </label><span><a href="${managed.managed.homepage}">Homepage</a></span>
					</div>
				</#if>
				<#if managed.managed.releaseDate?? && managed.managed.releaseDate?has_content>
					<div class="label-value">
						<label>Release Date</label><span>${managed.managed.releaseDate!"-"}</span>
					</div>
				</#if>
				<#if managed.managed.description?? && managed.managed.description?has_content>
					<div class="label-value">
						<label>Summary</label><span>${managed.managed.description}</span>
					</div>
				</#if>

				<section class="downloads">
					<h2><img src="${staticPath()}/images/icons/download.svg" alt="Downloads"/> Downloads</h2>
					<#list managed.managed.downloads as d>
						<#if !d.deleted && d.downloads?size gt 0>
							<div class="download">
								<div class="title">
									<img src="${staticPath()}/images/icons/os-${d.platform?lower_case}.svg" title="${d.platform}" alt="${d.platform}"/>
									${d.title} ${d.version}
								</div>
								<div class="info">
									<div>${d.originalFileName!fileName(d.localFile)}</div>
									<div>${fileSize(d.fileSize)}</div>
									<div>${d.description}</div>
								</div>
								<div class="links">
									<#list d.downloads as l>
											<#if l.state == 'OK'>
												<a href="${urlEncode(l.url)}">${urlHost(l.url)}</a>
											</#if>
									</#list>
								</div>
							</div>
						</#if>
					</#list>
				</section>
			</section>
		</div>

	</@content>

<#include "../_footer.ftl">
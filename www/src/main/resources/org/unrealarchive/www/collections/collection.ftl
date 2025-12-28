<#if collection.collection.leadImage?has_content>
	<#assign headerbg=urlEncode(rootPath(collection.collection.leadImage))>
<#else>
	<#assign headerbg=""/>
</#if>

<#include "../_header.ftl">
<#include "../macros.ftl">

<#assign ogDescription=(collection.collection.description!"A user-curated collection of Unreal Archive content.")>

<@heading bg=[headerbg]>
	<span class="crumbs" xmlns="http://www.w3.org/1999/html">
		<a href="../">Collections</a>
		/</span> ${collection.collection.title}
</@heading>

<@content class="split split7030">

	<div class="left">

		<#if collection.collection.leadImage?has_content>
			<img src="${rootPath(collection.collection.leadImage)}" class="full" alt="${collection.collection.title}"/>
		</#if>

		<section class="biglist bigger">
			<ul>
				<#list collection.items as i>
					<#assign bgi=""/>
					<#if i.content.leadImage?has_content>
						<#if i.content.leadImage?contains("://")>
							<#assign bgi=urlEncode(i.content.leadImage) />
						<#else>
							<#assign bgi=rootPath(i.content.leadImage) />
						</#if>
					</#if>
					<#outputformat "plainText">
						<#assign g><img src="${staticPath()}/images/games/icons/${i.content.game}.png" alt="${i.content.game}" title="${i.content.game}" /></#assign>
					</#outputformat>
					<@bigitem link="${relPath(i.content.pagePath(siteRoot))}" meta="${g}" bg="${bgi}">
						<span class="crumbs">
						  ${i.content.friendlyType}
						  /</span> ${i.content.name}
					</@bigitem>
				</#list>
			</ul>
		</section>
	</div>

	<div class="right">

		<section class="meta">

		<section class="sidebar">
			<h2><@icon "info"/>Information</h2>
			<#if collection.collection.description?? && collection.collection.description?has_content>
				<div class="label-value"><label>About</label><span>${collection.collection.description}</span></div>
			</#if>

			<div class="label-value"><label>Items</label><span>${collection.items?size}</span></div>
			<div class="label-value"><label>Collected By</label><span><@authorLink collection.collection /></span></div>

			<#if collection.collection.links?size gt 0>
				<div class="label-value"><label>Links</label><span>
					<#list collection.collection.links as t, u>
						<div><a href="${u}"><@icon name="external-link" small=true/>${t}</a></div>
					</#list>
				</span></div>
			</#if>

			<div class="label-value"><label>Date</label><span>${dateFmtShort(collection.collection.releaseDate)}</span></div>
		</section>

		<section class="downloads">
			<h2><@icon "download"/>Downloads</h2>
				<#list collection.collection.archives as d>
					<#if !d.deleted && d.downloads?size gt 0>
						<div class="download">
							<div class="title">
								<@icon name="os-${d.platform?lower_case}" title="${d.platform}"/>
								${d.title}
							</div>
							<div class="info">
								<div>${d.originalFileName!fileName(d.localFile)}</div>
								<div>${fileSize(d.fileSize)}</div>
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

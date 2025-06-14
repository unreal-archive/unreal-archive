<#assign headerbg>${staticPath()}/images/games/All.png</#assign>
<#if author.author.bgImage??>
	<#assign headerbg=urlEncode(author.author.bgImage)>
<#elseif author.leadImage??>
	<#assign headerbg=urlEncode(author.leadImage)>
</#if>

<#assign ogDescription="Unreal series content created by ${author.author.name}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

<@heading bg=[ogImage]>
	<span class="crumbs">
		<a href="${relPath(authorsPath + "/index.html")}">Authors</a> /</span>
		${author.author.name}
</@heading>

<@content class="split split7030" id="author">
	<div class="left">
		<section class="biglist bigger">
			<#if author.created?size gt 0>
				<h2>Original Works</h2>
				<#list author.created as group, content>
					<h3>${content?size} ${group}<#if content?size gt 1>s</#if></h3>
					<ul>
						<#list content as c>
							<#assign bgi=""/>
							<#if c.leadImage?has_content>
								<#if c.leadImage?contains("://")>
									<#assign bgi=urlEncode(c.leadImage) />
								<#else>
									<#assign bgi=rootPath(c.leadImage) />
								</#if>
							</#if>
							<#outputformat "plainText">
								<#assign g><img src="${staticPath()}/images/games/icons/${c.game}.png" alt="${c.game}" title="${c.game}" /></#assign>
							</#outputformat>
							<@bigitem link="${relPath(c.pagePath(siteRoot))}" meta="${g}" bg="${bgi}">${c.name}</@bigitem>
						</#list>
					</ul>
				</#list>
			</#if>

			<#if author.contributed?size gt 0>
				<h2>Collaborative Works</h2>
				<#list author.contributed as group, content>
					<h3>${content?size} ${group}<#if content?size gt 1>s</#if></h3>
					<ul>
						<#list content as c>
							<#assign bgi=""/>
							<#if c.leadImage?has_content>
								<#if c.leadImage?contains("://")>
									<#assign bgi=urlEncode(c.leadImage) />
								<#else>
									<#assign bgi=rootPath(c.leadImage) />
								</#if>
							</#if>
							<#outputformat "plainText">
								<#assign g><img src="${staticPath()}/images/games/icons/${c.game}.png" alt="${c.game}" title="${c.game}" /></#assign>
							</#outputformat>
							<@bigitem link="${relPath(c.pagePath(siteRoot))}" meta="${g}" bg="${bgi}">${c.name}</@bigitem>
						</#list>
					</ul>
				</#list>
			</#if>

			<#if author.modified?size gt 0>
				<h2>Modifications and Edits</h2>
				<#list author.modified as group, content>
					<h3>${content?size} ${group}<#if content?size gt 1>s</#if></h3>
					<ul>
						<#list content as c>
							<#assign bgi=""/>
							<#if c.leadImage?has_content>
								<#if c.leadImage?contains("://")>
									<#assign bgi=urlEncode(c.leadImage) />
								<#else>
									<#assign bgi=rootPath(c.leadImage) />
								</#if>
							</#if>
							<#outputformat "plainText">
								<#assign g><img src="${staticPath()}/images/games/icons/${c.game}.png" alt="${c.game}" title="${c.game}" /></#assign>
							</#outputformat>
							<@bigitem link="${relPath(c.pagePath(siteRoot))}" meta="${g}" bg="${bgi}">${c.name}</@bigitem>
						</#list>
					</ul>
				</#list>
			</#if>
		</section>
	</div>

	<div class="right">
		<section class="sidebar">
			<h2><@icon "user"/>Author Profile</h2>

			<#if author.author.profileImage??>
				<img src="${author.author.profileImage}" alt="${author.author.name}" class="profile" />
			<#else>
				<img src="${staticPath()}/images/none.png" class="profile" alt="no image"/>
			</#if>

			<#if author.author.about?? && author.author.about?has_content>
				<div class="label-value"><label>About</label><span>${author.author.about}</span></div>
			</#if>

			<#if author.author.showAliases && author.author.aliases?size gt 0>
				<div class="label-value"><label>Also known as</label><span>
					<#list author.author.aliases as alias>
						<div>${alias}</div>
					</#list>
				</span></div>
			</#if>

			<#if author.author.links?size gt 0>
				<div class="label-value"><label>Links</label><span>
					<#list author.author.links as t, u>
						<div><a href="${u}"><@icon name="external-link" small=true/>${t}</a></div>
					</#list>
				</span></div>
			</#if>

			<#if author.created?size gt 0>
				<h3><@icon "package"/>Original Works</h3>
				<div class="label-value">
				<#list author.created as group, contents>
					<label>${group}</label><span>${contents?size}</span>
				</#list>
				</div>
      </#if>

			<#if author.contributed?size gt 0>
				<h3><@icon "package"/>Collaborative Works</h3>
				<div class="label-value">
				<#list author.contributed as group, contents>
					<label>${group}</label><span>${contents?size}</span>
				</#list>
				</div>
      </#if>

			<#if author.modified?size gt 0>
				<h3><@icon "package"/>Modifications and Edits</h3>
				<div class="label-value">
				<#list author.modified as group, contents>
					<label>${group}</label><span>${contents?size}</span>
				</#list>
				</div>
      </#if>

		</section>
	</div>
</@content>

<#include "../../_footer.ftl">
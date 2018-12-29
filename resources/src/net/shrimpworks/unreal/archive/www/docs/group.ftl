<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading>
		<a href="${siteRoot}/index.html">Articles & Guides</a>
		<#list groupPath as p>
			/ <a href="${relUrl(siteRoot, p.path)}/index.html">${p.name}</a>
		</#list>
	</@heading>

	<@content class="biglist">
		<ul>
		<#list group.groups as k, g>
			<li style='background-image: url("${static}/images/games/${g.name}.png")'>
				<span class="meta">${g.docs}</span>
				<#if g.parent??>
					<a href="${relUrl(g.parent.path, g.path + "/index.html")}">${g.name}</a>
				<#else>
					<a href="${g.path}/index.html">${g.name}</a>
				</#if>
			</li>
		</#list>
		</ul>
	</@content>

	<#if group.documents?size gt 0>
		<@content class="list">
			<table class="docs">
				<thead>
				<tr>
					<th>&nbsp;</th>
					<th>Title</th>
					<th>Author</th>
					<th>Created</th>
					<th>Last Updated</th>
				</tr>
				</thead>
				<tbody>
					<#list group.documents as d>
						<tr class="${d?item_parity}">
							<td class="title-image" rowspan="2">
								<a href="${relUrl(group.path, d.path)}/index.html">
									<#if d.document.titleImage??>
										<img src="${relUrl(group.path, d.path)}/${d.document.titleImage}"/>
									<#else>
										<img src="${static!"static"}/images/none-document.png"/>
									</#if>
								</a>
							</td>
							<td nowrap="nowrap"><a href="${relUrl(group.path, d.path)}/index.html">${d.document.title}</a></td>
							<td>${d.document.author}</td>
							<td>${d.document.createdDate}</td>
							<td>${d.document.updatedDate}</td>
						</tr>
						<tr class="${d?item_parity}">
							<td colspan="4">${d.document.description}</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</@content>
	</#if>

<#include "../_footer.ftl">
<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=["${staticPath()}/images/contents/documents.png"]>
		<a href="${relPath(sectionPath + "/index.html")}">Articles & Guides</a>
		<#list groupPath as p>
			/ <a href="${relPath(p.path + "/index.html")}">${p.name}</a>
		</#list>
	</@heading>

	<@content class="biglist">
		<ul>
		<#list group.groups as k, g>
			<li style='background-image: url("${staticPath()}/images/games/${g.name}.png")'>
				<span class="meta">${g.docs}</span>
				<a href="${relPath(g.path + "/index.html")}">${g.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

	<#if group.documents?size gt 0>
		<@content class="list">
			<table class="docs">
				<thead>
				<tr>
					<th class="nomobile">&nbsp;</th>
					<th>Title</th>
					<th>Author</th>
					<th class="nomobile">Created</th>
					<th>Last Updated</th>
				</tr>
				</thead>
				<tbody>
					<#list group.documents as d>
						<tr class="${d?item_parity}">
							<td class="title-image nomobile" rowspan="2">
								<a href="${relPath(d.path + "/index.html")}">
									<#if d.document.titleImage??>
										<img src="${relPath(d.path + "/" + d.document.titleImage)}"/>
									<#else>
										<img src="${staticPath()}/images/none-document.png"/>
									</#if>
								</a>
							</td>
							<td nowrap="nowrap"><a href="${relPath(d.path + "/index.html")}">${d.document.title}</a></td>
							<td>${d.document.author}</td>
							<td class="nomobile">${d.document.createdDate}</td>
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
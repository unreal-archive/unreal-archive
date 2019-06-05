<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=["${staticPath()}/images/contents/patches.png"]>
		<a href="${relPath(sectionPath + "/index.html")}">Patches & Updates</a>
		<#list groupPath as p>
			/ <a href="${relPath(p.path + "/index.html")}">${p.name}</a>
		</#list>
	</@heading>

	<@content class="biglist">
		<ul>
		<#list group.groups as k, g>
			<li style='background-image: url("${staticPath()}/images/games/${g.name}.png")'>
				<span class="meta">${g.count}</span>
				<a href="${relPath(g.path + "/index.html")}">${g.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

	<#if group.content?size gt 0>
		<@content class="list">
			<table class="content">
				<thead>
				<tr>
					<th class="nomobile">&nbsp;</th>
					<th>Title</th>
					<th>Author</th>
					<th>Release Date</th>
				</tr>
				</thead>
				<tbody>
					<#list group.content as c>
						<tr class="${c?item_parity}">
							<td class="title-image nomobile" rowspan="2">
								<a href="${relPath(c.path + "/index.html")}">
									<#if c.managed.titleImage??>
										<img src="${relPath(c.path + "/" + c.managed.titleImage)}"/>
									<#else>
										<img src="${staticPath()}/images/none-managed.png"/>
									</#if>
								</a>
							</td>
							<td nowrap="nowrap"><a href="${relPath(c.path + "/index.html")}">${c.managed.title}</a></td>
							<td>${c.managed.author}</td>
							<td>${c.managed.releaseDate!"-"}</td>
						</tr>
						<tr class="${c?item_parity}">
							<td colspan="3">${c.managed.description}</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</@content>
	</#if>

<#include "../_footer.ftl">
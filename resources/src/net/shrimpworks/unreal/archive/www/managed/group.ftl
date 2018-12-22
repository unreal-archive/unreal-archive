<#include "../_header.ftl">

	<section class="header">
		<h1>
			${title}
		</h1>
	</section>

	<article class="biglist">
		<ul>
			<#list group.groups as k, g>
				<li style='background-image: url("${static}/images/games/${g.name}.png")'>
					<span class="meta">${g.count}</span>
					<#if g.parent??>
						<a href="${relUrl(g.parent.path, g.path + "/index.html")}">${g.name}</a>
					<#else>
						<a href="${g.path}/index.html">${g.name}</a>
					</#if>
				</li>
			</#list>
		</ul>
	</article>

<#if group.content?size gt 0>
	<article class="list">
		<table class="content">
			<thead>
			<tr>
				<th>&nbsp;</th>
				<th>Title</th>
				<th>Author</th>
				<th>Release Date</th>
			</tr>
			</thead>
			<tbody>
				<#list group.content as c>
					<tr class="${c?item_parity}">
						<td class="title-image" rowspan="2">
						<#if c.managed.titleImage??>
							<img src="${relUrl(group.path, c.path)}/${c.managed.titleImage}"/>
						<#else>
							<img src="${static!"static"}/images/none-managed.png"/>
						</#if>
						</td>
						<td nowrap="nowrap"><a href="${relUrl(group.path, c.path)}/index.html">${c.managed.title}</a></td>
						<td>${c.managed.author}</td>
						<td>${c.managed.releaseDate!"-"}</td>
					</tr>
					<tr class="${c?item_parity}">
						<td colspan="3">${c.managed.description}</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</article>
</#if>

<#include "../_footer.ftl">
<#assign ogImage="${staticPath()}/images/games/${game}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">${game} / Files /</span>
		${file.name}
	</@heading>

	<@content>
		<#compress>
		<div class="info">
			<section class="meta">
				<h2>File</h2>
				<div class="label-value">
					<label>Name</label><span>${file.name}</span>
				</div>
				<div class="label-value">
					<label>Type</label><span>${type}</span>
				</div>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(file.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>SHA1 Hash</label><span>${file.hash}</span>
				</div>
			</section>

			<section class="packages">
				<h2>In Packages</h2>
				<table>
					<thead>
					<tr>
						<th>Type</th>
						<th>Game</th>
						<th>Name</th>
						<th>Author</th>
					</tr>
					</thead>
					<tbody>
						<#list packages as c>
						<tr>
							<td>${c.friendlyType}</td>
							<td>${c.game}</td>
							<td>
								<a href="${relPath(c.pagePath(siteRoot))}">${c.name}</a>
							</td>
							<td><@authorLink c /></td>
						</tr>
						</#list>
					</tbody>
				</table>
			</section>

		</div>
  	</#compress>
	</@content>

<#include "../../_footer.ftl">
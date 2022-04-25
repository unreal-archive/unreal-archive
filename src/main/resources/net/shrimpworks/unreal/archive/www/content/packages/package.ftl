<#assign ogImage="${staticPath()}/images/games/${game}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">Packages / ${game} /</span> ${package}
	</@heading>

	<@content>

		<div class="info">

			<#if packageFiles?size gt 1>
				<section class="files">
					<h2><img src="${staticPath()}/images/icons/list.svg" alt="Files"/> Files</h2>
					<div>
						<p>This package has multiple variations with the same name, but likely different contents. The below table lists each variation.</p>
						<p>Click on a variation to find the release files which use each variation.</p>
					</div>
					<table>
						<thead>
						<tr>
							<th>Name</th>
							<th>File Size</th>
							<th>SHA1 Hash</th>
							<th>Usages</th>
						</tr>
						</thead>
						<tbody>
						<#list packageFiles as file, packages>
							<tr>
								<td><a href="#${file.hash}">${file.name}</a></td>
								<td>${fileSize(file.fileSize)}</td>
								<td>${file.hash}</td>
								<td>${packages?size}</td>
							</tr>
						</#list>
						</tbody>
					</table>
				</section>
      </#if>

			<#list packageFiles as file, packages>
				<section>
					<h2 id="${file.hash}"><img src="${staticPath()}/images/icons/package.svg" alt="Files"/> File</h2>
					<div class="label-value">
						<label>Name</label><span>${file.name}</span>
					</div>
					<div class="label-value">
						<label>File Size</label><span>${fileSize(file.fileSize)}</span>
					</div>
					<div class="label-value">
						<label>SHA1 Hash</label><span>${file.hash}</span>
					</div>

					<section class="packages">
						<h3>Usages (${packages?size})</h3>
						<table>
							<thead>
							<tr>
								<th>Type</th>
								<th>Name</th>
								<th>Author</th>
							</tr>
							</thead>
							<tbody>
								<#list packages as c>
								<tr>
									<td>${c.friendlyType}</td>
									<td>
										<a href="${relPath(c.slugPath(siteRoot) + ".html")}">${c.name}</a>
									</td>
									<td><@authorLink c.authorName /></td>
								</tr>
								</#list>
							</tbody>
						</table>
					</section>

				</section>
      </#list>

		</div>

	</@content>

<#include "../../_footer.ftl">
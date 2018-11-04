<#include "../_header.ftl">

	<#assign headerbg>${static}/images/games/${pack.page.game.name}.png</#assign>

	<#list pack.pack.attachments as a>
		<#if a.type == "IMAGE">
			<#assign headerbg=urlEncode(a.url)>
			<#break>
		</#if>
	</#list>

	<section class="header" <#if headerbg??>style="background-image: url('${headerbg}')"</#if>>
		<h1>
			<a href="${siteRoot}/index.html">Map Packs</a>
			/ <a href="${relUrl(siteRoot, pack.page.game.path)}/index.html">${pack.page.game.name}</a>
			/ ${pack.pack.name}
		</h1>
	</section>

	<article class="info">

		<div class="screenshots">
			<#if pack.pack.attachments?size == 0>
				<img src="${static}/images/none.png" class="thumb"/>
			<#else>
				<#list pack.pack.attachments as a>
					<#if a.type == "IMAGE">
						<img src="${urlEncode(a.url)}" class="thumb"/>
					</#if>
				</#list>
			</#if>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Map Information</h2>
				<div class="label-value">
					<label>Name</label><span>${pack.pack.name}</span>
				</div>
				<div class="label-value">
					<label>Maps</label><span>${pack.pack.maps?size}</span>
				</div>
				<div class="label-value">
					<label>Author</label><span>${pack.pack.author}</span>
				</div>
				<div class="label-value">
					<label>Release (est.)</label><span>${pack.pack.releaseDate}</span>
				</div>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(pack.pack.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>File Name</label><span>${pack.pack.originalFilename}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${pack.pack.hash}</span>
				</div>
			</section>

			<section class="maps">
				<h2>Maps</h2>
				<table>
					<thead>
					<tr>
						<th>Name</th>
						<th>Title</th>
						<th>Author</th>
					</tr>
					</thead>
					<tbody>
						<#list pack.pack.maps as m>
						<tr>
							<td>${m.name}</td>
							<td>${m.title}</td>
							<td>${m.author}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</section>

			<section class="files">
				<h2>Packaged Files</h2>
				<table>
					<thead>
					<tr>
						<th>Name</th>
						<th>Size</th>
						<th>Hash</th>
						<th>Also In</th>
					</tr>
					</thead>
					<tbody>
						<#list pack.pack.files as f>
						<tr>
							<td>${f.name}</td>
							<td>${fileSize(f.fileSize)}</td>
							<td>${f.hash}</td>
							<#if pack.alsoIn[f.hash]??>
								<td>
									<a href="${relUrl(siteRoot + "/../", "files/" + f.hash[0..1] + "/" + f.hash + ".html")}">${pack.alsoIn[f.hash]}</a>
								</td>
							<#else>
								<td>-</td>
							</#if>
						</tr>
						</#list>
					</tbody>
				</table>
				<#if pack.pack.otherFiles gt 0>
					<div class="otherFiles">
						<div class="label-value">
							<label>Misc Files</label><span>${pack.pack.otherFiles}</span>
						</div>
					</div>
				</#if>
			</section>

			<section class="downloads">
				<h2>Downloads</h2>
				<div class="links">
					<#list pack.pack.downloads as d>
						<#if !d.deleted>
							<#if d.main>
								<a href="${urlEncode(d.url)}" class="main">Primary</a>
							<#else>
								<a href="${urlEncode(d.url)}" class="secondary">${urlHost(d.url)}</a>
							</#if>
						</#if>
					</#list>
				</div>
			</section>

		</div>

	</article>

<#include "../_footer.ftl">
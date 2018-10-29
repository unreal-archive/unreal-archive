<#include "../_header.ftl">

<#list map.map.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

	<section class="header" <#if headerbg??>style="background-image: url('${headerbg}')"</#if>>
		<h1>
		${map.page.letter.gametype.game.name} / ${map.page.letter.gametype.name} / ${map.map.name}
		</h1>
	</section>

	<article class="mapinfo">
		<div class="screenshots">
			<#list map.map.attachments as a>
				<#if a.type == "IMAGE">
					<img src="${urlEncode(a.url)}" class="thumb"/>
				</#if>
			</#list>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Map Information</h2>
				<div class="label-value">
					<label>Name</label><span>${map.map.name}</span>
				</div>
				<div class="label-value">
					<label>Game Type</label><span>${map.map.gametype}</span>
				</div>
				<div class="label-value">
					<label>Title</label><span>${map.map.title}</span>
				</div>
				<div class="label-value">
					<label>Author</label><span>${map.map.author}</span>
				</div>
				<div class="label-value">
					<label>Player Count</label><span>${map.map.playerCount}</span>
				</div>
				<div class="label-value">
					<label>Release (est.)</label><span>${map.map.releaseDate}</span>
				</div>
				<div class="label-value">
					<label>Description</label><span>${map.map.description}</span>
				</div>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(map.map.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${map.map.hash}</span>
				</div>
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
						<#list map.map.files as f>
						<tr>
							<td>${f.name}</td>
							<td>${fileSize(f.fileSize)}</td>
							<td>${f.hash}</td>
							<#if map.alsoIn[f.hash]??>
								<td>
									<a href="${relUrl(siteRoot, "files/" + f.hash[0..1] + "/" + f.hash + ".html")}">${map.alsoIn[f.hash]}</a>
								</td>
							<#else>
								<td>-</td>
							</#if>
						</tr>
						</#list>
					</tbody>
				</table>
				<#if map.map.otherFiles gt 0>
					<div class="otherFiles">
						<div class="label-value">
							<label>Other Files</label><span>${map.map.otherFiles}</span>
						</div>
					</div>
				</#if>
			</section>

			<section class="downloads">
				<h2>Downloads</h2>
				<div class="links">
					<#list map.map.downloads as d>
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
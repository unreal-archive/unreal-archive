<#include "../_header.ftl">
<#include "../content/macros.ftl">

	<#assign game=map.page.letter.gametype.game>
	<#assign gametype=map.page.letter.gametype>

	<#assign headerbg>${static}/images/games/${game.name}.png</#assign>

	<#list map.map.attachments as a>
		<#if a.type == "IMAGE">
			<#assign headerbg=urlEncode(a.url)>
			<#break>
		</#if>
	</#list>

	<section class="header" style="background-image: url('${headerbg}')">
		<h1>
			<a href="${siteRoot}/index.html">Maps</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ <a href="${relUrl(siteRoot, gametype.path)}/index.html">${gametype.name}</a>
			/ ${map.map.name}
		</h1>
	</section>

	<article class="info">
		<div class="screenshots">
			<#if map.map.attachments?size == 0>
				<img src="${static}/images/none.png" class="thumb"/>
			<#else>
				<#list map.map.attachments as a>
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
					<label>Name</label><span>${map.map.name}</span>
				</div>
				<div class="label-value">
					<label>Game Type</label><span>
						<a href="${relUrl(siteRoot, gametype.path + "/index.html")}">${map.map.gametype}</a>
					</span>
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
					<label>File Name</label><span>${map.map.originalFilename}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${map.map.hash}</span>
				</div>
			</section>

			<#if map.variations?size gt 0>
				<section class="variations">
					<h2>Variations</h2>
					<table>
						<thead>
						<tr>
							<th>Name</th>
							<th>Release Date (est)</th>
							<th>File Name</th>
							<th>File Size</th>
						</tr>
						</thead>
						<tbody>
							<#list map.variations as v>
							<tr>
								<td><a href="${relUrl(siteRoot, v.path + ".html")}">${v.map.name}</a></td>
								<td>${v.map.releaseDate}</td>
								<td>${v.map.originalFilename}</td>
								<td>${fileSize(v.map.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<@files files=map.map.files alsoIn=map.alsoIn otherFiles=map.map.otherFiles/>

			<@downloads downloads=map.map.downloads/>

		</div>

	</article>

<#include "../_footer.ftl">
<#assign game=pack.page.gametype.game>
<#assign gametype=pack.page.gametype>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>

<#list pack.pack.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

<#assign ogDescription="${pack.pack.name}, a ${gametype.name} map pack for ${game.game.bigName} containing ${pack.pack.maps?size} maps, created by ${pack.pack.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<a href="${relPath(sectionPath + "/index.html")}">Map Packs</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
		/ ${pack.pack.name}
	</@heading>

	<@content class="info">

		<div class="screenshots">
			<@screenshots attachments=pack.pack.attachments/>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Map Pack Information</h2>
				<div class="label-value">
					<label>Name</label><span>${pack.pack.name}</span>
				</div>
				<div class="label-value">
					<label>Game Type</label><span>
						<a href="${relPath(gametype.path + "/index.html")}">${pack.pack.gametype}</a>
					</span>
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
				<div class="label-value nomobile">
					<label>Hash</label><span>${pack.pack.hash}</span>
				</div>
			</section>

			<#if pack.variations?size gt 0>
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
							<#list pack.variations as v>
							<tr>
								<td><a href="${relPath(v.path + ".html")}">${v.pack.name}</a></td>
								<td>${v.pack.releaseDate}</td>
								<td>${v.pack.originalFilename}</td>
								<td>${fileSize(v.pack.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<section class="maps">
				<h2>Maps</h2>
				<table>
					<thead>
					<tr>
						<th>Name</th>
						<th class="nomobile">Title</th>
						<th>Author</th>
					</tr>
					</thead>
					<tbody>
						<#list pack.pack.maps as m>
						<tr>
							<td>${m.name}</td>
							<td class="nomobile">${m.title}</td>
							<td>${m.author}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</section>

			<@files files=pack.pack.files alsoIn=pack.alsoIn otherFiles=pack.pack.otherFiles/>

			<@downloads downloads=pack.pack.downloads/>

		</div>

	</@content>

<#include "../../_footer.ftl">
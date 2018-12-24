<#include "../_header.ftl">
<#include "../content/macros.ftl">

	<#assign game=skin.page.letter.game>

	<#assign headerbg>${static}/images/games/${game.name}.png</#assign>

	<#list skin.skin.attachments as a>
		<#if a.type == "IMAGE">
			<#assign headerbg=urlEncode(a.url)>
			<#break>
		</#if>
	</#list>

	<section class="header" style="background-image: url('${headerbg}')">
		<h1>
			<a href="${siteRoot}/index.html">Skins</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ ${skin.skin.name}
		</h1>
	</section>

	<article class="info">
		<div class="screenshots">
			<#if skin.skin.attachments?size == 0>
				<img src="${static}/images/none.png" class="thumb"/>
			<#else>
				<#list skin.skin.attachments as a>
					<#if a.type == "IMAGE">
						<img src="${urlEncode(a.url)}" class="thumb"/>
					</#if>
				</#list>
			</#if>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Skin Information</h2>
				<div class="label-value">
					<label>Name</label><span>${skin.skin.name}</span>
				</div>
				<div class="label-value">
					<label>Author</label><span>${skin.skin.author}</span>
				</div>
				<div class="label-value">
					<label>Release (est.)</label><span>${skin.skin.releaseDate}</span>
				</div>
				<div class="label-value">
					<label>Team Skins</label><span>${skin.skin.teamSkins}</span>
				</div>
				<#if skin.skin.skins?size gt 1>
					<div class="label-value">
						<label>Included Skins</label><span>
							<#list skin.skin.skins as s>
								<div>${s}</div>
							</#list>
						</span>
					</div>
				</#if>
				<div class="label-value">
					<label>Faces</label><span>
						<#list skin.skin.faces as s>
							<div>${s}</div>
						</#list>
					</span>
				</div>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(skin.skin.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>File Name</label><span>${skin.skin.originalFilename}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${skin.skin.hash}</span>
				</div>
			</section>

			<#if skin.variations?size gt 0>
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
							<#list skin.variations as v>
							<tr>
								<td><a href="${relUrl(siteRoot, v.path + ".html")}">${v.skin.name}</a></td>
								<td>${v.skin.releaseDate}</td>
								<td>${v.skin.originalFilename}</td>
								<td>${fileSize(v.skin.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>


			<@files files=skin.skin.files alsoIn=skin.alsoIn otherFiles=skin.skin.otherFiles/>

			<@downloads downloads=skin.skin.downloads/>

		</div>

	</article>

<#include "../_footer.ftl">
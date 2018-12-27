<#include "../../_header.ftl">
<#include "../macros.ftl">

	<#assign game=model.page.letter.game>

	<#assign headerbg>${static}/images/games/${game.name}.png</#assign>

	<#list model.model.attachments as a>
		<#if a.type == "IMAGE">
			<#assign headerbg=urlEncode(a.url)>
			<#break>
		</#if>
	</#list>

	<section class="header" style="background-image: url('${headerbg}')">
		<h1>
			<a href="${siteRoot}/index.html">Models</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ ${model.model.name}
		</h1>
	</section>

	<article class="info">
		<div class="screenshots">
			<@screenshots attachments=model.model.attachments/>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Model Information</h2>
				<div class="label-value">
					<label>Name</label><span>${model.model.name}</span>
				</div>
				<div class="label-value">
					<label>Author</label><span>${model.model.author}</span>
				</div>
				<div class="label-value">
					<label>Release (est.)</label><span>${model.model.releaseDate}</span>
				</div>
				<#if model.model.models?size gt 1>
					<div class="label-value">
						<label>Included Models</label><span>
							<#list model.model.models as m>
								<div>${m}</div>
							</#list>
						</span>
					</div>
				</#if>
				<#if model.model.skins?size gt 1>
					<div class="label-value">
						<label>Included Skins</label><span>
							<#list model.model.skins as s>
								<div>${s}</div>
							</#list>
						</span>
					</div>
				</#if>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(model.model.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>File Name</label><span>${model.model.originalFilename}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${model.model.hash}</span>
				</div>
			</section>

			<#if model.variations?size gt 0>
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
							<#list model.variations as v>
							<tr>
								<td><a href="${relUrl(siteRoot, v.path + ".html")}">${v.model.name}</a></td>
								<td>${v.model.releaseDate}</td>
								<td>${v.model.originalFilename}</td>
								<td>${fileSize(v.model.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>


			<@files files=model.model.files alsoIn=model.alsoIn otherFiles=model.model.otherFiles/>

			<@downloads downloads=model.model.downloads/>

		</div>

	</article>

<#include "../../_footer.ftl">
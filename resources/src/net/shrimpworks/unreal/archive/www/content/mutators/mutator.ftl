<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<#assign game=mutator.page.letter.game>

	<#assign headerbg>${static}/images/games/${game.name}.png</#assign>

	<#list mutator.mutator.attachments as a>
		<#if a.type == "IMAGE">
			<#assign headerbg=urlEncode(a.url)>
			<#break>
		</#if>
	</#list>

	<@heading bg=[headerbg]>
			<a href="${siteRoot}/index.html">Mutators</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ ${mutator.mutator.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=mutator.mutator.attachments/>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Mutator Information</h2>
				<div class="label-value">
					<label>Name</label><span>${mutator.mutator.name}</span>
				</div>
				<div class="label-value">
					<label>Description</label><span>${mutator.mutator.description}</span>
				</div>
				<div class="label-value">
					<label>Author</label><span>${mutator.mutator.author}</span>
				</div>
				<div class="label-value">
					<label>Release (est.)</label><span>${mutator.mutator.releaseDate}</span>
				</div>
				<div class="label-value">
					<label>Custom Config Menus</label><span>${mutator.mutator.hasConfigMenu?string('Yes', 'No')}</span>
				</div>
				<div class="label-value">
					<label>Custom Keybinds</label><span>${mutator.mutator.hasKeybinds?string('Yes', 'No')}</span>
				</div>
				<#if mutator.mutator.mutators?size gt 0>
					<div class="label-value">
						<label>Included Mutators</label><span>
							<#list mutator.mutator.mutators as m>
								<div class="mini-head">${m.name}</div>
								<div class="mini-detail">${m.description}</div>
							</#list>
						</span>
					</div>
				</#if>
				<#if mutator.mutator.weapons?size gt 0>
					<div class="label-value">
						<label>Weapons</label><span>
							<#list mutator.mutator.weapons as m>
								<div class="mini-head">${m.name}</div>
								<div class="mini-detail">${m.description}</div>
							</#list>
						</span>
					</div>
				</#if>
				<#if mutator.mutator.vehicles?size gt 0>
					<div class="label-value">
						<label>Vehicles</label><span>
							<#list mutator.mutator.vehicles as m>
								<div class="mini-head">${m.name}</div>
								<div class="mini-detail">${m.description}</div>
							</#list>
						</span>
					</div>
				</#if>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(mutator.mutator.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>File Name</label><span>${mutator.mutator.originalFilename}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${mutator.mutator.hash}</span>
				</div>
			</section>

			<#if mutator.variations?size gt 0>
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
							<#list mutator.variations as v>
							<tr>
								<td><a href="${relUrl(siteRoot, v.path + ".html")}">${v.mutator.name}</a></td>
								<td>${v.mutator.releaseDate}</td>
								<td>${v.mutator.originalFilename}</td>
								<td>${fileSize(v.mutator.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>


			<@files files=mutator.mutator.files alsoIn=mutator.alsoIn otherFiles=mutator.mutator.otherFiles/>

			<@downloads downloads=mutator.mutator.downloads/>

		</div>

	</@content>

<#include "../../_footer.ftl">
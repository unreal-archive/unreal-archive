<#assign game=mutator.page.letter.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>

<#list mutator.mutator.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

<#if mutator.mutator.weapons?size gt 0>
	<#assign weaponString="weapon">
<#else>
	<#assign weaponString="">
</#if>

<#assign ogDescription="${mutator.mutator.name}, a custom ${weaponString} mutator for ${game.game.bigName}, created by ${mutator.mutator.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${relPath(sectionPath + "/index.html")}">Mutators</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ ${mutator.mutator.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=mutator.mutator.attachments/>
		</div>

		<div class="info">

			<#assign mutatorList>
				<#list mutator.mutator.mutators as m>
					<div class="mini-head">${m.name}</div>
					<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
				<#else>
					Unknown
				</#list>
			</#assign>

			<#assign weaponsList>
				<#list mutator.mutator.weapons as m>
					<div class="mini-head">${m.name}</div>
					<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
        <#else>
					None
				</#list>
			</#assign>

			<#assign vehicleList>
				<#list mutator.mutator.vehicles as m>
					<div class="mini-head">${m.name}</div>
					<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
        <#else>
					None
				</#list>
			</#assign>

			<#assign
			labels=[
					"Name",
					"Description",
					"Author",
					"Release (est.)",
					"Custom Config Menus",
					"Custom Keybinds",
					"Included Mutators",
					"Weapons",
					"Vehicles",
					"File Size",
					"File Name",
					"Hash"
			]

			values=[
					'${mutator.mutator.name}',
					'${mutator.mutator.description}',
					'${mutator.mutator.author}',
					'${dateFmtShort(mutator.mutator.releaseDate)}',
					'${mutator.mutator.hasConfigMenu?string("Yes", "No")}',
					'${mutator.mutator.hasKeybinds?string("Yes", "No")}',
					'${mutatorList}',
					'${weaponsList}',
					'${vehicleList}',
					'${fileSize(mutator.mutator.fileSize)}',
					'${mutator.mutator.originalFilename}',
					'${mutator.mutator.hash}'
			]>

			<@meta title="Mutator Information" labels=labels values=values/>

			<#if mutator.variations?size gt 0>
				<section class="variations">
					<h2><img src="${staticPath()}/images/icons/black/px22/variant.png" alt="Variations"/>Variations</h2>
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
								<td><a href="${relPath(v.path + ".html")}">${v.mutator.name}</a></td>
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
<#assign game=mutator.page.letter.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if mutator.mutator.leadImage?has_content>
    <#assign headerbg=urlEncode(mutator.mutator.leadImage)>
</#if>

<#assign ogDescription="${mutator.mutator.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${mutator.mutator.name}">
<#assign schemaItemAuthor="${mutator.mutator.author}">
<#assign schemaItemDate="${mutator.mutator.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Mutators</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/</span> ${mutator.mutator.name}
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

			<#assign author><@authorLink mutator.mutator.authorName /></#assign>
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
					"SHA1 Hash"
			]

			values=[
					'${mutator.mutator.name}',
					'${mutator.mutator.description}',
					'${author}',
					'${dateFmtShort(mutator.mutator.releaseDate)}',
					'${mutator.mutator.hasConfigMenu?string("Yes", "No")}',
					'${mutator.mutator.hasKeybinds?string("Yes", "No")}',
					'${mutatorList}',
					'${weaponsList}',
					'${vehicleList}',
					'${fileSize(mutator.mutator.fileSize)}',
					'${mutator.mutator.originalFilename}',
					'${mutator.mutator.hash}'
			]

      styles={"11": "nomobile"}
      >

			<@meta title="Mutator Information" labels=labels values=values styles=styles/>

			<#if mutator.variations?size gt 0>
				<section class="variations">
					<h2><img src="${staticPath()}/images/icons/variant.svg" alt="Variations"/>Variations</h2>
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

			<@dependencies deps=mutator.mutator.dependencies game=mutator.mutator.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Mutator] ${mutator.mutator.name}" hash="${mutator.mutator.hash}" name="${mutator.mutator.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">
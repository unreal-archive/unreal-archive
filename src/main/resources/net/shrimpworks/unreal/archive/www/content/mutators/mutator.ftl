<#assign game=mutator.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if mutator.item.leadImage?has_content>
    <#assign headerbg=urlEncode(mutator.item.leadImage)>
</#if>

<#assign ogDescription="${mutator.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${mutator.item.name}">
<#assign schemaItemAuthor="${mutator.item.author}">
<#assign schemaItemDate="${mutator.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Mutators</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/</span> ${mutator.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@links links=mutator.item.links/>

        <@screenshots attachments=mutator.item.attachments/>
		</div>

		<div class="info">

			<#assign mutatorList>
				<#list mutator.item.mutators as m>
					<div class="mini-head">${m.name}</div>
					<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
				<#else>
					Unknown
				</#list>
			</#assign>

			<#assign weaponsList>
				<#list mutator.item.weapons as m>
					<div class="mini-head">${m.name}</div>
					<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
        <#else>
					None
				</#list>
			</#assign>

			<#assign vehicleList>
				<#list mutator.item.vehicles as m>
					<div class="mini-head">${m.name}</div>
					<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
        <#else>
					None
				</#list>
			</#assign>

			<#assign author><@authorLink mutator.item.authorName /></#assign>
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
					'${mutator.item.name}',
					'${mutator.item.description}',
					'${author}',
					'${dateFmtShort(mutator.item.releaseDate)}',
					'${mutator.item.hasConfigMenu?string("Yes", "No")}',
					'${mutator.item.hasKeybinds?string("Yes", "No")}',
					'${mutatorList}',
					'${weaponsList}',
					'${vehicleList}',
					'${fileSize(mutator.item.fileSize)}',
					'${mutator.item.originalFilename}',
					'${mutator.item.hash}'
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
								<td><a href="${relPath(v.path + ".html")}">${v.item.name}</a></td>
								<td>${v.item.releaseDate}</td>
								<td>${v.item.originalFilename}</td>
								<td>${fileSize(v.item.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<@files files=mutator.item.files alsoIn=mutator.alsoIn otherFiles=mutator.item.otherFiles/>

			<@downloads downloads=mutator.item.downloads/>

			<@dependencies deps=mutator.item.dependencies game=mutator.item.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Mutator] ${mutator.item.name}" hash="${mutator.item.hash}" name="${mutator.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">
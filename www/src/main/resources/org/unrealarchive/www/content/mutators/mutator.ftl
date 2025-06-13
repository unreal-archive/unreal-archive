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
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Mutators</a>
			/</span> ${mutator.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@links links=mutator.item.links/>

        <@screenshots attachments=mutator.item.attachments/>
		</div>

		<div class="info">

			<#assign author><@authorLink mutator.item /></#assign>
			<#assign
			labels=[
					"Name",
					"Description",
					"Author",
					"Release (est.)",
					"File Size",
					"File Name",
					"SHA1 Hash"
			]

			values=[
					'${mutator.item.name}',
					'${mutator.item.description}',
					'${author}',
					'${dateFmtShort(mutator.item.releaseDate)}',
					'${fileSize(mutator.item.fileSize)}',
					'${mutator.item.originalFilename}',
					'${mutator.item.hash}'
			]

      styles={"6": "nomobile"}
      >

			<@meta title="Mutator Information" labels=labels values=values styles=styles/>

			<@variations variations=mutator.variations/>

			<@contents title="Contents">
				<#assign mutatorList>
					<#list mutator.item.mutators?sort_by("name") as m>
						<div class="mini-head">${m.name}</div>
						<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
					<#else>
						Unknown
					</#list>
				</#assign>

				<#assign weaponsList>
					<#list mutator.item.weapons?sort_by("name") as m>
						<div class="mini-head">${m.name}</div>
						<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
					<#else></#list>
				</#assign>

				<#assign vehicleList>
					<#list mutator.item.vehicles?sort_by("name") as m>
						<div class="mini-head">${m.name}</div>
						<div class="mini-detail">${m.description?replace("|", "<br/>")?no_esc}</div>
					<#else></#list>
				</#assign>

				<#assign
					labels=[
						"Custom Config Menus",
						"Custom Keybinds",
						"Included Mutators",
						"Weapons",
						"Vehicles"
					]
					values=[
						'${mutator.item.hasConfigMenu?string("Yes", "")}',
						'${mutator.item.hasKeybinds?string("Yes", "")}',
						'${mutatorList}',
						'${weaponsList}',
						'${vehicleList}'
					]
				>
				<@labellist labels=labels values=values/>
			</@contents>

			<@downloads downloads=mutator.item.downloads/>

			<@files game=game files=mutator.item.files alsoIn=mutator.alsoIn otherFiles=mutator.item.otherFiles/>

			<@dependencies game=game deps=mutator.item.dependencies/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Mutator] ${mutator.item.name}" hash="${mutator.item.hash}" name="${mutator.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">
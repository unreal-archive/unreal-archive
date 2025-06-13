<#assign game=pack.page.letter.group.game>
<#assign gametype=pack.page.letter.group>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if pack.item.leadImage?has_content>
    <#assign headerbg=urlEncode(pack.item.leadImage)>
</#if>

<#assign ogDescription="${pack.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${pack.item.name}">
<#assign schemaItemAuthor="${pack.item.author}">
<#assign schemaItemDate="${pack.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Map Packs</a>
			/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
			/</span> ${pack.item.name}
	</@heading>

	<@content class="info">

		<div class="side">
			<@links links=pack.item.links/>

			<@screenshots attachments=pack.item.attachments/>
		</div>

		<div class="info">

			<#assign themes><@themes themes=pack.item.themes/></#assign>

			<#assign author><@authorLink pack.item /></#assign>
			<#assign gametypeLink>
				<a href="<#if gameTypeInfoPath??>${relPath(gameTypeInfoPath + "/index.html")}<#else>${relPath(gametype.path + "/index.html")}</#if>">${pack.item.gametype}</a>
			</#assign>

			<#assign
			labels=[
				"Name",
				"Game Type",
				"Maps",
				"Author",
				"Release (est)",
        "Themes",
				"File Size",
				"File Name",
				"SHA1 Hash"
			]

			values=[
				'${pack.item.name}',
      	'${gametypeLink}'?no_esc,
				'${pack.item.maps?size}',
				'${author}',
				'${dateFmtShort(pack.item.releaseDate)}',
        '${themes}',
				'${fileSize(pack.item.fileSize)}',
				'${pack.item.originalFilename}',
				'${pack.item.hash}'
			]

      styles={"8": "nomobile"}
			>

			<@meta title="Map Pack Information" labels=labels values=values styles=styles/>

			<@variations variations=pack.variations/>

			<@contents title="Maps">
				<table>
					<thead>
					<tr>
						<th>Name</th>
						<th class="nomobile">Title</th>
						<th>Author</th>
					</tr>
					</thead>
					<tbody>
          <#list pack.item.maps?sort_by("name") as m>
						<tr>
							<td>${m.name}</td>
							<td class="nomobile">${m.title}</td>
							<td><@authorLink m /></td>
						</tr>
          </#list>
					</tbody>
				</table>
			</@contents>

			<@downloads downloads=pack.item.downloads/>

			<@files game=game files=pack.item.files alsoIn=pack.alsoIn otherFiles=pack.item.otherFiles/>

      <@dependencies game=game deps=pack.item.dependencies/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Map Pack] ${pack.item.name}" hash="${pack.item.hash}" name="${pack.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">
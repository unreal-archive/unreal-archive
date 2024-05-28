<#assign game=announcer.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if announcer.item.leadImage?has_content>
    <#assign headerbg=urlEncode(announcer.item.leadImage)>
</#if>

<#assign ogDescription="${announcer.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${announcer.item.name}">
<#assign schemaItemAuthor="${announcer.item.author}">
<#assign schemaItemDate="${announcer.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Announcers</a>
			/</span> ${announcer.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@links links=announcer.item.links/>

        <@screenshots attachments=announcer.item.attachments/>
		</div>

		<div class="info">

			<#assign author><@authorLink announcer.item.authorName /></#assign>
			<#assign
			labels=[
					"Name",
					"Author",
					"Release (est.)",
					"File Size",
					"File Name",
					"SHA1 Hash"
			]

			values=[
					'${announcer.item.name}',
					'${author}',
					'${dateFmtShort(announcer.item.releaseDate)}',
					'${fileSize(announcer.item.fileSize)}',
					'${announcer.item.originalFilename}',
					'${announcer.item.hash}'
			]

      styles={"5": "nomobile"}
      >

			<@meta title="Announcer Information" labels=labels values=values styles=styles/>

			<@variations variations=announcer.variations/>

			<#if announcer.item.announcers?size gt 0>
			<@contents title="announcers">
				<#assign announcersList><#list announcer.item.announcers?sort as v><div>${v.name}</div><#else>Unknown</#list></#assign>
				<#assign labels=["Included Announcers"] values=['${announcersList}']>
				<@labellist labels=labels values=values/>
			</@contents>
			</#if>

			<@files game=game files=announcer.item.files alsoIn=announcer.alsoIn otherFiles=announcer.item.otherFiles/>

			<@downloads downloads=announcer.item.downloads/>

      <@dependencies game=game deps=announcer.item.dependencies game=announcer.item.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Announcer] ${announcer.item.name}" hash="${announcer.item.hash}" name="${announcer.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">
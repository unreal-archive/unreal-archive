<#if gametype.gametype.bannerImage?? && gametype.gametype.bannerImage?length gt 0>
	<#assign headerbg>${relPath(gametype.path + "/" + gametype.gametype.bannerImage)}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/games/${gametype.game.name}.png</#assign>
</#if>

<#assign ogDescription=gametype.gametype.description>
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(gametype.game.root + "/index.html")}">${gametype.game.name}</a>
			/ <a href="${relPath(gametype.game.path + "/index.html")}">Game Types &amp; Mods</a>
			<#if gametype.variationOf??>
				/ <a href="../../index.html">${gametype.variationOf.name}</a>
			</#if>
			/ <a href="../index.html">${gametype.gametype.name}</a>
			/</span> ${release.title} ${release.version}
	</@heading>

	<@content class="split split7030" id="gametype-release">
		<div class="left">
			<section>
 				<h2><@icon "list"/>${gametype.gametype.name} ${release.title} ${release.version} Files</h2>
				<div>${gametype.gametype.description}</div>
				<ul class="readable">
					<#list release.files as f>
						<#if f.deleted><#continue/></#if>
						<li><a href="#${slug(f.title)}">${f.title}</a></li>
					</#list>
				</ul>
			</section>

      <#list release.files as f>
				<#if f.deleted><#continue/></#if>
				<section>
					<h2 id="${slug(f.title)}">
            <@icon name="os-${f.platform?lower_case}" title="${f.platform}"/>
						${f.title}
					</h2>
					<#assign
            labels=[
            "Title",
            "File Name",
            "File Size",
            "Platform",
            "SHA1 Hash"
            ]

            values=[
            '${f.title}',
            '${f.originalFilename}',
            '${fileSize(f.fileSize)}',
            '${f.platform}',
            '${f.hash}'
            ]
					>

					<@meta title="File Information" labels=labels values=values styles=styles h="h3"/>

          <@downloads downloads=f.downloads h="h3"/>

          <@files game=gametype.game files=f.files alsoIn=gametype.filesAlsoIn[slug(f.originalFilename)] otherFiles=f.otherFiles h="h3"/>

					<@dependencies game=gametype.game deps=f.dependencies h="h3"/>

				</section>
      </#list>
		</div>

		<div class="right">
			<section>
				<h2><@icon "info"/>Release Information</h2>
				<div class="label-value">
					<label>Title</label><span>${release.title}</span>
				</div>
				<div class="label-value">
					<label>Version</label><span>${release.version}</span>
				</div>
				<div class="label-value">
					<label>Release Date</label><span>${release.releaseDate!"-"}</span>
				</div>
				<#if release.description?length gt 0>
					<div class="label-value">
						<label>Description</label><span>${release.description}</span>
					</div>
        </#if>


				<@ghIssue
          text="Report a problem"
          repoUrl="${dataProjectUrl}"
          title="[GameType] ${gametype.gametype.name} release ${release.title}"
          hash="None"
          name="${gametype.gametype.name}"/>

			</section>
		</div>
	</@content>

<#include "../../_footer.ftl">
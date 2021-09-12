<#macro heading bg=[]>
	<#t/><#assign style>
		<#t/><#if bg?? && bg?size gt 0>
			<#t/>style='background-image:
			<#t/><#list bg as b>
			<#t/>url("${b}")
			<#t/><#if b?has_next>,</#if>
			<#t/></#list>
			<#t/>'
		<#t/></#if>
	<#t/></#assign>
	<section class="page header" ${style!""}>
		<div class="page">
			<h1>
				<#nested/>
			</h1>
		</div>
	</section>
</#macro>

<#macro content class="">
	<div class="page contentpage">
		<article <#if class?length gt 0>class="${class}"</#if>>
			<#nested>
		</article>
	</div>
</#macro>

<#macro letterPages letters currentLetter pages currentPage>
	<#if letters?? && letters?size gt 1 && currentLetter??>
		<nav class="letters">
			<#list letters as k, letter><#if letter.count??><#if letter.count gt 0><a href="${relPath(letter.path + "/index.html")}"<#if letter.letter == currentLetter>class="active"</#if>>${letter.letter}</a></#if>
			<#else><a href="${relPath(letter.path + "/index.html")}"<#if letter.letter == currentLetter>class="active"</#if>>${letter.letter}</a></#if></#list>
		</nav>
	</#if>

	<@paginator pages=pages currentPage=currentPage />
</#macro>

<#macro paginator pages currentPage>
	<#if pages?? && pages?size gt 1 && currentPage??>
		<nav class="pages">
			<#list pages as pg><a href="${relPath(pg.path + "/index.html")}" <#if pg.number == currentPage.number>class="active"</#if>>${pg.number}</a></#list>
		</nav>
	</#if>
</#macro>

<#macro screenshots attachments>
	<#if attachments?size == 0>
		<img src="${staticPath()}/images/none.png" class="thumb" alt="no image"/>
	<#else>
		<#list attachments as a>
			<#if a.type == "IMAGE">
				<img src="${urlEncode(a.url)}" class="thumb lb" alt="screenshot"/>
			</#if>
		</#list>
	</#if>
</#macro>

<#macro meta title labels values styles={} h="h2">
	<section class="meta">
		<${h}><img src="${staticPath()}/images/icons/info.svg" alt="Info"/>${title}</${h}>
		<#list labels as l>
			<#if values[l?index]?? && values[l?index]?has_content>
				<div class="label-value <#if styles[l?index?string]??>${styles[l?index?string]}</#if>">
					<label>${l}</label><span>${values[l?index]}</span>
				</div>
			</#if>
		</#list>
	</section>
</#macro>

<#macro files files alsoIn otherFiles h="h2">
	<#if files?size gt 0>
		<section class="files">
			<${h}><img src="${staticPath()}/images/icons/package.svg" alt="Files"/>Packaged Files</${h}>
			<table>
				<thead>
				<tr>
					<th>Name</th>
					<th>Size</th>
					<th class="nomobile">Hash</th>
					<th>Also In</th>
				</tr>
				</thead>
				<tbody>
				<#list files as f>
					<tr>
						<td>${f.name}</td>
						<td>${fileSize(f.fileSize)}</td>
						<td class="nomobile">${f.hash}</td>
						<#if alsoIn[f.hash]??>
							<td>
								<a href="${relPath(siteRoot + "/files/" + f.hash[0..1] + "/" + f.hash + ".html")}">${alsoIn[f.hash]}</a>
							</td>
						<#else>
							<td>-</td>
						</#if>
					</tr>
				</#list>
				</tbody>
			</table>
			<#if otherFiles gt 0>
				<div class="otherFiles">
					<div class="label-value">
						<label>Misc Files</label><span>${otherFiles}</span>
					</div>
				</div>
			</#if>
		</section>
	</#if>
</#macro>

<#macro downloads downloads h="h2">
	<section class="downloads">
		<${h}><img src="${staticPath()}/images/icons/download.svg" alt="Download"/> Downloads</${h}>
		<div class="links">
			<#list downloads as d>
				<#if d.state == 'OK'>
					<a href="${urlEncode(d.url)}" <#if d.main>class="main"</#if>>
						${urlHost(d.url)}
					</a>
				</#if>
			</#list>
		</div>
	</section>
</#macro>

<#macro ghIssue text repoUrl title name hash labels=[]>
	<#outputformat "plainText"><#assign sbody>
		Your description here


		---
		Hash: ${hash}
		Current name: ${name}
	</#assign></#outputformat>
	<#assign stitle><#if title??>${title}</#if></#assign>
	<#assign slabels><#if labels?? && labels?size gt 0>${labels?join(",")}</#if></#assign>
	<#assign url="${repoUrl}/issues/new?title=${stitle}&labels=${slabels}&body=${urlEncode(sbody)}">

	<section class="report">
		<a href="${url}"><img src="${staticPath()}/images/icons/alert.svg" alt="Alert Icon"/> ${text}</a>
	</section>
</#macro>

<#macro dependencies deps h="h2">
	<#if deps?size gt 0>
		<section class="dependencies">
			<${h}><img src="${staticPath()}/images/icons/file-check.svg" alt="Required Files"/> Required Files</${h}>
			<table>
				<thead>
				<tr>
					<th>&nbsp;</th>
					<th align="left">Requires</th>
					<th align="left">Status</th>
					<th>&nbsp;</th>
				</tr>
				</thead>
				<tbody>
				<#list deps as name, value>
					<tr>
						<td colspan="4"><b>${name}</b></td>
					</tr>
					<#list value as dep>
						<tr>
							<td>&nbsp;</td>
							<td>${dep.name}</td>
							<td>${dep.status}</td>
							<td>
								<#if dep.status == "OK">
									File is included
								</#if>
								<#if dep.status == "PARTIAL">
									Included file is missing required content
								</#if>
								<#if dep.status == "MISSING">
									File is not included
								</#if>
							</td>
						</tr>
					</#list>
				</#list>
				</tbody>
			</table>
		</section>
	</#if>
</#macro>

<#macro dependencyIcon deps>
	<#assign status=true/>
	<#list deps as name, value>
		<#list value as dep>
				<#if status && dep.status != "OK">
					<#assign status=false/>
					<#break >
				</#if>
   	</#list>
  </#list>
	<#if status>
		<img src="${staticPath()}/images/icons/file-check.svg" alt="No dependencies" title="No dependency problems" height="22"/>
	<#else>
		<img src="${staticPath()}/images/icons/file-x.svg" alt="Missing dependencies" title="Missing dependencies" height="22"/>
	</#if>
</#macro>

<#macro authorLink author display=author>
	<#if author?lower_case == "unknown" || author?lower_case == "various">
    ${display!author}
	<#else>
		<a href="${relPath(siteRoot + "/authors/" + authorSlug(author) + ".html")}">${display!author}</a>
	</#if>
</#macro>
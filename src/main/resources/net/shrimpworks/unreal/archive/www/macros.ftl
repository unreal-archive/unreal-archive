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
	<section class="header" ${style!""}>
		<div class="page">
			<h1>
				<#nested/>
			</h1>
		</div>
	</section>
</#macro>

<#macro content class="">
	<div class="page">
		<article <#if class?length gt 0>class="${class}"</#if>>
			<#nested>
		</article>
	</div>
</#macro>

<#macro letterPages letters currentLetter pages currentPage>
	<#if letters?? && letters?size gt 1 && currentLetter??>
		<nav class="letters">
			<#list letters as k, letter><a href="${relPath(letter.path + "/index.html")}"<#if letter.letter == currentLetter>class="active"</#if>>${letter.letter}</a></#list>
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
				<img src="${urlEncode(a.url)}" class="thumb" alt="screenshot"/>
			</#if>
		</#list>
	</#if>
</#macro>

<#macro meta title labels values styles={}>
	<section class="files">
		<h2><img src="${staticPath()}/images/icons/black/px22/info.png" alt="Info"/>${title}</h2>
		<#list labels as l>
			<#if values[l?index]??>
				<div class="label-value <#if styles[l?index?string]??>${styles[l?index?string]}</#if>">
					<label>${l}</label><span>${values[l?index]}</span>
				</div>
			</#if>
		</#list>
	</section>
</#macro>

<#macro files files alsoIn otherFiles>
	<section class="files">
		<h2><img src="${staticPath()}/images/icons/black/px22/package.png" alt="Files"/>Packaged Files</h2>
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
</#macro>

<#macro downloads downloads>
	<section class="downloads">
		<h2><img src="${staticPath()}/images/icons/black/px22/download.png" alt="Download"/> Downloads</h2>
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
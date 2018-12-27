<#macro heading bg=[]>
	<#assign style>
		<#t/><#if bg?? && bg?size gt 0>
			<#t/>style='background-image:
			<#t/><#list bg as b>
			<#t/>url("${b}")
			<#t/><#if b?has_next>,</#if>
			<#t/></#list>
			<#t/>'
		<#t/></#if>
	</#assign>
	<section class="header" ${style!""}>
		<h1>
			<#nested/>
		</h1>
	</section>
</#macro>

<#macro content class>
	<article class="${class}">
		<#nested>
	</article>
</#macro>

<#macro screenshots attachments>
	<#if attachments?size == 0>
		<img src="${static}/images/none.png" class="thumb"/>
	<#else>
		<#list attachments as a>
			<#if a.type == "IMAGE">
				<img src="${urlEncode(a.url)}" class="thumb"/>
			</#if>
		</#list>
	</#if>
</#macro>

<#macro files files alsoIn otherFiles>
	<section class="files">
		<h2>Packaged Files</h2>
		<table>
			<thead>
			<tr>
				<th>Name</th>
				<th>Size</th>
				<th>Hash</th>
				<th>Also In</th>
			</tr>
			</thead>
			<tbody>
			<#list files as f>
				<tr>
					<td>${f.name}</td>
					<td>${fileSize(f.fileSize)}</td>
					<td>${f.hash}</td>
					<#if alsoIn[f.hash]??>
						<td>
							<a href="${relUrl(siteRoot + "/../", "files/" + f.hash[0..1] + "/" + f.hash + ".html")}">${alsoIn[f.hash]}</a>
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
		<h2>Downloads</h2>
		<div class="links">
			<#list downloads as d>
				<#if !d.deleted>
					<#if d.main>
						<a href="${urlEncode(d.url)}" class="main">Primary</a>
					<#else>
						<a href="${urlEncode(d.url)}" class="secondary">${urlHost(d.url)}</a>
					</#if>
				</#if>
			</#list>
		</div>
	</section>
</#macro>
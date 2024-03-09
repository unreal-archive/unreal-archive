<#macro heading bg=[]>
	<#compress>
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
		<div class="cover">
			<div class="page">
				<h1>
					<#nested/>
				</h1>
			</div>
		</div>
	</section>
  </#compress>
</#macro>

<#macro content class="" id="content">
	<div class="page contentpage">
		<article id="${id}" <#if class?length gt 0>class="${class}"</#if>>
			<#nested>
		</article>
	</div>
</#macro>

<#macro letterPages letters currentLetter pages currentPage>
	<#compress>
	<#if letters?? && letters?size gt 1 && currentLetter??>
		<nav class="letters">
			<#list letters as k, letter><#if letter.count??><#if letter.count gt 0><a href="${relPath(letter.path + "/index.html")}"<#if letter.letter == currentLetter>class="active"</#if>>${letter.letter}</a></#if>
			<#else><a href="${relPath(letter.path + "/index.html")}"<#if letter.letter == currentLetter>class="active"</#if>>${letter.letter}</a></#if></#list>
		</nav>
	</#if>

	<@paginator pages=pages currentPage=currentPage />
  </#compress>
</#macro>

<#macro paginator pages currentPage>
	<#compress>
	<#if pages?? && pages?size gt 1 && currentPage??>
		<nav class="pages">
			<#list pages as pg><a href="${relPath(pg.path + "/index.html")}" <#if pg.number == currentPage.number>class="active"</#if>>${pg.number}</a></#list>
		</nav>
	</#if>
  </#compress>
</#macro>

<#macro screenshots attachments>
	<#compress>
	<div class="screenshots">
		<#if attachments?size == 0>
			<img src="${staticPath()}/images/none.png" class="thumb nomobile" alt="no image"/>
		<#else>
			<#list attachments as a>
				<#if a.type == "IMAGE">
					<img src="${urlEncode(a.url)}" class="thumb lb" alt="screenshot"/>
				</#if>
			</#list>
		</#if>
	</div>
  </#compress>
</#macro>

<#macro problems problems>
  <@links links=problems title="Problem Reports" icon="alert-triangle" class="problem"
	  info="Users have reproted issues or problems using this content at the links below."/>
</#macro>

<#macro links links title="Links" h="h2" icon="link" class="links" info="">
	<#compress>
	<#if links?? && links?size gt 0>
		<section class="${class}">
			<${h}><img src="${staticPath()}/images/icons/${icon}.svg" alt="Link"/>${title}</${h}>
		  <#if info?? && info?length gt 0><span>${info}</span></#if>
		  <ul>
				<#list links as name, url>
					<li><a href="${url}">${name}</a></li>
				</#list>
			</ul>
		</section>
	</#if>
  </#compress>
</#macro>

<#macro labellist labels values styles={}>
    <#compress>
        <#list labels as l>
            <#if values[l?index]?? && values[l?index]?has_content>
                <#if values[l?index]?is_markup_output && values[l?index]?markup_string?trim == "None">
                    <#continue/>
                <#elseif values[l?index]?is_string && values[l?index]?trim == "None">
                    <#continue/>
                </#if>
							<div class="label-value <#if styles[l?index?string]??>${styles[l?index?string]}</#if>">
								<label>${l}</label><span>${values[l?index]}</span>
							</div>
            </#if>
        </#list>
    </#compress>
</#macro>

<#macro meta title labels values styles={} h="h2">
	<#compress>
	<section class="meta">
		<${h}><img src="${staticPath()}/images/icons/info.svg" alt="Info"/>${title}</${h}>
		<@labellist labels=labels values=values styles=styles/>
	</section>
  </#compress>
</#macro>

<#macro contents title h="h2">
	<#compress>
	<section class="contents">
		<${h}><img src="${staticPath()}/images/icons/list.svg" alt="Contents"/>${title}</${h}>
		<#nested>
	</section>
	</#compress>
</#macro>

<#macro files files alsoIn otherFiles h="h2">
	<#compress>
	<#if files?size gt 0>
		<section class="files">
			<${h}><img src="${staticPath()}/images/icons/package.svg" alt="Files"/>Packaged Files</${h}>
			<table>
				<thead>
				<tr>
					<th>Name</th>
					<th>Size</th>
					<th class="nomobile">Hash</th>
					<th class="nomobile">Also In</th>
				</tr>
				</thead>
				<tbody>
				<#list files?sort_by("name") as f>
					<tr>
						<td>${f.name}</td>
						<td>${fileSize(f.fileSize)}</td>
						<td class="nomobile">${f.hash}</td>
						<td class="nomobile">
						  <#if alsoIn[f.hash]??>
							<a href="${relPath(siteRoot + "/files/" + f.hash[0..1] + "/" + f.hash + ".html")}">${alsoIn[f.hash]}</a>
							<#else>
							-
							</#if>
						</td>
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
  </#compress>
</#macro>

<#macro downloads downloads h="h2">
	<#compress>
	<section class="downloads">
		<${h}><img src="${staticPath()}/images/icons/download.svg" alt="Download"/> Download Mirrors</${h}>
		<div class="links">
			<#list downloads as d>
				<#if d.state == 'OK'>
					<a href="${urlEncode(d.url)}">${urlHost(d.url)}</a>
				</#if>
			</#list>
		</div>
	</section>
  </#compress>
</#macro>

<#macro variations variations h="h2">
	<#compress>
	<#if variations?size gt 0>
		<section class="variations">
			<${h}><img src="${staticPath()}/images/icons/variant.svg" alt="Variations"/>Variations</${h}>
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
				<#list variations as v>
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
  </#compress>
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
		<a href="${url}" id="r_${hash}"><img src="${staticPath()}/images/icons/alert.svg" alt="Alert Icon"/> ${text}</a>
		<script>
			const l = document.getElementById("r_${hash}"); l.href = l.href.replace('---', '---%0A		URL: ' + encodeURI(document.location.href));
		</script>
	</section>
</#macro>

<#macro dependencies deps h="h2" game="">
	<#compress>
	<#if deps?size gt 0>
		<section class="dependencies">
			<${h}><img src="${staticPath()}/images/icons/file-check.svg" alt="Required Files"/> Required Files</${h}>
			<table>
				<thead>
				<tr>
					<th>&nbsp;</th>
					<th align="left">Requires</th>
					<th align="left">Status</th>
					<th class="nomobile">&nbsp;</th>
				</tr>
				</thead>
				<tbody>
				<#list deps?keys?sort as name>
					<tr>
						<td colspan="4">
							<b>
								<#if game??>
									<a href="${relPath(siteRoot + "/packages/" + slug(game) + "/" + slug(plainName(name)) + "/index.html")}">${name}</a>
								<#else>
									${name}
								</#if>
							</b>
						</td>
					</tr>
					<#list deps[name]?sort_by('name') as dep>
						<tr>
							<td>&nbsp;</td>
							<#if game??>
								<td><a href="${relPath(siteRoot + "/packages/" + slug(game) + "/" + slug(dep.name) + "/index.html")}">${dep.name}</a></td>
							<#else>
								<td>${dep.name}</td>
							</#if>
							<td>${dep.status}</td>
							<td class="nomobile">
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
  </#compress>
</#macro>

<#macro dependencyIcon deps>
	<#compress>
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
  </#compress>
</#macro>

<#macro authorLink author display=author>
	<#compress>
	<#if author?lower_case == "unknown" || author?lower_case == "various">
    ${display!author}
	<#else>
		<a href="${relPath(siteRoot + "/authors/" + authorSlug(author) + ".html")}">${display!author}</a>
	</#if>
  </#compress>
</#macro>

<#macro themes themes>
	<#compress>
	<#if themes?size gt 0>
		<#list themes as theme, weight>
			<div class="themes">
					<#if weight lt 0.2>
						<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
							<#list 0..3 as n>
								<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
							</#list>
					<#elseif weight lt 0.4>
							<#list 0..1 as n>
								<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
							</#list>
							<#list 0..2 as n>
								<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
							</#list>
					<#elseif weight lt 0.6>
							<#list 0..2 as n>
								<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
							</#list>
							<#list 0..1 as n>
								<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
							</#list>
					<#elseif weight lt 0.8>
							<#list 0..3 as n>
								<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
							</#list>
						<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
					<#else>
							<#list 0..4 as n>
								<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
							</#list>
					</#if>
				<span>${theme}</span>
			</div>
		</#list>
	</#if>
  </#compress>
</#macro>

<#macro tline timeline game activeYear=0 activeMonth=0>
	<#compress>
	<div id="timeline" class="page nomobile">
		<#assign tlMax=0 />
		<#list timeline as year, months>
			<#list months as month, count>
				<#if count gt tlMax>
					<#assign tlMax=count />
				</#if>
			</#list>
		</#list>
		<#list timeline as year, months>
			<a href="${relPath(game.path + "/releases/${year?c}/index.html")}" class="year <#if year == activeYear>active</#if>">
				<div class="months">
					<#list months as month, count>
						<div class="month <#if month == activeMonth && year == activeYear>active</#if>" style="--max:${tlMax?c}; --cnt:${count?c}" title="${year?c}-${month?c}: ${count}"></div>
					</#list>
				</div>
				<div class="label">${year?c}</div>
			</a>
		</#list>
	</div>
  </#compress>
</#macro>
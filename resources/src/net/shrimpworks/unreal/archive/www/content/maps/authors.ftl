<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading>
		<a href="${siteRoot}/index.html">Maps</a>
		/ Authors
	</@heading>

	<@content class="biglist">
		<ul>
		<#list authors.authors as k, v>
			<li><a href="${v.slug}.html">${v.name}</a></li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">
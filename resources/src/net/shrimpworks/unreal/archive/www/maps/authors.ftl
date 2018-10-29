<#include "../_header.ftl">

	<section class="header">
		<h1>
			Authors
		</h1>
	</section>
	<article class="biglist">
		<ul>
		<#list authors.authors as k, v>
			<li><a href="${v.slug}.html">${v.name}</a></li>
		</#list>
		</ul>
	</article>

<#include "../_footer.ftl">
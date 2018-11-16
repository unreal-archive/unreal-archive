<#include "../_header.ftl">

	<section class="header">
		<h1>
			${title}
		</h1>
	</section>
	<article class="biglist">
		<ul>
		<#list group.groups as k, g>
			<li style='background-image: url("${static}/images/games/${g.name}.png")'>
				<span class="meta">${g.docs}</span>
				<#if g.parent??>
					<a href="${relUrl(g.parent.path, g.path + "/index.html")}">${g.name}</a>
				<#else>
					<a href="${g.path}/index.html">${g.name}</a>
				</#if>
			</li>
		</#list>
		</ul>
	</article>

<#include "../_footer.ftl">
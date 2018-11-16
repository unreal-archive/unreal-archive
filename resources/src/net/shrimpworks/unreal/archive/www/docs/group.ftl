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
				<a href="${relUrl(group.path, g.path + "/index.html")}">${g.name}</a>
			</li>
		</#list>
		</ul>
	</article>

<#include "../_footer.ftl">
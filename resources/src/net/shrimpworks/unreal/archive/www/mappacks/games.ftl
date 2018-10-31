<#include "../_header.ftl">

	<section class="header">
		<h1>
			Map Packs
		</h1>
	</section>
	<article class="biglist">
		<ul>
		<#list games.games as k, v>
			<li style='background-image: url("${static}/images/games/${v.name}.png")'>
				<span class="meta">${v.packs}</span>
				<a href="${v.path}/index.html">${v.name}</a>
			</li>
		</#list>
		</ul>
	</article>

<#include "../_footer.ftl">
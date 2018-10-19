<#include "_header.ftl">

	<section class="header">
		<h1>
		${page.letter.gametype.game.name} / ${page.letter.gametype.name} / ${page.letter.letter} / pg ${page.number}
		</h1>
	</section>
	<article>
		<ul>
		<#list page.maps as m>
			<li><a href="${relUrl(page.path, m.path + ".html")}">${m.map.title}</a></li>
		</#list>
		</ul>
	</article>

<#include "_footer.ftl">
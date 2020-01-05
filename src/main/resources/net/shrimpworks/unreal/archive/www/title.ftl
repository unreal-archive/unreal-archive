<header>
	<div class="page">
		<div class="heading">
			<a href="${relPath(siteRoot + "/index.html")}" class="header">
				<img src="${staticPath()}/images/logo.png" alt="Unreal Archive"/>
				<span class="a">UNREAL</span><span class="b">ARCHIVE</span>
			</a>
			<div class="burger">
				<label for="hamburger">&#9776;</label>
			</div>
			<div class="menu">
				<input type="checkbox" id="hamburger"/>
				<ul>
					<#if features??>
							<#if features.search><li><a href="${relPath(siteRoot + "/search/index.html")}">Search</a></li></#if>
							<#if features.latest><li><a href="${relPath(siteRoot + "/latest/index.html")}">Latest Additions</a></li></#if>
							<#if features.submit><li><a href="${relPath(siteRoot + "/submit/index.html")}">Submit Content</a></li></#if>
					</#if>
				</ul>
			</div>
		</div>
	</div>
</header>
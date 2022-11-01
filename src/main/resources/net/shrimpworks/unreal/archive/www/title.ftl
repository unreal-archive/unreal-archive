<header>
	<div class="page">
		<div class="heading">
			<a href="/index.html" class="header">
				<img src="${staticPath()}/images/logo.png" alt="Unreal Archive" width="80" height="80"/>
				<span class="a">UNREAL</span><span class="b">ARCHIVE</span>
			</a>
			<div class="burger">
				<label for="hamburger">&#9776;</label>
			</div>
			<div class="menu">
				<input type="checkbox" id="hamburger"/>
				<ul>
					<#if features??>
							<#if features.search><li><a href="${relPath(siteRoot + "/search/index.html")}"><img src="${staticPath()}/images/icons/search.svg" alt="Search"/> Search</a></li></#if>
							<#if features.latest><li><a href="${relPath(siteRoot + "/latest/index.html")}"><img src="${staticPath()}/images/icons/bulb.svg" alt="Bulb"/> Latest Additions</a></li></#if>
							<#if features.submit><li><a href="${relPath(siteRoot + "/submit/index.html")}"><img src="${staticPath()}/images/icons/upload.svg" alt="Upload"/> Submit Content</a></li></#if>
					</#if>
				</ul>
			</div>
		</div>
	</div>
</header>

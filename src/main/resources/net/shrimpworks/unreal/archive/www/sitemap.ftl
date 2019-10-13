<#ftl output_format="XML" auto_esc=false>
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
<#list pages as p>
	<url>
		<loc>${rootUrl}${relPath(p.path)}</loc>
		<lastmod>${p.lastMod}</lastmod>
		<changefreq>${p.changeFreq}</changefreq>
		<priority>${p.priority?string["0.##"]}</priority>
	</url>
</#list>
</urlset>
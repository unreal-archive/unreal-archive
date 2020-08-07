<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">\
	<channel>
		<title>Unreal Archive</title>
		<description>Downloads and guides for maps, mutators, skins, voices, models and mods, for the original classic Unreal, Unreal Tournament (UT99), and Unreal Tournament 2004 (UT2004) games</description>
		<link>${siteUrl}</link>
		<atom:link href="${siteUrl}/latest/feed.xml" rel="self" type="application/rss+xml"/>
		<image>${staticPath()}/images/logo.png</image>

		<pubDate>${.now?string["EEE, d MMM yyyy HH:mm:ss Z"]}</pubDate>
		<lastBuildDate>${.now?string["EEE, d MMM yyyy HH:mm:ss Z"]}</lastBuildDate>
		<generator>Unreal Archive</generator>

		<#assign count=0>
		<#list latest as date, content>
      <#list content as c>

				<#list c.attachments as a>
					<#if a.type == "IMAGE">
						<#assign titleimg=urlEncode(a.url)>
						<#break>
					</#if>
				</#list>

				<item>
					<title>${c.game} ${c.friendlyContentType()} - ${c.name}</title>
					<#if titleimg??>
						<enclosure url="${titleimg}" type="image/png" />
					</#if>
					<description>
							${c.autoDescription()}
					</description>
					<pubDate>${c.firstIndex}</pubDate>
					<link>${siteUrl}${relPath(c.slugPath(siteRoot))[2..]}.html</link>
					<guid isPermaLink="true">${siteUrl}${relPath(c.slugPath(siteRoot))[2..]}.html</guid>
				</item>

				<#assign count++>
			</#list>
			<#if count gt 10><#break></#if>
		</#list>

	</channel>
</rss>

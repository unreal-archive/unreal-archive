<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading>
	<span class="crumbs">
		Collections
		/</span> Editor
</@heading>

<@content class="collections-editor">

	<section class="list">
		<h2><@icon "info"/>About</h2>
		<p>
			This is where you manage Collections you've created but not yet submitted.
		</p>
		<p>
			How it works:
			<ul>
			<li>Create a new Collection using the <b>New Collection</b> button.</li>
				<li>Browse the site for the items you want to add to your collection.</li>
				<li>If you have any unsubmitted collections, an "Add to Collection" option will be available on all pages.</li>
				<li>Return to the <b>Collections Editor</b> (this page) to review and <b>Submit</b> collections.</li>
				<li>Saving will create a Pull Request on the
					<a href="https://github.com/unreal-archive/unreal-archive-data">Unreal Archive Data repository</a>, and
					once merged, will be available on the <a href="./index.html">Collections page</a>.</li>
			  <li>Note that these work-in-progress Collections only exist in your browser; if you use a private browser window or clear
					history, your changes will be lost.</li>
			</ul>
		</p>
		<p>
			<button id="new-collection-btn" type="button"><@icon "file-plus"/> New Collection</button>
		</p>
		<h2><@icon "collection"/>Work-in-progress Collections</h2>
		<div id="local-collections-root">
			<p>Nothing here yet.</p>
		</div>
	</section>

</@content>

<#include "../_footer.ftl">

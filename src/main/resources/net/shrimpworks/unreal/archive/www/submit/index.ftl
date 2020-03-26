<#assign extraCss="submit.css?v=3"/>
<#include "../_header.ftl">
<#include "../macros.ftl">

<@content class="readable">

	<h1>
		Submit Content
	</h1>

	<div id="form">
		<div id="upload-block">
			<div id="upload-controls" class="display-block">
				<h2>Choose Files to Submit</h2>

				<!--				<input type="radio" name="type" value="upload" id="rupload" checked>-->
				<!--				<label for="rupload">Upload Files From your Computer</label>-->

				<!--				<input type="radio" name="type" value="url" id="rurl">-->
				<!--				<label for="rurl">Submit via a URL</label>-->

				<!--				<hr />-->

				<div id="upload" class="display-block">
					<input type="file" id="files" accept=".zip,.rar,.7z,.ace,.gz,.bz2,.tar,.tgz,.exe,.umod,.ut2mod,.ut4mod" multiple style="display:none">
					<button id="select-files"><img src="${staticPath()}/images/icons/file-plus.svg" alt="Add files"/> Select Files</button>
					<button id="upload-files"><img src="${staticPath()}/images/icons/upload.svg" alt="Upload"/> Upload!</button>
				</div>

				<div id="url">
					<input type="text" id="link" placeholder="paste link here"/>
					<button id="upload-url"><img src="${staticPath()}/images/icons/upload.svg" alt="Upload"/> Submit!</button>
				</div>
			</div>

			<div id="progress-controls">
				<button id="abort">Cancel Upload</button>
				<progress id="progress" value="0" max="100" style="width: 100%"></progress>
			</div>

			<div id="files-list">
				<h2>Selected Files</h2>
			</div>
		</div>

		<div id="info-block">
			<div id="words" class="display-block">
				<h2>Information</h2>
				<p>
					To upload files, you may select one or more <b>.zip, .rar, .7z,
						.ace, .gz, .bz2, .tar, .tgz, .exe, .umod, ut2mod or .ut4mod</b>
					files to upload in a batch.
				</p>
				<p>
					Files you submit will be added to a queue and processed as in
					order of submission, so on occasion <b>you may need to wait a
						while</b> before your files are processed.
				</p>
				<p>
					If processed successfully, new submissions will be indexed, hosted
					and listed on the website as soon as it has been <b>reviewed and
						merged</b> into
					<a href="https://github.com/unreal-archive/unreal-archive-data">
						the main content archive
					</a>.
				</p>
				<p>
					<b>Tip:</b> If you're submitting your own content, remember to make
					sure the Author is set correctly. For maps, this is set in the
					LevelInfo of the map itself, for all other content, add a ReadMe
					file and put something like <b>Author: Your Name</b> in it so it
					can be correctly attributed to you.
				</p>
				<p>
					<b>Tip:</b> Make sure all required files to use your content are
					included in the uploaded archive, this includes textures, sounds,
					music, etc.
				</p>
				<p>
					<b>Tip:</b> Only submit completed work, rather than work-in-progress
					builds.
				</p>
			</div>

			<div id="log">
				<h2>Processing Status</h2>
			</div>
		</div>
	</div>

</@content>

<script type="application/javascript">
	let url = "../incoming/";

	document.addEventListener("DOMContentLoaded", function() {

		let selectFilesButton = document.querySelector('#select-files');
		let fileSelector = document.querySelector('#files');
		let uploadFilesButton = document.querySelector('#upload-files');
		let uploadUrlButton = document.querySelector('#upload-url');
		let abortButton = document.querySelector('#abort');

		let filesList = document.querySelector('#files-list');
	  let logList = document.querySelector('#log');
		let infoBlurb = document.querySelector('#words');

		let uploadControls = document.querySelector('#upload-controls');
		let progressControls = document.querySelector('#progress-controls')

		// let uploadFiles = document.querySelector('#upload');
		// let uploadUrl = document.querySelector('#url');

		let currentRequest = null;

		selectFilesButton.addEventListener('click', () => {
			fileSelector.click();
		});

		uploadFilesButton.addEventListener('click', () => {
			upload();
		});

		uploadUrlButton.addEventListener('click', () => {
			alert("lol");
		});

		abortButton.addEventListener('click', () => {
			toggleProgress(false);
			if (currentRequest) {
				currentRequest.abort();
				currentRequest = null;
			}
		});

		if (location.hash) {
			toggleProgress(true);
			abortButton.innerText = "Upload Another";
			pollJob(location.hash.substring(1), true);
		}

		// document.querySelectorAll('input[name=type]').forEach(e => e.addEventListener('change', () => {
		//   if (document.querySelector('#rupload').checked) {
		// 	  uploadFiles.classList.add("display-block");
		// 	  uploadUrl.classList.remove("display-block");
		//   } else {
		// 	  uploadFiles.classList.remove("display-block");
		// 	  uploadUrl.classList.add("display-block");
		//   }
		// }));

		function resetFilesList() {
			while (filesList.childNodes.length > 0) filesList.removeChild(filesList.childNodes[0]);
			let header = document.createElement("h2");
			header.innerText = "Selected Files";
			filesList.append(header);
		}

		function resetLog() {
			while (logList.childNodes.length > 0) logList.removeChild(logList.childNodes[0]);
			let header = document.createElement("h2");
			header.innerText = "Processing Status";
			logList.append(header);
		}

		fileSelector.addEventListener('change', e => {
			resetFilesList();

			for (let i = 0; i < e.target.files.length; i++) {
				let f = e.target.files[i];
				let name = document.createElement("div");
				name.innerText = f.name;
				name.classList.add("name");
				let size = document.createElement("div");
				size.innerText = (f.size / 1024 / 1024).toFixed(1) + " mb";
				size.classList.add("size");
				let row = document.createElement("div");
				row.classList.add("file");
				row.append(name, size);
				filesList.append(row);
			}

			if (!filesList.classList.contains("display-block")) filesList.classList.add("display-block");
		});

		function upload() {
			currentRequest = new XMLHttpRequest();
			let data = new FormData();

			let files = fileSelector.files;
			console.log("uploading", files);
			for (let i = 0; i < files.length; i++) {
				data.append('files', files[i]);
			}

			currentRequest.addEventListener('load', e => {
				abortButton.innerText = "Upload Another";

				history.pushState(null, document.title, '#' + e.target.response.toString());
				pollJob(e.target.response)
			});

			currentRequest.upload.addEventListener('progress', e => {
				document.querySelector('#progress').value = Math.round((e.loaded / e.total) * 100);
			});

			currentRequest.responseType = 'json';

			// Send POST request to the server side script
			currentRequest.open('post', url + 'upload');

			toggleProgress(true);
			abortButton.innerText = "Cancel Upload";

			currentRequest.send(data);
		}

		function toggleProgress(progressing) {
			if (progressing) {
				if (!logList.classList.contains("display-block")) logList.classList.add("display-block");
				if (!progressControls.classList.contains("display-block")) progressControls.classList.add("display-block");
		  	if (uploadControls.classList.contains("display-block")) uploadControls.classList.remove("display-block");
		  	if (infoBlurb.classList.contains("display-block")) infoBlurb.classList.remove("display-block");
				resetLog();
			} else {
				if (logList.classList.contains("display-block")) logList.classList.remove("display-block");
				if (progressControls.classList.contains("display-block")) progressControls.classList.remove("display-block");
				if (!uploadControls.classList.contains("display-block")) uploadControls.classList.add("display-block");
				if (filesList.classList.contains("display-block")) filesList.classList.remove("display-block");
		  	if (!infoBlurb.classList.contains("display-block")) infoBlurb.classList.add("display-block");
				history.pushState(null, document.title, '#');
			}
		}

		function pollJob(jobId, catchup = false) {
			if (jobId !== location.hash.substring(1)) return; // stop polling if we're not looking at this job

			fetch(url + 'job/' + jobId.toString() + (catchup ? '?catchup=1' : ''))
				.then(result => {
					if (result.ok) {
						result.json().then(json => {
							json.forEach(l => pushLog(l));
						});
						pollJob(jobId);
					} else {
						pushLog(result.statusText);
					}
				})
		}

		function pushLog(event) {
			let time = document.createElement("div");
			time.classList.add('time');
			let log = document.createElement("div");
			log.classList.add('message');

			let logRow = document.createElement("div");
			logRow.classList.add('log');

			let dateStamp;

			if (event instanceof String) {
				dateStamp = new Date();
				log.innerText = event;
				logRow.classList.add("error")
			} else {
				dateStamp = new Date(event.time);
				log.innerHTML = linkify(event.message);
				logRow.classList.add(event.type.toLowerCase())
			}

			time.innerText = '[' + dateStamp.toLocaleTimeString() + ']';

			logRow.append(time, log);

			if (!logList.classList.contains("display-block")) logList.classList.add("display-block");
			logList.append(logRow);
		}

		function linkify(message) {
			let linkPattern = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
			return message.replace(linkPattern, '<a href="$1" target="_blank">$1</a>');
		}
	});

</script>

<#include "../_footer.ftl">
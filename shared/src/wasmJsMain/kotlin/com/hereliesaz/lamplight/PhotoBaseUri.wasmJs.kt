package com.hereliesaz.lamplight

// A plain relative URL: CI copies the generated photo tree next to the deployed wasmJs
// bundle (see build-and-release.yml's build-web job), so this resolves against whatever
// origin/path the page itself is served from.
actual fun photoBaseUri(): String = "photos/"

package com.hereliesaz.lamplight

/**
 * Where bundled venue photo binaries live, as a URI/URL prefix a place id and filename can be
 * appended to directly. Deliberately not routed through Compose resources like the CSV/JSON
 * data: `Res.readBytes` is `suspend` to accommodate web's `fetch()`, a poor fit for up to
 * ~2,095 JPEGs, and would bloat the wasmJs bundle for no benefit.
 */
expect fun photoBaseUri(): String

package com.treasure.core.domain

import kotlinx.serialization.Serializable

/** A single line in the "hero specs" strip on the Detail screen. */
@Serializable
data class HeroSpec(val label: String, val value: String)

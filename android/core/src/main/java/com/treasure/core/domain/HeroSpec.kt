package com.treasure.core.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** A single line in the "hero specs" strip on the Detail screen. */
@Immutable
@Serializable
data class HeroSpec(val label: String, val value: String)

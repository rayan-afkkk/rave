package com.rave.app.domain.habit

import com.rave.app.domain.model.PbTag

/** Best marks for one activity type. Null = no previous value. */
data class PersonalBests(
    val fastest1kS: Double? = null,
    val fastest5kS: Double? = null,
    val longestM: Double? = null,
)

/** The marks set by a newly finished activity. */
data class ActivityMarks(
    val fastest1kS: Double?,
    val fastest5kS: Double?,
    val distanceM: Double,
)

/**
 * A PB needs a previous value to beat, so a user's very first activity isn't spammed with tags.
 * Distances under 1 km never count for "longest".
 */
object PbDetector {
    fun detect(previous: PersonalBests, new: ActivityMarks): Set<PbTag> = buildSet {
        if (new.fastest1kS != null && previous.fastest1kS != null && new.fastest1kS < previous.fastest1kS) {
            add(PbTag.FASTEST_1K)
        }
        if (new.fastest5kS != null && previous.fastest5kS != null && new.fastest5kS < previous.fastest5kS) {
            add(PbTag.FASTEST_5K)
        }
        if (new.distanceM >= 1_000 && previous.longestM != null && new.distanceM > previous.longestM) {
            add(PbTag.LONGEST_DISTANCE)
        }
    }
}

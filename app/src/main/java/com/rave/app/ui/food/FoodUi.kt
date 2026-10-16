package com.rave.app.ui.food

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rave.app.R
import com.rave.app.domain.food.Allergen
import com.rave.app.domain.food.FoodGoal
import com.rave.app.domain.food.FoodReason
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.food.ReasonType
import com.rave.app.domain.food.Swap
import com.rave.app.domain.food.Verdict
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor

@Composable
fun verdictColors(v: Verdict): TileColor = when (v) {
    Verdict.GO -> RaveTheme.colors.verdictGo
    Verdict.SOMETIMES -> RaveTheme.colors.verdictSometimes
    Verdict.SKIP -> RaveTheme.colors.verdictSkip
}

fun verdictIcon(v: Verdict): ImageVector = when (v) {
    Verdict.GO -> Icons.Rounded.CheckCircle
    Verdict.SOMETIMES -> Icons.Rounded.Info
    Verdict.SKIP -> Icons.Rounded.Block
}

@Composable
fun verdictTitle(v: Verdict) = stringResource(
    when (v) {
        Verdict.GO -> R.string.verdict_go
        Verdict.SOMETIMES -> R.string.verdict_sometimes
        Verdict.SKIP -> R.string.verdict_skip
    },
)

@Composable
fun verdictBody(v: Verdict) = stringResource(
    when (v) {
        Verdict.GO -> R.string.verdict_go_body
        Verdict.SOMETIMES -> R.string.verdict_sometimes_body
        Verdict.SKIP -> R.string.verdict_skip_body
    },
)

private fun g(v: Double?) = v?.let { if (it >= 10) "%.0f".format(it) else "%.1f".format(it) } ?: "?"

@Composable
fun reasonText(r: FoodReason): String = when (r.type) {
    ReasonType.HIGH_SUGAR -> stringResource(R.string.reason_high_sugar, g(r.amount))
    ReasonType.HIGH_SALT -> stringResource(R.string.reason_high_salt, g(r.amount))
    ReasonType.HIGH_SAT_FAT -> stringResource(R.string.reason_high_sat_fat, g(r.amount))
    ReasonType.HIGH_FAT -> stringResource(R.string.reason_high_fat, g(r.amount))
    ReasonType.CALORIE_DENSE -> stringResource(R.string.reason_calorie_dense, g(r.amount))
    ReasonType.ULTRA_PROCESSED -> stringResource(R.string.reason_ultra_processed)
    ReasonType.POOR_NUTRISCORE -> stringResource(R.string.reason_poor_nutriscore, r.text.orEmpty())
    ReasonType.MANY_ADDITIVES -> stringResource(R.string.reason_many_additives, r.amount?.toInt() ?: 0)
    ReasonType.CONTAINS_YOUR_ALLERGEN -> stringResource(R.string.reason_your_allergen, r.text.orEmpty())
    ReasonType.NOT_HALAL -> stringResource(R.string.reason_not_halal, r.text.orEmpty())
    ReasonType.GOOD_PROTEIN -> stringResource(R.string.reason_good_protein, g(r.amount))
    ReasonType.GOOD_FIBER -> stringResource(R.string.reason_good_fiber, g(r.amount))
    ReasonType.LOW_SUGAR -> stringResource(R.string.reason_low_sugar, g(r.amount))
    ReasonType.GOOD_NUTRISCORE -> stringResource(R.string.reason_good_nutriscore, r.text.orEmpty())
    ReasonType.MINIMALLY_PROCESSED -> stringResource(R.string.reason_minimally_processed)
}

@Composable
fun swapText(s: Swap) = stringResource(
    when (s) {
        Swap.ROASTED_CHANA -> R.string.swap_roasted_chana
        Swap.LEMON_WATER -> R.string.swap_lemon_water
        Swap.NUTS_OR_FRUIT -> R.string.swap_nuts_fruit
        Swap.DATES -> R.string.swap_dates
        Swap.CHANA_CHAAT -> R.string.swap_chana_chaat
        Swap.FRUIT_CHAAT -> R.string.swap_fruit_chaat
        Swap.PLAIN_YOGURT -> R.string.swap_plain_yogurt
        Swap.AIR_POPCORN -> R.string.swap_popcorn
    },
)

@Composable
fun slotName(s: MealSlot) = stringResource(
    when (s) {
        MealSlot.BREAKFAST -> R.string.slot_breakfast
        MealSlot.LUNCH -> R.string.slot_lunch
        MealSlot.DINNER -> R.string.slot_dinner
        MealSlot.SNACK -> R.string.slot_snack
    },
)

@Composable
fun allergenName(a: Allergen) = stringResource(
    when (a) {
        Allergen.MILK -> R.string.allergen_milk
        Allergen.GLUTEN -> R.string.allergen_gluten
        Allergen.PEANUTS -> R.string.allergen_peanuts
        Allergen.NUTS -> R.string.allergen_nuts
        Allergen.EGGS -> R.string.allergen_eggs
        Allergen.SOY -> R.string.allergen_soy
        Allergen.FISH -> R.string.allergen_fish
        Allergen.SHELLFISH -> R.string.allergen_shellfish
        Allergen.SESAME -> R.string.allergen_sesame
    },
)

@Composable
fun goalName(g: FoodGoal) = stringResource(
    when (g) {
        FoodGoal.LOSE -> R.string.goal_lose
        FoodGoal.MAINTAIN -> R.string.goal_maintain
        FoodGoal.GAIN -> R.string.goal_gain
    },
)

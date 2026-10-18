package com.rave.app.ui.workout.guide

// Generated pose data for the built-in exercises. Coordinates are in a 300 × 290 box seen from the
// side (the figure faces right unless flip = -1), with the floor at y = 256. NaN = joint not set.

private const val N = Float.NaN

private fun pose(vararg v: Float) = v

internal val MOVES: Map<String, Move> = mapOf(
    "bench_press" to Move(
        muscles = listOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.DELT),
        gear = listOf(Gear.BENCH, Gear.PLATE),
        flip = 1f, front = false,
        frames = listOf(
            pose(56f, 176f, 80f, 180f, 170f, 182f, 216f, 176f, 226f, 252f, 212f, 178f, 220f, 252f, 84f, 146f, 86f, 112f, 88f, 146f, 90f, 112f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(56f, 176f, 80f, 180f, 170f, 182f, 216f, 176f, 226f, 252f, 212f, 178f, 220f, 252f, 112f, 190f, 102f, 156f, 116f, 191f, 106f, 156f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "incline_db_press" to Move(
        muscles = listOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.DELT),
        gear = listOf(Gear.INCLINE, Gear.DB),
        flip = 1f, front = false,
        frames = listOf(
            pose(99f, 119f, 116f, 136f, 170f, 192f, 218f, 182f, 226f, 252f, 214f, 184f, 220f, 252f, 120f, 100f, 124f, 66f, 124f, 101f, 128f, 67f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(99f, 119f, 116f, 136f, 170f, 192f, 218f, 182f, 226f, 252f, 214f, 184f, 220f, 252f, 142f, 158f, 130f, 126f, 146f, 159f, 134f, 127f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "push_up" to Move(
        muscles = listOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.DELT),
        gear = listOf(Gear.MAT),
        flip = -1f, front = false,
        frames = listOf(
            pose(68f, 130f, 90f, 140f, 180f, 170f, 226f, 185f, 272f, 200f, 226f, 187f, 272f, 202f, 90f, 172f, 90f, 204f, 94f, 172f, 94f, 204f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(68f, 176f, 92f, 182f, 180f, 192f, 226f, 197f, 272f, 202f, 226f, 198f, 272f, 203f, 120f, 184f, 90f, 204f, 124f, 185f, 94f, 204f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "deadlift" to Move(
        muscles = listOf(Muscle.HAM, Muscle.GLUTE, Muscle.BACK),
        gear = listOf(Gear.PLATE),
        flip = 1f, front = false,
        frames = listOf(
            pose(180f, 142f, 158f, 156f, 90f, 184f, 140f, 210f, 150f, 252f, 136f, 212f, 146f, 252f, 156f, 192f, 152f, 226f, 160f, 192f, 156f, 226f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 151f, 100f, 153f, 136f, 155f, 100f, 157f, 136f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "barbell_row" to Move(
        muscles = listOf(Muscle.LAT, Muscle.BACK, Muscle.BICEPS),
        gear = listOf(Gear.PLATE),
        flip = 1f, front = false,
        frames = listOf(
            pose(196f, 100f, 176f, 112f, 110f, 150f, 150f, 192f, 150f, 252f, 146f, 194f, 146f, 252f, 178f, 148f, 180f, 184f, 182f, 148f, 184f, 184f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(196f, 100f, 176f, 112f, 110f, 150f, 150f, 192f, 150f, 252f, 146f, 194f, 146f, 252f, 146f, 124f, 156f, 156f, 150f, 124f, 160f, 156f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "lat_pulldown" to Move(
        muscles = listOf(Muscle.LAT, Muscle.BICEPS),
        gear = listOf(Gear.PULLDOWN),
        flip = 1f, front = false,
        frames = listOf(
            pose(160f, 98f, 156f, 122f, 150f, 196f, 202f, 194f, 204f, 252f, 198f, 196f, 200f, 252f, 158f, 88f, 160f, 54f, 162f, 88f, 164f, 54f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(164f, 98f, 158f, 122f, 150f, 196f, 202f, 194f, 204f, 252f, 198f, 196f, 200f, 252f, 146f, 156f, 172f, 132f, 150f, 157f, 176f, 133f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "pull_up" to Move(
        muscles = listOf(Muscle.LAT, Muscle.BICEPS, Muscle.FOREARM),
        gear = listOf(Gear.PULLBAR),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 74f, 150f, 96f, 150f, 166f, 153f, 208f, 150f, 246f, 147f, 208f, 146f, 246f, 146f, 62f, 146f, 30f, 154f, 62f, 154f, 30f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(154f, 10f, 150f, 34f, 146f, 106f, 164f, 146f, 148f, 184f, 160f, 148f, 144f, 186f, 122f, 52f, 146f, 30f, 178f, 52f, 154f, 30f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "squat" to Move(
        muscles = listOf(Muscle.QUAD, Muscle.GLUTE, Muscle.ABS),
        gear = listOf(Gear.PLATE_BACK),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 132f, 88f, 146f, 66f, 136f, 90f, 150f, 68f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(180f, 119f, 166f, 138f, 117f, 196f, 172f, 200f, 150f, 252f, 166f, 202f, 146f, 252f, 146f, 160f, 162f, 140f, 150f, 162f, 166f, 142f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "leg_press" to Move(
        muscles = listOf(Muscle.QUAD, Muscle.GLUTE),
        gear = listOf(Gear.LEGPRESS),
        flip = 1f, front = false,
        frames = listOf(
            pose(34f, 137f, 52f, 151f, 110f, 200f, 154f, 166f, 198f, 130f, 150f, 168f, 194f, 132f, 82f, 186f, 106f, 204f, 86f, 187f, 110f, 205f, N, N, N, N, N, N, N, N, 206f, 112f, 202f, 114f),
            pose(34f, 137f, 52f, 151f, 110f, 200f, 124f, 146f, 176f, 124f, 120f, 148f, 172f, 126f, 82f, 186f, 106f, 204f, 86f, 187f, 110f, 205f, N, N, N, N, N, N, N, N, 184f, 106f, 180f, 108f),
        ),
    ),
    "lunge" to Move(
        muscles = listOf(Muscle.QUAD, Muscle.GLUTE),
        gear = listOf(Gear.DB),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 46f, 150f, 70f, 150f, 146f, 176f, 198f, 190f, 252f, 132f, 198f, 98f, 250f, 152f, 106f, 154f, 142f, 156f, 106f, 158f, 142f, N, N, N, N, N, N, N, N, N, N, 112f, 256f),
            pose(150f, 86f, 150f, 110f, 150f, 186f, 200f, 192f, 190f, 252f, 140f, 240f, 98f, 250f, 152f, 146f, 154f, 182f, 156f, 146f, 158f, 182f, N, N, N, N, N, N, N, N, N, N, 112f, 256f),
        ),
    ),
    "rdl" to Move(
        muscles = listOf(Muscle.HAM, Muscle.GLUTE, Muscle.BACK),
        gear = listOf(Gear.PLATE),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 151f, 100f, 153f, 136f, 155f, 100f, 157f, 136f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(204f, 112f, 182f, 118f, 108f, 142f, 156f, 194f, 150f, 252f, 152f, 196f, 146f, 252f, 180f, 154f, 176f, 190f, 184f, 154f, 180f, 190f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "calf_raise" to Move(
        muscles = listOf(Muscle.CALF),
        gear = listOf(Gear.STEP, Gear.DB),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 28f, 150f, 52f, 150f, 128f, 152f, 184f, 150f, 240f, 146f, 184f, 146f, 240f, 151f, 88f, 153f, 124f, 155f, 88f, 157f, 124f, N, N, N, N, N, N, N, N, 166f, 240f, 162f, 240f),
            pose(150f, 12f, 150f, 36f, 150f, 112f, 152f, 168f, 150f, 224f, 146f, 168f, 146f, 224f, 151f, 72f, 153f, 108f, 155f, 72f, 157f, 108f, N, N, N, N, N, N, N, N, 164f, 240f, 160f, 240f),
        ),
    ),
    "ohp" to Move(
        muscles = listOf(Muscle.DELT, Muscle.TRICEPS),
        gear = listOf(Gear.PLATE),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 164f, 94f, 166f, 62f, 168f, 95f, 170f, 63f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 152f, 30f, 152f, -4f, 156f, 30f, 156f, -4f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "lateral_raise" to Move(
        muscles = listOf(Muscle.DELT),
        gear = listOf(Gear.DB),
        flip = 1f, front = true,
        frames = listOf(
            pose(150f, 40f, 150f, 64f, 150f, 140f, 138f, 196f, 136f, 252f, 162f, 196f, 164f, 252f, 128f, 106f, 126f, 140f, 172f, 106f, 174f, 140f, 134f, 72f, 166f, 72f, 141f, 138f, 159f, 138f, 124f, 254f, 176f, 254f),
            pose(150f, 40f, 150f, 64f, 150f, 140f, 138f, 196f, 136f, 252f, 162f, 196f, 164f, 252f, 100f, 76f, 66f, 80f, 200f, 76f, 234f, 80f, 134f, 72f, 166f, 72f, 141f, 138f, 159f, 138f, 124f, 254f, 176f, 254f),
        ),
    ),
    "bicep_curl" to Move(
        muscles = listOf(Muscle.BICEPS, Muscle.FOREARM),
        gear = listOf(Gear.DB),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 152f, 100f, 156f, 136f, 156f, 100f, 160f, 136f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 152f, 100f, 176f, 76f, 156f, 100f, 180f, 76f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "tricep_pushdown" to Move(
        muscles = listOf(Muscle.TRICEPS),
        gear = listOf(Gear.CABLE),
        flip = 1f, front = false,
        frames = listOf(
            pose(154f, 40f, 152f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 154f, 100f, 184f, 86f, 158f, 100f, 188f, 86f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(154f, 40f, 152f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 154f, 100f, 162f, 136f, 158f, 100f, 166f, 136f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "dip" to Move(
        muscles = listOf(Muscle.CHEST, Muscle.TRICEPS),
        gear = listOf(Gear.DIPBARS),
        flip = 1f, front = false,
        frames = listOf(
            pose(152f, 32f, 148f, 56f, 146f, 130f, 142f, 180f, 110f, 200f, 138f, 182f, 106f, 202f, 149f, 90f, 150f, 124f, 153f, 90f, 154f, 124f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(166f, 72f, 156f, 94f, 140f, 166f, 136f, 214f, 104f, 230f, 132f, 216f, 100f, 232f, 124f, 106f, 150f, 124f, 128f, 107f, 154f, 124f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "plank" to Move(
        muscles = listOf(Muscle.ABS, Muscle.DELT),
        gear = listOf(Gear.MAT),
        flip = -1f, front = false,
        frames = listOf(
            pose(66f, 206f, 90f, 214f, 176f, 222f, 224f, 232f, 272f, 242f, 224f, 233f, 272f, 243f, 92f, 250f, 126f, 252f, 96f, 250f, 130f, 252f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(66f, 204f, 90f, 212f, 176f, 216f, 224f, 229f, 272f, 242f, 224f, 230f, 272f, 243f, 92f, 250f, 126f, 252f, 96f, 250f, 130f, 252f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "crunch" to Move(
        muscles = listOf(Muscle.ABS),
        gear = listOf(Gear.MAT),
        flip = 1f, front = false,
        frames = listOf(
            pose(50f, 240f, 74f, 240f, 150f, 240f, 190f, 200f, 226f, 248f, 186f, 202f, 222f, 248f, 104f, 226f, 82f, 222f, 108f, 227f, 86f, 223f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(59f, 198f, 81f, 208f, 150f, 240f, 190f, 200f, 226f, 248f, 186f, 202f, 222f, 248f, 112f, 206f, 95f, 194f, 116f, 207f, 99f, 195f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "kettlebell_swing" to Move(
        muscles = listOf(Muscle.GLUTE, Muscle.HAM, Muscle.BACK),
        gear = listOf(Gear.KB),
        flip = 1f, front = false,
        frames = listOf(
            pose(196f, 98f, 176f, 110f, 112f, 150f, 150f, 194f, 150f, 252f, 146f, 196f, 146f, 252f, 164f, 144f, 150f, 176f, 168f, 144f, 154f, 176f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 182f, 74f, 216f, 78f, 186f, 75f, 220f, 79f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
    "burpee" to Move(
        muscles = listOf(Muscle.QUAD, Muscle.GLUTE, Muscle.CHEST, Muscle.ABS),
        gear = listOf(Gear.MAT),
        flip = 1f, front = false,
        frames = listOf(
            pose(150f, 40f, 150f, 64f, 150f, 140f, 152f, 196f, 150f, 252f, 146f, 196f, 146f, 252f, 151f, 100f, 153f, 136f, 155f, 100f, 157f, 136f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(200f, 140f, 182f, 156f, 124f, 206f, 178f, 196f, 150f, 252f, 174f, 198f, 146f, 252f, 196f, 204f, 204f, 248f, 200f, 204f, 208f, 248f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(232f, 200f, 210f, 210f, 126f, 222f, 80f, 234f, 36f, 246f, 80f, 235f, 36f, 247f, 208f, 230f, 206f, 250f, 212f, 230f, 210f, 250f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(200f, 140f, 182f, 156f, 124f, 206f, 178f, 196f, 150f, 252f, 174f, 198f, 146f, 252f, 196f, 204f, 204f, 248f, 200f, 204f, 208f, 248f, N, N, N, N, N, N, N, N, N, N, N, N),
            pose(150f, 20f, 150f, 44f, 150f, 120f, 152f, 176f, 150f, 232f, 146f, 176f, 146f, 232f, 154f, 10f, 156f, -24f, 158f, 10f, 160f, -24f, N, N, N, N, N, N, N, N, N, N, N, N),
        ),
    ),
)

package com.aplivit.infrastructure.content

import androidx.compose.ui.graphics.Path

/**
 * Predefined stroke paths for the Spanish alphabet.
 * Coordinate system: 0..200 (width) x 0..250 (height)
 * Baseline: y=210, top cap: y=30, x-height midline: y=120
 *
 * Each letter returns a list of strokes in correct drawing order.
 * Paths are defined in viewport coordinates and scaled at render time.
 */
object LetterPaths {

    fun getStrokesForLetter(letter: Char): List<Path> = when (letter.lowercaseChar()) {
        'a' -> letterA()
        'á' -> letterA()
        'b' -> letterB()
        'c' -> letterC()
        'd' -> letterD()
        'e' -> letterE()
        'é' -> letterE()
        'f' -> letterF()
        'g' -> letterG()
        'h' -> letterH()
        'i' -> letterI()
        'í' -> letterI()
        'j' -> letterJ()
        'k' -> letterK()
        'l' -> letterL()
        'm' -> letterM()
        'n' -> letterN()
        'ñ' -> letterN()
        'o' -> letterO()
        'ó' -> letterO()
        'p' -> letterP()
        'q' -> letterQ()
        'r' -> letterR()
        's' -> letterS()
        't' -> letterT()
        'u' -> letterU()
        'ú' -> letterU()
        'v' -> letterV()
        'w' -> letterW()
        'x' -> letterX()
        'y' -> letterY()
        'z' -> letterZ()
        else -> letterO()
    }

    // ── Letters ──────────────────────────────────────────────────────────────

    private fun letterA(): List<Path> = listOf(
        Path().apply {
            moveTo(100f, 30f)
            lineTo(35f, 210f)
        },
        Path().apply {
            moveTo(100f, 30f)
            lineTo(165f, 210f)
        },
        Path().apply {
            moveTo(60f, 138f)
            lineTo(140f, 138f)
        }
    )

    private fun letterB(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
        },
        Path().apply {
            moveTo(50f, 30f)
            cubicTo(140f, 30f, 140f, 120f, 50f, 120f)
        },
        Path().apply {
            moveTo(50f, 120f)
            cubicTo(150f, 120f, 150f, 210f, 50f, 210f)
        }
    )

    private fun letterC(): List<Path> = listOf(
        Path().apply {
            moveTo(160f, 70f)
            cubicTo(130f, 20f, 40f, 25f, 40f, 120f)
            cubicTo(40f, 215f, 130f, 220f, 160f, 170f)
        }
    )

    private fun letterD(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
        },
        Path().apply {
            moveTo(50f, 30f)
            cubicTo(180f, 30f, 180f, 210f, 50f, 210f)
        }
    )

    private fun letterE(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
        },
        Path().apply {
            moveTo(50f, 30f)
            lineTo(155f, 30f)
        },
        Path().apply {
            moveTo(50f, 120f)
            lineTo(135f, 120f)
        },
        Path().apply {
            moveTo(50f, 210f)
            lineTo(155f, 210f)
        }
    )

    private fun letterF(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
        },
        Path().apply {
            moveTo(50f, 30f)
            lineTo(155f, 30f)
        },
        Path().apply {
            moveTo(50f, 120f)
            lineTo(135f, 120f)
        }
    )

    private fun letterG(): List<Path> = listOf(
        Path().apply {
            moveTo(160f, 70f)
            cubicTo(130f, 20f, 40f, 25f, 40f, 120f)
            cubicTo(40f, 215f, 130f, 220f, 160f, 170f)
            lineTo(160f, 120f)
            lineTo(115f, 120f)
        }
    )

    private fun letterH(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
        },
        Path().apply {
            moveTo(150f, 30f)
            lineTo(150f, 210f)
        },
        Path().apply {
            moveTo(50f, 120f)
            lineTo(150f, 120f)
        }
    )

    private fun letterI(): List<Path> = listOf(
        Path().apply {
            moveTo(70f, 30f)
            lineTo(130f, 30f)
        },
        Path().apply {
            moveTo(100f, 30f)
            lineTo(100f, 210f)
        },
        Path().apply {
            moveTo(70f, 210f)
            lineTo(130f, 210f)
        }
    )

    private fun letterJ(): List<Path> = listOf(
        Path().apply {
            moveTo(90f, 30f)
            lineTo(150f, 30f)
        },
        Path().apply {
            moveTo(120f, 30f)
            lineTo(120f, 175f)
            cubicTo(120f, 225f, 45f, 225f, 45f, 175f)
        }
    )

    private fun letterK(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
        },
        Path().apply {
            moveTo(155f, 30f)
            lineTo(50f, 120f)
        },
        Path().apply {
            moveTo(50f, 120f)
            lineTo(155f, 210f)
        }
    )

    private fun letterL(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 210f)
            lineTo(155f, 210f)
        }
    )

    private fun letterM(): List<Path> = listOf(
        Path().apply {
            moveTo(30f, 210f)
            lineTo(30f, 30f)
            lineTo(100f, 145f)
            lineTo(170f, 30f)
            lineTo(170f, 210f)
        }
    )

    private fun letterN(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 210f)
            lineTo(50f, 30f)
            lineTo(150f, 210f)
            lineTo(150f, 30f)
        }
    )

    private fun letterO(): List<Path> = listOf(
        Path().apply {
            // Approximate ellipse with 4 cubic beziers
            // Center (100, 120), rx=70, ry=90, k≈0.5523
            val cx = 100f; val cy = 120f
            val rx = 70f; val ry = 90f
            val kx = rx * 0.5523f; val ky = ry * 0.5523f
            moveTo(cx, cy - ry)
            cubicTo(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
            cubicTo(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
            cubicTo(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
            cubicTo(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        }
    )

    private fun letterP(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 210f)
            lineTo(50f, 30f)
        },
        Path().apply {
            moveTo(50f, 30f)
            cubicTo(155f, 30f, 155f, 120f, 50f, 120f)
        }
    )

    private fun letterQ(): List<Path> = listOf(
        Path().apply {
            val cx = 100f; val cy = 120f
            val rx = 70f; val ry = 90f
            val kx = rx * 0.5523f; val ky = ry * 0.5523f
            moveTo(cx, cy - ry)
            cubicTo(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
            cubicTo(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
            cubicTo(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
            cubicTo(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        },
        Path().apply {
            moveTo(125f, 170f)
            lineTo(170f, 225f)
        }
    )

    private fun letterR(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 210f)
            lineTo(50f, 30f)
        },
        Path().apply {
            moveTo(50f, 30f)
            cubicTo(155f, 30f, 155f, 120f, 50f, 120f)
        },
        Path().apply {
            moveTo(50f, 120f)
            lineTo(160f, 210f)
        }
    )

    private fun letterS(): List<Path> = listOf(
        Path().apply {
            moveTo(155f, 65f)
            cubicTo(135f, 20f, 45f, 20f, 45f, 85f)
            cubicTo(45f, 135f, 155f, 125f, 155f, 175f)
            cubicTo(155f, 225f, 60f, 230f, 40f, 185f)
        }
    )

    private fun letterT(): List<Path> = listOf(
        Path().apply {
            moveTo(40f, 30f)
            lineTo(160f, 30f)
        },
        Path().apply {
            moveTo(100f, 30f)
            lineTo(100f, 210f)
        }
    )

    private fun letterU(): List<Path> = listOf(
        Path().apply {
            moveTo(50f, 30f)
            lineTo(50f, 170f)
            cubicTo(50f, 225f, 150f, 225f, 150f, 170f)
            lineTo(150f, 30f)
        }
    )

    private fun letterV(): List<Path> = listOf(
        Path().apply {
            moveTo(30f, 30f)
            lineTo(100f, 210f)
            lineTo(170f, 30f)
        }
    )

    private fun letterW(): List<Path> = listOf(
        Path().apply {
            moveTo(20f, 30f)
            lineTo(55f, 210f)
            lineTo(100f, 110f)
            lineTo(145f, 210f)
            lineTo(180f, 30f)
        }
    )

    private fun letterX(): List<Path> = listOf(
        Path().apply {
            moveTo(40f, 30f)
            lineTo(160f, 210f)
        },
        Path().apply {
            moveTo(160f, 30f)
            lineTo(40f, 210f)
        }
    )

    private fun letterY(): List<Path> = listOf(
        Path().apply {
            moveTo(30f, 30f)
            lineTo(100f, 120f)
        },
        Path().apply {
            moveTo(170f, 30f)
            lineTo(100f, 120f)
            lineTo(100f, 210f)
        }
    )

    private fun letterZ(): List<Path> = listOf(
        Path().apply {
            moveTo(40f, 30f)
            lineTo(160f, 30f)
            lineTo(40f, 210f)
            lineTo(160f, 210f)
        }
    )
}

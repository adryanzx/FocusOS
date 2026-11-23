package br.focusos

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.*

@Composable
fun FocusCar(color: Color, anim: Boolean, modifier: Modifier = Modifier) {
    val a = remember { Animatable(if (anim) 0f else 1f) }
    LaunchedEffect(Unit) { if (anim) a.animateTo(1f, tween(1200)) }
    val light = lerp(color, Color.White, .35f); val dark = lerp(color, Color.Black, .45f)
    Canvas(modifier.aspectRatio(1000f / 360f).graphicsLayer { alpha = a.value; translationX = (1f - a.value) * -120f }) {
        scale(size.width / 1000f, Offset.Zero) {
            drawOval(Brush.radialGradient(listOf(Color(0xCC000000), Color.Transparent), Offset(500f, 335f), 480f), Offset(40f, 312f), Size(920f, 46f))
            val body = Path().apply {
                moveTo(70f, 268f); lineTo(58f, 195f); cubicTo(66f, 150f, 130f, 135f, 220f, 126f); lineTo(520f, 116f)
                cubicTo(625f, 118f, 690f, 160f, 765f, 190f); lineTo(925f, 204f); cubicTo(968f, 212f, 985f, 236f, 978f, 268f); close()
            }
            drawPath(body, Brush.verticalGradient(listOf(light, color, dark), 110f, 270f))
            val win = Path().apply { moveTo(215f, 140f); lineTo(515f, 130f); cubicTo(600f, 132f, 655f, 160f, 700f, 182f); lineTo(260f, 182f); close() }
            drawPath(win, Color(0xFF0B1118))
            drawLine(dark, Offset(430f, 131f), Offset(425f, 182f), 9f)
            drawLine(Color(0x66FFFFFF), Offset(120f, 215f), Offset(930f, 222f), 2f)
            drawRoundRect(Color(0xFFDDEBFF), Offset(915f, 208f), Size(55f, 14f), CornerRadius(7f))
            drawRoundRect(Color(0xFFD22B2B), Offset(58f, 200f), Size(14f, 36f), CornerRadius(5f))
            for (cx in listOf(240f, 770f)) {
                drawCircle(Color.Black, 76f, Offset(cx, 272f)); drawCircle(Color(0xFF15181C), 62f, Offset(cx, 282f))
                drawCircle(Color(0xFFAEB6C0), 40f, Offset(cx, 282f))
                for (k in 0 until 5) { val g = k * 72f * PI.toFloat() / 180f
                    drawLine(Color(0xFF2A2F36), Offset(cx, 282f), Offset(cx + 38f * cos(g), 282f + 38f * sin(g)), 6f) }
                drawCircle(Color(0xFF2A2F36), 14f, Offset(cx, 282f))
            }
        }
    }
}

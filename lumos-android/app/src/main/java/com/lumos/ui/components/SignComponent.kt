package com.lumos.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lumos.R
import com.lumos.utils.Utils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LandscapeModeWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var originalOrientation by remember { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

    DisposableEffect(lifecycleOwner) {
        val activity = context as Activity
        originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Ocultar status bar e navigation bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // APIs anteriores
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                activity.requestedOrientation = originalOrientation
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            activity.requestedOrientation = originalOrientation
            lifecycleOwner.lifecycle.removeObserver(observer)

            // Restaurar barras
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    content()
}


@Composable
fun SignatureScreenLandscape(
    description: String,
    onSave: (Bitmap, Instant) -> Unit,
    onCancel: () -> Unit
) {
    LandscapeModeWrapper {
        var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
        var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
        val isCanvasEmpty = strokes.isEmpty() && currentStroke.isEmpty()
        val currentDate = Utils.dateTime
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
        val formattedDate = currentDate.atZone(ZoneId.systemDefault()).format(formatter)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> currentStroke = listOf(offset) },
                        onDrag = { change, _ ->
                            val lastPoint = currentStroke.lastOrNull()
                            if (lastPoint == null || (change.position - lastPoint).getDistance() > 3f) {
                                currentStroke = currentStroke + change.position
                            }
                        },
                        onDragEnd = {
                            strokes = strokes + listOf(currentStroke)
                            currentStroke = emptyList()
                        }
                    )
                }
        ) {
            val canvasWidth = constraints.maxWidth
            val canvasHeight = constraints.maxHeight

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(5.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_lumos),
                            contentDescription = "Ícone do App",
                            modifier = Modifier
                                .size(30.dp)
                        )
                        Text("Lumos OP ©", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(description, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(35.dp))
                }

                Text("Solicite a assinatura do responsável", style = MaterialTheme.typography.titleMedium)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
            ) {
                val paint = Stroke(width = 4f)
                val padding = 32f
                val lineY = size.height - 100f
                val lineEndX = size.width - padding

                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, lineY),
                    end = Offset(lineEndX, lineY),
                    strokeWidth = 2f
                )

                drawContext.canvas.nativeCanvas.apply {
                    val text = if(isCanvasEmpty) "Assine aqui" else ""
                    val paintText = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 42f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    drawText(text, size.width / 2, lineY - 20, paintText)
                }
                drawContext.canvas.nativeCanvas.apply {
                    val paintText = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 42f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    drawText(formattedDate, size.width - 250, lineY + 60, paintText)
                }

                fun List<Offset>.toSmoothPath(): Path {
                    val path = Path()
                    if (this.isEmpty()) return path
                    path.moveTo(this[0].x, this[0].y)

                    for (i in 1 until this.size - 1) {
                        val prev = this[i - 1]
                        val current = this[i]
                        val next = this[i + 1]

                        val ctrlPointX = (current.x + next.x) / 2
                        val ctrlPointY = (current.y + next.y) / 2

                        path.quadraticBezierTo(current.x, current.y, ctrlPointX, ctrlPointY)
                    }
                    // Linha para o último ponto
                    path.lineTo(this.last().x, this.last().y)
                    return path
                }

                // Desenhe os strokes suavizados
                strokes.forEach { strokePoints ->
                    val path = strokePoints.toSmoothPath()
                    drawPath(path, Color.Black, style = paint)
                }
                // Também para o stroke atual
                if (currentStroke.isNotEmpty()) {
                    val path = currentStroke.toSmoothPath()
                    drawPath(path, Color.Black, style = paint)
                }
            }

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes = strokes.dropLast(1) // Remove o último traço
                        }
                        currentStroke = emptyList() // Limpa o traço atual que está sendo desenhado
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Desfazer")
                }

                Button(
                    onClick = {
                        val bitmap = captureSignatureAsBitmap(strokes, canvasWidth, canvasHeight)
                        onSave(bitmap, currentDate)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCanvasEmpty
                ) {
                    Text("Salvar Assinatura")
                }
            }
        }
    }
}
fun captureSignatureAsBitmap(
    strokes: List<List<Offset>>,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 6f
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }

    strokes.forEach { strokePoints ->
        if (strokePoints.size < 2) return@forEach

        val path = android.graphics.Path()
        path.moveTo(strokePoints[0].x, strokePoints[0].y)

        for (i in 1 until strokePoints.size) {
            path.lineTo(strokePoints[i].x, strokePoints[i].y)
        }
        canvas.drawPath(path, paint)
    }

    return bitmap
}



@Preview(
    showBackground = true,
    widthDp = 640,
    heightDp = 360,
    name = "Simulação Celular Landscape"
)
@Composable
fun SignatureScreenPreviewLandscape() {
    Box(
        modifier = Modifier
            .size(width = 640.dp, height = 360.dp)
            .background(Color.White)
            .border(2.dp, Color.Gray, RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        SignatureScreenLandscape(
            description = "Prefeitura Municipal de Belo Horizonte",
            onSave = { _,_ -> },
            onCancel = { /* noop */ },
        )
    }
}


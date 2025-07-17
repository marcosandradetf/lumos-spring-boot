package com.lumos.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lumos.R

@Composable
fun SignatureScreenLandscape(
    description: String,
    onSave: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> currentStroke = listOf(offset) },
                    onDrag = { change, _ -> currentStroke = currentStroke + change.position },
                    onDragEnd = {
                        strokes = strokes + listOf(currentStroke)
                        currentStroke = emptyList()
                    }
                )
            }
    ) {
        val canvasWidth = constraints.maxWidth
        val canvasHeight = constraints.maxHeight

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_lumos), // Agora no drawable
                    contentDescription = "Ícone do App",
                    modifier = Modifier.size(30.dp)
                )
                Text("Lumos OP ©", style = MaterialTheme.typography.labelSmall)
            }
            Text(description ?: "", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(35.dp))
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
                val text = "Assine aqui"
                val paintText = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 42f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                drawText(text, size.width / 2, lineY - 20, paintText)
            }
            drawContext.canvas.nativeCanvas.apply {
                val text = "17/07/2025 às 13:52"
                val paintText = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 42f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                drawText(text, size.width - 250, lineY + 60, paintText)
            }

            strokes.forEach { strokePoints ->
                for (i in 1 until strokePoints.size) {
                    drawLine(
                        color = Color.Black,
                        start = strokePoints[i - 1],
                        end = strokePoints[i],
                        strokeWidth = paint.width
                    )
                }
            }
            for (i in 1 until currentStroke.size) {
                drawLine(
                    color = Color.Black,
                    start = currentStroke[i - 1],
                    end = currentStroke[i],
                    strokeWidth = paint.width
                )
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
                    strokes = emptyList()
                    currentStroke = emptyList()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Limpar")
            }
            Button(
                onClick = {
                    val bitmap = captureSignatureAsBitmap(strokes, canvasWidth, canvasHeight)
                    onSave(bitmap)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Salvar Assinatura")
            }
        }
    }
}

fun captureSignatureAsBitmap(strokes: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    strokes.forEach { stroke ->
        for (i in 1 until stroke.size) {
            canvas.drawLine(
                stroke[i - 1].x,
                stroke[i - 1].y,
                stroke[i].x,
                stroke[i].y,
                paint
            )
        }
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
            onSave = { /* noop */ },
            onCancel = { /* noop */ }
        )
    }
}


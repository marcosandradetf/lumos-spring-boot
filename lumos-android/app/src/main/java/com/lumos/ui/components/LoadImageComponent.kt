package com.lumos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

@Composable
fun LoadImageComponent(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    onRefreshUrl: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var onImageLoaded by remember { mutableStateOf(false) }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val animatedSize by animateDpAsState(
        targetValue = if (isExpanded) with(density) {
            (windowInfo.containerSize.height * 0.7f).toDp()
        } else 70.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val animatedCorner by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else cornerRadius.dp,
        animationSpec = tween(250)
    )

    // Gap superior (para dar o efeito de afastar do topo ao expandir)
    val topPadding by animateDpAsState(
        targetValue = if (isExpanded) 60.dp else 0.dp,
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .padding(top = topPadding)
            .clip(RoundedCornerShape(animatedCorner))
            .shadow(6.dp, RoundedCornerShape(animatedCorner))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { if (onImageLoaded) isExpanded = !isExpanded }
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(animatedSize)
                .clip(RoundedCornerShape(animatedCorner)),
            contentScale = if (isExpanded) ContentScale.Fit else ContentScale.Crop
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is AsyncImagePainter.State.Error -> {
                    val throwable = (painter.state as? AsyncImagePainter.State.Error)?.result?.throwable
                    val httpCode = (throwable as? coil.network.HttpException)?.response?.code
                    if (httpCode == 403) onRefreshUrl()

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Photo,
                            contentDescription = "Erro ao carregar imagem",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                else -> {
                    onImageLoaded = true
                    SubcomposeAsyncImageContent()
                }
            }
        }

        // 🔘 Botão de fechar no modo expandido
        if (isExpanded) {
            IconButton(
                onClick = { isExpanded = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

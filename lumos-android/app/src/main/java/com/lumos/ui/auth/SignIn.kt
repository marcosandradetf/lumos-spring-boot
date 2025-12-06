package com.lumos.ui.auth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lumos.R
import com.lumos.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onTestClick: () -> Unit = {},
    context: Context
) {
    var step by remember { mutableIntStateOf(1) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val message = viewModel.message

    val scope = rememberCoroutineScope()

    // micro animações modernas
    val transition = updateTransition(step, label = "stepTransition")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(450, easing = FastOutSlowInEasing) },
        label = ""
    ) { if (it == step) 1f else 0f }

    val cardModifier = Modifier
        .fillMaxWidth(0.85f)
        .defaultMinSize(minHeight = 260.dp) // 👈 ISSO FIXA O PROBLEMA
        .animateContentSize()
        .graphicsLayer { this.alpha = alpha }

    val infinite = rememberInfiniteTransition(label = "")
    val scale by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = ""
    )
    val alphaTitle by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200),
        label = ""
    )

    if (message != null) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
        viewModel.message = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // LOGO minimalista moderna
        Image(
            painter = painterResource(R.drawable.ic_lumos),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )


        Text(
            text = "Lumos OP™",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
            modifier = Modifier.alpha(alphaTitle)
        )

        Spacer(modifier = Modifier.height(35.dp))

        // CARD STEP-TO-STEP
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ) + fadeIn() togetherWith
                                slideOutVertically(
                                    targetOffsetY = { -it / 2 },
                                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                                ) + fadeOut()
                    }
                ) { currentStep ->

                    when (currentStep) {

                        1 -> {
                            Column {
                                Text(
                                    "Quem está acessando hoje?",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 20.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Usuário ou CPF") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Spacer(Modifier.height(22.dp))

                                Button(
                                    onClick = { if (username.isNotEmpty()) step = 2 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) { Text("Avançar") }
                            }
                        }

                        2 -> {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        step = 1
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 5.dp)
                                            .size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Trocar usuário", color = MaterialTheme.colorScheme.primary)
                                }

                                Text(
                                    "Agora falta só sua senha",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 20.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Senha") },
                                    singleLine = true,
                                    visualTransformation =
                                        if (passwordVisible) VisualTransformation.None
                                        else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            passwordVisible = !passwordVisible
                                        }) {
                                            Icon(
                                                imageVector =
                                                    if (passwordVisible)
                                                        Icons.Default.Visibility
                                                    else Icons.Default.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Spacer(Modifier.height(22.dp))

                                Button(
                                    onClick = {
                                        if (password.isNotEmpty()) {
                                            scope.launch { viewModel.login(username, password) }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) { Text("Entrar") }
                            }
                        }
                    }
                }

            }
        }

        Spacer(Modifier.height(12.dp))

        if (step == 2) {
            TextButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Entre em contato com o administrador da sua empresa.",
                        Toast.LENGTH_LONG
                    ).show()
                }, // aqui você coloca sua ação real
                modifier = Modifier.height(45.dp)
            ) {
                Text("Esqueceu a senha?")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Marketing suave moderno
        AutoSwitchingFeatures()

        Spacer(modifier = Modifier.height(30.dp))

        // BOTÕES SECUNDÁRIOS
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    val url = "https://lumos.thryon.com.br"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(45.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4C6FFF),
                                Color(0xFF875BFF)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent, // ← obrigatório!
                    contentColor = Color.White          // texto branco
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Saiba Mais")
            }


            TextButton(
                onClick = {
                    Toast.makeText(context, "Disponível em breve", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.height(45.dp),
            ) { Text("Conhecer a Plataforma", color = MaterialTheme.colorScheme.secondary) }

        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AutoSwitchingFeatures() {
    val features = listOf(
        "O que é o Lumos?",
        "Equipes em tempo real",
        "Controle de estoque",
        "Pré-medição e contratos",
        "Rotinas de manutenção e instalação de LEDs",
        "Relatórios e KPIs",
        "Dashboard ao vivo",
        "Sua empresa merece o melhor!"
    )

    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            index = (index + 1) % features.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = {
            slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 2 } // entra de baixo
            ) + fadeIn(tween(500)) togetherWith
                    slideOutVertically(
                        animationSpec = tween(500),
                        targetOffsetY = { -it / 2 } // sai para cima
                    ) + fadeOut(tween(500))
        },
        label = "featureSwitch"
    ) { idx ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(6.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(25)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = features[idx],
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}


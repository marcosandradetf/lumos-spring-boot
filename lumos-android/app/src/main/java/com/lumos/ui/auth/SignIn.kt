package com.lumos.ui.auth

import android.R.style
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumos.ui.theme.LumosTheme


@Composable
fun Login(
    sendCred: (String, String) -> Unit,
    signUp: () -> Unit,
) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFFFFFFF))
            .padding(15.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Lumos™",
                fontSize = 25.sp,
            )
        }
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFFFF),
            ),
        ) {
            Column(
                modifier = Modifier
                    .padding(25.dp)
                    .background(color = Color(0xFFFFFFFF))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Insira suas credenciais para acessar o sistema",
                        modifier = Modifier.padding(bottom = 30.dp),
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                OutlinedTextField(
                    textStyle = TextStyle(Color(0xFF613F23)),
                    value = user,
                    onValueChange = { user = it },
                    label = { Text(text = "Usuário ou email:", color = Color(0xFF9EA4B6)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    textStyle = TextStyle(Color(0xFF613F23)),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Senha:", color = Color(0xFF9EA4B6)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                ElevatedButton(
                    onClick = { sendCred(user, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(Color(0xFF3F00E7))
                ) {
                    Text(
                        text = "Entrar",
                        color = Color.White
                    )
                }
            }
        }
        Text(
            text = "Esqueceu a senha?",
            modifier = Modifier
                .clickable { }
                .padding(20.dp),
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SimpleComposablePreview() {
    LumosTheme {
        Login(
            sendCred = { _, _ -> /* Mock do login */ },
            signUp = { /* Mock da navegação para criar conta */ }
        )
    }
}

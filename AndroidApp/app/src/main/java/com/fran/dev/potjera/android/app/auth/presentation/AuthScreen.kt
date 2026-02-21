package com.fran.dev.potjera.android.app.auth.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fran.dev.potjera.android.app.domain.models.user.User

@Composable
fun AuthScreen(onSignInSuccess: (user: User) -> Unit) {

    val authViewModel: AuthViewModel = hiltViewModel()
    val loading by authViewModel.loading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Login, 1 = Sign Up

    // Login fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Signup fields
    var signupUsername by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    val gradientColors = listOf(Color(0xFFf97316), Color(0xFFe040fb), Color(0xFF7c3aed))
    val bgColor = Color(0xFF0e0b1a)
    val cardColor = Color(0xFF16122a)
    val inputBg = Color(0xFF0e0b1a)
    val borderColor = Color(0xFF2a2440)
    val mutedText = Color(0xFF9b91cc)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Background glow effects
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7c3aed).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.1f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFf97316).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.width * 0.5f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Logo Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "The Chase Quiz",
                style = TextStyle(
                    brush = Brush.linearGradient(gradientColors),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Beat the hunter. Win the prize.",
                fontSize = 13.sp,
                color = mutedText
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Tab Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = inputBg,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            listOf("Login", "Sign Up").forEachIndexed { index, label ->
                                val isActive = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(11.dp))
                                        .then(
                                            if (isActive) Modifier.background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFe040fb), Color(0xFFf97316))
                                                )
                                            ) else Modifier
                                        )
                                        .clickable { selectedTab = index }
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isActive) Color.White else mutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Animated content switch
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) + slideInVertically(
                                animationSpec = tween(250),
                                initialOffsetY = { it / 10 }
                            ) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "tab_content"
                    ) { tab ->
                        if (tab == 0) {
                            LoginPanel(
                                loading = loading,
                                email = loginEmail,
                                onEmailChange = { loginEmail = it },
                                password = loginPassword,
                                onPasswordChange = { loginPassword = it },
                                passwordVisible = loginPasswordVisible,
                                onTogglePassword = { loginPasswordVisible = !loginPasswordVisible },
                                inputBg = inputBg,
                                borderColor = borderColor,
                                mutedText = mutedText,
                                gradientColors = listOf(Color(0xFFe040fb), Color(0xFFf97316)),
                                onLogin = {
                                    authViewModel.signIn(loginEmail, loginPassword) { user ->
                                        onSignInSuccess(user)
                                    }
                                }
                            )
                        } else {
                            SignupPanel(
                                loading = loading,
                                username = signupUsername,
                                onUsernameChange = { signupUsername = it },
                                email = signupEmail,
                                onEmailChange = { signupEmail = it },
                                password = signupPassword,
                                onPasswordChange = { signupPassword = it },
                                passwordVisible = signupPasswordVisible,
                                onTogglePassword = {
                                    signupPasswordVisible = !signupPasswordVisible
                                },
                                inputBg = inputBg,
                                borderColor = borderColor,
                                mutedText = mutedText,
                                gradientColors = listOf(Color(0xFFe040fb), Color(0xFFf97316)),
                                onSignup = {
                                    authViewModel.signUp(
                                        signupUsername,
                                        signupEmail,
                                        signupPassword
                                    ) { user ->
                                        onSignInSuccess(user)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Features row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureItem(
                    icon = Icons.Default.Done,
                    label = "Fast-paced",
                    tint = Color(0xFFf97316),
                    bg = Color(0xFFf97316).copy(alpha = 0.15f)
                )
                FeatureItem(
                    icon = Icons.Default.Build,
                    label = "Win Prizes",
                    tint = Color(0xFFa78bfa),
                    bg = Color(0xFF7c3aed).copy(alpha = 0.15f)
                )
                FeatureItem(
                    icon = Icons.Default.Person,
                    label = "Multiplayer",
                    tint = Color(0xFFe040fb),
                    bg = Color(0xFFe040fb).copy(alpha = 0.12f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF9b91cc).copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    ) {
                        append("By continuing, you agree to our ")
                    }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFFe040fb).copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    ) {
                        append("Terms of Service")
                    }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF9b91cc).copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    ) {
                        append(" and ")
                    }
                    withStyle(
                        SpanStyle(
                            color = Color(0xFFe040fb).copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    ) {
                        append("Privacy Policy")
                    }
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoginPanel(
    loading: Boolean,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onTogglePassword: () -> Unit,
    inputBg: Color, borderColor: Color, mutedText: Color,
    gradientColors: List<Color>, onLogin: () -> Unit
) {
    Column {
        AuthField(
            label = "EMAIL", value = email, onValueChange = onEmailChange,
            placeholder = "Enter your email", inputBg = inputBg,
            borderColor = borderColor, mutedText = mutedText,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            label = "PASSWORD", value = password, onValueChange = onPasswordChange,
            placeholder = "Enter your password", inputBg = inputBg,
            borderColor = borderColor, mutedText = mutedText,
            isPassword = true, passwordVisible = passwordVisible,
            onTogglePassword = onTogglePassword
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "Forgot password?",
                color = Color(0xFFe040fb),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        GradientButton(text = "Login", gradientColors = gradientColors, loading = loading, onClick = onLogin)
    }
}

@Composable
fun SignupPanel(
    loading: Boolean,
    username: String, onUsernameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean, onTogglePassword: () -> Unit,
    inputBg: Color, borderColor: Color, mutedText: Color,
    gradientColors: List<Color>, onSignup: () -> Unit
) {
    Column {
        AuthField(
            label = "USERNAME", value = username, onValueChange = onUsernameChange,
            placeholder = "Choose your username", inputBg = inputBg,
            borderColor = borderColor, mutedText = mutedText
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            label = "EMAIL", value = email, onValueChange = onEmailChange,
            placeholder = "Enter your email", inputBg = inputBg,
            borderColor = borderColor, mutedText = mutedText,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            label = "PASSWORD", value = password, onValueChange = onPasswordChange,
            placeholder = "Create a password", inputBg = inputBg,
            borderColor = borderColor, mutedText = mutedText,
            isPassword = true, passwordVisible = passwordVisible,
            onTogglePassword = onTogglePassword
        )

        Spacer(modifier = Modifier.height(20.dp))

        GradientButton(text = "Create Account", gradientColors = gradientColors, loading = loading, onClick = onSignup)
    }
}

@Composable
fun AuthField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String, inputBg: Color, borderColor: Color, mutedText: Color,
    isPassword: Boolean = false, passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = mutedText,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = mutedText.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = if (isPassword) ({
                IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.ArrowDropDown else Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = mutedText
                    )
                }
            }) else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = Color(0xFFe040fb).copy(alpha = 0.5f),
                unfocusedBorderColor = borderColor,
                cursorColor = Color(0xFFe040fb)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GradientButton(text: String, gradientColors: List<Color>, loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, label: String, tint: Color, bg: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = label, fontSize = 11.sp, color = Color(0xFF9b91cc))
    }
}

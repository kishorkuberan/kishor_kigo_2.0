package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.media.MediaAttachmentHelper
import com.example.ui.components.KigoAvatar
import com.example.ui.components.KigoLogoBadge
import com.example.ui.components.KigoSubScreenTopBar
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.viewmodel.AuthUiState

@Composable
fun SplashScreen() {
    val pulse = rememberInfiniteTransition(label = "splash")
    val alpha by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040914), Color(0xFF081120), Color(0xFF0D1B35))
                )
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KigoLogoBadge(size = 96.dp, modifier = Modifier.alpha(alpha))
            Spacer(Modifier.height(20.dp))
            Text(
                text = "KIGO CHAT",
                fontFamily = SpaceGroteskFamily,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Connect Freely. Chat Your Way.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF00E5FF)
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                color = Color(0xFF00E5FF),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onNavigateLogin: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateHelp: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_aurora),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Box(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    KigoLogoBadge(size = 84.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "KIGO CHAT",
                        fontFamily = SpaceGroteskFamily,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Connect Freely. Chat Your Way.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "VERSION 1.0 · REAL-TIME ENCRYPTED MESSAGING",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                    FeatureHighlightCard(
                        icon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        title = "End-to-End Encrypted Direct Chats",
                        description = "ECDH P-256 key exchange with AES-256-GCM encryption for private conversations."
                    )
                    Spacer(Modifier.height(10.dp))
                    FeatureHighlightCard(
                        icon = { Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary) },
                        title = "Permanent Unique KIGO ID",
                        description = "Receive a collision-free KIGO-XXXXXX identity generated atomically on the backend."
                    )
                    Spacer(Modifier.height(10.dp))
                    FeatureHighlightCard(
                        icon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                        title = "Customisation Studio & Rich Media",
                        description = "AMOLED & custom themes, voice clips, documents, groups, and live Firestore sync."
                    )

                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("welcome_login_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNavigateRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("welcome_register_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create New KIGO Account")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onNavigateHelp) {
                        Text("Help, Security & Setup Info")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LoginScreen(
    authState: AuthUiState,
    onGoogleSignIn: (Activity) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = context as? Activity

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Sign In", subtitle = "KIGO CHAT V1.0", onBack = onBack) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                KigoLogoBadge(size = 72.dp)
                Spacer(Modifier.height(16.dp))
                Text("Welcome Back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Authenticate securely with Google Identity & Credential Manager to sync your KIGO ID, chats, and preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                if (!authState.errorMessage.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = { activity?.let(onGoogleSignIn) },
                    enabled = !authState.isLoading && activity != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_google_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Continue with Google", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onNavigateForgotPassword) {
                        Text("Account Recovery")
                    }
                    TextButton(onClick = onNavigateRegister) {
                        Text("Create Account")
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    authState: AuthUiState,
    onGoogleSignIn: (Activity) -> Unit,
    onNavigateLogin: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = context as? Activity

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Create Account", subtitle = "Step 1 · Authentication", onBack = onBack) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                KigoLogoBadge(size = 72.dp)
                Spacer(Modifier.height(16.dp))
                Text("Join KIGO CHAT", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Register with your verified Google account. Right after sign-in, you will choose your @username and receive your permanent KIGO-XXXXXX ID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("What happens on registration:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("• Verified email identity via Google Sign-In", style = MaterialTheme.typography.bodySmall)
                        Text("• Atomic KIGO-XXXXXX ID minted in Firestore", style = MaterialTheme.typography.bodySmall)
                        Text("• Local ECDH P-256 keypair generated for E2EE", style = MaterialTheme.typography.bodySmall)
                        Text("• Zero-trust PII isolation in private subcollection", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(20.dp))
                if (!authState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = authState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { activity?.let(onGoogleSignIn) },
                    enabled = !authState.isLoading && activity != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("register_google_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Register with Google", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onNavigateLogin) {
                    Text("Already have an account? Sign In")
                }
            }
        }
    }
}

@Composable
fun EmailVerificationScreen(
    email: String?,
    isVerified: Boolean,
    authState: AuthUiState,
    onRefreshStatus: () -> Unit,
    onContinue: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Email Verification", subtitle = "Account Security Gate", onBack = onSignOut) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.MarkEmailRead else Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (isVerified) "Email Verified" else "Verification Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isVerified) {
                    "Your account (${email ?: "verified"}) is confirmed. Messaging and group features are unlocked."
                } else {
                    "Unverified accounts cannot access messaging features. Verify ${email ?: "your email"} and refresh below."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            if (isVerified) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("verify_continue_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Continue to KIGO CHAT")
                }
            } else {
                Button(
                    onClick = onRefreshStatus,
                    enabled = !authState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("verify_refresh_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Verification Status")
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSignOut) {
                Text("Switch Account / Sign Out")
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Account Recovery", subtitle = "Forgot Password & Reset", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Password & Account Recovery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "KIGO CHAT authenticates via Google Identity & Credential Manager, so your password and 2-Step Verification are protected directly by your Google Account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "To reset your password or recover access, visit myaccount.google.com/security from your device browser, then tap 'Continue with Google' in KIGO CHAT.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Sign In")
            }
        }
    }
}

@Composable
fun ProfileCreationScreen(
    initialDisplayName: String,
    email: String?,
    isBusy: Boolean,
    bannerMessage: String?,
    onCreateProfile: (username: String, displayName: String, about: String, photoUrl: String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(initialDisplayName.ifBlank { "KIGO Explorer" }) }
    var username by remember {
        mutableStateOf(
            initialDisplayName.lowercase().replace(Regex("[^a-z0-9_]"), "_").take(18).ifBlank { "kigo_user" }
        )
    }
    var about by remember { mutableStateOf("Connect Freely. Chat Your Way.") }
    var photoBase64 by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaAttachmentHelper.prepareImageFromUri(context, uri)
                .onSuccess { photoBase64 = it.base64Data }
                .onFailure { localError = it.localizedMessage }
        }
    }

    Scaffold(
        topBar = {
            KigoSubScreenTopBar(
                title = "Create Your KIGO Profile",
                subtitle = "Step 2 · Unique KIGO ID Assignment",
                onBack = onSignOut
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    KigoAvatar(
                        displayName = displayName,
                        photoUrl = photoBase64,
                        size = 96.dp
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .testTag("profile_create_avatar_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Pick avatar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = email ?: "Verified Google Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_create_display_name")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' } },
                    label = { Text("Unique Username (@handle)") },
                    prefix = { Text("@") },
                    singleLine = true,
                    supportingText = { Text("3-30 chars · letters, numbers, _ or .") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_create_username")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it.take(300) },
                    label = { Text("About") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("profile_create_about")
                )

                if (!localError.isNullOrBlank() || !bannerMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = localError ?: bannerMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        if (username.length < 3) {
                            localError = "Username must be at least 3 characters."
                        } else if (displayName.isBlank()) {
                            localError = "Display name cannot be empty."
                        } else {
                            localError = null
                            onCreateProfile(username, displayName, about, photoBase64)
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("profile_create_submit_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Generate My KIGO ID & Start Chatting", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

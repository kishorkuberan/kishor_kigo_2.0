package com.example.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crypto.KigoCryptoManager
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.repository.KigoAuthRepository
import com.example.data.repository.KigoChatRepository
import com.example.data.repository.KigoSettingsRepository
import com.example.data.repository.KigoSocialRepository
import com.example.data.repository.KigoUserRepository
import com.example.ui.components.KigoLogoBadge
import com.example.ui.screens.ApplicationSettingsScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.CustomisationStudioScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.FriendRequestsScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.IndividualConversationScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MediaGalleryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.ProfileCreationScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UserSearchScreen
import com.example.ui.screens.VoiceMessagesScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.KigoChatTheme
import com.example.ui.viewmodel.KigoAuthViewModel
import com.example.ui.viewmodel.KigoConversationViewModel
import com.example.ui.viewmodel.KigoMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private object KigoRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val AUTH_HELP = "auth_help"

    const val HOME = "home"
    const val CHATS = "chats"
    const val FRIENDS = "friends"
    const val GROUPS = "groups"
    const val CONVERSATION = "conversation/{conversationId}"
    const val USER_SEARCH = "user_search"
    const val FRIEND_REQUESTS = "friend_requests"
    const val MEDIA_GALLERY = "media_gallery"
    const val VOICE_MESSAGES = "voice_messages"
    const val STUDIO = "customisation_studio"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val PRIVACY = "privacy_settings"
    const val SETTINGS = "app_settings"
    const val HELP = "help_support"

    fun conversation(conversationId: String) = "conversation/$conversationId"
}

private data class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(KigoRoutes.HOME, "Home", Icons.Default.Home, "nav_home"),
    TopLevelTab(KigoRoutes.CHATS, "Chats", Icons.AutoMirrored.Filled.Chat, "nav_chats"),
    TopLevelTab(KigoRoutes.FRIENDS, "Friends", Icons.Default.People, "nav_friends"),
    TopLevelTab(KigoRoutes.GROUPS, "Groups", Icons.Default.Groups, "nav_groups")
)

@Composable
fun KigoApp(
    deepLinkKigoId: String? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { KigoAuthRepository() }
    val authViewModel: KigoAuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { KigoAuthViewModel(authRepository) }
        }
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        authViewModel.runStartupSessionCheck(context)
    }

    if (authState.isCheckingSession) {
        KigoChatTheme(settings = UserSettings()) {
            SplashScreen()
        }
        return
    }

    val user = currentUser
    if (user == null) {
        KigoChatTheme(settings = UserSettings()) {
            UnauthenticatedNav(
                authViewModel = authViewModel,
                onSignedIn = { signedInUser -> currentUser = signedInUser }
            )
        }
        return
    }

    var verificationBypassAck by remember(user.uid) { mutableStateOf(user.isEmailVerified) }
    if (!user.isEmailVerified && !verificationBypassAck) {
        KigoChatTheme(settings = UserSettings()) {
            EmailVerificationScreen(
                email = user.email,
                isVerified = user.isEmailVerified,
                authState = authState,
                onRefreshStatus = {
                    authViewModel.reloadUser { refreshed ->
                        if (refreshed != null) {
                            currentUser = refreshed
                            if (refreshed.isEmailVerified) verificationBypassAck = true
                        }
                    }
                },
                onContinue = { verificationBypassAck = true },
                onSignOut = { Firebase.auth.signOut() }
            )
        }
        return
    }

    val mainViewModel: KigoMainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val appCtx = context.applicationContext
                KigoMainViewModel(
                    currentUserId = user.uid,
                    userRepository = KigoUserRepository(appCtx),
                    socialRepository = KigoSocialRepository(appCtx),
                    chatRepository = KigoChatRepository(appCtx),
                    settingsRepository = KigoSettingsRepository(appCtx),
                    authRepository = authRepository,
                    cryptoManager = KigoCryptoManager(appCtx)
                )
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, user.uid) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mainViewModel.setOnlineStatus(true)
                Lifecycle.Event.ON_STOP -> mainViewModel.setOnlineStatus(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val settings by mainViewModel.userSettings.collectAsStateWithLifecycle()
    val profileState by mainViewModel.myProfileState.collectAsStateWithLifecycle()
    val isBusy by mainViewModel.isBusy.collectAsStateWithLifecycle()
    val bannerMessage by mainViewModel.bannerMessage.collectAsStateWithLifecycle()

    KigoChatTheme(settings = settings) {
        when (val state = profileState) {
            is UiState.Loading -> SplashScreen()
            is UiState.Error -> {
                Scaffold { padding ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
                    ) {
                        KigoLogoBadge(size = 72.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Unable to sync profile", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { mainViewModel.signOut(context) }) {
                            Text("Sign Out & Retry")
                        }
                    }
                }
            }
            is UiState.Success -> {
                val myProfile = state.data
                if (myProfile == null) {
                    ProfileCreationScreen(
                        initialDisplayName = user.displayName ?: "",
                        email = user.email,
                        isBusy = isBusy,
                        bannerMessage = bannerMessage,
                        onCreateProfile = { username, displayName, about, photoUrl ->
                            mainViewModel.createProfile(
                                username = username,
                                displayName = displayName,
                                about = about,
                                photoUrl = photoUrl,
                                email = user.email,
                                onCreated = { mainViewModel.setOnlineStatus(true) }
                            )
                        },
                        onSignOut = { mainViewModel.signOut(context) }
                    )
                } else {
                    AuthenticatedKigoShell(
                        myProfile = myProfile,
                        settings = settings,
                        mainViewModel = mainViewModel,
                        deepLinkKigoId = deepLinkKigoId,
                        onConsumeDeepLink = onConsumeDeepLink
                    )
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedNav(
    authViewModel: KigoAuthViewModel,
    onSignedIn: (FirebaseUser) -> Unit
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val startGoogleSignIn: (Activity) -> Unit = { activity ->
        authViewModel.signInWithGoogle(activity) { user ->
            onSignedIn(user)
        }
    }

    NavHost(
        navController = navController,
        startDestination = KigoRoutes.WELCOME
    ) {
        composable(KigoRoutes.WELCOME) {
            WelcomeScreen(
                onNavigateLogin = {
                    authViewModel.clearMessages()
                    navController.navigate(KigoRoutes.LOGIN)
                },
                onNavigateRegister = {
                    authViewModel.clearMessages()
                    navController.navigate(KigoRoutes.REGISTER)
                },
                onNavigateHelp = { navController.navigate(KigoRoutes.AUTH_HELP) }
            )
        }
        composable(KigoRoutes.LOGIN) {
            LoginScreen(
                authState = authState,
                onGoogleSignIn = startGoogleSignIn,
                onNavigateRegister = {
                    authViewModel.clearMessages()
                    navController.navigate(KigoRoutes.REGISTER)
                },
                onNavigateForgotPassword = { navController.navigate(KigoRoutes.FORGOT_PASSWORD) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(KigoRoutes.REGISTER) {
            RegistrationScreen(
                authState = authState,
                onGoogleSignIn = startGoogleSignIn,
                onNavigateLogin = {
                    authViewModel.clearMessages()
                    navController.navigate(KigoRoutes.LOGIN)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(KigoRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(KigoRoutes.AUTH_HELP) {
            HelpSupportScreen(
                isAuthenticated = false,
                onSubmitSupportReport = { _, _ -> },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun AuthenticatedKigoShell(
    myProfile: UserProfile,
    settings: UserSettings,
    mainViewModel: KigoMainViewModel,
    deepLinkKigoId: String?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val conversations by mainViewModel.conversations.collectAsStateWithLifecycle()
    val friendships by mainViewModel.friendships.collectAsStateWithLifecycle()
    val friendRequests by mainViewModel.friendRequests.collectAsStateWithLifecycle()
    val groups by mainViewModel.groups.collectAsStateWithLifecycle()
    val notifications by mainViewModel.notifications.collectAsStateWithLifecycle()
    val blockedUsers by mainViewModel.blockedUsers.collectAsStateWithLifecycle()
    val peerProfiles by mainViewModel.peerProfiles.collectAsStateWithLifecycle()
    val searchResults by mainViewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by mainViewModel.isSearching.collectAsStateWithLifecycle()
    val mediaGallery by mainViewModel.mediaGallery.collectAsStateWithLifecycle()
    val bannerMessage by mainViewModel.bannerMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        mainViewModel.clearBanner()
    }

    LaunchedEffect(deepLinkKigoId) {
        if (!deepLinkKigoId.isNullOrBlank()) {
            mainViewModel.searchUsers(deepLinkKigoId)
            navController.navigate(KigoRoutes.USER_SEARCH)
            onConsumeDeepLink()
        }
    }

    val totalUnreadChats = remember(conversations, myProfile.userId) {
        conversations.sumOf { (it.unreadCounts[myProfile.userId] ?: 0L).toInt() }
    }
    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.read }
    }
    val pendingIncomingCount = remember(friendRequests, myProfile.userId) {
        friendRequests.count { it.toUserId == myProfile.userId && it.status == "pending" }
    }

    val showTopLevelNav = currentRoute in TOP_LEVEL_TABS.map { it.route }

    val navigateTab: (String) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo(KigoRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && showTopLevelNav) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(Modifier.height(12.dp))
                    KigoLogoBadge(size = 38.dp)
                    Spacer(Modifier.height(16.dp))
                    TOP_LEVEL_TABS.forEach { tab ->
                        val badgeCount = when (tab.route) {
                            KigoRoutes.CHATS -> totalUnreadChats
                            KigoRoutes.FRIENDS -> pendingIncomingCount
                            else -> 0
                        }
                        NavigationRailItem(
                            selected = currentRoute == tab.route,
                            onClick = { navigateTab(tab.route) },
                            icon = {
                                TabIconWithBadge(icon = tab.icon, label = tab.label, badgeCount = badgeCount)
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useRail && showTopLevelNav) {
                        if (settings.navigationStyle == "floating") {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        TOP_LEVEL_TABS.forEach { tab ->
                                            val selected = currentRoute == tab.route
                                            val badgeCount = when (tab.route) {
                                                KigoRoutes.CHATS -> totalUnreadChats
                                                KigoRoutes.FRIENDS -> pendingIncomingCount
                                                else -> 0
                                            }
                                            IconButton(
                                                onClick = { navigateTab(tab.route) },
                                                modifier = Modifier.testTag(tab.testTag)
                                            ) {
                                                TabIconWithBadge(
                                                    icon = tab.icon,
                                                    label = tab.label,
                                                    badgeCount = badgeCount,
                                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                TOP_LEVEL_TABS.forEach { tab ->
                                    val badgeCount = when (tab.route) {
                                        KigoRoutes.CHATS -> totalUnreadChats
                                        KigoRoutes.FRIENDS -> pendingIncomingCount
                                        else -> 0
                                    }
                                    NavigationBarItem(
                                        selected = currentRoute == tab.route,
                                        onClick = { navigateTab(tab.route) },
                                        icon = {
                                            TabIconWithBadge(icon = tab.icon, label = tab.label, badgeCount = badgeCount)
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = KigoRoutes.HOME,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    composable(KigoRoutes.HOME) {
                        HomeDashboardScreen(
                            myProfile = myProfile,
                            conversations = conversations,
                            friendships = friendships,
                            peerProfiles = peerProfiles,
                            unreadNotificationCount = unreadNotificationsCount,
                            onOpenConversation = { cid -> navController.navigate(KigoRoutes.conversation(cid)) },
                            onOpenDirectChat = { pid ->
                                mainViewModel.openDirectChat(pid) { cid ->
                                    navController.navigate(KigoRoutes.conversation(cid))
                                }
                            },
                            onNavigateSearch = { navController.navigate(KigoRoutes.USER_SEARCH) },
                            onNavigateNotifications = { navController.navigate(KigoRoutes.NOTIFICATIONS) },
                            onNavigateProfile = { navController.navigate(KigoRoutes.PROFILE) },
                            onNavigateStudio = { navController.navigate(KigoRoutes.STUDIO) },
                            onNavigateMediaGallery = { navController.navigate(KigoRoutes.MEDIA_GALLERY) },
                            onNavigateVoiceHub = { navController.navigate(KigoRoutes.VOICE_MESSAGES) },
                            onNavigateSettings = { navController.navigate(KigoRoutes.SETTINGS) },
                            onNavigateFriends = { navigateTab(KigoRoutes.FRIENDS) }
                        )
                    }

                    composable(KigoRoutes.CHATS) {
                        ChatsScreen(
                            myUserId = myProfile.userId,
                            conversations = conversations,
                            peerProfiles = peerProfiles,
                            onOpenConversation = { cid -> navController.navigate(KigoRoutes.conversation(cid)) },
                            onToggleArchive = { mainViewModel.toggleArchiveConversation(it) },
                            onNewChat = { navController.navigate(KigoRoutes.USER_SEARCH) }
                        )
                    }

                    composable(KigoRoutes.FRIENDS) {
                        FriendsScreen(
                            myUserId = myProfile.userId,
                            friendships = friendships,
                            peerProfiles = peerProfiles,
                            blockedUsers = blockedUsers,
                            pendingIncomingCount = pendingIncomingCount,
                            onOpenDirectChat = { pid ->
                                mainViewModel.openDirectChat(pid) { cid ->
                                    navController.navigate(KigoRoutes.conversation(cid))
                                }
                            },
                            onNavigateUserSearch = { navController.navigate(KigoRoutes.USER_SEARCH) },
                            onNavigateFriendRequests = { navController.navigate(KigoRoutes.FRIEND_REQUESTS) },
                            onRemoveFriend = { mainViewModel.removeFriend(it) },
                            onBlockUser = { tid, name, kigo -> mainViewModel.blockUser(tid, name, kigo) },
                            onReportUser = { tid, reason, details -> mainViewModel.submitReport(tid, reason, details) }
                        )
                    }

                    composable(KigoRoutes.GROUPS) {
                        GroupsScreen(
                            myUserId = myProfile.userId,
                            groups = groups,
                            friendships = friendships,
                            peerProfiles = peerProfiles,
                            observeMembers = { gid -> mainViewModel.observeGroupMembers(gid) },
                            onOpenConversation = { cid -> navController.navigate(KigoRoutes.conversation(cid)) },
                            onCreateGroup = { name, desc, memberIds, isPriv, reqApp ->
                                mainViewModel.createGroup(
                                    name = name,
                                    description = desc,
                                    photoUrl = "",
                                    selectedFriendIds = memberIds,
                                    isPrivate = isPriv,
                                    requireApproval = reqApp,
                                    onCreated = { cid -> navController.navigate(KigoRoutes.conversation(cid)) }
                                )
                            },
                            onAddMember = { g, uid -> mainViewModel.addMemberToGroup(g, uid) },
                            onRemoveOrLeave = { g, uid -> mainViewModel.removeOrLeaveGroup(g, uid) },
                            onToggleAdmin = { g, uid -> mainViewModel.toggleGroupAdmin(g, uid) }
                        )
                    }

                    composable(
                        route = KigoRoutes.CONVERSATION,
                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                    ) { entry ->
                        val cid = entry.arguments?.getString("conversationId") ?: return@composable
                        val convVm: KigoConversationViewModel = viewModel(
                            key = "${cid}_${myProfile.userId}",
                            factory = viewModelFactory {
                                initializer {
                                    val appCtx = context.applicationContext
                                    KigoConversationViewModel(
                                        conversationId = cid,
                                        currentUserId = myProfile.userId,
                                        chatRepository = KigoChatRepository(appCtx),
                                        userRepository = KigoUserRepository(appCtx)
                                    )
                                }
                            }
                        )
                        IndividualConversationScreen(
                            viewModel = convVm,
                            myProfile = myProfile,
                            allConversations = conversations,
                            peerProfiles = peerProfiles,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.USER_SEARCH) {
                        UserSearchScreen(
                            searchResults = searchResults,
                            isSearching = isSearching,
                            friendships = friendships,
                            friendRequests = friendRequests,
                            myUserId = myProfile.userId,
                            onSearch = { mainViewModel.searchUsers(it) },
                            onSendRequest = { mainViewModel.sendFriendRequest(it) },
                            onOpenDirectChat = { pid ->
                                mainViewModel.openDirectChat(pid) { cid ->
                                    navController.navigate(KigoRoutes.conversation(cid))
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.FRIEND_REQUESTS) {
                        FriendRequestsScreen(
                            myUserId = myProfile.userId,
                            friendRequests = friendRequests,
                            onAccept = { mainViewModel.acceptFriendRequest(it) },
                            onReject = { mainViewModel.rejectFriendRequest(it) },
                            onCancel = { mainViewModel.cancelFriendRequest(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.MEDIA_GALLERY) {
                        MediaGalleryScreen(
                            mediaMessages = mediaGallery,
                            onRefresh = { mainViewModel.refreshMediaGallery() },
                            onOpenConversation = { cid -> navController.navigate(KigoRoutes.conversation(cid)) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.VOICE_MESSAGES) {
                        val chatRepo = remember(myProfile.userId) { KigoChatRepository(context.applicationContext) }
                        VoiceMessagesScreen(
                            myProfile = myProfile,
                            conversations = conversations,
                            peerProfiles = peerProfiles,
                            mediaMessages = mediaGallery,
                            onRefreshMedia = { mainViewModel.refreshMediaGallery() },
                            onSendVoiceToConversation = { targetConv, base64, durationMs ->
                                // Launch inside ViewModel scope via direct repository call
                                val peerId = if (targetConv.type == "direct") {
                                    targetConv.participantIds.firstOrNull { it != myProfile.userId }
                                } else null
                                val peerKey = peerId?.let { peerProfiles[it]?.publicKeyBase64 }
                                scope.launch {
                                    chatRepo.sendMessage(
                                        conversation = targetConv,
                                        senderProfile = myProfile,
                                        peerPublicKeyBase64 = peerKey,
                                        type = "voice",
                                        plainText = "",
                                        mediaDataBase64 = base64,
                                        mediaName = "voice_${System.currentTimeMillis()}.m4a",
                                        mediaMimeType = "audio/mp4",
                                        mediaDurationMs = durationMs
                                    )
                                    mainViewModel.refreshMediaGallery()
                                }
                            },
                            onOpenConversation = { cid -> navController.navigate(KigoRoutes.conversation(cid)) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.STUDIO) {
                        CustomisationStudioScreen(
                            currentSettings = settings,
                            displayName = myProfile.displayName,
                            kigoId = myProfile.kigoId,
                            onSaveSettings = { mainViewModel.saveSettings(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notifications,
                            onMarkRead = { mainViewModel.markNotificationRead(it) },
                            onOpenConversation = { cid -> navController.navigate(KigoRoutes.conversation(cid)) },
                            onOpenFriendRequests = { navController.navigate(KigoRoutes.FRIEND_REQUESTS) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.PROFILE) {
                        ProfileScreen(
                            myProfile = myProfile,
                            settings = settings,
                            onUpdateProfile = { dn, un, about, status, photo ->
                                mainViewModel.updateProfile(myProfile, dn, un, about, status, photo)
                            },
                            onToggleOnlineStatus = { mainViewModel.setOnlineStatus(it) },
                            onNavigatePrivacy = { navController.navigate(KigoRoutes.PRIVACY) },
                            onNavigateStudio = { navController.navigate(KigoRoutes.STUDIO) },
                            onNavigateAppSettings = { navController.navigate(KigoRoutes.SETTINGS) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.PRIVACY) {
                        PrivacySettingsScreen(
                            myProfile = myProfile,
                            blockedUsers = blockedUsers,
                            onUpdatePrivacy = { onl, ls, rr, req, disc ->
                                mainViewModel.updatePrivacy(onl, ls, rr, req, disc)
                            },
                            onUnblockUser = { mainViewModel.unblockUser(it) },
                            onExportData = { cb -> mainViewModel.exportMyData(cb) },
                            onDeleteAccount = {
                                mainViewModel.deleteAccount(context) {
                                    // Auth listener transitions automatically to Welcome screen
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.SETTINGS) {
                        ApplicationSettingsScreen(
                            settings = settings,
                            keyFingerprint = remember(myProfile.userId) { mainViewModel.getMyKeyFingerprint() },
                            onSaveSettings = { mainViewModel.saveSettings(it) },
                            onNavigateStudio = { navController.navigate(KigoRoutes.STUDIO) },
                            onNavigatePrivacy = { navController.navigate(KigoRoutes.PRIVACY) },
                            onNavigateHelp = { navController.navigate(KigoRoutes.HELP) },
                            onSignOut = { mainViewModel.signOut(context) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(KigoRoutes.HELP) {
                        HelpSupportScreen(
                            isAuthenticated = true,
                            onSubmitSupportReport = { reason, details ->
                                mainViewModel.submitReport(myProfile.userId, reason, details)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    icon: ImageVector,
    label: String,
    badgeCount: Int,
    tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    if (badgeCount > 0) {
        BadgedBox(badge = { Badge { Text(badgeCount.coerceAtMost(99).toString()) } }) {
            Icon(icon, contentDescription = label, tint = tint)
        }
    } else {
        Icon(icon, contentDescription = label, tint = tint)
    }
}

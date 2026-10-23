package com.shopkeeper.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.shopkeeper.ui.theme.AmberLight
import com.shopkeeper.ui.theme.AmberWarning
import com.shopkeeper.ui.theme.BlueLight
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.RedLight
import com.shopkeeper.ui.theme.Slate100
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate700
import com.shopkeeper.ui.theme.Slate900
import com.shopkeeper.ui.viewmodel.AuthState
import com.shopkeeper.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onNeedsSetup: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by authViewModel.authState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                authViewModel.handleSignInSuccess(account)
            } else {
                authViewModel.showError("Google Sign-In returned null account.")
            }
        } catch (e: ApiException) {
            val errorDescription = when (e.statusCode) {
                10 -> "DEVELOPER_ERROR (Code 10): SHA-1 fingerprint needs to be registered in Google Cloud Console for com.shopkeeper"
                12500 -> "SIGN_IN_FAILED (Code 12500): Check Google Cloud Console OAuth setup."
                12501 -> "Sign-in was cancelled."
                12502 -> "Sign-in already in progress."
                else -> "Google Sign-In Error [Code ${e.statusCode}]: ${e.localizedMessage}"
            }
            authViewModel.showError(errorDescription)
        } catch (e: Exception) {
            authViewModel.showError("Sign-in error: ${e.localizedMessage}")
        }
    }

    if (state is AuthState.Authenticated) {
        onAuthenticated()
    } else if (state is AuthState.NeedsShopSetup || state is AuthState.SelectExistingShop) {
        onNeedsSetup()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate100)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shop Icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(BlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = "Shop Logo",
                        tint = BluePrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ShopKeeper",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Slate900
                )

                Text(
                    text = "Instant price lookup & shared inventory",
                    fontSize = 13.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                Divider(color = Slate100)
                Spacer(modifier = Modifier.height(20.dp))

                // One-Tap Google Sign-In Button
                Button(
                    onClick = {
                        launcher.launch(authViewModel.getSignInIntent())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = PureWhite
                    ),
                    enabled = state !is AuthState.Loading
                ) {
                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Signing in...", color = PureWhite, fontWeight = FontWeight.SemiBold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Explore Demo / Offline Mode Button
                OutlinedButton(
                    onClick = { authViewModel.continueInDemoMode() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Slate700)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Explore in Offline / Demo Mode",
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900,
                            fontSize = 14.sp
                        )
                    }
                }

                // Error Banner
                if (state is AuthState.Error) {
                    val errorMsg = (state as AuthState.Error).message
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedLight)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "⚠️ $errorMsg",
                                color = RedAlert,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                            if (errorMsg.contains("DEVELOPER_ERROR") || errorMsg.contains("Code 10")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap 'Explore in Offline / Demo Mode' above to test prices, search, and stock right now!",
                                    color = Slate700,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Box explaining Google Cloud setup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Cloud Configuration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate700
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Google requires all Android apps using Google Sign-In to be registered once in Google Cloud Console with the app's SHA-1 fingerprint:",
                            fontSize = 11.sp,
                            color = Slate500,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Package: com.shopkeeper\nSHA-1: 3E:E2:8D:64:14:59:81:81:E6:6C:FC:34:E6:2C:4B:CD:1F:E3:0D:26",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

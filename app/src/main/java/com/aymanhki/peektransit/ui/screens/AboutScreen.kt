package com.aymanhki.peektransit.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.R
import android.content.pm.PackageManager
import com.aymanhki.peektransit.ui.components.CustomTopAppBar

data class LinkItem(
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val text: String,
    val url: String
)

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    
    val linkItems = listOf(
        LinkItem(
            icon = Icons.Default.Email,
            text = "Email",
            url = "mailto:agamyahk@myumanitoba.ca"
        ),
        LinkItem(
            iconRes = R.drawable.instagram_icon,
            text = "Instagram",
            url = "https://www.instagram.com/aymanhki/"
        ),
        LinkItem(
            iconRes = R.drawable.github_icon,
            text = "GitHub",
            url = "https://github.com/aymhki"
        )
    )
    
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { 
                    Text(
                        text = "About",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(30.dp))
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Peek Transit",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "by Ayman Agamy",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "agamyahk@myumanitoba",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Version ${packageInfo?.versionName ?: "Unknown"} (${packageInfo?.longVersionCode ?: "Unknown"})",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(linkItems) { linkItem ->
                    LinkItemCard(
                        linkItem = linkItem,
                        onClick = { openUrl(context, linkItem.url) }
                    )
                }
            }
        }
    }
}

@Composable
fun LinkItemCard(
    linkItem: LinkItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                linkItem.icon != null -> {
                    Icon(
                        imageVector = linkItem.icon,
                        contentDescription = linkItem.text,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                linkItem.iconRes != null -> {
                    Image(
                        painter = painterResource(id = linkItem.iconRes),
                        contentDescription = linkItem.text,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = linkItem.text,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
} 
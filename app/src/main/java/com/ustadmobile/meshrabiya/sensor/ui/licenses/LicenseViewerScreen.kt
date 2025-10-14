package com.ustadmobile.meshrabiya.sensor.ui.licenses

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException

/**
 * Compose screen for displaying license information and third-party attributions.
 * 
 * This screen provides a Material Design interface for browsing license information,
 * including copyright notices, license texts, and compliance information.
 */

enum class LicenseViewType {
    OVERVIEW, NOTICE, LICENSES, README
}

data class LicenseInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector = Icons.Default.Description
)

@Composable
fun LicenseViewerScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentView by remember { mutableStateOf(LicenseViewType.OVERVIEW) }
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = when (currentView) {
                        LicenseViewType.OVERVIEW -> "Open Source Licenses"
                        LicenseViewType.NOTICE -> "Copyright Notice" 
                        LicenseViewType.LICENSES -> "License Texts"
                        LicenseViewType.README -> "License Information"
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (currentView == LicenseViewType.OVERVIEW) {
                        onNavigateBack()
                    } else {
                        currentView = LicenseViewType.OVERVIEW
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.primarySurface,
            contentColor = MaterialTheme.colors.onSurface
        )
        
        // Content based on current view
        when (currentView) {
            LicenseViewType.OVERVIEW -> {
                LicenseOverviewContent(
                    onViewChange = { currentView = it },
                    context = context
                )
            }
            LicenseViewType.NOTICE -> {
                LicenseTextContent(
                    title = "Copyright Notice",
                    fileName = "NOTICE.txt",
                    context = context
                )
            }
            LicenseViewType.LICENSES -> {
                LicenseFilesContent(context = context)
            }
            LicenseViewType.README -> {
                LicenseTextContent(
                    title = "License Information",
                    fileName = "licenses/README.txt",
                    context = context
                )
            }
        }
    }
}

@Composable
private fun LicenseOverviewContent(
    onViewChange: (LicenseViewType) -> Unit,
    context: Context
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📱 Abhaya Sensor Android", 
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This application uses open source components to provide camera, sensor, and mesh networking capabilities.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
        
        item {
            // License Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📄 License Summary",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val components = listOf(
                        "🔒 Main App" to "AGPL-3.0 - Network service source availability",
                        "🔗 Meshrabiya API" to "LGPL-3.0 - Allows proprietary applications",  
                        "📱 Android Libraries" to "Apache-2.0 - Permissive with patent protection",
                        "⚡ Kotlin" to "Apache-2.0 - Programming language and coroutines"
                    )
                    
                    components.forEach { (component, license) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = component,
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.onSurface
                            )
                            Text(
                                text = license,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        
        // Navigation Cards
        val navigationItems = listOf(
            LicenseInfo(
                id = "notice",
                name = "Copyright Notice",
                description = "View copyright information and attribution notices",
                icon = Icons.Default.Info
            ),
            LicenseInfo(
                id = "licenses", 
                name = "License Texts",
                description = "Read complete license texts (AGPL, LGPL, Apache)",
                icon = Icons.Default.Description
            ),
            LicenseInfo(
                id = "readme",
                name = "License Information", 
                description = "Compliance requirements and usage guidelines",
                icon = Icons.Default.Article
            )
        )
        
        items(navigationItems) { item ->
            LicenseNavigationCard(
                licenseInfo = item,
                onClick = {
                    when (item.id) {
                        "notice" -> onViewChange(LicenseViewType.NOTICE)
                        "licenses" -> onViewChange(LicenseViewType.LICENSES)
                        "readme" -> onViewChange(LicenseViewType.README)
                    }
                }
            )
        }
        
        item {
            // Footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Questions?",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "For licensing questions or compliance support:",
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "github.com/dreadstar/abhaya-sensor-android",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LicenseNavigationCard(
    licenseInfo: LicenseInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = licenseInfo.icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = licenseInfo.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = licenseInfo.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LicenseTextContent(
    title: String,
    fileName: String,
    context: Context,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf("Loading...") }
    var isError by remember { mutableStateOf(false) }
    
    LaunchedEffect(fileName) {
        try {
            content = context.assets.open(fileName).bufferedReader().use { it.readText() }
            isError = false
        } catch (e: IOException) {
            content = "Error loading $fileName: ${e.message}"
            isError = true
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (isError) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f)
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun LicenseFilesContent(
    context: Context,
    modifier: Modifier = Modifier
) {
    val licenses = remember {
        mapOf(
            "AGPL-3.0.txt" to "GNU Affero General Public License v3.0",
            "LGPL-3.0.txt" to "GNU Lesser General Public License v3.0", 
            "Apache-2.0.txt" to "Apache License 2.0"
        )
    }
    
    var selectedLicense by remember { mutableStateOf<String?>(null) }
    
    if (selectedLicense != null) {
        // Show individual license
        LicenseTextContent(
            title = licenses[selectedLicense] ?: "License",
            fileName = "licenses/$selectedLicense",
            context = context,
            modifier = modifier
        )
    } else {
        // Show license list
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Select a license to view:",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(licenses.toList()) { (fileName, displayName) ->
                LicenseFileCard(
                    fileName = fileName,
                    displayName = displayName,
                    onClick = { selectedLicense = fileName }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LicenseFileCard(
    fileName: String,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LicenseViewerScreenPreview() {
    MaterialTheme {
        LicenseViewerScreen()
    }
}
package com.example.docusign

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.docusign.ui.theme.DocuSignTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocuSignTheme {
                Document_Preview(scanner)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Document_Preview(scanner: GmsDocumentScanner) {
    var imageUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as MainActivity

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) {
                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                imageUris = imageUris.let { existingUris ->
                    result?.pages?.map { page -> page.imageUri }?.also {uris -> existingUris.toMutableList().addAll(uris) } ?: existingUris
                }

                result?.pdf?.let { pdf ->
                    try {
                        val fos = FileOutputStream(
                            File(
                                activity.filesDir,
                                "File_${System.currentTimeMillis()}.pdf"
                            )
                        )
                        activity.contentResolver.openInputStream(pdf.uri)?.use {
                            it.copyTo(fos)
                        }
                    } catch (e: FileNotFoundException) {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = e.message ?: "Error",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }
            }
        })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Document Scanner") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(
                        text = "click to scan",
                        style = TextStyle(fontSize = 16.sp)
                    )
                },
                icon = { Icon(Icons.Default.DocumentScanner, "") },
                onClick = {
                    scanner.getStartScanIntent(activity)
                        .addOnSuccessListener {
                            scannerLauncher.launch(
                                IntentSenderRequest.Builder(it)
                                    .build()
                            )
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackBarHostState.showSnackbar(
                                    message = it.message ?: "Error",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                },
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                containerColor = FloatingActionButtonDefaults.containerColor
            )
        }
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = rememberLazyStaggeredGridState(),
            contentPadding = PaddingValues(4.dp),
            userScrollEnabled = true,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(imageUris) { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(45.dp, 45.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivity_Preview() {
    DocuSignTheme {
        Document_Preview(GmsDocumentScanning.getClient(GmsDocumentScannerOptions.Builder().build()))
    }
}
@file:OptIn(ExperimentalLayoutApi::class)

package com.mhss.app.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mhss.app.ui.R
import com.mhss.app.presentation.components.AiResultSheet
import com.mhss.app.presentation.components.GradientIconButton
import com.mhss.app.presentation.components.ShareNoteAsPlainTextOption
import com.mhss.app.ui.components.common.MyBrainAppBar
import com.mhss.app.ui.theme.Orange
import com.mhss.app.ui.toUserMessage
import com.mhss.app.util.date.formatDateDependingOnDay
import com.mikepenz.markdown.coil2.Coil2ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.mhss.app.ui.components.common.TTSSpeakerIcon
import com.mhss.app.ui.navigation.Screen
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.foundation.shape.CircleShape
import android.graphics.*
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import android.app.Activity
import android.graphics.Bitmap
import com.mhss.app.core.util.toBase64
import com.mhss.app.ui.components.notes.NotePinsRow




@Composable
fun NoteDetailsScreen(
    navController: NavHostController,
    noteId: Int,
    folderId: Int,
    viewModel: NoteDetailsViewModel = koinViewModel(
        parameters = { parametersOf(noteId, folderId) }
    ),
) {
    val state = viewModel.noteUiState
    val incomingPrefillTitle =
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("prefillNoteTitle")

    // Om vi öppnar en helt ny note och titeln är tom, sätt den en gång
    LaunchedEffect(incomingPrefillTitle) {
        if (!incomingPrefillTitle.isNullOrBlank() && state.note == null && viewModel.title.isBlank()) {
            viewModel.onEvent(NoteDetailsEvent.UpdateTitle(incomingPrefillTitle))
            // Ta bort värdet så det inte återanvänds av misstag
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("prefillNoteTitle")
        }
    }
    var openDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var openFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showShareMenu by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedPins = state.pins.toList()
    val title = viewModel.title
    val content = viewModel.content
    val pinned = state.pinned
    val readingMode = state.readingMode
    val folder = state.folder
    var lastModified by remember { mutableStateOf("") }
    var wordCountString by remember { mutableStateOf("") }
    val aiEnabled by viewModel.aiEnabled.collectAsStateWithLifecycle()
    val aiState = viewModel.aiState
    val showAiSheet = aiState.showAiSheet

    var showPinsDialog by rememberSaveable { mutableStateOf(false) }

    // === NYTT: OCR med språkval, preprocessing och CameraX ===
    var isOcrRunning by rememberSaveable { mutableStateOf(false) }

// Språkväljare (sv/en). ML Kit Latin används för båda – men vi sparar valet för ev. framtida regler.
    var selectedLang by rememberSaveable { mutableStateOf("sv") } // "sv" eller "en"

    // Skapa en fil-URI i cache som kameran/galleri skriver till (kräver FileProvider i manifestet)
    fun newImageUri(): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val photo = File.createTempFile("ocr_", ".jpg", imagesDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photo
        )
    }

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // EXIF -> rotationsgrader
    fun exifRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) { 0 }
    }

    // Ladda bitmap med korrekt rotation
    fun loadBitmapCorrectRotation(context: Context, uri: Uri): Bitmap {
        val src = if (Build.VERSION.SDK_INT >= 28) {
            val srcImage = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(srcImage) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val deg = exifRotationDegrees(context, uri)
        if (deg == 0) return src
        val m = Matrix().apply { postRotate(deg.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    // Enkel preprocessing: gråskala + kontrast (1.25x som default)
    fun preprocessBitmap(src: Bitmap, contrast: Float = 1.25f): Bitmap {
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = ColorMatrix().apply {
            setSaturation(0f) // gråskala
            val c = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, 0f,
                    0f, contrast, 0f, 0f, 0f,
                    0f, 0f, contrast, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            postConcat(c)
        }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    // (valfritt) spara kopia i Galleri så den syns i Photos
    fun saveToGalleryFromUri(src: Uri): Uri? {
        return try {
            val resolver = context.contentResolver
            val name = "MyBrain_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyBrain")
            }
            val target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (target != null) {
                resolver.openInputStream(src).use { input ->
                    resolver.openOutputStream(target).use { output ->
                        if (input == null || output == null) return null
                        input.copyTo(output)
                    }
                }
            }
            target
        } catch (_: Exception) { null }
    }

    // Kör OCR på vald Uri, med preprocessing och korrekt rotation
    fun runOcrOnUri(uri: Uri) {
        isOcrRunning = true
        try {
            val bmp = loadBitmapCorrectRotation(context, uri)
            val pre = preprocessBitmap(bmp, contrast = 1.25f)

            // Bitmap → Base64 (för OpenAI Vision)
            val base64 = pre.toBase64()

            viewModel.runVisionOcr(
                base64Image = base64,
                onDone = {
                    isOcrRunning = false
                },
                onError = {
                    isOcrRunning = false
                    // TODO: visa fel/snackbar/logg om du vill
                }
            )
        } catch (e: Exception) {
            isOcrRunning = false
            // TODO: visa fel/snackbar/logg
        }
    }


// === CameraX: visa en dialog med preview och ta bild ===
    var showCameraX by rememberSaveable { mutableStateOf(false) }

// === Image CROP (uCrop) ===
// Launcher som tar emot resultatet från uCrop och skickar vidare till OCR
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            val data = res.data
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(data!!)
            if (resultUri != null) {
                // -> Nu kör vi OCR på den beskurna bilden
                runOcrOnUri(resultUri)
            }
        } else if (res.resultCode == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
            val err = com.yalantis.ucrop.UCrop.getError(res.data!!)
            // TODO: visa fel/snackbar/logg om du vill
        }
    }

    /**
     * Startar uCrop för att låta användaren beskära bilden innan OCR.
     * @param source Uri till originalbilden
     */
    fun startCrop(source: Uri) {
        // Säkerställ Activity-context (UCrop behöver det)
        val activity = (context as? Activity) ?: return

        // Skapa en måldestination i cache
        val destUri = Uri.fromFile(
            File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        )

        // uCrop-konfiguration
        val options = com.yalantis.ucrop.UCrop.Options().apply {
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setShowCropGrid(true)
            setCompressionQuality(95)
        }

        // Bygg intenten med Activity-context
        val intent = com.yalantis.ucrop.UCrop
            .of(source, destUri)
            .withOptions(options)
            .withMaxResultSize(4096, 4096)
            .getIntent(activity)

        // Viktigt för content:// URIs
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        // Starta crop-aktiviteten
        cropLauncher.launch(intent)
    }
// === /Image CROP (uCrop) ===

// Galleri: Photo Picker + fallback GetContent
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { startCrop(it) } }

    val getContent = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { startCrop(it) } }

// Kameratillstånd
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCameraX = true
        else {
            // TODO: feedback: "Kameratillstånd krävs"
        }
    }

// Starta kameraflödet (CameraX) med permission-check
    val onCameraClick: () -> Unit = {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) showCameraX = true else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

// Starta galleri-flödet (Photo Picker med fallback)
    val onGalleryClick: () -> Unit = {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            getContent.launch("image/*")
        }
    }
// === /NYTT OCR ===



    var isTTSPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(content) {
        delay(500)
        wordCountString = content.countWords().toString()
    }
    LaunchedEffect(state.note) {
        if (state.note != null) {
            lastModified = state.note.updatedDate.formatDateDependingOnDay(context)
        }
    }
    LaunchedEffect(state.navigateUp) {
        if (state.navigateUp) {
            isTTSPlaying = false    // ← Lägg till denna rad!
            openDeleteDialog = false
            navController.navigateUp()
        }
    }
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            viewModel.onEvent(
                NoteDetailsEvent.ScreenOnStop
            )
        }
    }

    // === “Bearbetar…” overlay när OCR körs ===
    if (isOcrRunning) {
        Dialog(onDismissRequest = { /* blockera dismiss medan vi bearbetar */ }) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Bearbetar…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            MyBrainAppBar(
                title = "",
                actions = {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TTSSpeakerIcon(
                            text = content,
                            isPlaying = isTTSPlaying,
                            onPlayPauseClick = { isTTSPlaying = !isTTSPlaying }
                        )

                        IconButton(onClick = {
                            navController.navigate(
                                Screen.NotesScreen(
                                    addNote = true,
                                    prefillNoteTitle = title
                                )
                            )
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plain_text),
                                contentDescription = stringResource(R.string.add_note)
                            )
                        }

                        IconButton(onClick = {
                            navController.navigate(
                                Screen.TasksScreen(
                                    addTask = true,
                                    prefillTitle = title
                                )
                            )
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_medicine),
                                contentDescription = stringResource(R.string.add_task)
                            )
                        }

                        if (folder != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(25.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(25.dp))
                                    .clickable { openFolderDialog = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_folder),
                                    stringResource(R.string.folders),
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = folder.name,
                                    modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            IconButton(onClick = { openFolderDialog = true }) {
                                Icon(
                                    painterResource(R.drawable.ic_home_filled),
                                    stringResource(R.string.folders),
                                )
                            }
                        }

                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(
                                painterResource(android.R.drawable.ic_dialog_email),
                                stringResource(R.string.share_note),
                            )
                        }

                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            ShareNoteAsPlainTextOption(
                                title = title,
                                content = content,
                                onOptionSelected = { showShareMenu = false }
                            )
                        }

                        if (state.note != null) {
                            IconButton(onClick = { openDeleteDialog = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.delete_task)
                                )
                            }
                        }


                        IconButton(onClick = { showPinsDialog = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pin_filled),
                                contentDescription = "Pins",
                                modifier = Modifier.size(24.dp),
                                tint = Color.Red
                            )
                        }

                        IconButton(onClick = {
                            viewModel.onEvent(NoteDetailsEvent.ToggleReadingMode)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_font),
                                contentDescription = stringResource(R.string.reading_mode),
                                modifier = Modifier.size(24.dp),
                                tint = if (readingMode) Color.Green else Color.Gray
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onEvent(NoteDetailsEvent.UpdateTitle(it)) },
                label = { Text(text = stringResource(R.string.title)) },
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (selectedPins.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                NotePinsRow(
                    pinIds = selectedPins,
                    modifier = Modifier.fillMaxWidth(),
                    iconSize = 24.dp
                )
            }


            AnimatedVisibility(aiEnabled) {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        GradientIconButton(
                            text = stringResource(id = R.string.summarize),
                            iconPainter = painterResource(id = R.drawable.ic_summarize),
                        ) {
                            viewModel.onEvent(NoteDetailsEvent.Summarize(content))
                            keyboardController?.hide()
                        }
                    }
                    item {
                        GradientIconButton(
                            text = stringResource(id = R.string.auto_format),
                            iconPainter = painterResource(id = R.drawable.ic_auto_format),
                        ) {
                            viewModel.onEvent(NoteDetailsEvent.AutoFormat(content))
                            keyboardController?.hide()
                        }
                    }
                    item {
                        GradientIconButton(
                            text = stringResource(id = R.string.correct_spelling),
                            iconPainter = painterResource(id = R.drawable.ic_spelling),
                        ) {
                            viewModel.onEvent(NoteDetailsEvent.CorrectSpelling(content))
                            keyboardController?.hide()
                        }
                    }

                    item {
                        GradientIconButton(
                            text = "",
                            iconPainter = painterResource(id = R.drawable.ic_camera),
                        ) {
                            onCameraClick()         // <— byt till denna
                            keyboardController?.hide()
                        }
                    }
                    item {
                        GradientIconButton(
                            text = "",
                            iconPainter = painterResource(id = android.R.drawable.ic_menu_gallery),
                        ) {
                            onGalleryClick()
                            keyboardController?.hide()
                        }
                    }
                }
            }

            if (readingMode)
                Markdown(
                    content = content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .padding(8.dp),
                    imageTransformer = Coil2ImageTransformerImpl,
                    colors = markdownColor(
                        linkText = Color.Blue
                    ),
                    typography = markdownTypography(
                        text = MaterialTheme.typography.bodyMedium,
                        h1 = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        h2 = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        h3 = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        h4 = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        h5 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        h6 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                )
            else
                OutlinedTextField(
                    value = content,
                    onValueChange = { viewModel.onEvent(NoteDetailsEvent.UpdateContent(it)) },
                    label = {
                        Text(text = stringResource(R.string.note_content))
                    },
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 8.dp)
                )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lastModified,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
                Text(
                    text = wordCountString,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
        }
        AnimatedVisibility(
            visible = showAiSheet,
            enter = slideInVertically(
                initialOffsetY = { it }, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessVeryLow
                )
            ),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(700))
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        viewModel.onEvent(NoteDetailsEvent.AiResultHandled)
                    }, contentAlignment = Alignment.BottomCenter
            ) {
                AiResultSheet(
                    loading = aiState.loading,
                    result = aiState.result,
                    error = aiState.error?.toUserMessage(),
                    onCopyClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ai result", aiState.result.toString())
                        clipboard.setPrimaryClip(clip)
                        viewModel.onEvent(NoteDetailsEvent.AiResultHandled)
                    },
                    onReplaceClick = {
                        viewModel.onEvent(NoteDetailsEvent.UpdateContent(aiState.result.toString()))
                        viewModel.onEvent(NoteDetailsEvent.AiResultHandled)
                    },
                    onAddToNoteClick = {
                        viewModel.onEvent(NoteDetailsEvent.UpdateContent(aiState.result + "\n" + content))
                        viewModel.onEvent(NoteDetailsEvent.AiResultHandled)
                    }
                )
            }
        }
        if (openDeleteDialog)
            AlertDialog(
                shape = RoundedCornerShape(25.dp),
                onDismissRequest = { openDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_note_confirmation_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_note_confirmation_message,
                            state.note?.title!!
                        )
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(25.dp),
                        onClick = {
                            viewModel.onEvent(NoteDetailsEvent.DeleteNote(state.note!!))
                        },
                    ) {
                        Text(stringResource(R.string.delete_note), color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        shape = RoundedCornerShape(25.dp),
                        onClick = {
                            openDeleteDialog = false
                        }) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            )
        if (openFolderDialog) {
            AlertDialog(
                onDismissRequest = { openFolderDialog = false },
                confirmButton = {},
                text = {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(stringResource(R.string.change_folder))
                        FlowRow {
                            Row(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(25.dp))
                                    .clickable {
                                        viewModel.onEvent(NoteDetailsEvent.UpdateFolder(null))
                                        openFolderDialog = false
                                    }
                                    .background(
                                        if (folder == null) MaterialTheme.colorScheme.onBackground else Color.Transparent
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.none),
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (folder == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                )
                            }

                            state.folders.forEach {
                                Row(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(25.dp))
                                        .border(1.dp, Color.Gray, RoundedCornerShape(25.dp))
                                        .clickable {
                                            viewModel.onEvent(NoteDetailsEvent.UpdateFolder(it))
                                            openFolderDialog = false
                                        }
                                        .background(
                                            if (folder?.id == it.id) MaterialTheme.colorScheme.onBackground else Color.Transparent
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_folder),
                                        stringResource(R.string.folders),
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            top = 8.dp,
                                            bottom = 8.dp
                                        ),
                                        tint = if (folder?.id == it.id) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = it.name,
                                        modifier = Modifier.padding(
                                            end = 8.dp,
                                            top = 8.dp,
                                            bottom = 8.dp
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (folder?.id == it.id) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showPinsDialog) {
            NotePinsDialog(
                selectedPins = selectedPins,
                important = pinned,
                onToggleImportant = { viewModel.onEvent(NoteDetailsEvent.UpdatePinned(!pinned)) },
                onTogglePin = { pin -> viewModel.onEvent(NoteDetailsEvent.TogglePin(pin.id)) },
                onDismiss = { showPinsDialog = false }
            )
        }

        if (showCameraX) {
            CameraXCaptureDialog(
                onDismiss = { showCameraX = false },
                onPhoto = { uri -> startCrop(uri) },
                newImageUri = { newImageUri() }
            )
        }
    }
}

@Composable
fun CameraXCaptureDialog(
    onDismiss: () -> Unit,
    onPhoto: (Uri) -> Unit,
    newImageUri: () -> Uri
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
        ) {
            val context = LocalContext.current
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()

                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, selector, preview, imageCapture
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Avtryckare
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp)
            ) {
                Button(
                    shape = CircleShape,
                    onClick = {
                        val ic = imageCapture ?: return@Button
                        // Spara direkt till MediaStore (syns i Galleri)
                        val output = ImageCapture.OutputFileOptions.Builder(
                            context.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, "MyBrain_${System.currentTimeMillis()}.jpg")
                                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyBrain")
                            }
                        ).build()

                        ic.takePicture(
                            output,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onError(exception: ImageCaptureException) {
                                    onDismiss()
                                }

                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val saved = outputFileResults.savedUri ?: newImageUri()
                                    onPhoto(saved)
                                    onDismiss()
                                }
                            }
                        )
                    }
                ) { Text("Ta bild") }
            }

            // Stäng-knapp
            Box(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)) {
                OutlinedButton(onClick = onDismiss, shape = CircleShape) { Text("Stäng") }
            }
        }
    }
}

private fun String.countWords(): Int {
    var count = 0
    var inWord = false

    forEach { char ->
        if (char == ' ' || char == '\n') {
            inWord = false
        } else if (!inWord) {
            count++
            inWord = true
        }
    }

    return count

}
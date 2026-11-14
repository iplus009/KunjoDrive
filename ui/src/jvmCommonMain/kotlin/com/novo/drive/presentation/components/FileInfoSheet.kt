package com.novo.drive.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.core.files.AppStoragePaths
import com.novo.drive.core.ocr.OcrIndexService
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.common.IplusProtectedZipBundler
import com.novo.drive.presentation.platform.LocalFileSharer
import com.novo.drive.resources.Res
import com.novo.drive.resources.info_added
import com.novo.drive.resources.info_backup
import com.novo.drive.resources.info_backup_done
import com.novo.drive.resources.info_backup_failed
import com.novo.drive.resources.info_backup_none
import com.novo.drive.resources.info_backup_queued
import com.novo.drive.resources.info_backup_uploading
import com.novo.drive.resources.info_created
import com.novo.drive.resources.info_duration
import com.novo.drive.resources.info_encrypted
import com.novo.drive.resources.info_extension
import com.novo.drive.resources.info_local_copy
import com.novo.drive.resources.info_modified
import com.novo.drive.resources.info_no
import com.novo.drive.resources.info_path
import com.novo.drive.resources.info_resolution
import com.novo.drive.resources.info_sha256
import com.novo.drive.resources.info_size
import com.novo.drive.resources.info_type
import com.novo.drive.resources.info_yes
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileInfoSheet(
    file: DriveFile,
    onDismiss: () -> Unit,
    onShareToDewanGram: ((DriveFile) -> Unit)? = null
) {
    val fileRepository: FileRepository = koinInject()
    val ocrIndexService: OcrIndexService = koinInject()
    val storagePaths: AppStoragePaths = koinInject()
    val fileSharer = LocalFileSharer.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val versionKey = file.versionGroupId ?: file.id
    val versions by remember(versionKey) {
        fileRepository.observeFileVersions(versionKey)
    }.collectAsState(initial = emptyList())

    val ocrText by remember(file.id) {
        ocrIndexService.observeTextForFile(file.id)
    }.collectAsState(initial = null)

    var extractingOcr by remember { mutableStateOf(false) }
    var showZipDialog by remember { mutableStateOf(false) }
    var zipStatusMessage by remember { mutableStateOf<String?>(null) }

    if (showZipDialog) {
        var zipPassword by remember { mutableStateOf("") }
        var creatingZip by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!creatingZip) showZipDialog = false },
            title = { Text("Password-Protected AES-256 .ZIP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "এই ফাইলটিকে শক্তিশালী AES-256 পাসওয়ার্ডযুক্ত .zip ফাইলে লক করে যেকোনো অ্যাপে শেয়ার করুন:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = zipPassword,
                        onValueChange = { zipPassword = it },
                        label = { Text("পাসওয়ার্ড দিন") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = file.localPath?.takeIf { File(it).exists() } ?: run {
                            zipStatusMessage = "লোকাল কপি পাওয়া যায়নি। আগে ফাইলটি ডাউনলোড করুন।"
                            return@Button
                        }
                        creatingZip = true
                        scope.launch {
                            val outDir = File(storagePaths.cacheDir, "protected_zips")
                            val created = IplusProtectedZipBundler.createAes256Zip(
                                outputDir = outDir,
                                archiveBaseName = file.name.substringBeforeLast('.'),
                                localFilePaths = listOf(path),
                                password = zipPassword
                            )
                            creatingZip = false
                            showZipDialog = false
                            if (created != null) {
                                fileSharer.share(
                                    paths = listOf(created.absolutePath),
                                    mimeType = "application/zip",
                                    chooserTitle = "Share Protected ZIP"
                                )
                            } else {
                                zipStatusMessage = "লোকাল কপি পাওয়া যায়নি। আগে ফাইলটি ডাউনলোড করুন।"
                            }
                        }
                    },
                    enabled = !creatingZip && zipPassword.length >= 4,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(if (creatingZip) "তৈরি হচ্ছে..." else "লক ও শেয়ার করুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showZipDialog = false },
                    enabled = !creatingZip,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "v${file.versionNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Primary Share to DewanGram Action Button
            FilledTonalButton(
                onClick = {
                    onDismiss()
                    onShareToDewanGram?.invoke(file)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "দেওয়ানগ্রামে পাঠান (Share to DewanGram)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            InfoRow(stringResource(Res.string.info_size), Formatters.bytes(file.sizeBytes))
            InfoRow(stringResource(Res.string.info_type), file.mimeType)
            file.extension.takeIf { it.isNotEmpty() }?.let {
                InfoRow(stringResource(Res.string.info_extension), it.uppercase(Locale.ROOT))
            }
            if (file.width != null && file.height != null) {
                InfoRow(stringResource(Res.string.info_resolution), "${file.width} × ${file.height}")
            }
            file.durationMs?.let {
                InfoRow(
                    stringResource(Res.string.info_duration),
                    Formatters.duration(it)
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            InfoRow(stringResource(Res.string.info_created), Formatters.dateTime(file.createdAt))
            InfoRow(stringResource(Res.string.info_modified), Formatters.dateTime(file.modifiedAt))
            InfoRow(stringResource(Res.string.info_added), Formatters.dateTime(file.addedAt))

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            InfoRow(
                stringResource(Res.string.info_backup),
                when (file.backupState) {
                    BackupState.BACKED_UP -> stringResource(Res.string.info_backup_done)
                    BackupState.UPLOADING -> stringResource(Res.string.info_backup_uploading)
                    BackupState.QUEUED -> stringResource(Res.string.info_backup_queued)
                    BackupState.FAILED -> stringResource(Res.string.info_backup_failed)
                    BackupState.NONE -> stringResource(Res.string.info_backup_none)
                }
            )
            InfoRow(
                stringResource(Res.string.info_local_copy),
                stringResource(if (file.hasLocalCopy) Res.string.info_yes else Res.string.info_no)
            )
            InfoRow(
                stringResource(Res.string.info_encrypted),
                stringResource(if (file.isEncrypted) Res.string.info_yes else Res.string.info_no)
            )
            file.localPath?.let {
                InfoRow(
                    stringResource(Res.string.info_path),
                    it,
                    monospace = true
                )
            }
            file.contentHash?.let {
                InfoRow(
                    stringResource(Res.string.info_sha256),
                    it,
                    monospace = true
                )
            }

            // Version History Section (when multiple versions exist)
            if (versions.size > 1) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ভার্সন হিস্ট্রি (${versions.size} টি ভার্সন)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                versions.sortedByDescending { it.versionNumber }.forEach { ver ->
                    InfoRow(
                        label = "v${ver.versionNumber}" + if (ver.id == file.id) " (বর্তমান)" else "",
                        value = "${Formatters.bytes(ver.sizeBytes)} • ${Formatters.dateTime(ver.addedAt)}"
                    )
                }
            }

            // Offline OCR Image Text Section
            if (file.category == FileCategory.IMAGE && file.hasLocalCopy) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "অফলাইন OCR টেক্সট",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!ocrText.isNullOrBlank()) {
                        FilledTonalButton(
                            onClick = { clipboard.setText(AnnotatedString(ocrText.orEmpty())) },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("কপি")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = {
                                extractingOcr = true
                                scope.launch {
                                    ocrIndexService.indexSingleFile(file.id)
                                    extractingOcr = false
                                }
                            },
                            enabled = !extractingOcr,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(if (extractingOcr) "স্ক্যান হচ্ছে..." else "টেক্সট স্ক্যান করুন")
                        }
                    }
                }
                if (!ocrText.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ocrText.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Password-Protected AES-256 ZIP Share
            if (file.hasLocalCopy && !file.localPath.isNullOrBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                FilledTonalButton(
                    onClick = { showZipDialog = true },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("পাসওয়ার্ড লকড AES-256 .ZIP শেয়ার করুন")
                }
                zipStatusMessage?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(0.35f)
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            modifier = Modifier.weight(0.65f)
        )
    }
}

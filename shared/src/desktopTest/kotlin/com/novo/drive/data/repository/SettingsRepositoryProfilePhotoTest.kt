package com.novo.drive.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.novo.drive.core.crypto.CredentialCipher
import com.novo.drive.testing.unused
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsRepositoryProfilePhotoTest {

    private val tempDir = File.createTempFile("prefs-test", "").apply {
        delete()
        mkdirs()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
    }

    @Test
    fun `clearing profilePhotoPath removes it from DataStore instead of keeping the old path`() = runBlocking {
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempDir, "settings.preferences_pb") }
        )
        val repo = SettingsRepositoryImpl(store, unused<CredentialCipher>())

        repo.update {
            it.copy(
                avatarType = "CUSTOM",
                avatarValue = "🖼️",
                profilePhotoPath = "content://media/external/images/42"
            )
        }
        assertEquals("content://media/external/images/42", repo.preferences.first().profilePhotoPath)

        repo.update {
            it.copy(
                avatarType = "DEFAULT",
                avatarValue = "",
                profilePhotoPath = null
            )
        }
        val afterReset = repo.preferences.first()
        assertNull(afterReset.profilePhotoPath)
        assertEquals("DEFAULT", afterReset.avatarType)
        assertEquals("", afterReset.avatarValue)
    }
}


package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.ActionType
import com.example.data.remote.GeminiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from string resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura AI Phone Assistant", appName)
  }

  @Test
  fun `gemini service local fallback interprets call intent`() = runBlocking {
    val service = GeminiService()
    val parsed = service.interpretVoiceCommand("اتصل بأمي الآن", "نور", "العربية")
    assertEquals(ActionType.CALL_CONTACT, parsed.actionType)
    assertNotNull(parsed.responseSpeechText)
    assertTrue(parsed.responseSpeechText.contains("اتصال"))
  }

  @Test
  fun `gemini service local fallback interprets silent mode intent`() = runBlocking {
    val service = GeminiService()
    val parsed = service.interpretVoiceCommand("خلي الهاتف صامت", "نور", "العربية")
    assertEquals(ActionType.TOGGLE_SILENT_MODE, parsed.actionType)
  }

  @Test
  fun `gemini service local fallback interprets battery diagnostic`() = runBlocking {
    val service = GeminiService()
    val parsed = service.interpretVoiceCommand("افحص طاقة البطارية والرام", "نور", "العربية")
    assertNotNull(parsed.actionType)
  }
}

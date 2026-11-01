package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fashion Aura", appName)
  }

  @Test
  fun `smart order parser extracts phone and name`() {
    val sampleChat = "নাম: Synthetic Test Customer\nফোন: 01700000001\nঠিকানা: Sunamganj\nসাইজ: 42"
    val draft = com.example.data.parser.SmartOrderParser.parse(sampleChat)
    assertEquals("Synthetic Test Customer", draft.customerName)
    assertEquals("01700000001", draft.customerPhone)
    assertEquals("42", draft.size)
  }
}

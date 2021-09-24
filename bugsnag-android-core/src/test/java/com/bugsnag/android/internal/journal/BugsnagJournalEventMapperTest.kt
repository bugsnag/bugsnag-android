package com.bugsnag.android.internal.journal

import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertNull
import org.junit.Test

class BugsnagJournalEventMapperTest {

    @Test
    fun nullMapNullEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        assertNull(mapper.convertToEvent(null))
    }

    @Test
    fun emptyMapNullEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        assertNull(mapper.convertToEvent(emptyMap()))
    }
}

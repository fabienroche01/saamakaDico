package com.saamaka.dico.testeurs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningAccessPolicyTest {
    @Test fun premiumAndTesterAlwaysHaveAccess() {
        assertTrue(hasLearningSectionAccess(AccessLevel.PREMIUM, "WORDS", emptyMap()))
        assertTrue(hasLearningSectionAccess(AccessLevel.TESTER, "AUDIO", emptyMap()))
    }
    @Test fun exhaustedReviewQuotaLocksAllReviewSections() {
        val remaining = mapOf(LearningActivity.REVIEW to 0)
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "WORDS", remaining))
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "FAVORITES", remaining))
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "WORD_OF_DAY", remaining))
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "AUDIO", remaining))
    }
    @Test fun remainingTrialKeepsSectionOpen() {
        assertTrue(hasLearningSectionAccess(AccessLevel.FREE_ACCOUNT, "WORDS", mapOf(LearningActivity.REVIEW to 1)))
    }

    @Test fun coursesRouteDoesNotReplaceOrConsumeAnExistingLearningActivity() {
        assertTrue(learningActivityForSection("COURSES") == null)
        assertTrue(hasLearningSectionAccess(AccessLevel.GUEST, "COURSES", emptyMap()))
    }
}

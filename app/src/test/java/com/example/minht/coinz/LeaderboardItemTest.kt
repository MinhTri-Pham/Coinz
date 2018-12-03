package com.example.minht.coinz

import org.junit.Test

import org.junit.Assert.*

class LeaderboardItemTest {
    @Test
    fun toString_IsCorrect() {
        val liUser = LeaderboardItem(2,"someone",123.15432,true);
        assertEquals(liUser.toString(),"2. someone: 123.15 (current user)")

        val liNonUser = LeaderboardItem(7,"someone",123.15832,false);
        assertEquals(liNonUser.toString(),"7. someone: 123.16")
    }
}
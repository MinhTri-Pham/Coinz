package com.example.minht.coinz

import org.junit.Test

import org.junit.Assert.*

class CoinTest {
    @Test
    fun equals_sameFields() {
        val coin1 = Coin("abcd","PENY",2.34,false,false)
        val coin2 = Coin("abcd","PENY",2.34,false,false)
        assertEquals(coin1,coin2)
    }

    @Test
    fun equals_differentFields() {
        val coin1 = Coin("abcd","PENY",1.34,false,false)
        val coin2 = Coin("abcd","QUID",2.34,false,true)
        assertEquals(coin1,coin2)
    }

    @Test
    fun equals_differentIds() {
        val coin1 = Coin("abcd","PENY",1.34,false,false)
        val coin2 = Coin("abcdef","QUID",2.34,false,true)
        assertNotEquals(coin1,coin2)
    }

    @Test
    fun toString_isCorrect() {
        val coin1 = Coin("abcd","PENY",2.34,false,false)
        val coin2 = Coin("abcde","PENY",2.34,false,false)
        val coin3 = Coin("abcdef","QUID",1.34,true,false)
        assertEquals(coin1.toString(),coin2.toString())
        assertNotEquals(coin1.toString(),coin3.toString())
    }
}
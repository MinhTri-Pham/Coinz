package com.example.minht.coinz

import org.junit.Test

import org.junit.Assert.*

class GiftTest {

    @Test
    fun description_OneCoin() {
        val content = ArrayList<Coin>()
        content.add(Coin("abc","DOLR", 9.21,false,false))
        val gift = Gift("2018/12/16","someone",content)
        assertEquals(gift.shortDescription(),"2018/12/16: someone sent you 1 coin!")
        assertEquals(gift.showContents(),"DOLR, Value: 9.21")
    }

    @Test
    fun description_MultCoins() {
        val content = ArrayList<Coin>()
        content.add(Coin("abc","DOLR", 9.21,false,false))
        content.add(Coin("abc","SHIL", 6.12,false,false))
        val gift = Gift("2018/12/16","someone",content)
        assertEquals(gift.shortDescription(),"2018/12/16: someone sent you 2 coins!")
        assertEquals(gift.showContents(),"DOLR, Value: 9.21\nSHIL, Value: 6.12")
    }
}
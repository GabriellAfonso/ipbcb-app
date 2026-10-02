package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.util.WallClock

class FakeWallClock(var now: Long = 0L) : WallClock {
    override fun nowMillis(): Long = now
}

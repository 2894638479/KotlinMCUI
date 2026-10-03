package io.github.u2894638479.kotlinmcui.input

/**
 * A platform-neutral key identifier.
 *
 * Use the companion constants for standard keys. For platform-specific keys
 * not in the standard set, use [custom] with the platform's raw key code.
 *
 * ### Code ranges
 * - 1–19: navigation keys
 * - 20–29: modifier keys
 * - 30–55: letters A–Z
 * - 60–69: digits 0–9
 * - 70–94: function keys F1–F25
 * - 1000+: extension range for platform-specific keys via [custom]
 */
@JvmInline
value class Key(val code: Int) {
    companion object {
        // Navigation — 1–19
        val ENTER = Key(1)
        val ESCAPE = Key(2)
        val BACKSPACE = Key(3)
        val DELETE = Key(4)
        val TAB = Key(5)
        val LEFT = Key(6)
        val RIGHT = Key(7)
        val UP = Key(8)
        val DOWN = Key(9)
        val HOME = Key(10)
        val END = Key(11)
        val PAGE_UP = Key(12)
        val PAGE_DOWN = Key(13)
        val INSERT = Key(14)

        // Modifier keys — 20–29
        val LEFT_SHIFT = Key(20)
        val RIGHT_SHIFT = Key(21)
        val LEFT_CTRL = Key(22)
        val RIGHT_CTRL = Key(23)
        val LEFT_ALT = Key(24)
        val RIGHT_ALT = Key(25)
        val LEFT_SUPER = Key(26)
        val RIGHT_SUPER = Key(27)

        // Letters A–Z — 30–55
        val A = Key(30); val B = Key(31); val C = Key(32); val D = Key(33)
        val E = Key(34); val F = Key(35); val G = Key(36); val H = Key(37)
        val I = Key(38); val J = Key(39); val K = Key(40); val L = Key(41)
        val M = Key(42); val N = Key(43); val O = Key(44); val P = Key(45)
        val Q = Key(46); val R = Key(47); val S = Key(48); val T = Key(49)
        val U = Key(50); val V = Key(51); val W = Key(52); val X = Key(53)
        val Y = Key(54); val Z = Key(55)

        // Digits 0–9 — 60–69
        val N0 = Key(60); val N1 = Key(61); val N2 = Key(62); val N3 = Key(63)
        val N4 = Key(64); val N5 = Key(65); val N6 = Key(66); val N7 = Key(67)
        val N8 = Key(68); val N9 = Key(69)

        // Function keys — 70–94
        val F1 = Key(70); val F2 = Key(71); val F3 = Key(72); val F4 = Key(73)
        val F5 = Key(74); val F6 = Key(75); val F7 = Key(76); val F8 = Key(77)
        val F9 = Key(78); val F10 = Key(79); val F11 = Key(80); val F12 = Key(81)
        val F13 = Key(82); val F14 = Key(83); val F15 = Key(84); val F16 = Key(85)
        val F17 = Key(86); val F18 = Key(87); val F19 = Key(88); val F20 = Key(89)
        val F21 = Key(90); val F22 = Key(91); val F23 = Key(92); val F24 = Key(93)
        val F25 = Key(94)

        /** First code of the extension range reserved for [custom]. */
        const val CUSTOM_RANGE_START = 1000

        /**
         * Create a [Key] with a platform-specific raw code. Use for keys not in the standard set.
         *
         * The code must lie in the extension range, otherwise it would silently be
         * indistinguishable from a standard key.
         */
        fun custom(code: Int): Key {
            require(code >= CUSTOM_RANGE_START) {
                "custom key codes must be in the extension range ($CUSTOM_RANGE_START and above), but was $code"
            }
            return Key(code)
        }
    }
}

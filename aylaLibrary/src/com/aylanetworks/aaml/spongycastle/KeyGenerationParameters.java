/**
 * The Bouncy Castle License
 *
 * Copyright (c) 2000-2015 The Legion Of The Bouncy Castle Inc. (http://www.bouncycastle.org)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.aylanetworks.aaml.spongycastle;

import java.security.SecureRandom;

/**
 * The base class for parameters to key generators.
 */
class KeyGenerationParameters
{
    private SecureRandom    random;
    private int             strength;

    /**
     * initialise the generator with a source of randomness
     * and a strength (in bits).
     *
     * @param random the random byte source.
     * @param strength the size, in bits, of the keys we want to produce.
     */
    public KeyGenerationParameters(
        SecureRandom    random,
        int             strength)
    {
        this.random = random;
        this.strength = strength;
    }

    /**
     * return the random source associated with this
     * generator.
     *
     * @return the generators random source.
     */
    public SecureRandom getRandom()
    {
        return random;
    }

    /**
     * return the bit strength for keys produced by this generator,
     *
     * @return the strength of the keys this generator produces (in bits).
     */
    public int getStrength()
    {
        return strength;
    }
}











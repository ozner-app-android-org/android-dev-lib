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

/**
 * base interface that a public/private key block cipher needs
 * to conform to.
 */
public interface AsymmetricBlockCipher
{
    /**
     * initialise the cipher.
     *
     * @param forEncryption if true the cipher is initialised for 
     *  encryption, if false for decryption.
     * @param param the key and other data required by the cipher.
     */
    public void init(boolean forEncryption, CipherParameters param);

    /**
     * returns the largest size an input block can be.
     *
     * @return maximum size for an input block.
     */
    public int getInputBlockSize();

    /**
     * returns the maximum size of the block produced by this cipher.
     *
     * @return maximum size of the output block produced by the cipher.
     */
    public int getOutputBlockSize();

    /**
     * process the block of len bytes stored in in from offset inOff.
     *
     * @param in the input data
     * @param inOff offset into the in array where the data starts
     * @param len the length of the block to be processed.
     * @return the resulting byte array of the encryption/decryption process.
     * @exception InvalidCipherTextException data decrypts improperly.
     * @exception DataLengthException the input data is too large for the cipher.
     */
    public byte[] processBlock(byte[] in, int inOff, int len)
        throws InvalidCipherTextException;
}

















/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.compression;

import static io.netty.handler.codec.compression.Bzip2Constants.*;

/**
 * Reads and decompresses a single Bzip2 block.<br><br>
 *
 * Block decoding consists of the following stages:<br>
 * 1. Read block header<br>
 * 2. Read Huffman tables<br>
 * 3. Read and decode Huffman encoded data - {@link #decodeHuffmanData(Bzip2HuffmanStageDecoder)}<br>
 * 4. Run-Length Decoding[2] - {@link #decodeHuffmanData(Bzip2HuffmanStageDecoder)}<br>
 * 5. Inverse Move To Front Transform - {@link #decodeHuffmanData(Bzip2HuffmanStageDecoder)}<br>
 * 6. Inverse Burrows Wheeler Transform - {@link #initialiseInverseBWT()}<br>
 * 7. Run-Length Decoding[1] - {@link #read()}<br>
 * 8. Optional Block De-Randomisation - {@link #read()} (through {@link #decodeNextBWTByte()})
 * 读取和解压单个Bzip2块
 */
final class Bzip2BlockDecompressor {
    /**
     * A reader that provides bit-level reads.提供位级读取的读取器。
     */
    private final Bzip2BitReader reader;

    /**
     * Calculates the block CRC from the fully decoded bytes of the block.从块的完全解码字节计算块CRC。
     */
    private final Crc32 crc = new Crc32();

    /**
     * The CRC of the current block as read from the block header.当前块的CRC从块头中读取。
     */
    private final int blockCRC;

    /**
     * {@code true} if the current block is randomised, otherwise {@code false}.如果当前块是随机的，则为真，否则为假。
     */
    private final boolean blockRandomised;

    /* Huffman Decoding stage */
    /**
     * The end-of-block Huffman symbol. Decoding of the block ends when this is encountered.end-of-block霍夫曼的象征。当遇到这种情况时，块的解码就结束了。
     */
    int huffmanEndOfBlockSymbol;

    /**
     * Bitmap, of ranges of 16 bytes, present/not present.位图，范围为16字节，表示/不表示。
     */
    int huffmanInUse16;

    /**
     * A map from Huffman symbol index to output character. Some types of data (e.g. ASCII text)
     * may contain only a limited number of byte values; Huffman symbols are only allocated to
     * those values that actually occur in the uncompressed data.从Huffman符号索引到输出字符的映射。某些类型的数据(例如ASCII文本)可能只包含有限数量的字节值;Huffman符号仅分配给未压缩数据中实际出现的值。
     */
    final byte[] huffmanSymbolMap = new byte[256];

    /* Move To Front stage */
    /**
     * Counts of each byte value within the {@link Bzip2BlockDecompressor#huffmanSymbolMap} data.
     * Collected at the Move To Front stage, consumed by the Inverse Burrows Wheeler Transform stage.对huffmanSymbolMap数据中的每个字节值进行计数。收集在移动到前面阶段，消费的反向Burrows惠勒变换阶段。
     */
    private final int[] bwtByteCounts = new int[256];

    /**
     * The Burrows-Wheeler Transform processed data. Read at the Move To Front stage, consumed by the
     * Inverse Burrows Wheeler Transform stage.Burrows-Wheeler转换处理过的数据。读到前面的步骤，用逆Burrows Wheeler变换阶段。
     */
    private final byte[] bwtBlock;

    /**
     * Starting pointer into BWT for after untransform.启动指针到BWT后进行untransform。
     */
    private final int bwtStartPointer;

    /* Inverse Burrows-Wheeler Transform stage */
    /**
     * At each position contains the union of :-
     *   An output character (8 bits)
     *   A pointer from each position to its successor (24 bits, left shifted 8 bits)
     * As the pointer cannot exceed the maximum block size of 900k, 24 bits is more than enough to
     * hold it; Folding the character data into the spare bits while performing the inverse BWT,
     * when both pieces of information are available, saves a large number of memory accesses in
     * the final decoding stages.
     * 在每个位置都包含:-一个输出字符(8位)一个指针从每个位置到它的继任者(24位，左移8位)，因为指针不能超过900k的最大块大小，24位足够容纳它;当执行反向BWT时，将字符数据折叠到备用位中，当这两个信息都可用时，将在最后的解码阶段节省大量的内存访问。
     */
    private int[] bwtMergedPointers;

    /**
     * The current merged pointer into the Burrow-Wheeler Transform array.当前合并到Burrow-Wheeler转换数组中的指针。
     */
    private int bwtCurrentMergedPointer;

    /**
     * The actual length in bytes of the current block at the Inverse Burrows Wheeler Transform
     * stage (before final Run-Length Decoding).逆Burrows Wheeler转换阶段的当前块的实际长度(在最终的运行长度解码之前)。
     */
    private int bwtBlockLength;

    /**
     * The number of output bytes that have been decoded up to the Inverse Burrows Wheeler Transform stage.已被解码到反Burrows Wheeler转换阶段的输出字节数。
     */
    private int bwtBytesDecoded;

    /* Run-Length Encoding and Random Perturbation stage */
    /**
     * The most recently RLE decoded byte.最近的RLE解码字节。
     */
    private int rleLastDecodedByte = -1;

    /**
     * The number of previous identical output bytes decoded. After 4 identical bytes, the next byte
     * decoded is an RLE repeat count.先前解码的相同输出字节数。在4个相同的字节之后，下一个字节解码是一个RLE重复计数。
     */
    private int rleAccumulator;

    /**
     * The RLE repeat count of the current decoded byte. When this reaches zero, a new byte is decoded.当前解码字节的RLE重复计数。当这个为0时，将解码一个新的字节。
     */
    private int rleRepeat;

    /**
     * If the current block is randomised, the position within the RNUMS randomisation array.如果当前块是随机的，则在RNUMS随机数组中的位置。
     */
    private int randomIndex;

    /**
     * If the current block is randomised, the remaining count at the current RNUMS position.如果当前块是随机的，则剩余的计数位于当前RNUMS位置。
     */
    private int randomCount = Bzip2Rand.rNums(0) - 1;

    /**
     * Table for Move To Front transformations.表移动到前转换。
     */
    private final Bzip2MoveToFrontTable symbolMTF = new Bzip2MoveToFrontTable();

    // This variables is used to save current state if we haven't got enough readable bits
    private int repeatCount;
    private int repeatIncrement = 1;
    private int mtfValue;

    Bzip2BlockDecompressor(final int blockSize, final int blockCRC, final boolean blockRandomised,
                           final int bwtStartPointer, final Bzip2BitReader reader) {

        bwtBlock = new byte[blockSize];

        this.blockCRC = blockCRC;
        this.blockRandomised = blockRandomised;
        this.bwtStartPointer = bwtStartPointer;

        this.reader = reader;
    }

    /**
     * Reads the Huffman encoded data from the input stream, performs Run-Length Decoding and
     * applies the Move To Front transform to reconstruct the Burrows-Wheeler Transform array.从输入流中读取Huffman编码的数据，执行运行长度解码并应用移动到前端转换来重构Burrows-Wheeler转换数组。
     */
    boolean decodeHuffmanData(final Bzip2HuffmanStageDecoder huffmanDecoder) {
        final Bzip2BitReader reader = this.reader;
        final byte[] bwtBlock = this.bwtBlock;
        final byte[] huffmanSymbolMap = this.huffmanSymbolMap;
        final int streamBlockSize = this.bwtBlock.length;
        final int huffmanEndOfBlockSymbol = this.huffmanEndOfBlockSymbol;
        final int[] bwtByteCounts = this.bwtByteCounts;
        final Bzip2MoveToFrontTable symbolMTF = this.symbolMTF;

        int bwtBlockLength = this.bwtBlockLength;
        int repeatCount = this.repeatCount;
        int repeatIncrement = this.repeatIncrement;
        int mtfValue = this.mtfValue;

        for (;;) {
            if (!reader.hasReadableBits(HUFFMAN_DECODE_MAX_CODE_LENGTH)) {
                this.bwtBlockLength = bwtBlockLength;
                this.repeatCount = repeatCount;
                this.repeatIncrement = repeatIncrement;
                this.mtfValue = mtfValue;
                return false;
            }
            final int nextSymbol = huffmanDecoder.nextSymbol();

            if (nextSymbol == HUFFMAN_SYMBOL_RUNA) {
                repeatCount += repeatIncrement;
                repeatIncrement <<= 1;
            } else if (nextSymbol == HUFFMAN_SYMBOL_RUNB) {
                repeatCount += repeatIncrement << 1;
                repeatIncrement <<= 1;
            } else {
                if (repeatCount > 0) {
                    if (bwtBlockLength + repeatCount > streamBlockSize) {
                        throw new DecompressionException("block exceeds declared block size");
                    }
                    final byte nextByte = huffmanSymbolMap[mtfValue];
                    bwtByteCounts[nextByte & 0xff] += repeatCount;
                    while (--repeatCount >= 0) {
                        bwtBlock[bwtBlockLength++] = nextByte;
                    }

                    repeatCount = 0;
                    repeatIncrement = 1;
                }

                if (nextSymbol == huffmanEndOfBlockSymbol) {
                    break;
                }

                if (bwtBlockLength >= streamBlockSize) {
                    throw new DecompressionException("block exceeds declared block size");
                }

                mtfValue = symbolMTF.indexToFront(nextSymbol - 1) & 0xff;

                final byte nextByte = huffmanSymbolMap[mtfValue];
                bwtByteCounts[nextByte & 0xff]++;
                bwtBlock[bwtBlockLength++] = nextByte;
            }
        }
        this.bwtBlockLength = bwtBlockLength;
        initialiseInverseBWT();
        return true;
    }

    /**
     * Set up the Inverse Burrows-Wheeler Transform merged pointer array.设置反向Burrows-Wheeler转换合并指针数组。
     */
    private void initialiseInverseBWT() {
        final int bwtStartPointer = this.bwtStartPointer;
        final byte[] bwtBlock  = this.bwtBlock;
        final int[] bwtMergedPointers = new int[bwtBlockLength];
        final int[] characterBase = new int[256];

        if (bwtStartPointer < 0 || bwtStartPointer >= bwtBlockLength) {
            throw new DecompressionException("start pointer invalid");
        }

        // Cumulative character counts
        System.arraycopy(bwtByteCounts, 0, characterBase, 1, 255);
        for (int i = 2; i <= 255; i++) {
            characterBase[i] += characterBase[i - 1];
        }

        // Merged-Array Inverse Burrows-Wheeler Transform
        // Combining the output characters and forward pointers into a single array here, where we
        // have already read both of the corresponding values, cuts down on memory accesses in the
        // final walk through the array
        for (int i = 0; i < bwtBlockLength; i++) {
            int value = bwtBlock[i] & 0xff;
            bwtMergedPointers[characterBase[value]++] = (i << 8) + value;
        }

        this.bwtMergedPointers = bwtMergedPointers;
        bwtCurrentMergedPointer = bwtMergedPointers[bwtStartPointer];
    }

    /**
     * Decodes a byte from the final Run-Length Encoding stage, pulling a new byte from the
     * Burrows-Wheeler Transform stage when required.从最终的运行时编码阶段解码一个字节，在需要时从Burrows-Wheeler转换阶段提取一个新的字节。
     * @return The decoded byte, or -1 if there are no more bytes
     */
    public int read() {
        while (rleRepeat < 1) {
            if (bwtBytesDecoded == bwtBlockLength) {
                return -1;
            }

            int nextByte = decodeNextBWTByte();
            if (nextByte != rleLastDecodedByte) {
                // New byte, restart accumulation
                rleLastDecodedByte = nextByte;
                rleRepeat = 1;
                rleAccumulator = 1;
                crc.updateCRC(nextByte);
            } else {
                if (++rleAccumulator == 4) {
                    // Accumulation complete, start repetition
                    int rleRepeat = decodeNextBWTByte() + 1;
                    this.rleRepeat = rleRepeat;
                    rleAccumulator = 0;
                    crc.updateCRC(nextByte, rleRepeat);
                } else {
                    rleRepeat = 1;
                    crc.updateCRC(nextByte);
                }
            }
        }
        rleRepeat--;

        return rleLastDecodedByte;
    }

    /**
     * Decodes a byte from the Burrows-Wheeler Transform stage. If the block has randomisation
     * applied, reverses the randomisation.从Burrows-Wheeler转换阶段解码字节。如果应用了随机化，则反转随机化。
     * @return The decoded byte
     */
    private int decodeNextBWTByte() {
        int mergedPointer = bwtCurrentMergedPointer;
        int nextDecodedByte =  mergedPointer & 0xff;
        bwtCurrentMergedPointer = bwtMergedPointers[mergedPointer >>> 8];

        if (blockRandomised) {
            if (--randomCount == 0) {
                nextDecodedByte ^= 1;
                randomIndex = (randomIndex + 1) % 512;
                randomCount = Bzip2Rand.rNums(randomIndex);
            }
        }
        bwtBytesDecoded++;

        return nextDecodedByte;
    }

    public int blockLength() {
        return bwtBlockLength;
    }

    /**
     * Verify and return the block CRC. This method may only be called
     * after all of the block's bytes have been read.验证并返回块CRC。此方法只能在读取块的所有字节之后调用。
     * @return The block CRC
     */
    int checkCRC() {
        final int computedBlockCRC = crc.getCRC();
        if (blockCRC != computedBlockCRC) {
            throw new DecompressionException("block CRC error");
        }
        return computedBlockCRC;
    }
}

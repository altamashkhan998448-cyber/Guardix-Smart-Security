package com.guardix.security.face

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class FaceRecognitionEngine(
    context: Context
) : Closeable {

    companion object {
        private const val MODEL_NAME = "mobilefacenet.tflite"
        private const val EXPECTED_EMBEDDING_SIZE = 192
    }

    private val interpreter: Interpreter

    private val inputShape: IntArray
    private val outputShape: IntArray

    private val inputType: DataType
    private val outputType: DataType

    private val inputWidth: Int
    private val inputHeight: Int
    private val channelsFirst: Boolean

    init {

        val modelBuffer = context.assets.open(MODEL_NAME).use {
            val bytes = it.readBytes()

            ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(modelBuffer, options)

        inputShape = interpreter.getInputTensor(0).shape()
        outputShape = interpreter.getOutputTensor(0).shape()

        inputType = interpreter.getInputTensor(0).dataType()
        outputType = interpreter.getOutputTensor(0).dataType()

        if (inputShape.size != 4 || inputShape[0] != 1) {
            throw IllegalStateException(
                "Unsupported model input shape: ${inputShape.contentToString()}"
            )
        }

        channelsFirst = inputShape[1] == 3

        if (channelsFirst) {
            inputHeight = inputShape[2]
            inputWidth = inputShape[3]
        } else {
            inputHeight = inputShape[1]
            inputWidth = inputShape[2]

            if (inputShape[3] != 3) {
                throw IllegalStateException(
                    "Model must accept 3 RGB channels."
                )
            }
        }

        if (inputWidth <= 0 || inputHeight <= 0) {
            throw IllegalStateException("Invalid model input dimensions.")
        }

        if (
            inputType != DataType.FLOAT32 &&
            inputType != DataType.UINT8 &&
            inputType != DataType.INT8
        ) {
            throw IllegalStateException(
                "Unsupported input data type: $inputType"
            )
        }

        if (
            outputType != DataType.FLOAT32 &&
            outputType != DataType.UINT8 &&
            outputType != DataType.INT8
        ) {
            throw IllegalStateException(
                "Unsupported output data type: $outputType"
            )
        }

        val outputSize = outputShape.fold(1) { total, dimension ->
            total * dimension
        }

        if (outputSize != EXPECTED_EMBEDDING_SIZE) {
            throw IllegalStateException(
                "Expected 192 face values, but model outputs $outputSize. " +
                    "Output shape: ${outputShape.contentToString()}"
            )
        }
    }

    fun getFaceEmbedding(faceBitmap: Bitmap): FloatArray {

        require(!faceBitmap.isRecycled) {
            "Face image has already been recycled."
        }

        require(faceBitmap.width > 0 && faceBitmap.height > 0) {
            "Invalid face image dimensions."
        }

        val resizedBitmap = Bitmap.createScaledBitmap(
            faceBitmap,
            inputWidth,
            inputHeight,
            true
        )

        try {

            val inputBuffer = ByteBuffer.allocateDirect(
                interpreter.getInputTensor(0).numBytes()
            ).order(ByteOrder.nativeOrder())

            val quantization = interpreter.getInputTensor(0)
                .quantizationParams()

            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {

                    val pixel = resizedBitmap.getPixel(x, y)

                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF

                    if (channelsFirst) {
                        continue
                    }

                    putChannel(inputBuffer, red, inputType, quantization)
                    putChannel(inputBuffer, green, inputType, quantization)
                    putChannel(inputBuffer, blue, inputType, quantization)
                }
            }

            if (channelsFirst) {

                inputBuffer.clear()

                for (channel in 0..2) {
                    for (y in 0 until inputHeight) {
                        for (x in 0 until inputWidth) {

                            val pixel = resizedBitmap.getPixel(x, y)

                            val value = when (channel) {
                                0 -> (pixel shr 16) and 0xFF
                                1 -> (pixel shr 8) and 0xFF
                                else -> pixel and 0xFF
                            }

                            putChannel(
                                inputBuffer,
                                value,
                                inputType,
                                quantization
                            )
                        }
                    }
                }
            }

            inputBuffer.rewind()

            val outputBuffer = ByteBuffer.allocateDirect(
                interpreter.getOutputTensor(0).numBytes()
            ).order(ByteOrder.nativeOrder())

            interpreter.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()

            val outputQuantization = interpreter.getOutputTensor(0)
                .quantizationParams()

            val embedding = FloatArray(EXPECTED_EMBEDDING_SIZE)

            for (i in embedding.indices) {

                embedding[i] = when (outputType) {

                    DataType.FLOAT32 -> outputBuffer.float

                    DataType.UINT8 -> {
                        val value = outputBuffer.get().toInt() and 0xFF

                        if (outputQuantization.scale > 0f) {
                            (value - outputQuantization.zeroPoint) *
                                outputQuantization.scale
                        } else {
                            value.toFloat()
                        }
                    }

                    DataType.INT8 -> {
                        val value = outputBuffer.get().toInt()

                        if (outputQuantization.scale > 0f) {
                            (value - outputQuantization.zeroPoint) *
                                outputQuantization.scale
                        } else {
                            value.toFloat()
                        }
                    }

                    else -> throw IllegalStateException(
                        "Unsupported output type."
                    )
                }
            }

            return normalizeEmbedding(embedding)

        } finally {

            if (resizedBitmap !== faceBitmap &&
                !resizedBitmap.isRecycled
            ) {
                resizedBitmap.recycle()
            }
        }
    }

    private fun putChannel(
        buffer: ByteBuffer,
        pixel: Int,
        dataType: DataType,
        quantization: org.tensorflow.lite.Tensor.QuantizationParams
    ) {

        val normalized = (pixel - 127.5f) / 128f

        when (dataType) {

            DataType.FLOAT32 -> {
                buffer.putFloat(normalized)
            }

            DataType.UINT8 -> {
                val value = if (quantization.scale > 0f) {
                    normalized / quantization.scale +
                        quantization.zeroPoint
                } else {
                    pixel.toFloat()
                }

                buffer.put(
                    value.toInt().coerceIn(0, 255).toByte()
                )
            }

            DataType.INT8 -> {
                val value = if (quantization.scale > 0f) {
                    normalized / quantization.scale +
                        quantization.zeroPoint
                } else {
                    normalized * 127f
                }

                buffer.put(
                    value.toInt().coerceIn(-128, 127).toByte()
                )
            }

            else -> throw IllegalStateException(
                "Unsupported input type: $dataType"
            )
        }
    }

    private fun normalizeEmbedding(
        embedding: FloatArray
    ): FloatArray {

        if (embedding.any { !it.isFinite() }) {
            throw IllegalStateException(
                "Model generated invalid face values."
            )
        }

        val magnitude = sqrt(
            embedding.sumOf {
                (it * it).toDouble()
            }
        ).toFloat()

        if (!magnitude.isFinite() || magnitude < 0.000001f) {
            throw IllegalStateException(
                "Invalid face embedding magnitude."
            )
        }

        return FloatArray(embedding.size) { index ->
            embedding[index] / magnitude
        }
    }

    fun compareFaces(
        first: FloatArray,
        second: FloatArray
    ): Float {

        require(first.size == EXPECTED_EMBEDDING_SIZE)
        require(second.size == EXPECTED_EMBEDDING_SIZE)

        require(first.all { it.isFinite() })
        require(second.all { it.isFinite() })

        return first.indices.sumOf { index ->
            (first[index] * second[index]).toDouble()
        }.toFloat()
    }

    override fun close() {
        interpreter.close()
    }
}
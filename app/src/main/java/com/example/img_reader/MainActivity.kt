package com.example.img_reader

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewRecognizedText: TextView
    private lateinit var buttonChooseImage: Button

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        textViewRecognizedText = findViewById(R.id.textViewRecognizedText)
        buttonChooseImage = findViewById(R.id.buttonChooseImage)

        buttonChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_IMAGE) {
            val fileUri: Uri = data?.data!!
            imageView.setImageURI(fileUri)

            // Convert URI to Bitmap
            val bitmap = uriToBitmap(fileUri)
            recognizeTextFromImage(bitmap)
            detectFacesInImage(bitmap)
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun recognizeTextFromImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                textViewRecognizedText.text = visionText.text
            }
            .addOnFailureListener { e ->
                textViewRecognizedText.text = "Failed to recognize text: ${e.message}"
            }
    }

    private fun detectFacesInImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(highAccuracyOpts)

        detector.process(image)
            .addOnSuccessListener { faces ->
                // Create a mutable bitmap to draw on
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)
                val paint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                }

                // Process each detected face
                val faceCount = faces.size
                textViewRecognizedText.append("\nDetected $faceCount face(s)\n")

                for (face in faces) {
                    val bounds = face.boundingBox

                    // Draw a small square around the face
                    canvas.drawRect(bounds, paint)

                    val rotY = face.headEulerAngleY  // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ  // Head is tilted sideways rotZ degrees

                    textViewRecognizedText.append(
                        "Face bounds: $bounds\nRotation Y: $rotY\nRotation Z: $rotZ\n\n"
                    )
                }

                // Set the image with the drawn rectangles
                imageView.setImageBitmap(mutableBitmap)
            }
            .addOnFailureListener { e ->
                textViewRecognizedText.append("\nFailed to detect faces: ${e.message}")
            }
    }
}

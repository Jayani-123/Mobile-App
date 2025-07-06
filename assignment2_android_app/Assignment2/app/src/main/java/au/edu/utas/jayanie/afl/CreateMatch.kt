package au.edu.utas.jayanie.afl

import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import au.edu.utas.jayanie.afl.databinding.ActivityCreateMatchBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CreateMatch : AppCompatActivity() {
    private lateinit var ui: ActivityCreateMatchBinding
    private lateinit var getContentTeam1: ActivityResultLauncher<String>
    private lateinit var getContentTeam2: ActivityResultLauncher<String>
    private var imageUri1: Uri? = null
    private var imageUri2: Uri? = null
    private var team1LogoBase64: String = ""
    private var team2LogoBase64: String = ""
    private var selectedColor1 ="#FF000000"
    private var selectedColor2 = "#FF000000"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ui = ActivityCreateMatchBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Create Match"

        // Initialize image pickers
        getContentTeam1 = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri1 = it
                ui.imageView1.setImageURI(it)
                team1LogoBase64 = convertImageToBase64(it)
            }
        }

        getContentTeam2 = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri2 = it
                ui.imageView2.setImageURI(it)
                team2LogoBase64 = convertImageToBase64(it)
            }
        }

        // Color pickers
        ui.colorPickerButton1.setOnClickListener { showColorPicker(1) }
        ui.colorPickerButton2.setOnClickListener { showColorPicker(2) }

        // Date and time pickers
        ui.editTextDate.setOnClickListener { showDatePicker() }
        ui.matchTime.setOnClickListener { showTimePicker() }

        // Image selection buttons
        ui.selectLogoButton1.setOnClickListener { getContentTeam1.launch("image/*") }
        ui.selectLogoButton2.setOnClickListener { getContentTeam2.launch("image/*") }

        // Submit button
        ui.btnnext.setOnClickListener {
            uploadMatch()

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun convertImageToBase64(uri: Uri): String {
        return try {

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

            // Resize the image before converting to Base64
            val resizedBitmap = resizeImage(bitmap, 300, 300)

            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
            ""
        }
    }

    private fun showColorPicker(teamNumber: Int) {
        val currentColor = if (teamNumber == 1)  Color.parseColor(selectedColor1) else Color.parseColor(selectedColor2)
        val button = if (teamNumber == 1) ui.colorPickerButton1 else ui.colorPickerButton2

        AmbilWarnaDialog(this, currentColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog?) {}
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                // Convert to hex and store
                val hexColor = String.format("#%08X", color)
                if (teamNumber == 1) {
                    selectedColor1 = hexColor
                } else {
                    selectedColor2 = hexColor
                }
                button.setBackgroundColor(color)
            }
        }).show()
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select match date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            ui.editTextDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(calendar.time))
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER_TAG")
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                ui.matchTime.setText(SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun uploadMatch() {
        val matchName = ui.matchName.text.toString().trim()
        val venue = ui.venue.text.toString().trim()
        val date = ui.editTextDate.text.toString().trim()
        val time = ui.matchTime.text.toString().trim()
        val team1Name = ui.team1Name.text.toString().trim()
        val team2Name = ui.team2Name.text.toString().trim()


        if (matchName.isEmpty() || venue.isEmpty() || date.isEmpty() || time.isEmpty() ||
            team1Name.isEmpty() || team2Name.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (team1LogoBase64.isEmpty() || team2LogoBase64.isEmpty()) {
            Toast.makeText(this, "Please select logos for both teams", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Creating match...")
            setCancelable(false)
            show()
        }

        val match = Match(
            id = "",
            matchName = matchName,
            venue = venue,
            date = date,
            time = time,
            team1Name = team1Name,
            team2Name = team2Name,
            team1color = selectedColor1,
            team2color = selectedColor2,
            team1Logo = team1LogoBase64, // Storing as Base64 string
            team2Logo = team2LogoBase64 , // Storing as Base64 string
            createdAt =  null // Server-side timestamp
        )

        Firebase.firestore.collection("matches")
            .add(match)
            .addOnSuccessListener {documentReference ->
                progressDialog.dismiss()
                Toast.makeText(this, "Match created successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, CreatePlayer::class.java).apply {
                    putExtra("MATCH_ID", documentReference.id)
                    putExtra("TEAM1_LOGO", team1LogoBase64)
                    putExtra("TEAM1_NAME", team1Name)
                    putExtra("TEAM1_COLOR", selectedColor1)
                    putExtra("TEAM2_LOGO", team2LogoBase64)
                    putExtra("TEAM2_NAME", team2Name)
                    putExtra("TEAM2_COLOR", selectedColor2)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
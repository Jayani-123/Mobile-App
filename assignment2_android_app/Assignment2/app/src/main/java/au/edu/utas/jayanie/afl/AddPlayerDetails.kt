package au.edu.utas.jayanie.afl

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import au.edu.utas.jayanie.afl.databinding.ActivityAddPlayerDetailsBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPlayerDetails : AppCompatActivity() {
    private lateinit var ui: ActivityAddPlayerDetailsBinding
    private lateinit var getPlayerImage: ActivityResultLauncher<String>
    private var playerImageBase64: String = ""
    private var matchId: String? = null
    private var team1Name: String? = null
    private var team2Name: String? = null
    private var currentTeam: String? = null
    private var isEditing = false
    private var existingPlayerId: String? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var cameraImageUri: Uri? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Retrieve the match ID and team from the intent
        matchId = intent.getStringExtra("matchId") ?: run {
            Toast.makeText(this, "Error: Match ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // Get team names from intent
        team1Name = intent.getStringExtra("team1")
        team2Name = intent.getStringExtra("team2")
        currentTeam = intent.getStringExtra("currentTeam") ?: "team1"

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize view binding
        ui = ActivityAddPlayerDetailsBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Add Player"

        // Initialize spinners
        setupSpinners()

        // Making number drop down list
        val playerNumbers = (1..99).map { it.toString() }
        val numberAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            playerNumbers
        )
        numberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ui.number.adapter = numberAdapter

        // Making Age drop down list
        val playerAge = (14..50).map { it.toString() }
        val ageAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            playerAge
        )
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ui.age.adapter = ageAdapter

        // Making Height drop down list
        val playerHeight = (150..350).map { it.toString() }
        val heightAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            playerHeight
        )
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ui.Height.adapter = heightAdapter

        // Initialize the camera launcher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let {
                    ui.imageView3.setImageURI(it)
                    playerImageBase64 = convertImageToBase64(it)
                }
            }
        }

        // Image picker for player image
        getPlayerImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                ui.imageView3.setImageURI(it)
                playerImageBase64 = convertImageToBase64(it)
            }
        }

        ui.btnImage.setOnClickListener {
            showImagePickerOptions()
        }

        ui.addPlayerButton.setOnClickListener {
            addPlayerToMatch(matchId.toString())
        }
        // Handle editing existing player
        val extras = intent.extras
        if (extras != null) {
            // Check if we're editing an existing player
            existingPlayerId = extras.getString("playerId")
            if (existingPlayerId != null) {
                isEditing = true
                supportActionBar?.title = "Edit Player Details"

                // Get all player details from intent
                val name = extras.getString("name", "")
                val position = extras.getString("position", "")
                val number = extras.getInt("number", 0)
                val age = extras.getInt("age", 0)
                val height = extras.getInt("height", 0)
                playerImageBase64 = extras.getString("image", "")
                val teamName = extras.getString("teamName", "")

                ui.enterName.setText(name)

                // Set position spinner selection
                val positions = resources.getStringArray(R.array.positions)
                val positionIndex = positions.indexOf(position)
                ui.position.setSelection(if (positionIndex >= 0) positionIndex else 0)

                // Set number spinner selection
                val numberIndex = (ui.number.adapter as ArrayAdapter<String>).getPosition(number.toString())
                ui.number.setSelection(if (numberIndex >= 0) numberIndex else 0)

                // Set age spinner selection
                val ageIndex = (ui.age.adapter as ArrayAdapter<String>).getPosition(age.toString())
                ui.age.setSelection(if (ageIndex >= 0) ageIndex else 0)

                // Set height spinner selection
                val heightIndex = (ui.Height.adapter as ArrayAdapter<String>).getPosition(height.toString())
                ui.Height.setSelection(if (heightIndex >= 0) heightIndex else 0)


                // Set team spinner selection
                val teams = listOfNotNull(team1Name, team2Name)
                val teamIndex = teams.indexOf(teamName)
                if (teamIndex >= 0) {
                    ui.Team.setSelection(teamIndex)
                }

                // Load image if exists
                if (playerImageBase64.isNotEmpty()) {
                    val bitmap = base64ToBitmap(playerImageBase64)
                    ui.imageView3.setImageBitmap(bitmap)
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(ui.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSpinners() {
        // Position spinner
        val positions = resources.getStringArray(R.array.positions)
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positions)
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ui.position.adapter = positionAdapter

        // Team spinner with actual team names
        val teams = listOfNotNull(team1Name, team2Name)
        ui.Team.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teams).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Set selection based on whether we're editing or creating
        if (isEditing) {
            // When editing, select the player's existing team
            val teamName = intent.getStringExtra("teamName") ?: ""
            val teams = listOfNotNull(team1Name, team2Name)
            val teamIndex = teams.indexOf(teamName)
            if (teamIndex >= 0) {
                ui.Team.setSelection(teamIndex)
            }
        } else {
            // When creating, select based on currentTeam
            when (currentTeam) {
                "team1" -> ui.Team.setSelection(0)
                "team2" -> ui.Team.setSelection(1)
            }
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openCamera()
                1 -> getPlayerImage.launch("image/*")
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        try {
            // Create a file to save the image
            val photoFile = createImageFile()
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile
            )
            cameraImageUri = uri

            // Check camera permission first
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(uri)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraImageUri?.let { uri ->
                        takePictureLauncher.launch(uri)
                    }
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun base64ToBitmap(base64Str: String): Bitmap {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun convertImageToBase64(uri: Uri): String {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
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

    private fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun addPlayerToMatch(matchId: String) {
        val number = ui.number.selectedItem.toString().toIntOrNull() ?: 0
        val name = ui.enterName.text.toString()
        val position = ui.position.selectedItem.toString()
        val age = ui.age.selectedItem.toString().toIntOrNull() ?: 0
        val height = ui.Height.selectedItem.toString().toIntOrNull() ?: 0
        val selectedTeamName = ui.Team.selectedItem.toString()
        val selectedTeamId = when (selectedTeamName) {
            team1Name -> "team1"
            team2Name -> "team2"
            else -> currentTeam ?: "team1"
        }

        // Input validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter player name", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val matchRef = db.collection("matches").document(matchId)

        // Use existing ID if editing, otherwise create new ID
        val playerId = existingPlayerId ?: "${matchId}_${selectedTeamId}_${number}"

        val player = Player(
            id = playerId,
            number = number,
            name = name,
            position = position,
            age = age,
            image = playerImageBase64,
            height = height,
            teamName = selectedTeamName
        )

        val newPlayerCollection = when (selectedTeamId) {
            "team1" -> "playersTeam1"
            "team2" -> "playersTeam2"
            else -> "playersTeam1"
        }

        val newPlayersArrayField = when (selectedTeamId) {
            "team1" -> "playersTeam1Ids"
            "team2" -> "playersTeam2Ids"
            else -> "playersTeam1Ids"
        }

        if (isEditing) {
            // Get the original team info
            val originalTeamId = when (intent.getStringExtra("teamName")) {
                team1Name -> "team1"
                team2Name -> "team2"
                else -> currentTeam ?: "team1"
            }

            val originalPlayerCollection = when (originalTeamId) {
                "team1" -> "playersTeam1"
                "team2" -> "playersTeam2"
                else -> "playersTeam1"
            }

            val originalPlayersArrayField = when (originalTeamId) {
                "team1" -> "playersTeam1Ids"
                "team2" -> "playersTeam2Ids"
                else -> "playersTeam1Ids"
            }

            // Check if team has changed
            if (originalTeamId == selectedTeamId) {
                // Team hasn't changed - just update the player
                val updates = hashMapOf<String, Any>(
                    "number" to number,
                    "name" to name,
                    "position" to position,
                    "age" to age,
                    "height" to height,
                    "image" to playerImageBase64,
                    "teamName" to selectedTeamName
                )

                matchRef.collection(originalPlayerCollection).document(playerId)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Player updated successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating player: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Team has changed - need to move the player
                val batch = db.batch()

                // 1. Delete from original collection
                batch.delete(matchRef.collection(originalPlayerCollection).document(playerId))

                // 2. Remove from original team's array
                batch.update(matchRef, originalPlayersArrayField, FieldValue.arrayRemove(playerId))

                // 3. Add to new collection
                batch.set(matchRef.collection(newPlayerCollection).document(playerId), player)

                // 4. Add to new team's array
                batch.update(matchRef, newPlayersArrayField, FieldValue.arrayUnion(playerId))

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Player moved to $selectedTeamName successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error moving player: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            // Add new player
            matchRef.collection(newPlayerCollection).document(playerId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Toast.makeText(this, "Player with this number already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        val batch = db.batch()
                        val playerRef = matchRef.collection(newPlayerCollection).document(playerId)
                        batch.set(playerRef, player)
                        batch.update(matchRef, newPlayersArrayField, FieldValue.arrayUnion(playerId))

                        batch.commit()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Player added successfully to $selectedTeamName!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to add player: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }
    }
}
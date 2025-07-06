import 'dart:io';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:image/image.dart' as img;

class AddPlayer extends StatefulWidget {
  final String matchId;
  final String team1;
  final String team2;
  final Map<String, dynamic>? initialData;

  const AddPlayer({
    super.key,
    required this.matchId,
    required this.team1,
    required this.team2,
    this.initialData,
  });

  @override
  State<AddPlayer> createState() => _AddPlayerFormState();
}

class _AddPlayerFormState extends State<AddPlayer> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final ImagePicker _picker = ImagePicker();
  XFile? _imageFile;
  String? _base64Image;
  String? _imageFormat;

  late String _selectedTeam;
  String? _selectedPosition;
  String? _selectedNumber;
  String? _selectedAge;
  String? _selectedHeight;

  final List<String> _positions = ['Goalkeeper', 'Defender', 'Midfielder', 'Forward'];
  final List<String> _numbers = List.generate(99, (index) => (index + 1).toString());
  final List<String> _ages = List.generate(37, (index) => (index + 14).toString());
  final List<String> _heights = List.generate(201, (index) => (index + 150).toString());

  @override
  void initState() {
    super.initState();
    _selectedTeam = widget.initialData?['team'] ?? widget.team1;
    _selectedPosition = widget.initialData?['position'] ?? _positions.first;
    _selectedNumber = widget.initialData?['number']?.toString() ?? _numbers.first;
    _selectedAge = widget.initialData?['age']?.toString() ?? _ages.first;
    _selectedHeight = widget.initialData?['height']?.toString() ?? _heights.first;

    if (widget.initialData != null) {
      _nameController.text = widget.initialData!['name'] ?? '';
      _base64Image = widget.initialData!['image'];
      _imageFormat = widget.initialData!['imageFormat'];
    }
  }

  Future<void> _showImagePickerOptions() async {
    return showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("Add Photo"),
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[
                GestureDetector(
                  child: const Text("Take Photo"),
                  onTap: () {
                    Navigator.pop(context);
                    _pickImage(ImageSource.camera);
                  },
                ),
                const Padding(padding: EdgeInsets.all(8.0)),
                GestureDetector(
                  child: const Text("Choose from Gallery"),
                  onTap: () {
                    Navigator.pop(context);
                    _pickImage(ImageSource.gallery);
                  },
                ),
                const Padding(padding: EdgeInsets.all(8.0)),
                GestureDetector(
                  child: const Text("Cancel"),
                  onTap: () {
                    Navigator.pop(context);
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _pickImage(ImageSource source) async {
    try {
      final pickedFile = await _picker.pickImage(source: source);
      if (pickedFile == null) return;

      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const Center(child: CircularProgressIndicator()),
      );

      final originalBytes = await File(pickedFile.path).readAsBytes();
      if (originalBytes.isEmpty) throw Exception("Image file is empty");

      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) throw Exception("Could not decode image");

      final isPng = pickedFile.path.toLowerCase().endsWith('.png') || _isLikelyPng(originalBytes);
      _imageFormat = isPng ? 'png' : 'jpg';

      final resized = img.copyResize(decoded, width: 300);

      List<int> compressedBytes = isPng
          ? img.encodePng(resized)
          : img.encodeJpg(resized, quality: 70);

      if (compressedBytes.length > 900000) {
        throw Exception("Image is too large after compression");
      }

      if (mounted) {
        Navigator.pop(context);
        setState(() {
          _imageFile = pickedFile;
          _base64Image = base64Encode(compressedBytes);
        });
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Error processing image: ${e.toString()}")),
        );
      }
    }
  }

  bool _isLikelyPng(List<int> bytes) {
    return bytes.length >= 8 &&
        bytes[0] == 0x89 &&
        bytes[1] == 0x50 &&
        bytes[2] == 0x4E &&
        bytes[3] == 0x47;
  }

  void _submitForm() async {
    if (!_formKey.currentState!.validate()) return;

    if (_base64Image == null || _base64Image!.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please add a player photo')),
      );
      return;
    }

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => const Center(child: CircularProgressIndicator()),
    );

    try {
      final firestore = FirebaseFirestore.instance;
      final matchRef = firestore.collection('matches').doc(widget.matchId);

      final playerData = {
        'name': _nameController.text,
        'position': _selectedPosition,
        'number': int.parse(_selectedNumber!),
        'age': int.parse(_selectedAge!),
        'height': int.parse(_selectedHeight!),
        'image': _base64Image,
        'imageFormat': _imageFormat,
        'team': _selectedTeam,
        'createdAt': FieldValue.serverTimestamp(),
      };

      final newTeamCollection =
      _selectedTeam == widget.team1 ? 'playersTeam1' : 'playersTeam2';

      // EDIT existing player
      if (widget.initialData != null && widget.initialData!.containsKey('id')) {
        final playerId = widget.initialData!['id'];
        final oldTeam = widget.initialData!['team'];
        final oldTeamCollection =
        oldTeam == widget.team1 ? 'playersTeam1' : 'playersTeam2';

        playerData['id'] = playerId;

        // If team changed, move player
        if (oldTeam != _selectedTeam) {
          await matchRef.collection(oldTeamCollection).doc(playerId).delete();
          await matchRef
              .collection(newTeamCollection)
              .doc(playerId)
              .set(playerData);
        } else {
          // Just update the same document
          await matchRef
              .collection(newTeamCollection)
              .doc(playerId)
              .update(playerData);
        }

        if (mounted) {
          Navigator.pop(context); // close loading
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Player updated successfully')),
          );
          Navigator.pop(context, playerData);
        }
      } else {
        // ADD new player
        final newDoc =
        await matchRef.collection(newTeamCollection).add(playerData);
        final newId = newDoc.id;
        playerData['id'] = newId;

        // Update document to include the ID inside the data
        await matchRef
            .collection(newTeamCollection)
            .doc(newId)
            .update({'id': newId});

        if (mounted) {
          Navigator.pop(context); // close loading
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Player added successfully')),
          );
          Navigator.pop(context, playerData);
        }
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context); // close loading
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Error saving player: ${e.toString()}")),
        );
      }
    }
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.blue,
        title: Text(
          widget.initialData != null ? "Edit Player" : "Add Player",
          style: const TextStyle(color: Colors.white),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: "Player Name",
                  border: OutlineInputBorder(),
                ),
                validator: (value) => value!.isEmpty ? 'Enter name' : null,
              ),
              const SizedBox(height: 20),
              _buildDropdown("Position", _positions, _selectedPosition, (val) {
                setState(() => _selectedPosition = val!);
              }),
              const SizedBox(height: 20),
              _buildDropdown("Number", _numbers, _selectedNumber, (val) {
                setState(() => _selectedNumber = val!);
              }),
              const SizedBox(height: 20),
              _buildDropdown("Age", _ages, _selectedAge, (val) {
                setState(() => _selectedAge = val!);
              }),
              const SizedBox(height: 20),
              _buildDropdown("Height (cm)", _heights, _selectedHeight, (val) {
                setState(() => _selectedHeight = val!);
              }),
              const SizedBox(height: 20),
              _buildDropdown("Team", [widget.team1, widget.team2], _selectedTeam, (val) {
                setState(() => _selectedTeam = val!);
              }),
              const SizedBox(height: 20),
              Center(
                child: Column(
                  children: [
                    if (_base64Image != null)
                      Image.memory(
                        base64Decode(_base64Image!),
                        height: 100,
                        width: 100,
                        fit: BoxFit.cover,
                      ),
                    const SizedBox(height: 10),
                    ElevatedButton.icon(
                      icon: const Icon(Icons.photo_camera),
                      label: const Text("Add Photo"),
                      onPressed: _showImagePickerOptions,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 30),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  icon: const Icon(Icons.add),
                  label: Text(widget.initialData != null ? "Update Player" : "Add Player"),
                  onPressed: _submitForm,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 15),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDropdown(String label, List<String> items, String? selectedValue, void Function(String?) onChanged) {
    return DropdownButtonFormField<String>(
      value: selectedValue,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
      items: items.map((String value) {
        return DropdownMenuItem<String>(
          value: value,
          child: Text(value),
        );
      }).toList(),
      onChanged: onChanged,
      validator: (value) => value == null ? "Please select $label" : null,
    );
  }
}

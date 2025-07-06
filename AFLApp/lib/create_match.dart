import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'playerlist.dart';

class CreateMatch extends StatefulWidget {
  const CreateMatch({super.key});

  @override
  State<CreateMatch> createState() => _CreateMatchPageState();
}

class _CreateMatchPageState extends State<CreateMatch> {
  final _formKey = GlobalKey<FormState>();

  final TextEditingController _matchNameController = TextEditingController();
  final TextEditingController _venueController = TextEditingController();
  final TextEditingController _team1NameController = TextEditingController();
  final TextEditingController _team2NameController = TextEditingController();


  DateTime? _selectedDate;
  TimeOfDay? _selectedTime;

  Future<void> _pickDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );
    if (picked != null) {
      setState(() => _selectedDate = picked);
    }
  }

  Future<void> _pickTime(BuildContext context) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
    );
    if (picked != null) {
      setState(() => _selectedTime = picked);
    }
  }

  String get formattedDate =>
      _selectedDate == null
          ? ''
          : '${_selectedDate!.day}/${_selectedDate!.month}/${_selectedDate!
          .year}';

  String get formattedTime =>
      _selectedTime == null ? '' : _selectedTime!.format(context);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF00529B), // Blue background
        title: const Text(
          "Create Match",
          style: TextStyle(color: Colors.white), // White title text
        ),
        iconTheme: const IconThemeData(
            color: Colors.white), // Makes drawer/menu icon white
      ),
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              _buildTextField("Match Name", _matchNameController),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: GestureDetector(
                      onTap: () => _pickDate(context),
                      child: AbsorbPointer(
                        child: TextFormField(
                          decoration: const InputDecoration(
                            labelText: "Select date",
                            labelStyle: TextStyle(color: Color(0xFF00529B)),
                            enabledBorder: UnderlineInputBorder(
                              borderSide: BorderSide(color: Color(0xFF00529B)),
                            ),
                          ),
                          style: const TextStyle(color: Color(0xFF00529B)),
                          validator: (_) =>
                          _selectedDate == null ? "Date required" : null,
                          controller:
                          TextEditingController(text: formattedDate),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: GestureDetector(
                      onTap: () => _pickTime(context),
                      child: AbsorbPointer(
                        child: TextFormField(
                          decoration: const InputDecoration(
                            labelText: "Select time",
                            labelStyle: TextStyle(color: Color(0xFF00529B)),
                            enabledBorder: UnderlineInputBorder(
                              borderSide: BorderSide(color: Color(0xFF00529B)),
                            ),
                          ),
                          style: const TextStyle(color: Color(0xFF00529B)),
                          validator: (_) =>
                          _selectedTime == null ? "Time required" : null,
                          controller:
                          TextEditingController(text: formattedTime),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              _buildTextField("Venue", _venueController),
              const SizedBox(height: 20),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                      child: _buildTeamCard("Team 1", _team1NameController)),
                  const SizedBox(width: 10),
                  Expanded(
                      child: _buildTeamCard("Team 2", _team2NameController)),
                ],
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () async {
                  if (_formKey.currentState!.validate()) {
                    // Check if team names are the same
                    if (_team1NameController.text.trim().toLowerCase() ==
                        _team2NameController.text.trim().toLowerCase()) {
                      await showDialog(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: const Text('Invalid Team Names'),
                          content: const Text('Team 1 and Team 2 cannot have the same name.'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context),
                              child: const Text('OK'),
                            ),
                          ],
                        ),
                      );
                      return;
                    }

                    try {
                      final docRef = await FirebaseFirestore.instance
                          .collection('matches')
                          .add({
                        'name': _matchNameController.text,
                        'datetime': '${_selectedDate!.toIso8601String()} ${_selectedTime!.format(context)}',
                        'venue': _venueController.text,
                        'team1name': _team1NameController.text,
                        'team2name': _team2NameController.text,
                        'isFinished': false,
                        'createdAt': FieldValue.serverTimestamp(),
                      });

                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text("Match saved to Firebase")),
                      );
                      print("Team 1 entered: ${_team1NameController.text}");
                      print("Team 2 entered: ${_team2NameController.text}");

                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => PlayerList(
                            matchId: docRef.id,
                            team1: _team1NameController.text,
                            team2: _team2NameController.text,
                          ),
                        ),
                      );

                    } catch (e) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text("Error saving match: $e")),
                      );
                    }
                  }
                },

                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF00529B),
                  foregroundColor: Colors.white,
                ),
                child: const Text("Submit"),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(String label, TextEditingController controller) {
    return TextFormField(
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(color: Color(0xFF00529B)),
        enabledBorder: const UnderlineInputBorder(
          borderSide: BorderSide(color: Color(0xFF00529B)),
        ),
      ),
      style: const TextStyle(color: Color(0xFF00529B)),
      validator: (value) =>
      value == null || value.isEmpty ? "$label required" : null,
    );
  }

  Widget _buildTeamCard(String title, TextEditingController controller) {
    return Card(
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Text(
              title,
              style: const TextStyle(fontSize: 18, color: Color(0xFF00529B)),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: controller,
              decoration: const InputDecoration(
                labelText: "Team Name",
                labelStyle: TextStyle(color: Color(0xFF00529B)),
                enabledBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: Color(0xFF00529B)),
                ),
              ),
              style: const TextStyle(color: Color(0xFF00529B)),
              validator: (value) =>
              value == null || value.isEmpty ? "Team name required" : null,
            ),
          ],
        ),
      ),
    );
  }

}
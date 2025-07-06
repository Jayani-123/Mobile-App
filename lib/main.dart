import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'firebase_options.dart';
import 'create_match.dart';
import 'record_match.dart';
import 'match_history.dart';
import 'team_rank.dart';

Future main() async {
  WidgetsFlutterBinding.ensureInitialized();

  var app = await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  print("\n\nConnected to Firebase App ${app.options.projectId}\n\n");

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AFL Record',
      theme: ThemeData.light().copyWith(
        scaffoldBackgroundColor: const Color(0xFF00529B),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            minimumSize: const Size(200, 45),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
      ),
      home: const AFLHomePage(),
    );
  }
}

class AFLHomePage extends StatelessWidget {
  const AFLHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF00529B),
        title: const Text(
          "AFL Record",
          style: TextStyle(color: Colors.white),
        ),
      ),
      body: Center(
        child: SingleChildScrollView(
          child: Column(
            children: [
              Image.asset(
                'assets/afl_icon.png', // Ensure this image is declared in pubspec.yaml
                height: 250,
              ),
              const SizedBox(height: 30),
              AFLButton(
                text: 'Create Match',
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const CreateMatch(),
                    ),
                  );
                },
              ),
              AFLButton(
                text: 'Recording Match',
                onPressed: () async {
                  final query = await FirebaseFirestore.instance
                      .collection('matches')
                      .where('isFinished', isEqualTo: false)
                      .limit(1)
                      .get();

                  if (query.docs.isNotEmpty) {
                    final doc = query.docs.first;
                    final data = doc.data();

                    final matchId = doc.id;
                    final team1 = data['team1name'] ?? 'Team 1';
                    final team2 = data['team2name'] ?? 'Team 2';

                    final team1Snapshot = await FirebaseFirestore.instance
                        .collection('matches')
                        .doc(matchId)
                        .collection('playersTeam1')
                        .get();
                    final team2Snapshot = await FirebaseFirestore.instance
                        .collection('matches')
                        .doc(matchId)
                        .collection('playersTeam2')
                        .get();

                    final team1Players = team1Snapshot.docs
                        .map((doc) => {...doc.data(), 'id': doc.id})
                        .toList();
                    final team2Players = team2Snapshot.docs
                        .map((doc) => {...doc.data(), 'id': doc.id})
                        .toList();

                    if (team1Players.length < 2 || team2Players.length < 2) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(
                            'Each team must have at least 2 players to start recording.',
                          ),
                        ),
                      );
                      return;
                    }

                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => RecordMatch(
                          matchId: matchId,
                          team1: team1,
                          team2: team2,
                          team1Players: team1Players,
                          team2Players: team2Players,
                        ),
                      ),
                    );
                  } else {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text("No unfinished match found.")),
                    );
                  }
                },
              ),
              AFLButton(
                text: 'Match History',
                onPressed: () {

                Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const MatchHistoryPage(),
                ),
                );
                },
              ),
              AFLButton(
                text: 'Team Rank',
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => TeamRankPage(),
                    ),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class AFLButton extends StatelessWidget {
  final String text;
  final VoidCallback onPressed;

  const AFLButton({super.key, required this.text, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.white,
          foregroundColor: const Color(0xFF0C3D66),
          minimumSize: const Size(200, 45),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
        ),
        onPressed: onPressed,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(text),
            const SizedBox(width: 10),

          ],
        ),
      ),
    );
  }
}
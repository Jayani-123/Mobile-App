import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'dart:convert';

class ComparePlayersScreen extends StatefulWidget {
  final String matchId;
  const ComparePlayersScreen({required this.matchId, Key? key}) : super(key: key);

  @override
  _ComparePlayersScreenState createState() => _ComparePlayersScreenState();
}

class _ComparePlayersScreenState extends State<ComparePlayersScreen> {
  Player? selectedPlayer1;
  Player? selectedPlayer2;
  List<Player> team1Players = [];
  List<Player> team2Players = [];
  Map<String, PlayerStats> playerStatsDict = {};
  bool isLoading = true;
  String? errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchTeamPlayers();
  }

  Future<void> _fetchTeamPlayers() async {
    try {
      final team1Snapshot = await FirebaseFirestore.instance
          .collection('matches')
          .doc(widget.matchId)
          .collection('playersTeam1')
          .get();

      team1Players = team1Snapshot.docs.map((doc) {
        final data = doc.data();
        return Player(
          documentID: doc.id,
          name: data['name'] ?? '',
          number: data['number']?.toString() ?? '',
          position: data['position'] ?? '',
          team: data['team'] ?? 'Team 1',
          age: data['age']?.toString() ?? '',
          height: data['height']?.toString() ?? '',
          image: data['image'],
        );
      }).toList();

      final team2Snapshot = await FirebaseFirestore.instance
          .collection('matches')
          .doc(widget.matchId)
          .collection('playersTeam2')
          .get();

      team2Players = team2Snapshot.docs.map((doc) {
        final data = doc.data();
        return Player(
          documentID: doc.id,
          name: data['name'] ?? '',
          number: data['number']?.toString() ?? '',
          position: data['position'] ?? '',
          team: data['team'] ?? 'Team 2',
          age: data['age']?.toString() ?? '',
          height: data['height']?.toString() ?? '',
          image: data['image'],
        );
      }).toList();

      await _fetchPlayerStats();
    } catch (e) {
      setState(() {
        errorMessage = 'Failed to load players: $e';
        isLoading = false;
      });
    }
  }

  Future<void> _fetchPlayerStats() async {
    try {
      const types = ['Goal', 'Behind', 'Kick', 'Handball', 'Mark', 'Tackle'];
      final results = <String, List<QueryDocumentSnapshot>>{};

      await Future.wait(types.map((type) async {
        final snapshot = await FirebaseFirestore.instance
            .collection('matches')
            .doc(widget.matchId)
            .collection('actions')
            .where('action', isEqualTo: type)
            .get();
        results[type] = snapshot.docs;
      }));

      playerStatsDict = {};

      for (final type in types) {
        for (final doc in results[type] ?? []) {
          final data = doc.data();
          final number = data['playerNumber']?.toString() ?? 'Unknown';
          final playerName = data['playerName']?.toString() ?? 'Unknown Player';

          playerStatsDict.putIfAbsent(
            number,
                () => PlayerStats(
              number: number,
              name: playerName,
              goals: 0,
              behinds: 0,
              kicks: 0,
              handballs: 0,
              marks: 0,
              tackles: 0,
            ),
          );

          final stats = playerStatsDict[number]!;
          switch (type) {
            case 'Goal': playerStatsDict[number] = stats.copyWith(goals: stats.goals + 1); break;
            case 'Behind': playerStatsDict[number] = stats.copyWith(behinds: stats.behinds + 1); break;
            case 'Kick': playerStatsDict[number] = stats.copyWith(kicks: stats.kicks + 1); break;
            case 'Handball': playerStatsDict[number] = stats.copyWith(handballs: stats.handballs + 1); break;
            case 'Mark': playerStatsDict[number] = stats.copyWith(marks: stats.marks + 1); break;
            case 'Tackle': playerStatsDict[number] = stats.copyWith(tackles: stats.tackles + 1); break;
          }
        }
      }

      setState(() {
        isLoading = false;
      });
    } catch (e) {
      setState(() {
        errorMessage = 'Failed to load player stats: $e';
        isLoading = false;
      });
    }
  }

  void _showPlayerSelectionDialog(int playerNumber) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('Add Player $playerNumber'),
          content: SizedBox(
            width: double.maxFinite,
            child: ListView(
              shrinkWrap: true,
              children: [
                const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: Text(
                    'Team 1 Players',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
                ...team1Players.map((player) {
                  return ListTile(
                    leading: CircleAvatar(
                      backgroundImage: player.image != null
                          ? MemoryImage(base64Decode(player.image!))
                          : null,
                    ),
                    title: Text(player.name),
                    subtitle: Text('#${player.number} • ${player.position}'),
                    onTap: () {
                      Navigator.pop(context);
                      setState(() {
                        if (playerNumber == 1) {
                          selectedPlayer1 = player;
                        } else {
                          selectedPlayer2 = player;
                        }
                      });
                    },
                  );
                }).toList(),
                const Divider(),
                const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: Text(
                    'Team 2 Players',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
                ...team2Players.map((player) {
                  return ListTile(
                    leading: CircleAvatar(
                      backgroundImage: player.image != null
                          ? MemoryImage(base64Decode(player.image!))
                          : null,
                    ),
                    title: Text(player.name),
                    subtitle: Text('#${player.number} • ${player.position}'),
                    onTap: () {
                      Navigator.pop(context);
                      setState(() {
                        if (playerNumber == 1) {
                          selectedPlayer1 = player;
                        } else {
                          selectedPlayer2 = player;
                        }
                      });
                    },
                  );
                }).toList(),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
          ],
        );
      },
    );
  }

  Widget _buildPlayerSelectionCard(Player? player, int playerNumber) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: InkWell(
        onTap: () => _showPlayerSelectionDialog(playerNumber),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (player == null)
                Column(
                  children: [
                    const Icon(Icons.person_add, size: 40, color: Color(0xFF00529B)),
                    const SizedBox(height: 8),
                    Text(
                      'Add Player $playerNumber',
                      style: const TextStyle(
                        fontSize: 16,
                        color: Color(0xFF00529B),
                      ),
                    ),
                  ],
                )
              else
                Column(
                  children: [
                    CircleAvatar(
                      radius: 30,
                      backgroundImage: player.image != null
                          ? MemoryImage(base64Decode(player.image!))
                          : null,
                      child: player.image == null
                          ? const Icon(Icons.person, size: 30)
                          : null,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      player.name,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '#${player.number} • ${player.position}',
                      style: const TextStyle(fontSize: 14),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }

  TableRow _buildStatRow(String left, String center, String right) {
    return TableRow(
      children: [
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: Text(
            left,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: int.tryParse(left) != null && int.tryParse(right) != null
                  ? int.parse(left) > int.parse(right)
                  ? Colors.green
                  : int.parse(left) < int.parse(right)
                  ? Colors.red
                  : Colors.black
                  : Colors.black,
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: Text(
            center,
            textAlign: TextAlign.center,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: Text(
            right,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: int.tryParse(left) != null && int.tryParse(right) != null
                  ? int.parse(right) > int.parse(left)
                  ? Colors.green
                  : int.parse(right) < int.parse(left)
                  ? Colors.red
                  : Colors.black
                  : Colors.black,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildComparisonCard() {
    final stats1 = selectedPlayer1 != null ? playerStatsDict[selectedPlayer1!.number] : null;
    final stats2 = selectedPlayer2 != null ? playerStatsDict[selectedPlayer2!.number] : null;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: _buildPlayerSelectionCard(selectedPlayer1, 1),
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 8.0),
                  child: Text('VS', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                ),
                Expanded(
                  child: _buildPlayerSelectionCard(selectedPlayer2, 2),
                ),
              ],
            ),
            if (selectedPlayer1 != null && selectedPlayer2 != null) ...[
              const SizedBox(height: 20),
              const Divider(),
              const SizedBox(height: 10),
              const Text(
                'Player Comparison',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              Table(
                columnWidths: const {0: FlexColumnWidth(3), 1: FlexColumnWidth(4), 2: FlexColumnWidth(3)},
                children: [
                  _buildStatRow(selectedPlayer1!.team, 'Team', selectedPlayer2!.team),
                  _buildStatRow(selectedPlayer1!.name, 'Name', selectedPlayer2!.name),
                  _buildStatRow('#${selectedPlayer1!.number}', 'Number', '#${selectedPlayer2!.number}'),
                  _buildStatRow(selectedPlayer1!.position, 'Position', selectedPlayer2!.position),
                  _buildStatRow(selectedPlayer1!.age, 'Age', selectedPlayer2!.age),
                  _buildStatRow(selectedPlayer1!.height, 'Height', selectedPlayer2!.height),
                  _buildStatRow('${stats1?.goals ?? 0}', 'Goals', '${stats2?.goals ?? 0}'),
                  _buildStatRow('${stats1?.behinds ?? 0}', 'Behinds', '${stats2?.behinds ?? 0}'),
                  _buildStatRow('${stats1?.score ?? 0}', 'Score', '${stats2?.score ?? 0}'),
                  _buildStatRow('${stats1?.kicks ?? 0}', 'Kicks', '${stats2?.kicks ?? 0}'),
                  _buildStatRow('${stats1?.handballs ?? 0}', 'Handballs', '${stats2?.handballs ?? 0}'),
                  _buildStatRow('${stats1?.disposals ?? 0}', 'Disposals', '${stats2?.disposals ?? 0}'),
                  _buildStatRow('${stats1?.marks ?? 0}', 'Marks', '${stats2?.marks ?? 0}'),
                  _buildStatRow('${stats1?.tackles ?? 0}', 'Tackles', '${stats2?.tackles ?? 0}'),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : errorMessage != null
          ? Center(child: Text(errorMessage!))
          : SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _buildComparisonCard(),
          ],
        ),
      ),
    );
  }
}

class Player {
  final String documentID;
  final String name;
  final String number;
  final String position;
  final String team;
  final String age;
  final String height;
  final String? image;

  Player({
    required this.documentID,
    required this.name,
    required this.number,
    required this.position,
    required this.team,
    required this.age,
    required this.height,
    this.image,
  });
}

class PlayerStats {
  final String number;
  final String name;
  final int goals;
  final int behinds;
  final int kicks;
  final int handballs;
  final int marks;
  final int tackles;

  PlayerStats({
    required this.number,
    required this.name,
    required this.goals,
    required this.behinds,
    required this.kicks,
    required this.handballs,
    required this.marks,
    required this.tackles,
  });

  int get disposals => kicks + handballs;
  int get score => goals * 6 + behinds;

  PlayerStats copyWith({
    String? number,
    String? name,
    int? goals,
    int? behinds,
    int? kicks,
    int? handballs,
    int? marks,
    int? tackles,
  }) {
    return PlayerStats(
      number: number ?? this.number,
      name: name ?? this.name,
      goals: goals ?? this.goals,
      behinds: behinds ?? this.behinds,
      kicks: kicks ?? this.kicks,
      handballs: handballs ?? this.handballs,
      marks: marks ?? this.marks,
      tackles: tackles ?? this.tackles,
    );
  }
}
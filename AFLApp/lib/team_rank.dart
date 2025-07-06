import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';

class TeamRankPage extends StatefulWidget {
  const TeamRankPage({Key? key}) : super(key: key);

  @override
  _TeamRankPageState createState() => _TeamRankPageState();
}

class _TeamRankPageState extends State<TeamRankPage> {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  List<Team> _teams = [];
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadTeamData();
  }

  Future<void> _loadTeamData() async {
    try {
      final matchSnapshot = await _firestore.collection('matches').get();
      final matchDocs = matchSnapshot.docs;

      var teamPoints = <String, int>{};
      var teamStats = <String, TeamStats>{};

      final futures = matchDocs.map((matchDoc) async {
        final matchId = matchDoc.id;
        final actionsSnapshot = await _firestore
            .collection('matches')
            .doc(matchId)
            .collection('actions')
            .get();

        final actionDocs = actionsSnapshot.docs;
        final teamActions = <String, List<QueryDocumentSnapshot>>{};
        for (final doc in actionDocs) {
          final teamName = doc.get('team') ?? '';
          teamActions.putIfAbsent(teamName, () => []).add(doc);
        }

        final teamNames = teamActions.keys.where((name) => name.isNotEmpty).toList();
        if (teamNames.length != 2) return;

        final team1Name = teamNames[0];
        final team2Name = teamNames[1];

        final team1Actions = teamActions[team1Name] ?? [];
        final team2Actions = teamActions[team2Name] ?? [];

        final team1Goals = team1Actions.where((a) => a.get('action') == 'Goal').length;
        final team1Behinds = team1Actions.where((a) => a.get('action') == 'Behind').length;
        final team1Score = (team1Goals * 6) + team1Behinds;

        final team2Goals = team2Actions.where((a) => a.get('action') == 'Goal').length;
        final team2Behinds = team2Actions.where((a) => a.get('action') == 'Behind').length;
        final team2Score = (team2Goals * 6) + team2Behinds;

        teamStats.update(
          team1Name,
              (stats) => TeamStats(
            goals: stats.goals + team1Goals,
            behinds: stats.behinds + team1Behinds,
            score: stats.score + team1Score,
          ),
          ifAbsent: () => TeamStats(
            goals: team1Goals,
            behinds: team1Behinds,
            score: team1Score,
          ),
        );

        teamStats.update(
          team2Name,
              (stats) => TeamStats(
            goals: stats.goals + team2Goals,
            behinds: stats.behinds + team2Behinds,
            score: stats.score + team2Score,
          ),
          ifAbsent: () => TeamStats(
            goals: team2Goals,
            behinds: team2Behinds,
            score: team2Score,
          ),
        );

        if (team1Score > team2Score) {
          teamPoints.update(team1Name, (points) => points + 4, ifAbsent: () => 4);
          teamPoints.update(team2Name, (points) => points + 0, ifAbsent: () => 0);
        } else if (team2Score > team1Score) {
          teamPoints.update(team2Name, (points) => points + 4, ifAbsent: () => 4);
          teamPoints.update(team1Name, (points) => points + 0, ifAbsent: () => 0);
        } else {
          teamPoints.update(team1Name, (points) => points + 2, ifAbsent: () => 2);
          teamPoints.update(team2Name, (points) => points + 2, ifAbsent: () => 2);
        }
      });

      await Future.wait(futures);

      setState(() {
        _teams = teamStats.entries.map((entry) {
          final teamName = entry.key;
          final stats = entry.value;
          return Team(
            name: teamName,
            goals: stats.goals,
            behinds: stats.behinds,
            score: stats.score,
            points: teamPoints[teamName] ?? 0,
          );
        }).toList();

        _teams.sort((a, b) {
          if (a.points != b.points) {
            return b.points.compareTo(a.points);
          }
          return b.score.compareTo(a.score);
        });

        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load team data: $e';
        _isLoading = false;
      });
    }
  }

  Color getRankColor(int index) {
    switch (index) {
      case 0:
        return Colors.red;
      case 1:
        return Colors.orange;
      case 2:
        return Colors.yellow;
      default:
        return const Color(0xFF00529B);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Team Rank', style: TextStyle(color: Colors.white)),
        backgroundColor: const Color(0xFF00529B),
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      backgroundColor: Colors.white,
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? Center(child: Text(_errorMessage!))
          : _teams.isEmpty
          ? const Center(child: Text('No teams found'))
          : Padding(
        padding: const EdgeInsets.all(12),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            boxShadow: [
              BoxShadow(
                color: Colors.blue.withOpacity(0.2),
                spreadRadius: 2,
                blurRadius: 8,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          child: SingleChildScrollView(
            scrollDirection: Axis.vertical,
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: DataTable(
                headingRowColor: MaterialStateProperty.all(Colors.blue[600]),
                columnSpacing: 24,
                columns: const [
                  DataColumn(label: Text('Rank', style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white))),
                  DataColumn(label: Text('Team', style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white))),
                  DataColumn(label: Text('Point', style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white))),
                  DataColumn(label: Text('Score', style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white))),
                  DataColumn(label: Text('Goal', style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white))),
                  DataColumn(label: Text('Behind', style: TextStyle(fontWeight: FontWeight.bold,color: Colors.white))),
                ],
                rows: List<DataRow>.generate(
                  _teams.length,
                      (index) {
                    final team = _teams[index];
                    return DataRow.byIndex(
                      index: index,
                      color: MaterialStateProperty.resolveWith<Color?>(
                            (Set<MaterialState> states) {
                          return index.isEven ? Colors.lightBlue[50] : null;
                        },
                      ),
                      cells: [
                        DataCell(
                          Container(
                            width: 32,
                            height: 32,
                            decoration: BoxDecoration(
                              color: getRankColor(index),
                              shape: BoxShape.circle,
                            ),
                            alignment: Alignment.center,
                            child: Text(
                              '${index + 1}',
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ),
                        DataCell(Text(team.name)),
                        DataCell(Text('${team.points}')),
                        DataCell(Text('${team.score}')),
                        DataCell(Text('${team.goals}')),
                        DataCell(Text('${team.behinds}')),
                      ],
                    );
                  },
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class Team {
  final String name;
  final int goals;
  final int behinds;
  final int score;
  final int points;

  Team({
    required this.name,
    required this.goals,
    required this.behinds,
    required this.score,
    required this.points,
  });
}

class TeamStats {
  final int goals;
  final int behinds;
  final int score;

  TeamStats({
    required this.goals,
    required this.behinds,
    required this.score,
  });
}

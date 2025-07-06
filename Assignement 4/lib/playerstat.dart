import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'compare_players.dart';

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

class TeamComparisonScreen extends StatefulWidget {
  final String matchId;
  final String team1Name;
  final String team2Name;

  const TeamComparisonScreen({
    required this.matchId,
    required this.team1Name,
    required this.team2Name,
    Key? key,
  }) : super(key: key);

  @override
  _TeamComparisonScreenState createState() => _TeamComparisonScreenState();
}

class _TeamComparisonScreenState extends State<TeamComparisonScreen> {
  List<PlayerStats> team1Stats = [];
  List<PlayerStats> team2Stats = [];
  bool isLoading = true;
  String? errorMessage;
  int selectedQuarter = 0; // 0 = All quarters, 1-4 = specific quarter

  @override
  void initState() {
    super.initState();
    _loadPlayerStats();
  }

  Future<void> _loadPlayerStats() async {
    try {
      setState(() {
        isLoading = true;
        errorMessage = null;
      });

      await Future.wait([
        _loadTeamStats(widget.team1Name, true),
        _loadTeamStats(widget.team2Name, false),
      ]);
    } catch (e) {
      setState(() {
        errorMessage = 'Failed to load player stats: $e';
        isLoading = false;
      });
    }
  }

  Future<void> _loadTeamStats(String teamName, bool isTeam1) async {
    final types = ['Goal', 'Behind', 'Kick', 'Handball', 'Mark', 'Tackle'];
    final results = <String, List<QueryDocumentSnapshot>>{};

    await Future.wait(types.map((type) async {
      Query query = FirebaseFirestore.instance
          .collection('matches')
          .doc(widget.matchId)
          .collection('actions')
          .where('team', isEqualTo: teamName)
          .where('action', isEqualTo: type);

      if (selectedQuarter > 0) {
        query = query.where('quarter', isEqualTo: selectedQuarter);
      }

      final snapshot = await query.get();
      results[type] = snapshot.docs;
    }));

    final playerStatsMap = <String, PlayerStats>{};

    for (final type in types) {
      for (final doc in results[type] ?? []) {
        final data = doc.data() as Map<String, dynamic>;
        final numberRaw = data['playerNumber'];
        final number = numberRaw != null ? numberRaw.toString() : 'Unknown';
        final name = data['playerName']?.toString() ?? 'Unknown Player';

        playerStatsMap.putIfAbsent(
          number,
              () => PlayerStats(
            number: number,
            name: name,
            goals: 0,
            behinds: 0,
            kicks: 0,
            handballs: 0,
            marks: 0,
            tackles: 0,
          ),
        );

        final stats = playerStatsMap[number]!;

        switch (type) {
          case 'Goal':
            playerStatsMap[number] = stats.copyWith(goals: stats.goals + 1);
            break;
          case 'Behind':
            playerStatsMap[number] = stats.copyWith(behinds: stats.behinds + 1);
            break;
          case 'Kick':
            playerStatsMap[number] = stats.copyWith(kicks: stats.kicks + 1);
            break;
          case 'Handball':
            playerStatsMap[number] = stats.copyWith(handballs: stats.handballs + 1);
            break;
          case 'Mark':
            playerStatsMap[number] = stats.copyWith(marks: stats.marks + 1);
            break;
          case 'Tackle':
            playerStatsMap[number] = stats.copyWith(tackles: stats.tackles + 1);
            break;
        }
      }
    }

    final playerStatsList = playerStatsMap.values.toList();
    playerStatsList.sort((a, b) => a.number.compareTo(b.number));

    setState(() {
      if (isTeam1) {
        team1Stats = playerStatsList;
      } else {
        team2Stats = playerStatsList;
      }
      isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: const Text('Players'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Team Stats'),
              Tab(text: 'Player Compare'),
            ],
          ),
        ),
        backgroundColor: Colors.white,
        body: errorMessage != null
            ? Center(child: Text(errorMessage!))
            : isLoading
            ? const Center(child: CircularProgressIndicator())
            : TabBarView(
          children: [
            Column(
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text('Filter by Quarter:'),
                      const SizedBox(width: 10),
                      DropdownButton<int>(
                        value: selectedQuarter,
                        items: [
                          const DropdownMenuItem(
                            value: 0,
                            child: Text('All Quarters'),
                          ),
                          ...List.generate(4, (index) => index + 1)
                              .map((quarter) => DropdownMenuItem(
                            value: quarter,
                            child: Text('Quarter $quarter'),
                          ))
                              .toList(),
                        ],
                        onChanged: (value) {
                          setState(() {
                            selectedQuarter = value ?? 0;
                            _loadPlayerStats();
                          });
                        },
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(8.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _buildTeamTable(widget.team1Name, team1Stats),
                        const SizedBox(height: 20),
                        _buildTeamTable(widget.team2Name, team2Stats),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            ComparePlayersScreen(matchId: widget.matchId),
          ],
        ),
      ),
    );
  }

  Widget _buildTeamTable(String teamName, List<PlayerStats> stats) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          color:Colors.blue,
          child: Text(
            '$teamName (${selectedQuarter == 0 ? 'All Quarters' : 'Quarter $selectedQuarter'})',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
            textAlign: TextAlign.center,
          ),
        ),
        Table(
          columnWidths: const {
            0: FlexColumnWidth(1),
            1: FlexColumnWidth(3),
            2: FlexColumnWidth(1),
            3: FlexColumnWidth(1),
            4: FlexColumnWidth(1),
            5: FlexColumnWidth(1),
            6: FlexColumnWidth(1),
            7: FlexColumnWidth(1),
          },
          border: TableBorder.all(color: Colors.grey),
          children: [
            const TableRow(
              decoration: BoxDecoration(color: Color(0xFFFFF9C4)),
              children: [
                Padding(padding: EdgeInsets.all(4), child: Text('#', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('Name', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('G', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('B', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('K', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('H', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('M', textAlign: TextAlign.center)),
                Padding(padding: EdgeInsets.all(4), child: Text('T', textAlign: TextAlign.center)),
              ],
            ),
            for (var stat in stats)
              TableRow(
                children: [
                  Padding(padding: const EdgeInsets.all(4), child: Text(stat.number, textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text(stat.name, textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text('${stat.goals}', textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text('${stat.behinds}', textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text('${stat.kicks}', textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text('${stat.handballs}', textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text('${stat.marks}', textAlign: TextAlign.center)),
                  Padding(padding: const EdgeInsets.all(4), child: Text('${stat.tackles}', textAlign: TextAlign.center)),
                ],
              ),
          ],
        ),
      ],
    );
  }
}

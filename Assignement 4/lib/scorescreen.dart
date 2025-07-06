import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:fl_chart/fl_chart.dart';
import 'dart:math';

/// Represents the scores and statistics for a quarter of a match
class QuarterScores {
  int goals;
  int behinds;
  int kicks;
  int handballs;
  int marks;
  int tackles;

  QuarterScores({
    this.goals = 0,
    this.behinds = 0,
    this.kicks = 0,
    this.handballs = 0,
    this.marks = 0,
    this.tackles = 0,
  });

  /// Calculates total points (goals * 6 + behinds)
  int totalPoints() => goals * 6 + behinds;

  /// Formats score as "goals.behinds (totalPoints)"
  String formattedScore() => '$goals.$behinds (${totalPoints()})';

  /// Calculates total disposals (kicks + handballs)
  int disposals() => kicks + handballs;

  /// Formats disposals as "(kicks + handballs) total"
  String formattedDisposals() => '($kicks + $handballs) ${disposals()}';

  /// Formats marks count as string
  String formattedMarks() => '$marks';

  /// Formats tackles count as string
  String formattedTackles() => '$tackles';
}

/// Screen that displays match scores and statistics
class ScoreScreen extends StatefulWidget {
  final String matchId;
  final String team1Name;
  final String team2Name;

  const ScoreScreen({
    required this.matchId,
    required this.team1Name,
    required this.team2Name,
    Key? key,
  }) : super(key: key);

  @override
  _ScoreScreenState createState() => _ScoreScreenState();
}

class _ScoreScreenState extends State<ScoreScreen> {
  late Map<String, QuarterScores> team1QuarterlyScores = {};
  late Map<String, QuarterScores> team2QuarterlyScores = {};
  late QuarterScores team1TotalStats = QuarterScores();
  late QuarterScores team2TotalStats = QuarterScores();
  bool matchIsFinished = false;
  bool isLastMatch = false;
  String leadingText = "Loading...";
  List<FlSpot> wormScoreData = [];

  @override
  void initState() {
    super.initState();
    loadMatchData();
  }

  /// Loads all match data including status and team statistics
  Future<void> loadMatchData() async {
    await loadMatchStatus();
    await loadTeamStatistics(widget.team1Name, true);
    await loadTeamStatistics(widget.team2Name, false);
    await loadWormData();
  }

  /// Loads whether the match is finished from Firestore
  Future<void> loadMatchStatus() async {
    if (widget.matchId.isEmpty) return;
    try {
      final doc = await FirebaseFirestore.instance
          .collection('matches')
          .doc(widget.matchId)
          .get();

      if (doc.exists) {
        setState(() {
          matchIsFinished = doc.get('isFinished') ?? false;
        });
      }
    } catch (e) {
      print("Error fetching match status: $e");
    }
  }

  /// Loads all match actions to build worm graph data
  Future<void> loadWormData() async {
    try {
      final allActions = await FirebaseFirestore.instance
          .collection('matches')
          .doc(widget.matchId)
          .collection('actions')
          .get();

      setState(() {
        wormScoreData = buildWormScoreData(allActions.docs);
      });
    } catch (e) {
      print("Error loading worm data: $e");
    }
  }

  /// Processes all match actions to create worm graph data points
  List<FlSpot> buildWormScoreData(List<QueryDocumentSnapshot> allActions) {
    allActions.sort((a, b) => a.get('timestamp').compareTo(b.get('timestamp')));

    List<FlSpot> scorePoints = [];
    int team1Points = 0;
    int team2Points = 0;

    for (int i = 0; i < allActions.length; i++) {
      var action = allActions[i];
      String team = action.get('team');
      String type = action.get('action');

      int value = 0;
      if (type == 'Goal') value = 6;
      if (type == 'Behind') value = 1;

      if (team == widget.team1Name) {
        team1Points += value;
      } else if (team == widget.team2Name) {
        team2Points += value;
      }

      int margin = team1Points - team2Points;
      scorePoints.add(FlSpot(i.toDouble(), margin.toDouble()));
    }

    return scorePoints;
  }

  /// Loads statistics for a specific team from Firestore
  Future<void> loadTeamStatistics(String teamName, bool isTeam1) async {
    if (teamName.isEmpty) return;

    final types = ['Goal', 'Behind', 'Kick', 'Handball', 'Mark', 'Tackle'];
    final results = <String, List<QueryDocumentSnapshot>>{};

    try {
      await Future.wait(types.map((type) async {
        final query = await FirebaseFirestore.instance
            .collection('matches')
            .doc(widget.matchId)
            .collection('actions')
            .where('team', isEqualTo: teamName)
            .where('action', isEqualTo: type)
            .get();
        results[type] = query.docs;
      }));

      processTeamData(results, isTeam1);
    } catch (e) {
      print("Error loading team statistics: $e");
    }
  }

  /// Processes raw Firestore data into QuarterScores objects
  void processTeamData(
      Map<String, List<QueryDocumentSnapshot>> results, bool isTeam1) {
    final goals = results['Goal'] ?? [];
    final behinds = results['Behind'] ?? [];
    final kicks = results['Kick'] ?? [];
    final handballs = results['Handball'] ?? [];
    final marks = results['Mark'] ?? [];
    final tackles = results['Tackle'] ?? [];

    final scores = calculateQuarterlyScores(goals, behinds);
    final total = QuarterScores(
      goals: goals.length,
      behinds: behinds.length,
      kicks: kicks.length,
      handballs: handballs.length,
      marks: marks.length,
      tackles: tackles.length,
    );

    setState(() {
      if (isTeam1) {
        team1QuarterlyScores = scores;
        team1TotalStats = total;
      } else {
        team2QuarterlyScores = scores;
        team2TotalStats = total;
      }
      updateLeadingText();
    });
  }

  /// Calculates quarterly scores from goal and behind actions
  Map<String, QuarterScores> calculateQuarterlyScores(
      List<QueryDocumentSnapshot> goals, List<QueryDocumentSnapshot> behinds) {
    final result = <String, QuarterScores>{};
    for (var quarter = 1; quarter <= 4; quarter++) {
      final gCount = goals.where((doc) => doc.get('quarter') == quarter).length;
      final bCount = behinds.where((doc) => doc.get('quarter') == quarter).length;
      result['Q$quarter'] = QuarterScores(goals: gCount, behinds: bCount);
    }
    return result;
  }

  /// Updates the leading text based on current scores
  void updateLeadingText() {
    final team1Points = team1TotalStats.totalPoints();
    final team2Points = team2TotalStats.totalPoints();

    setState(() {
      leadingText = matchIsFinished
          ? team1Points > team2Points
          ? "${widget.team1Name} wins!"
          : team2Points > team1Points
          ? "${widget.team2Name} wins!"
          : "Match is a draw"
          : team1Points > team2Points
          ? "${widget.team1Name} is leading"
          : team2Points > team1Points
          ? "${widget.team2Name} is leading"
          : "Scores are tied";
    });
  }


  /// Builds the Scoreworm margin line chart widget
  Widget _buildGraph() {
    final quarterMarginData = buildQuarterMarginData();

    if (quarterMarginData.isEmpty) {
      return const Center(child: Text('No data yet'));
    }

    // Determine min/max Y values based on data
    double minY = quarterMarginData.map((spot) => spot.y).reduce(min) - 5;
    double maxY = quarterMarginData.map((spot) => spot.y).reduce(max) + 5;

    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        children: [
          Text(
            'Score Worm Graph',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
              color: Colors.blue,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _buildLegendItem(Colors.blue, widget.team1Name),
              const SizedBox(width: 20),
              _buildLegendItem(Colors.red, widget.team2Name),
            ],
          ),
          const SizedBox(height: 16),
          Expanded(
            child: LineChart(
              LineChartData(
                minX: 1,
                maxX: 4,
                minY: minY,
                maxY: maxY,
                titlesData: FlTitlesData(
                  bottomTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      getTitlesWidget: (value, meta) {
                        // Only show labels for whole numbers (quarters)
                        if (value == value.toInt()) {
                          return Padding(
                            padding: const EdgeInsets.only(top: 8.0),
                            child: Text(
                              'Q${value.toInt()}',
                              style: const TextStyle(fontSize: 12),
                            ),
                          );
                        }
                        return const SizedBox.shrink();
                      },
                      reservedSize: 28,
                      interval: 1, // This ensures we only get labels at integer values
                    ),
                  ),
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 40,
                      getTitlesWidget: (value, meta) {
                        return Text(
                          value.toInt().toString(),
                          style: const TextStyle(fontSize: 10),
                        );
                      },
                    ),
                  ),
                  rightTitles: AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  topTitles: AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                gridData: FlGridData(
                  show: true,
                  drawVerticalLine: true,
                  horizontalInterval: 10,
                  verticalInterval: 1,
                ),
                borderData: FlBorderData(show: true),
                lineBarsData: [
                  // Main line with colored segments
                  ..._buildColoredLineSegments(quarterMarginData),
                  // Zero line
                  LineChartBarData(
                    spots: [FlSpot(1, 0), FlSpot(4, 0)],
                    isCurved: false,
                    color: Colors.grey,
                    barWidth: 1,
                    dotData: FlDotData(show: false),
                  ),
                ],
                lineTouchData: LineTouchData(
                  touchTooltipData: LineTouchTooltipData(
                    getTooltipItems: (List<LineBarSpot> touchedSpots) {
                      return touchedSpots.map((spot) {
                        final quarter = spot.x.toInt();
                        final margin = spot.y.toInt();
                        final t1Score = team1QuarterlyScores['Q$quarter']?.totalPoints() ?? 0;
                        final t2Score = team2QuarterlyScores['Q$quarter']?.totalPoints() ?? 0;

                        String teamText;
                        if (margin > 0) {
                          teamText = '${widget.team1Name} by $margin\n($t1Score - $t2Score)';
                        } else if (margin < 0) {
                          teamText = '${widget.team2Name} by ${-margin}\n($t2Score - $t1Score)';
                        } else {
                          teamText = 'Scores level\n($t1Score - $t2Score)';
                        }

                        return LineTooltipItem(
                          'Quarter $quarter\n$teamText',
                          const TextStyle(color: Colors.white),
                        );
                      }).toList();
                    },
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  List<LineChartBarData> _buildColoredLineSegments(List<FlSpot> spots) {
    List<LineChartBarData> segments = [];

    for (int i = 0; i < spots.length - 1; i++) {
      final startSpot = spots[i];
      final endSpot = spots[i+1];
      final startMargin = startSpot.y;
      final endMargin = endSpot.y;

      Color color;
      if (startMargin >= 0 && endMargin >= 0) {
        color = Colors.blue;
      } else if (startMargin < 0 && endMargin < 0) {
        color = Colors.red;
      } else {
        // Transition between positive and negative
        color = Colors.grey;
      }

      segments.add(
        LineChartBarData(
          spots: [startSpot, endSpot],
          isCurved: false,
          color: color,
          barWidth: 2,
          dotData: FlDotData(
            show: i == spots.length - 2, // Only show dot at last point
          ),
          belowBarData: BarAreaData(show: false),
          // Disable touch for the connecting lines
          preventCurveOverShooting: true,
          preventCurveOvershootingThreshold: 0,
        ),
      );
    }

    // Add separate bar data just for the dots with touch handling
    segments.add(
      LineChartBarData(
        spots: spots,
        isCurved: false,
        color: Colors.transparent,
        barWidth: 0,
        dotData: FlDotData(
          show: true,
          getDotPainter: (spot, percent, barData, index) {
            final margin = spot.y;
            return FlDotCirclePainter(
              radius: 5,
              color: margin >= 0 ? Colors.blue : Colors.red,
              strokeWidth: 2,
              strokeColor: Colors.white,
            );
          },
        ),
        belowBarData: BarAreaData(show: false),
      ),
    );

    return segments;
  }
  /// Builds a legend item for the graph
  Widget _buildLegendItem(Color color, String text) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 4),
        Text(text, style: const TextStyle(fontSize: 12)),
      ],
    );
  }

  /// Creates data points for quarter-by-quarter margin (non-cumulative)
  List<FlSpot> buildQuarterMarginData() {
    List<FlSpot> spots = [];

    for (int i = 1; i <= 4; i++) {
      final quarterKey = 'Q$i';
      final t1Score = team1QuarterlyScores[quarterKey]?.totalPoints() ?? 0;
      final t2Score = team2QuarterlyScores[quarterKey]?.totalPoints() ?? 0;
      final margin = t1Score - t2Score;

      spots.add(FlSpot(i.toDouble(), margin.toDouble()));
    }

    return spots;
  }

  /// Builds the main UI with tabs for scores and graph
  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: const Text('Match Scores'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Score'),
              Tab(text: 'Graph'),
            ],
          ),
        ),
        backgroundColor: Colors.white,
        body: TabBarView(
          children: [
            // Score Tab
            SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.emoji_events, color: Colors.orange),
                          const SizedBox(width: 12),
                          Text(
                            leadingText,
                            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.blue,
                            ),
                          ),
                        ],
                      ),
                    ),
                    _buildScoreTable(),
                  ],
                ),
              ),
            ),
            // Graph Tab
            Center(child: _buildGraph()),
          ],
        ),
      ),
    );
  }

  /// Builds the main score table widget
  Widget _buildScoreTable() {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Table(
          columnWidths: const {
            0: FlexColumnWidth(2),
            1: FlexColumnWidth(1),
            2: FlexColumnWidth(2),
          },
          children: [
            // Header row
            TableRow(
              decoration: BoxDecoration(
                color: Colors.lightBlue,
                border: const Border(bottom: BorderSide(width: 1, color: Colors.grey)),
              ),
              children: [
                _buildHeaderCell(widget.team1Name),
                _buildHeaderCell('VS'),
                _buildHeaderCell(widget.team2Name),
              ],
            ),

            // Quarter rows
            for (var quarter in ['Q1', 'Q2', 'Q3', 'Q4'])
              TableRow(
                decoration: const BoxDecoration(
                  border: Border(bottom: BorderSide(width: 0.5, color: Colors.grey)),
                ),
                children: [
                  _buildScoreCell(team1QuarterlyScores[quarter]?.formattedScore() ?? '0.0 (0)'),
                  _buildQuarterLabelCell(quarter),
                  _buildScoreCell(team2QuarterlyScores[quarter]?.formattedScore() ?? '0.0 (0)'),
                ],
              ),

            // Final score row
            TableRow(
              decoration: BoxDecoration(
                color: Color(0xFFFFF9C4),
                border: const Border(bottom: BorderSide(width: 1, color: Colors.grey)),
              ),
              children: [
                _buildFinalScoreCell(
                    '${team1TotalStats.totalPoints()} (${team1TotalStats.goals}.${team1TotalStats.behinds})'),
                _buildLabelCell('Final'),
                _buildFinalScoreCell(
                    '${team2TotalStats.totalPoints()} (${team2TotalStats.goals}.${team2TotalStats.behinds})'),
              ],
            ),

            // Disposals row
            TableRow(
              decoration: const BoxDecoration(
                border: Border(bottom: BorderSide(width: 0.5, color: Colors.grey)),
              ),
              children: [
                _buildStatCell(team1TotalStats.formattedDisposals()),
                _buildLabelCell('Disposals'),
                _buildStatCell(team2TotalStats.formattedDisposals()),
              ],
            ),

            // Marks row
            TableRow(
              decoration: const BoxDecoration(
                border: Border(bottom: BorderSide(width: 0.5, color: Colors.grey)),
              ),
              children: [
                _buildStatCell(team1TotalStats.formattedMarks()),
                _buildLabelCell('Marks'),
                _buildStatCell(team2TotalStats.formattedMarks()),
              ],
            ),

            // Tackles row
            TableRow(
              children: [
                _buildStatCell(team1TotalStats.formattedTackles()),
                _buildLabelCell('Tackles'),
                _buildStatCell(team2TotalStats.formattedTackles()),
              ],
            ),
          ],
        ),
      ),
    );
  }

  /// Helper to build header table cells
  Widget _buildHeaderCell(String text) {
    return TableCell(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 16,color: Colors.white,
          ),
        ),
      ),
    );
  }

  /// Helper to build regular score cells
  Widget _buildScoreCell(String text) {
    return TableCell(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 14),
        ),
      ),
    );
  }

  /// Helper to build final score cells with bold styling
  Widget _buildFinalScoreCell(String text) {
    return TableCell(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle( color: Color(0xFF00529B),
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }

  /// Helper to build quarter label cells
  Widget _buildQuarterLabelCell(String quarter) {
    return TableCell(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Text(
          quarter,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            color: Color(0xFF00529B),
          ),
        ),
      ),
    );
  }

  /// Helper to build label cells with grey text
  Widget _buildLabelCell(String text) {
    return TableCell(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            color: Color(0xFF00529B),
          ),
        ),
      ),
    );
  }

  /// Helper to build statistic cells
  Widget _buildStatCell(String text) {
    return TableCell(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 14),
        ),
      ),
    );
  }
}
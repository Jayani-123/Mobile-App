import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'actions_view.dart';
import 'scorescreen.dart';
import 'playerstat.dart';
import 'package:fluttertoast/fluttertoast.dart';

/// Screen for recording match data including scores, player actions, and timing
class RecordMatch extends StatefulWidget {
  final String matchId;
  final String team1;
  final String team2;
  final List<Map<String, dynamic>> team1Players;
  final List<Map<String, dynamic>> team2Players;

  const RecordMatch({
    Key? key,
    required this.matchId,
    required this.team1,
    required this.team2,
    required this.team1Players,
    required this.team2Players,
  }) : super(key: key);

  @override
  _RecordMatchState createState() => _RecordMatchState();
}

class _RecordMatchState extends State<RecordMatch> with SingleTickerProviderStateMixin {
  // Tab controller for the main navigation tabs
  late TabController _tabController;
  int _currentTabIndex = 0;

  // Firestore instance for database operations
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  // Match scoring variables
  int _team1Score = 0;
  int _team2Score = 0;

  // Timing variables
  int _elapsedSeconds = 0;
  int _quarterElapsedSeconds = 0;
  bool _isTimerRunning = false;
  int _currentQuarter = 1;
  final int _quarterDuration = 1200; // 20 minutes in seconds
  Timer? _timer;

  // Player selection variables
  Map<String, dynamic>? _selectedPlayer;
  String? _selectedTeam;

  // Match action tracking
  List<Map<String, dynamic>> _matchActions = [];

  @override
  void initState() {
    super.initState();
    // Initialize tab controller with 4 tabs
    _tabController = TabController(length: 4, vsync: this);
    _tabController.addListener(() {
      setState(() {
        _currentTabIndex = _tabController.index;
      });
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _timer?.cancel();
    super.dispose();
  }

  /// Starts the match timer
  void _startTimer() {
    if (!_isTimerRunning) {
      _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
        setState(() {
          _quarterElapsedSeconds++;
          _elapsedSeconds++;
          // Check if quarter time has elapsed
          if (_quarterElapsedSeconds >= _quarterDuration) {
            _timer?.cancel();
            _isTimerRunning = false;
            _showQuarterEndAlert();
          }
        });
      });
      _isTimerRunning = true;
    }
  }

  /// Pauses the match timer
  void _pauseTimer() {
    if (_isTimerRunning) {
      _timer?.cancel();
      _isTimerRunning = false;
    }
  }

  /// Stops the match timer and shows quarter selection
  void _stopTimer() {
    _timer?.cancel();
    _isTimerRunning = false;
    _showQuarterSelectionAlert();
  }

  /// Advances to the next quarter or ends match if all quarters completed
  void _startNextQuarter() {
    if (_currentQuarter < 4) {
      setState(() {
        _currentQuarter++;
        _quarterElapsedSeconds = 0;
        if (_isTimerRunning) {
          _startTimer();
        }
      });
    } else {
      _showMatchEndAlert();
    }
  }

  /// Updates match status in Firestore when match ends
  void _updateMatchFinishedStatus() async {
    try {
      await _firestore.collection('matches').doc(widget.matchId).update({
        'isFinished': true,
        'team1Score': _team1Score,
        'team2Score': _team2Score,
      });
    } catch (e) {
      print('Error updating match status: $e');
    }
  }

  /// Records a match action with validation checks
  void _recordMatchAction(String action) async {
    // Validate timer is running
    if (!_isTimerRunning) {
      await showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Match Not Started'),
          content: const Text('Please start the match by pressing the play button.'),
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

    // Validate player is selected
    if (_selectedPlayer == null || _selectedTeam == null) {
      await showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Player Not Selected'),
          content: const Text('Please select a player first.'),
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

    // Validate action sequences
    if (action == 'Goal') {
      if (_matchActions.isEmpty ||
          _matchActions.last['action'] != 'Kick' ||
          _matchActions.last['team'] != _selectedTeam) {
        await showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Invalid Goal Action'),
            content: const Text('Goal can only be recorded after a kick by the same team.'),
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
      if (_selectedTeam == widget.team1) _team1Score += 6;
      if (_selectedTeam == widget.team2) _team2Score += 6;
    } else if (action == 'Behind') {
      if (_matchActions.isEmpty ||
          !['Kick', 'Handball'].contains(_matchActions.last['action']) ||
          _matchActions.last['team'] != _selectedTeam) {
        await showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Invalid Behind Action'),
            content: const Text('Behind can only be recorded after a kick or handball by the same team.'),
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
      if (_selectedTeam == widget.team1) _team1Score += 1;
      if (_selectedTeam == widget.team2) _team2Score += 1;
    }

    // Create action data structure
    final actionData = {
      'action': action,
      'playerId': _selectedPlayer!['id'],
      'playerName': _selectedPlayer!['name'],
      'playerNumber': _selectedPlayer!['number'],
      'team': _selectedTeam,
      'timestamp': DateTime.now(),
      'quarter': _currentQuarter,
      'quarterTime': _getQuarterTime(),
    };

    // Update state and Firestore
    setState(() {
      _matchActions.add(actionData);
    });

    _firestore
        .collection('matches')
        .doc(widget.matchId)
        .collection('actions')
        .add(actionData);

    Fluttertoast.showToast(
      msg: "$action recorded for ${_selectedPlayer!['name']}",
      toastLength: Toast.LENGTH_SHORT,
      gravity: ToastGravity.BOTTOM,
      backgroundColor: Colors.black87,
      textColor: Colors.white,
      fontSize: 16.0,
    );
  }

  /// Formats the current quarter time as MM:SS
  String _getQuarterTime() {
    final minutes = _quarterElapsedSeconds ~/ 60;
    final seconds = _quarterElapsedSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  /// Shows alert when a quarter ends
  void _showQuarterEndAlert() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Quarter Ended'),
        content: Text('Quarter $_currentQuarter has ended after 20 minutes.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _startNextQuarter();
            },
            child: const Text('Start Next Quarter'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _updateMatchFinishedStatus();
            },
            child: const Text('End Match'),
          ),
        ],
      ),
    );
  }

  /// Shows alert when match ends
  void _showMatchEndAlert() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Match Ended'),
        content: const Text('All quarters have been completed.'),
        actions: [
          TextButton(
            onPressed: () {
              _updateMatchFinishedStatus();
            },
            child: const Text('End Match'),
          ),
        ],
      ),
    );
  }

  /// Shows dialog to select next quarter or end match
  void _showQuarterSelectionAlert() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Select Next Quarter'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_currentQuarter < 4)
              ListTile(
                title: Text('Quarter ${_currentQuarter + 1}'),
                onTap: () {
                  Navigator.pop(context);
                  _startNextQuarter();
                },
              ),
            ListTile(
              title: const Text('End Match'),
              onTap: () {
                Navigator.pop(context);
                _updateMatchFinishedStatus();;
              },
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text(
          'Recording Match',
          style: TextStyle(color: Colors.white),
        ),
        iconTheme: const IconThemeData(color: Colors.white),
        backgroundColor: const Color(0xFF00529B),
        bottom: TabBar(
          controller: _tabController,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          tabs: const [
            Tab(text: 'Record', icon: Icon(Icons.radio_button_checked)),
            Tab(text: 'Actions', icon: Icon(Icons.list)),
            Tab(text: 'Score', icon: Icon(Icons.score)),
            Tab(text: 'Players', icon: Icon(Icons.people)),
          ],
        ),
      ),
      backgroundColor: Colors.white,
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildRecordTab(),
          ActionsView(
            matchId: widget.matchId,
            team1Name: widget.team1,
            team2Name: widget.team2,
          ),
          ScoreScreen(
            matchId: widget.matchId,
            team1Name: widget.team1,
            team2Name: widget.team2,
          ),
          TeamComparisonScreen(
            matchId: widget.matchId,
            team1Name: widget.team1,
            team2Name: widget.team2,
          ),
        ],
      ),
    );
  }

  /// Builds the main recording tab with all controls
  Widget _buildRecordTab() {
    return SingleChildScrollView(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Quarter selection section
            _buildQuarterButtons(),
            const SizedBox(height: 4),
            const Text(
              'Current Quarter Time:',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
// Timer controls section
            _buildTimerControls(),
            const SizedBox(height: 20),

            // Player selection section
            const Text(
              'To Record Action Select Player then Actions:',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            _buildActionButtons(),
            const SizedBox(height: 20),

            // Action buttons section
            const SizedBox(height: 8),
            _buildPlayerLists(),
          ],
        ),
      ),
    );
  }

  /// Builds quarter selection buttons
  Widget _buildQuarterButtons() {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 8,
      runSpacing: 8,
      children: List.generate(4, (index) {
        final quarter = index + 1;
        return SizedBox(
          width: 80,
          child: ElevatedButton(
            onPressed: quarter <= _currentQuarter
                ? () {
              setState(() {
                _currentQuarter = quarter;
                _quarterElapsedSeconds = 0;
              });
            }
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: quarter == _currentQuarter ? Colors.blue[900] : Colors.grey,
              padding: const EdgeInsets.symmetric(vertical: 12),
            ),
            child: Text('Q$quarter', style: const TextStyle(fontSize: 16,color: Colors.white)),
          ),
        );
      }),
    );
  }

  Widget _buildTimerControls() {
    // Calculate progress percentage (0.0 to 1.0)
    final progress = _quarterElapsedSeconds / _quarterDuration;

    return Card(
      elevation: 4,
      margin: const EdgeInsets.symmetric(horizontal: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // Progress bar showing quarter completion
            LinearProgressIndicator(
              value: progress,
              backgroundColor: Colors.grey[200],
              color: Colors.blue[800],
              minHeight: 8,
            ),
            const SizedBox(height: 16),

            // Timer controls row
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  icon: const Icon(Icons.play_arrow),
                  onPressed: _startTimer,
                  color: _isTimerRunning ? Colors.grey : Colors.blue,
                  iconSize: 32,
                ),
                IconButton(
                  icon: const Icon(Icons.pause),
                  onPressed: _pauseTimer,
                  color: !_isTimerRunning ? Colors.grey : Colors.blue,
                  iconSize: 32,
                ),
                IconButton(
                  icon: const Icon(Icons.stop),
                  onPressed: _stopTimer,
                  color: Colors.blue,
                  iconSize: 32,
                ),
                const SizedBox(width: 20),

                // Time display
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: Colors.blue[50],
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    'Q$_currentQuarter: ${_getQuarterTime()}',
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                ),
              ],
            ),

            // Time remaining text
            const SizedBox(height: 8),
            Text(
              '${(_quarterDuration - _quarterElapsedSeconds) ~/ 60}m ${(_quarterDuration - _quarterElapsedSeconds) % 60}s remaining',
              style: TextStyle(
                color: Colors.grey[600],
                fontStyle: FontStyle.italic,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Builds action recording buttons
  Widget _buildActionButtons() {
    final actions = [
      {'label': 'Mark', 'icon': Icons.sports},
      {'label': 'Tackle', 'icon': Icons.sports_martial_arts},
      {'label': 'Kick', 'icon': Icons.sports_gymnastics},
      {'label': 'Handball', 'icon': Icons.sports_handball},
      {'label': 'Goal', 'icon': Icons.emoji_events},
      {'label': 'Behind', 'icon': Icons.sports_football},
    ];

    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 8,
      runSpacing: 8,
      children: actions.map((action) {
        return SizedBox(
          width: 160,
          child: ElevatedButton.icon(
            icon: Icon(action['icon'] as IconData,color: Colors.white,),
            label: Text(action['label'] as String, style: const TextStyle(color: Colors.white)),
            onPressed: () => _recordMatchAction(action['label'] as String),
            style: ElevatedButton.styleFrom(
              backgroundColor: _getActionButtonColor(action['label'] as String),
              padding: const EdgeInsets.symmetric(vertical: 12),
            ),
          ),
        );
      }).toList(),
    );
  }

  /// Builds player lists for both teams
  Widget _buildPlayerLists() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(widget.team1, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 8),
              _buildTeamPlayersList(widget.team1, widget.team1Players),
            ],
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(widget.team2, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 8),
              _buildTeamPlayersList(widget.team2, widget.team2Players),
            ],
          ),
        ),
      ],
    );
  }

  /// Builds a scrollable list of players for one team
  Widget _buildTeamPlayersList(String teamName, List<Map<String, dynamic>> players) {
    return Container(
      height: 250,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListView.builder(
        itemCount: players.length,
        itemBuilder: (context, index) {
          final player = players[index];
          return ListTile(
            dense: true,
            title: Text('${player['number']}. ${player['name']}'),
            tileColor: _selectedPlayer == player && _selectedTeam == teamName
                ? Colors.blue.withOpacity(0.2)
                : null,
            onTap: () {
              setState(() {
                _selectedPlayer = player;
                _selectedTeam = teamName;
              });
            },
          );
        },
      ),
    );
  }

  /// Returns color for each action button type
  Color _getActionButtonColor(String action) {
    switch (action) {
      case 'Mark':
        return Colors.teal;
      case 'Tackle':
        return Colors.purple;
      case 'Kick':
        return Colors.cyan;
      case 'Handball':
        return Colors.green;
      case 'Goal':
        return Colors.red;
      case 'Behind':
        return Colors.orange;
      default:
        return Colors.blue;
    }
  }
}
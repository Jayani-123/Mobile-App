import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:csv/csv.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

class ActionsView extends StatefulWidget {
  final String matchId;
  final String team1Name;
  final String team2Name;

  const ActionsView({
    Key? key,
    required this.matchId,
    required this.team1Name,
    required this.team2Name,
  }) : super(key: key);

  @override
  _ActionsViewState createState() => _ActionsViewState();
}

class _ActionsViewState extends State<ActionsView> {
  List<Map<String, dynamic>> _actions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadActions();
  }

  Future<void> _loadActions() async {
    try {
      final snapshot = await FirebaseFirestore.instance
          .collection('matches')
          .doc(widget.matchId)
          .collection('actions')
          .orderBy('timestamp')
          .get();

      final actions = snapshot.docs.map((doc) {
        final data = doc.data();
        return {
          'action': data['action'] ?? '',
          'playerId': data['playerId'] ?? '',
          'playerName': data['playerName'] ?? '',
          'playerNumber': data['playerNumber'] ?? '',
          'team': data['team'] ?? '',
          'timestamp': data['timestamp']?.toDate(),
          'quarter': data['quarter'] ?? 1,
          'quarterTime': data['quarterTime'] ?? '',
        };
      }).toList();

      setState(() {
        _actions = actions;
        _isLoading = false;
      });
    } catch (e) {
      print('Error fetching actions: $e');
      setState(() => _isLoading = false);
    }
  }

  Future<void> _shareActionsAsCSV() async {
    if (_actions.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No actions to export.')),
      );
      return;
    }

    final csvData = [
      ['Team', 'Quarter', 'Time', 'Action', 'Player Number', 'Player Name', 'Timestamp'],
      ..._actions.map((action) => [
        action['team'],
        'Q${action['quarter']}',
        action['quarterTime'],
        action['action'],
        action['playerNumber'],
        action['playerName'],
        (action['timestamp'] as DateTime?)?.toIso8601String() ?? '',
      ]),
    ];

    final csvString = const ListToCsvConverter().convert(csvData);

    try {
      final dir = await getTemporaryDirectory();
      final filePath = '${dir.path}/match_actions_${widget.matchId}.csv';
      final file = File(filePath);
      await file.writeAsString(csvString);

      await Share.shareXFiles(
        [XFile(file.path)],
        text: 'Match actions exported as CSV.',
      );
    } catch (e) {
      print('CSV export failed: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Failed to share CSV file.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text('Match Actions'),
        actions: [
          IconButton(
            icon: const Icon(Icons.share),
            tooltip: 'Share CSV',
            onPressed: _shareActionsAsCSV,
          ),
        ],
      ),
      backgroundColor: Colors.white,
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _actions.isEmpty
          ? const Center(child: Text('No actions recorded yet'))
          : SingleChildScrollView(
        scrollDirection: Axis.vertical,
        child: SizedBox(
          width: screenWidth,
          child: DataTable(
            columnSpacing: 12,
            headingRowColor: MaterialStateProperty.all(Colors.blue[600]),
            columns: [
              DataColumn(
                label: Center(
                  child: Text(
                    widget.team1Name,
                    style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
              const DataColumn(
                label: Center(
                  child: Text(
                    'Qtr',
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
              const DataColumn(
                label: Center(
                  child: Text(
                    'Time',
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
              const DataColumn(
                label: Center(
                  child: Text(
                    'Action',
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
              DataColumn(
                label: Center(
                  child: Text(
                    widget.team2Name,
                    style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
              ),
            ],
            rows: _actions.map((action) {
              final isTeam1 = action['team'] == widget.team1Name;
              return DataRow(cells: [
                DataCell(Text(
                  isTeam1 ? '${action['playerNumber']}. ${action['playerName']}' : '',
                  textAlign: TextAlign.left,
                )),
                DataCell(Text('Q${action['quarter']}', textAlign: TextAlign.center)),
                DataCell(Text(action['quarterTime'], textAlign: TextAlign.center)),
                DataCell(Text(action['action'], textAlign: TextAlign.center)),
                DataCell(Text(
                  !isTeam1 ? '${action['playerNumber']}. ${action['playerName']}' : '',
                  textAlign: TextAlign.right,
                )),
              ]);
            }).toList(),
          ),
        ),
      ),
    );
  }
}
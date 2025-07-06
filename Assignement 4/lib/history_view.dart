import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'actions_view.dart';
import 'scorescreen.dart';
import 'playerstat.dart';

/// Screen for recording match data including scores, player actions, and timing
class HistoryView extends StatefulWidget {
  final String matchId;
  final String team1;
  final String team2;


  const HistoryView({
    Key? key,
    required this.matchId,
    required this.team1,
    required this.team2,

  }) : super(key: key);

  @override
  _HistoryViewState createState() => _HistoryViewState();
}

class _HistoryViewState extends State<HistoryView> with SingleTickerProviderStateMixin {
  // Tab controller for the main navigation tabs
  late TabController _tabController;
  int _currentTabIndex = 0;

  // Firestore instance for database operations
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  @override
  void initState() {
    super.initState();
    // Initialize tab controller with 4 tabs
    _tabController = TabController(length: 3, vsync: this);
    _tabController.addListener(() {
      setState(() {
        _currentTabIndex = _tabController.index;
      });
    });
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Match History',
          style: TextStyle(color: Colors.white),
        ),
        iconTheme: const IconThemeData(color: Colors.white),
        backgroundColor: const Color(0xFF00529B),
        bottom: TabBar(
          controller: _tabController,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          tabs: const [

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




}
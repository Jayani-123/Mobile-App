import 'package:flutter/material.dart';
import 'add_player.dart';
import 'record_match.dart';

class PlayerList extends StatefulWidget {
  final String matchId;
  final String team1;
  final String team2;

  const PlayerList({
    super.key,
    required this.matchId,
    required this.team1,
    required this.team2,
  });

  @override
  State<PlayerList> createState() => _PlayerListState();
}

class _PlayerListState extends State<PlayerList> {
  final List<Map<String, dynamic>> _team1Players = [];
  final List<Map<String, dynamic>> _team2Players = [];
  String _searchQuery = '';

  void _navigateToAddPlayer() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddPlayer(
          matchId: widget.matchId,
          team1: widget.team1,
          team2: widget.team2,
        ),
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      _handlePlayerResult(result);
    }
  }

  void _handlePlayerResult(Map<String, dynamic> result) {
    final player = {
      'id': result['id'],
      'name': result['name'].toString(),
      'position': result['position'].toString(),
      'number': result['number'].toString(),
      'image': result['image'].toString(),
      'imageFormat': result['imageFormat'].toString(),
      'team': result['team'].toString(),
      'age': result['age']?.toString() ?? '',
      'height': result['height']?.toString() ?? '',
    };

    final targetTeam = player['team'] == widget.team1 ? _team1Players : _team2Players;
    final playerNumber = player['number'];
    final isDuplicate = targetTeam.any((p) => p['number'] == playerNumber);

    if (isDuplicate) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Duplicate Number'),
          content: Text(
              'Player number $playerNumber already exists in ${player['team']}. Would you like to change the number or cancel adding this player?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.pop(context);
                _navigateToAddPlayerWithData(player);
              },
              child: const Text('Change Number'),
            ),
          ],
        ),
      );
    } else {
      setState(() {
        if (player['team'] == widget.team1) {
          _team1Players.add(player);
        } else {
          _team2Players.add(player);
        }
      });
    }
  }

  void _navigateToAddPlayerWithData(Map<String, dynamic> playerData) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddPlayer(
          matchId: widget.matchId,
          team1: widget.team1,
          team2: widget.team2,
          initialData: playerData,
        ),
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      _handlePlayerResult(result);
    }
  }

  void _editPlayer(Map<String, dynamic> player, String team) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddPlayer(
          matchId: widget.matchId,
          team1: widget.team1,
          team2: widget.team2,
          initialData: player,
        ),
      ),
    );

    if (result != null && result is Map<String, dynamic>) {
      _updatePlayer(player, team, result);
    }
  }

  void _updatePlayer(Map<String, dynamic> oldPlayer, String team, Map<String, dynamic> newData) {
    final updatedPlayer = {
      'id': newData['id'],
      'name': newData['name'].toString(),
      'position': newData['position'].toString(),
      'number': newData['number'].toString(),
      'image': newData['image'].toString(),
      'imageFormat': newData['imageFormat'].toString(),
      'team': newData['team'].toString(),
      'age': newData['age']?.toString() ?? '',
      'height': newData['height']?.toString() ?? '',
    };

    if (oldPlayer['number'] != updatedPlayer['number']) {
      final targetTeam = updatedPlayer['team'] == widget.team1 ? _team1Players : _team2Players;
      final playerNumber = updatedPlayer['number'];
      final isDuplicate = targetTeam.any((p) => p['number'] == playerNumber && p['id'] != oldPlayer['id']);

      if (isDuplicate) {
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Duplicate Number'),
            content: Text('Player number $playerNumber already exists in ${updatedPlayer['team']}. Please choose a different number.'),
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
    }

    setState(() {
      final targetList = team == widget.team1 ? _team1Players : _team2Players;
      final index = targetList.indexWhere((p) => p['id'] == oldPlayer['id']);
      if (index != -1) {
        if (updatedPlayer['team'] == team) {
          targetList[index] = updatedPlayer;
        } else {
          targetList.removeAt(index);
          if (updatedPlayer['team'] == widget.team1) {
            _team1Players.add(updatedPlayer);
          } else {
            _team2Players.add(updatedPlayer);
          }
        }
      }
    });
  }

  void _deletePlayer(Map<String, dynamic> player, String team) {
    setState(() {
      if (team == widget.team1) {
        _team1Players.removeWhere((p) => p['id'] == player['id']);
      } else {
        _team2Players.removeWhere((p) => p['id'] == player['id']);
      }
    });
  }


  List<Map<String, dynamic>> _filterPlayers(List<Map<String, dynamic>> players) {
    if (_searchQuery.isEmpty) {
      return players;
    }
    return players
        .where((player) =>
    player['name'].toLowerCase().contains(_searchQuery.toLowerCase()) ||
        player['number'].toLowerCase().contains(_searchQuery.toLowerCase()))
        .toList();
  }

  bool _canStartMatch() {
    return _team1Players.length >= 2 && _team2Players.length >= 2;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF00529B),
        title: const Text(
          "Players",
          style: TextStyle(color: Colors.white),
        ),
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      backgroundColor: Colors.white,
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // Search bar
            Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: TextField(
                decoration: InputDecoration(
                  hintText: 'Search players...',
                  prefixIcon: const Icon(Icons.search),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                onChanged: (value) {
                  setState(() {
                    _searchQuery = value;
                  });
                },
              ),
            ),
            ElevatedButton.icon(
              onPressed: _navigateToAddPlayer,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF00529B),
                foregroundColor: Colors.white,
              ),
              icon: const Icon(Icons.add),
              label: const Text('Add Player'),
            ),
            const SizedBox(height: 20),
            Expanded(
              child: Row(
                children: [
                  _buildTeamColumn(widget.team1, _filterPlayers(_team1Players)),
                  const SizedBox(width: 10),
                  _buildTeamColumn(widget.team2, _filterPlayers(_team2Players)),
                ],
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton.icon(
              icon: const Icon(Icons.sports),
              label: const Text("Start Match"),
              onPressed: () async {
                if (!_canStartMatch()) {
                  await showDialog(
                    context: context,
                    builder: (context) => AlertDialog(
                      title: const Text('Cannot Start Match'),
                      content: const Text('Each team must have at least 2 players to start the match.'),
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

                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => RecordMatch(
                      matchId: widget.matchId,
                      team1: widget.team1,
                      team2: widget.team2,
                      team1Players: _team1Players,
                      team2Players: _team2Players,
                    ),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF00529B),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTeamColumn(String teamName, List<Map<String, dynamic>> players) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            teamName,
            style: const TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Color(0xFF00529B),
            ),
          ),
          const SizedBox(height: 10),
          Flexible(
            child: players.isEmpty
                ? Center(
              child: Text(
                _searchQuery.isEmpty
                    ? 'Add at least 2 players'
                    : 'No matching players',
                style: const TextStyle(color: Colors.grey),
              ),
            )
                : ListView.builder(
              itemCount: players.length,
              itemBuilder: (context, index) {
                final player = players[index];
                return Card(
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                " ${player['number']} - ${player['name']}",
                                style: const TextStyle(
                                  fontSize: 16,
                                  color: Color(0xFF00529B),
                                ),
                                overflow: TextOverflow.ellipsis,
                                maxLines: 1,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 10),
                        PopupMenuButton<String>(
                          onSelected: (value) {
                            if (value == 'edit') {
                              _editPlayer(player, teamName);
                            } else if (value == 'delete') {
                              _deletePlayer(player, teamName);
                            }
                          },
                          itemBuilder: (context) => [
                            const PopupMenuItem(
                                value: 'edit', child: Text('Edit')),
                            const PopupMenuItem(
                                value: 'delete', child: Text('Delete')),
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
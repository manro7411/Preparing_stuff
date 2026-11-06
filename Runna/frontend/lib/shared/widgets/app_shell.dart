import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class AppDestination {
  const AppDestination(this.path, this.label, this.icon);

  final String path;
  final String label;
  final IconData icon;
}

const appDestinations = [
  AppDestination('/dashboard', 'Overview', Icons.space_dashboard_outlined),
  AppDestination('/training', 'Training', Icons.directions_run),
  AppDestination('/recovery', 'Recovery', Icons.favorite_border),
  AppDestination('/nutrition', 'Nutrition', Icons.restaurant_outlined),
  AppDestination('/analytics', 'Analytics', Icons.insights_outlined),
  AppDestination('/ai-coach', 'AI coach', Icons.chat_bubble_outline),
  AppDestination('/profile', 'Profile', Icons.person_outline),
  AppDestination('/subscription', 'Subscription', Icons.credit_card_outlined),
];

class AppShell extends StatelessWidget {
  const AppShell({required this.child, required this.location, super.key});

  final Widget child;
  final String location;

  @override
  Widget build(BuildContext context) {
    final selected = appDestinations.indexWhere(
      (destination) => destination.path == location,
    );
    final index = selected < 0 ? 0 : selected;
    return LayoutBuilder(builder: (context, constraints) {
      final wide = constraints.maxWidth >= 900;
      return Scaffold(
        appBar: AppBar(title: Text('RunCoach / ${appDestinations[index].label}')),
        drawer: wide
            ? null
            : NavigationDrawer(
                selectedIndex: index,
                onDestinationSelected: (next) {
                  Navigator.of(context).pop();
                  context.go(appDestinations[next].path);
                },
                children: [
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text('RunCoach'),
                  ),
                  for (final destination in appDestinations)
                    NavigationDrawerDestination(
                      icon: Icon(destination.icon),
                      label: Text(destination.label),
                    ),
                ],
              ),
        body: SafeArea(
          child: Row(children: [
            if (wide) ...[
              SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight - 100),
                  child: IntrinsicHeight(
                    child: NavigationRail(
                      selectedIndex: index,
                      extended: true,
                      onDestinationSelected: (next) =>
                          context.go(appDestinations[next].path),
                      destinations: [
                        for (final destination in appDestinations)
                          NavigationRailDestination(
                            icon: Icon(destination.icon),
                            label: Text(destination.label),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
              const VerticalDivider(width: 1),
            ],
            Expanded(child: child),
          ]),
        ),
      );
    });
  }
}
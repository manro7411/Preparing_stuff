import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Overview',
        emptyLabel: 'No training data yet',
        icon: Icons.directions_run,
      );
}
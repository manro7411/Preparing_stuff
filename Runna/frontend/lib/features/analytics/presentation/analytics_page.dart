import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class AnalyticsPage extends StatelessWidget {
  const AnalyticsPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Analytics',
        emptyLabel: 'No activity history yet',
        icon: Icons.insights_outlined,
      );
}
import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class TrainingPage extends StatelessWidget {
  const TrainingPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Training',
        emptyLabel: 'No training plan yet',
        icon: Icons.directions_run,
      );
}
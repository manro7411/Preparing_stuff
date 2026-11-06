import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class OnboardingPage extends StatelessWidget {
  const OnboardingPage({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('RunCoach')),
        body: const FeaturePage(
          title: 'Onboarding',
          emptyLabel: 'Account setup unavailable',
          icon: Icons.directions_run,
        ),
      );
}
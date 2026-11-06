import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class AiCoachPage extends StatelessWidget {
  const AiCoachPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'AI coach',
        emptyLabel: 'No coaching recommendations yet',
        icon: Icons.chat_bubble_outline,
      );
}
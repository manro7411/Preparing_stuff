import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class RecoveryPage extends StatelessWidget {
  const RecoveryPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Recovery',
        emptyLabel: 'No sleep or recovery records yet',
        icon: Icons.favorite_border,
      );
}
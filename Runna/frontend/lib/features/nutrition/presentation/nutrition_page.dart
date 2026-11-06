import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class NutritionPage extends StatelessWidget {
  const NutritionPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Nutrition',
        emptyLabel: 'No nutrition or hydration plan yet',
        icon: Icons.restaurant_outlined,
      );
}
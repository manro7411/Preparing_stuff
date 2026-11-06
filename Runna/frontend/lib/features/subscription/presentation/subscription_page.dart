import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class SubscriptionPage extends StatelessWidget {
  const SubscriptionPage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Subscription',
        emptyLabel: 'No subscription connected',
        icon: Icons.credit_card_outlined,
      );
}
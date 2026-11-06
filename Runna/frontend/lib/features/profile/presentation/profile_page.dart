import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) => const FeaturePage(
        title: 'Profile',
        emptyLabel: 'No athlete profile connected',
        icon: Icons.person_outline,
      );
}
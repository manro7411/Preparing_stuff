import 'package:flutter/material.dart';

import '../../../shared/widgets/feature_page.dart';

class SignInPage extends StatelessWidget {
  const SignInPage({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('RunCoach')),
        body: const FeaturePage(
          title: 'Sign in',
          emptyLabel: 'Sign-in unavailable',
          icon: Icons.lock_outline,
        ),
      );
}
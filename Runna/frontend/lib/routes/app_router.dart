import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/ai_coach/presentation/ai_coach_page.dart';
import '../features/analytics/presentation/analytics_page.dart';
import '../features/authentication/presentation/sign_in_page.dart';
import '../features/dashboard/presentation/dashboard_page.dart';
import '../features/nutrition/presentation/nutrition_page.dart';
import '../features/onboarding/presentation/onboarding_page.dart';
import '../features/profile/presentation/profile_page.dart';
import '../features/recovery/presentation/recovery_page.dart';
import '../features/subscription/presentation/subscription_page.dart';
import '../features/training/presentation/training_page.dart';
import '../shared/widgets/app_shell.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final router = GoRouter(
    initialLocation: '/dashboard',
    routes: [
      GoRoute(path: '/', redirect: (context, state) => '/dashboard'),
      GoRoute(path: '/onboarding', builder: (context, state) => const OnboardingPage()),
      GoRoute(path: '/sign-in', builder: (context, state) => const SignInPage()),
      ShellRoute(
        builder: (context, state, child) =>
            AppShell(location: state.uri.path, child: child),
        routes: [
          GoRoute(path: '/dashboard', builder: (context, state) => const DashboardPage()),
          GoRoute(path: '/training', builder: (context, state) => const TrainingPage()),
          GoRoute(path: '/nutrition', builder: (context, state) => const NutritionPage()),
          GoRoute(path: '/recovery', builder: (context, state) => const RecoveryPage()),
          GoRoute(path: '/analytics', builder: (context, state) => const AnalyticsPage()),
          GoRoute(path: '/ai-coach', builder: (context, state) => const AiCoachPage()),
          GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
          GoRoute(path: '/subscription', builder: (context, state) => const SubscriptionPage()),
        ],
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      appBar: AppBar(title: const Text('RunCoach')),
      body: Center(
        child: TextButton.icon(
          onPressed: () => context.go('/dashboard'),
          icon: const Icon(Icons.home_outlined),
          label: const Text('Return to overview'),
        ),
      ),
    ),
  );
  ref.onDispose(router.dispose);
  return router;
});
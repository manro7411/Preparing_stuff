import 'package:flutter/material.dart';

class FeaturePage extends StatelessWidget {
  const FeaturePage({
    required this.title,
    required this.emptyLabel,
    required this.icon,
    super.key,
  });

  final String title;
  final String emptyLabel;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Semantics(
      label: title,
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 48, color: colors.primary),
              const SizedBox(height: 16),
              Text(emptyLabel,
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.titleMedium),
            ],
          ),
        ),
      ),
    );
  }
}
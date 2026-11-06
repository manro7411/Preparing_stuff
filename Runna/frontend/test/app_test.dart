import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:runcoach/app.dart';

void main() {
  testWidgets('Overview renders without invented metrics', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: RunCoachApp()));
    await tester.pumpAndSettle();

    expect(find.text('No training data yet'), findsOneWidget);
    expect(find.text('RunCoach / Overview'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Compact navigation reaches training', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: RunCoachApp()));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Open navigation menu'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Training'));
    await tester.pumpAndSettle();

    expect(find.text('No training plan yet'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Wide navigation fits the viewport', (tester) async {
    await tester.binding.setSurfaceSize(const Size(1280, 800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(const ProviderScope(child: RunCoachApp()));
    await tester.pumpAndSettle();

    expect(find.byType(NavigationRail), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
import 'package:flutter_test/flutter_test.dart';
import 'package:runcoach/shared/services/token_store.dart';

void main() {
  test('In-memory token store clears the session', () async {
    final store = InMemoryTokenStore();

    expect(await store.readAccessToken(), isNull);
    await store.writeAccessToken('test-token');
    expect(await store.readAccessToken(), 'test-token');
    await store.clear();
    expect(await store.readAccessToken(), isNull);
  });
}
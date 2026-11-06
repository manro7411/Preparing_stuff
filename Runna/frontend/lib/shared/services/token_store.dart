abstract interface class TokenStore {
  Future<String?> readAccessToken();
  Future<void> writeAccessToken(String token);
  Future<void> clear();
}

class InMemoryTokenStore implements TokenStore {
  String? _accessToken;

  @override
  Future<String?> readAccessToken() async => _accessToken;

  @override
  Future<void> writeAccessToken(String token) async {
    _accessToken = token;
  }

  @override
  Future<void> clear() async {
    _accessToken = null;
  }
}
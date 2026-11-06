import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../shared/services/token_store.dart';
import '../config/app_config.dart';

final tokenStoreProvider = Provider<TokenStore>((ref) => InMemoryTokenStore());

final dioProvider = Provider<Dio>((ref) {
  final tokenStore = ref.watch(tokenStoreProvider);
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 20),
    headers: {'Accept': 'application/json'},
  ));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      try {
        final token = await tokenStore.readAccessToken();
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      } catch (error) {
        handler.reject(DioException(requestOptions: options, error: error));
      }
    },
  ));
  ref.onDispose(() => dio.close(force: true));
  return dio;
});
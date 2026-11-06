import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_provider.dart';
import '../domain/athlete_profile.dart';
import '../domain/profile_repository.dart';

final profileRepositoryProvider = Provider<ProfileRepository>(
  (ref) => DioProfileRepository(ref.watch(dioProvider)),
);

final currentProfileProvider = FutureProvider.autoDispose<AthleteProfile>(
  (ref) => ref.watch(profileRepositoryProvider).current(),
);

class DioProfileRepository implements ProfileRepository {
  DioProfileRepository(this._dio);

  final Dio _dio;

  @override
  Future<AthleteProfile> current() async {
    final response = await _dio.get<Map<String, dynamic>>('/api/v1/athletes/me');
    final data = response.data;
    if (data == null) {
      throw const FormatException('Profile response is empty');
    }
    return AthleteProfile.fromJson(data);
  }
}
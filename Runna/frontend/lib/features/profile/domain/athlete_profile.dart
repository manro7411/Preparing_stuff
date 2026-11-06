import 'package:freezed_annotation/freezed_annotation.dart';

part 'athlete_profile.freezed.dart';
part 'athlete_profile.g.dart';

@freezed
abstract class AthleteProfile with _$AthleteProfile {
  const factory AthleteProfile({
    required String id,
    required String displayName,
    required String timeZone,
  }) = _AthleteProfile;

  factory AthleteProfile.fromJson(Map<String, dynamic> json) =>
      _$AthleteProfileFromJson(json);
}
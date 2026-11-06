import 'athlete_profile.dart';

abstract interface class ProfileRepository {
  Future<AthleteProfile> current();
}
import 'package:freezed_annotation/freezed_annotation.dart';

part 'api_error.freezed.dart';
part 'api_error.g.dart';

@freezed
abstract class ApiError with _$ApiError {
  const factory ApiError({
    required int status,
    required String code,
    required String message,
    required String requestId,
  }) = _ApiError;

  factory ApiError.fromJson(Map<String, dynamic> json) => _$ApiErrorFromJson(json);
}
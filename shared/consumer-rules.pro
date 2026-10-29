# Applied to every app consuming :shared (phone and watch).

# protobuf-javalite looks up message fields by name via reflection
# (e.g. "Field queries_ for X not found") and does not ship its own R8 rules.
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Horologist derives Data Layer paths from class simple names, so these must
# not be obfuscated, or phone and watch end up talking on different paths:
#  - gRPC: "/grpc/${Stub.simpleName}" (GrpcExtensions.pathFor), which must also
#    match the intent-filter paths declared in both AndroidManifests
#  - DataStore: "/proto/${T.simpleName}" (WearDataLayerRegistry.dataStorePath)
# Class.getSimpleName() on nested classes (e.g. WorkoutServiceGrpcKt$WorkoutServiceCoroutineStub)
# reads the InnerClasses attribute, which R8 full mode only keeps on classes matched by -keep
# (with -keepnames alone it returns "WorkoutServiceGrpcKt$WorkoutServiceCoroutineStub")
-keepattributes InnerClasses,EnclosingMethod
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.**GrpcKt
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.**GrpcKt$*CoroutineStub
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.Workout
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.Workout$WorkoutStaticData
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.Workout$WorkoutDynamicData
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.Media
-keep,allowoptimization class agdesigns.elevatefitness.shared.grpc.Media$MediaPlaying

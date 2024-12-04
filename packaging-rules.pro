-dontobfuscate

-keep class net.biesemeyer.funlock.** { *; }

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable

-repackageclasses net.biesemeyer.funlock.shaded
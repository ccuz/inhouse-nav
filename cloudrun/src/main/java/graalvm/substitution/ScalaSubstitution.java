package graalvm.substitution;

import java.lang.reflect.Field;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import sun.misc.Unsafe;

/**
 * For scala 2.13
 * see https://medium.com/graalvm/instant-netty-startup-using-graalvm-native-image-generation-ed6f14ff7692
 * and https://github.com/vmencik/akka-graal-native for their rational
 */
@TargetClass(className = "scala.runtime.Statics")
final class Target_scala_runtime_Statics {

    @Substitute
    public static void releaseFence() throws Throwable {
        // GraalVM is based on Java 8 so Unsafe is the only available option
        ScalaSubstitutions.UNSAFE.storeFence();
    }

}

class ScalaSubstitutions {

    static final Unsafe UNSAFE;

    static {
        try {
            sun.misc.Unsafe found = null;
            for (Field field : sun.misc.Unsafe.class.getDeclaredFields()) {
                if (field.getType() == sun.misc.Unsafe.class) {
                    field.setAccessible(true);
                    found = (sun.misc.Unsafe) field.get(null);
                    break;
                }
            }
            if (found == null)
                throw new IllegalStateException("Can't find instance of sun.misc.Unsafe");
            else UNSAFE = found;
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
}
package de.fourteen.jcase;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.Callable;

import static de.fourteen.jcase.UseCase.intents;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

public abstract class Extensions<ALTERNATIVE_EXITS extends AlternativeExits> {

  private UseCase useCase;

  public void setUseCase(UseCase useCase) {
    this.useCase = useCase;
  }

  @Hidden
  @SuppressWarnings("unchecked")
  public ALTERNATIVE_EXITS resulting_in() {
    Class<?> subclass = getClass();

    // Traverse up the hierarchy to find the parameterized Extensions class
    ParameterizedType parameterizedSuperclass = null;
    Class<?> currentClass = subclass;

    while (currentClass != null) {
      if (currentClass.getGenericSuperclass() instanceof ParameterizedType pt) {
        // Check if this is the Extensions class with type parameter
        if (pt.getRawType() instanceof Class<?> rawType &&
            Extensions.class.isAssignableFrom(rawType)) {
          parameterizedSuperclass = pt;
          break;
        }
      }
      currentClass = currentClass.getSuperclass();
    }
    assert parameterizedSuperclass != null;
    var clazz = (Class<ALTERNATIVE_EXITS>) parameterizedSuperclass.getActualTypeArguments()[0];
    try {
      Class<?> proxyClass = new ByteBuddy()
          .subclass(clazz)
          .method(not(isAnnotatedWith(Hidden.class)))
          .intercept(to(new MethodInterceptor(useCase)))
          .make()
          .load(clazz.getClassLoader())
          .getLoaded();
      Constructor<?> constructor = proxyClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      return (ALTERNATIVE_EXITS) constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("rawtypes")
  public static class MethodInterceptor {
    private final UseCase useCase;

    public MethodInterceptor(final UseCase useCase) {
      this.useCase = useCase;
    }

    @SuppressWarnings("unchecked")
    @RuntimeType
    public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method)
                throws Exception {
      useCase.addAlternativeExit(zuper);
      String methodName = method.getName().replaceAll("_", " ");
      System.out.println(intents(-2) + methodName);
      return zuper.call();
    }
  }

  @Hidden
  public void leading_to_use_case(int useCaseId) {
    System.out.println(intents(-2) + "(Use Case " + useCaseId + ")");
  }
}

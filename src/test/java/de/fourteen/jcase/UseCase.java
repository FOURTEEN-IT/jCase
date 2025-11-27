package de.fourteen.jcase;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.DynamicContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class UseCase<PRECONDITIONS extends Preconditions<PRECONDITIONS>, SUCCESS_END_CONDITIONS extends SuccessEndConditions, FAILED_END_CONDITIONS extends FailedEndConditions, STEPS extends Steps, EXTENSIONS extends Extensions<?>, VARIATIONS extends Variations, EXCEPTIONS extends Exceptions> {

  private static final int numberOfIntents = 6;
  private static final Map<Integer, String> useCaseTitlesById = new HashMap<>();

  private final List<Callable<?>> successEndConditions = new ArrayList<>();
  private final List<Callable<?>> failedEndConditions = new ArrayList<>();
  private final List<Callable<?>> steps = new ArrayList<>();
  private final Map<Integer, Map<String, Callable<?>>> extensionsByStepId =
      new HashMap<>();
  private final Map<String, Callable<?>> alternativeExitsByExtensionId =
      new HashMap<>();
  private final List<Map.Entry<String, Callable<?>>> variations =
      new ArrayList<>();
  private final List<Map.Entry<String, Callable<?>>> exceptions =
      new ArrayList<>();

  private String currentExtensionId;
  private String title;

  static String intents(int substracted) {
    return "\t".repeat(numberOfIntents - substracted);
  }

  static String intents() {
    return intents(0);
  }

  private static String intended(String original) {
    return original + ":" + intents((original.length() + 1) / 4);
  }

  void addAlternativeExit(Callable<?> alternativeExit) {
    alternativeExitsByExtensionId.put(currentExtensionId, alternativeExit);
  }

  void addExtension(int stepId, String extensionId, Callable<?> extension) {
    extensionsByStepId
        .computeIfAbsent(stepId, k -> new HashMap<>())
        .put(extensionId, extension);
  }

  protected void id(int id) {
    StackWalker walker =
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    StackWalker.StackFrame caller =
        walker.walk(frames -> frames.skip(1).findFirst().orElseThrow());
    title = caller.getMethodName().replaceAll("_", " ");
    System.out.println(intended("Use Case " + id) + title);
    useCaseTitlesById.put(id, title);
  }

  protected void used_by(int... id) {
    System.out.print(intended("Used by"));
    for (int i : id) {
      if (useCaseTitlesById.containsKey(i)) {
        System.out.print(useCaseTitlesById.get(i) + " ");
      }
      System.out.print("(Use Case " + i + ")");
      if (i != id[id.length - 1]) {
        System.out.print(", ");
      }
    }
    System.out.println();
  }

  protected void description(String description) {
    System.out.println(intended("Description") + description);
  }

  protected PRECONDITIONS preconditions() {
    return intercept(
        0,
        new PrintingMethodInterceptor(
            "Precoditions",
            new OriginalValueReturningMethodInterceptor()
        )
    );
  }

  protected SUCCESS_END_CONDITIONS success_end_condition() {
    return intercept(
        1, new PrintingMethodInterceptor(
            "Success End Conditions",
            new AddingCallableMethodInterceptor(
                successEndConditions::add,
                (zuper, method) -> null
            )
        )
    );
  }

  protected FAILED_END_CONDITIONS failed_end_condition() {
    return intercept(
        2, new PrintingMethodInterceptor(
            "Failed End Conditions",
            new AddingCallableMethodInterceptor(
                failedEndConditions::add,
                (zuper, method) -> null
            )
        )
    );
  }

  protected void actors(String actors) {
    System.out.println(intended("Actors") + actors);
  }

  protected void triggers(String triggers) {
    System.out.println(intended("Triggers") + triggers);
  }

  protected STEPS step() {
    return intercept(
        3, new PrintingMethodInterceptor(
            "Steps",
            () -> steps.size() + 1 + "\t",
            new AddingCallableMethodInterceptor(
                steps::add,
                (zuper, method) -> steps.size()
            )
        )
    );
  }

  protected EXTENSIONS extension_of(int stepId) {
    String extensionId = "" + stepId + (char) ('a' + Optional
        .ofNullable(extensionsByStepId.get(stepId))
        .map(Map::size)
        .orElse(0));
    currentExtensionId = extensionId;
    return intercept(
        4, new PrintingMethodInterceptor(
            "Extensions", () -> extensionId + "\t", (zuper, method) -> {
          Object result =
              new AddingCallableMethodInterceptor(
                  c -> addExtension(stepId, extensionId, c),
                  new OriginalValueReturningMethodInterceptor()
              ).intercept(zuper, method);
          if (result instanceof Extensions<?> extensions) {
            // Ensure the UseCase is set on the actual returned instance
            try {
              java.lang.reflect.Field field =
                  Extensions.class.getDeclaredField("useCase");
              field.setAccessible(true);
              field.set(extensions, this);
            } catch (Exception e) {
              extensions.setUseCase(this);
            }
          }
          return result;
        }
        )
    );
  }

  protected VARIATIONS variation_of(int stepId) {
    var printedStepIds = new ArrayList<>();
    return intercept(
        5, new PrintingMethodInterceptor(
            "Variations", () -> stepId + "\t", new AddingEntryMethodInterceptor(
            variations::add,
            "Variation of step " + stepId + ": ",
            (zuper, method) -> null
        )
        )
    );
  }

  protected EXCEPTIONS exception_in(int stepId) {
    return intercept(
        6, new PrintingMethodInterceptor(
            "Exceptions", () -> stepId + "\t", new AddingEntryMethodInterceptor(
            exceptions::add,
            "Exception in step " + stepId + ": ",
            (zuper, method) -> null
        )
        )
    );
  }

  protected void other_information(String otherInformation) {
    System.out.println(intended("Other information") + otherInformation);
  }

  protected void other_information(String otherInformation, String... moreOtherInformation) {
    other_information(otherInformation);
    for (String moreOtherInformation1 : moreOtherInformation) {
      System.out.println(intents() + moreOtherInformation1);
    }
  }

  protected void open_issue(String openIssue) {
    System.out.println(intended("Open issues") + openIssue);
  }

  protected void open_issues(String openIssue, String... moreOpenIssues) {
    open_issue(openIssue);
    for (String moreOpenIssue : moreOpenIssues) {
      System.out.println(intents() + moreOpenIssue);
    }
  }

  protected void due_date(String dueDate) {
    System.out.println(intended("Due date") + dueDate);
  }

  protected DynamicContainer jUnitTestCases() {
    return dynamicContainer(
        title, Stream.of(
            dynamicTest(
                "Main Szenario", () -> {
                  for (Callable<?> step : steps) {
                    step.call();
                  }
                  for (Callable<?> endCondition : successEndConditions) {
                    endCondition.call();
                  }
                  for (Callable<?> endCondition : failedEndConditions) {
                    endCondition.call();
                  }
                }
            ), dynamicContainer(
                "Extensions",
                extensionsByStepId
                    .keySet()
                    .stream()
                    .map(stepId -> dynamicContainer(
                        "Step " + stepId,
                        extensionsByStepId
                            .get(stepId)
                            .entrySet()
                            .stream()
                            .filter(entry -> alternativeExitsByExtensionId.containsKey(
                                entry.getKey()))
                            .map(entry -> dynamicTest(
                                "Extension " + entry.getKey(), () -> {
                                  for (int i = 0; i < steps.size(); i++) {
                                    if (i + 1 < stepId) {
                                      steps.get(i).call();
                                    }
                                  }
                                  entry.getValue().call();
                                  alternativeExitsByExtensionId
                                      .get(entry.getKey())
                                      .call();
                                }
                            ))
                    ))
            ), dynamicContainer(
                "Variations", variations.stream().map(entry -> {
                  return dynamicTest(
                      entry.getKey(), () -> {
                        for (int i = 0; i < steps.size(); i++) {
                          if (entry.getKey().contains("" + i)) {
                            entry.getValue().call();
                          } else {
                            steps.get(i).call();
                          }
                        }
                      }
                  );
                })
            ), dynamicContainer(
                "Exceptions", exceptions.stream().map(entry -> {
                  return dynamicTest(
                      entry.getKey(), () -> {
                        for (int i = 0; i < steps.size(); i++) {
                          if (entry.getKey().contains("" + i)) {
                            assertThrows(Throwable.class, () -> entry.getValue().call());
                          } else {
                            steps.get(i).call();
                          }
                        }
                      }
                  );
                })
            )
        )
    );
  }

  @SuppressWarnings("unchecked")
  public <T> T intercept(int parameterIndex,
                         MethodInterceptor methodInterceptor) {
    Class<?> subclass = getClass();
    ParameterizedType superclass =
        (ParameterizedType) subclass.getGenericSuperclass();
    var clazz = (Class<T>) superclass.getActualTypeArguments()[parameterIndex];
    try {
      Class<?> proxyClass = new ByteBuddy()
          .subclass(clazz)
          .method(not(isAnnotatedWith(Hidden.class)))
          .intercept(to(methodInterceptor))
          .make()
          .load(clazz.getClassLoader())
          .getLoaded();
      Constructor<?> constructor = proxyClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      return (T) constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public interface MethodInterceptor {
    Object intercept(Callable<?> zuper, Method method) throws Exception;
  }

  public static class OriginalValueReturningMethodInterceptor
      implements MethodInterceptor {
    @RuntimeType
    public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method)
        throws Exception {
      return zuper.call();
    }
  }

  public record AddingCallableMethodInterceptor(Consumer<Callable<?>> callableAdder,
                                                MethodInterceptor wrappee)
      implements MethodInterceptor {

    @RuntimeType
    public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method)
        throws Exception {
      callableAdder.accept(zuper);
      return wrappee.intercept(zuper, method);
    }
  }

  public record AddingEntryMethodInterceptor(Consumer<Map.Entry<String, Callable<?>>> callableAdder,
                                             String prefix,
                                             MethodInterceptor wrappee)
      implements MethodInterceptor {

    @RuntimeType
    public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method)
        throws Exception {
      callableAdder.accept(entry(
          prefix + " " + method.getName().replaceAll("_", " "),
          zuper
      ));
      return wrappee.intercept(zuper, method);
    }
  }

  public record PrintingMethodInterceptor(String introduction,
                                          Supplier<String> prefixSupplier,
                                          MethodInterceptor wrappee)
      implements MethodInterceptor {

    private static final List<String> knownIntroductions = new ArrayList<>();

    public PrintingMethodInterceptor(String introduction,
                                     MethodInterceptor wrappee) {
      this(introduction, () -> "", wrappee);
    }

    @RuntimeType
    public Object intercept(@SuperCall Callable<?> zuper, @Origin Method method)
        throws Exception {
      String methodName = method.getName().replaceAll("_", " ");
      if (!knownIntroductions.contains(introduction)) {
        System.out.print(intended(introduction));
        knownIntroductions.add(introduction);
      } else {
        System.out.print(intents());
      }
      System.out.println(prefixSupplier.get() + methodName);
      return wrappee.intercept(zuper, method);
    }
  }
}

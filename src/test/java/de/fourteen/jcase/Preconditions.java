package de.fourteen.jcase;

import static de.fourteen.jcase.UseCase.intents;

public abstract class Preconditions<SELF extends Preconditions<?>> {

  @Hidden
  @SuppressWarnings("unchecked")
  public SELF and() {
    System.out.println(intents() + "and");
    return (SELF) this;
  }
}

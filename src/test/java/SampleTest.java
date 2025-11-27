import de.fourteen.jcase.*;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

class SampleTest
    extends UseCase<SampleTest.SamplePreconditions, SampleTest.SampleSuccessEndConditions, SampleTest.SampleFailedEndConditions, SampleTest.SampleSteps, SampleTest.SampleExtensions, SampleTest.SampleVariations, SampleTest.SampleExceptions> {

  @TestFactory
  DynamicContainer Buy_Goods() {
    id(5);
    used_by(2, 3);
    description(
        "Buyer issues request directly to our company, expects goods shipped and to be billed.");
    preconditions()
        .we_know_Buyer()
        .and()
        .their_address()
        .and()
        .needed_buyer_information();
    success_end_condition().buyer_has_goods();
    success_end_condition().we_have_money_for_the_goods();
    failed_end_condition().we_have_not_sent_the_goods();
    failed_end_condition().buyer_has_not_spent_the_money();
    actors(
        "Buyer, any agent (or computer) acting for the customer, credit card company, bank, shipping service ");
    triggers("purchase request comes in");
    int purchaseRequestStep = step().buyer_calls_in_with_a_purchase_request();
    step().company_captures_buyers_name_and_address_and_requested_goods();
    int informationStep =
        step().company_gives_buyer_information_on_goods_prices_and_delivery_dates();
    int signingStep = step().buyer_signs_for_order();
    step().company_creates_order_and_ships_order_to_buyer();
    step().company_ships_invoice_to_buyer();
    int paymentStep = step().buyers_pays_invoice();
    extension_of(informationStep)
        .company_is_out_of_one_of_the_ordered_items()
        .resulting_in()
        .renegotiate_order();
    extension_of(signingStep)
        .buyer_pays_directly_with_credit_card()
        .leading_to_use_case(44);
    extension_of(paymentStep).buyer_returns_goods().leading_to_use_case(105);
    variation_of(purchaseRequestStep).buyer_may_use_phone_in();
    variation_of(purchaseRequestStep).buyer_may_use_fax_in();
    variation_of(purchaseRequestStep).buyer_may_use_web_order_form();
    variation_of(purchaseRequestStep).buyer_may_use_electronic_interchange();
    variation_of(paymentStep).buyer_may_pay_by_cash();
    variation_of(paymentStep).buyer_may_pay_by_money_order();
    variation_of(paymentStep).buyer_may_pay_by_check();
    variation_of(paymentStep).buyer_may_pay_by_credit_card();
    exception_in(signingStep).credit_card_not_accepted();
    other_information(
        "5 minutes for order", "45 days until paid", "expect 200 per day");
    open_issues(
        "What if we have part of the order?",
        "What is credit card is stolen?"
    );
    due_date("release 1.0");
    return jUnitTestCases();
  }

  public static class SamplePreconditions
      extends Preconditions<SamplePreconditions> {
    public SamplePreconditions we_know_Buyer() {
      // setup so that we know buyer
      return this;
    }

    public SamplePreconditions their_address() {
      // setup so that we know buyer's address'
      return this;
    }

    public void needed_buyer_information() {
      // setup so that we know buyer's information'
    }
  }

  public static class SampleSuccessEndConditions extends SuccessEndConditions {
    public void buyer_has_goods() {
      // assertThat(byer).hasGoods();
    }

    public void we_have_money_for_the_goods() {
      // assertThat(we).haveMoneyForTheGoods();
    }
  }

  public static class SampleFailedEndConditions extends FailedEndConditions {
    public void we_have_not_sent_the_goods() {
      // if(weHaveNotSentTheGoods()) {
      //   fail(...)
      // }
    }

    public void buyer_has_not_spent_the_money() {
      // if(buyerHasNotSpentTheMoney()) {
      //   fail(...)
      // }
    }
  }

  public static class SampleSteps extends Steps {
    public int buyer_calls_in_with_a_purchase_request() {
      // code to perform this step
      return 0;
    }

    public int company_captures_buyers_name_and_address_and_requested_goods() {
      // code to perform this step
      return 0;
    }

    public int company_gives_buyer_information_on_goods_prices_and_delivery_dates() {
      // code to perform this step
      return 0;
    }

    public int buyer_signs_for_order() {
      // code to perform this step
      return 0;
    }

    public int company_creates_order_and_ships_order_to_buyer() {
      // code to perform this step
      return 0;
    }

    public int company_ships_invoice_to_buyer() {
      // code to perform this step
      return 0;
    }

    public int buyers_pays_invoice() {
      // code to perform this step
      return 0;
    }
  }

  public static class SampleExtensions
      extends Extensions<SampleAlternativeExits> {

    public SampleExtensions company_is_out_of_one_of_the_ordered_items() {
      // setup so that the company is out of one of the ordered items
      return this;
    }

    public SampleExtensions buyer_pays_directly_with_credit_card() {
      // setup so that buyer pays directly with credit card
      return this;
    }

    public SampleExtensions buyer_returns_goods() {
      // setup so that buyer returns goods
      return this;
    }
  }

  public static class SampleAlternativeExits extends AlternativeExits {
    public void renegotiate_order() {
      // assertThat(company).renegotiatesOrder();
    }
  }

  public static class SampleVariations extends Variations {

    public void buyer_may_use_phone_in() {
      // code to perform this variation
    }

    public void buyer_may_use_fax_in() {
      // code to perform this variation
    }

    public void buyer_may_use_web_order_form() {
      // code to perform this variation
    }

    public void buyer_may_use_electronic_interchange() {
      // code to perform this variation
    }

    public void buyer_may_pay_by_cash() {
      // code to perform this variation
    }

    public void buyer_may_pay_by_money_order() {
      // code to perform this variation
    }

    public void buyer_may_pay_by_check() {
      // code to perform this variation
    }

    public void buyer_may_pay_by_credit_card() {
      // code to perform this variation
    }
  }

  public static class SampleExceptions extends Exceptions {
    public void credit_card_not_accepted() {
      // setup so that credit card is not accepted
      throw new RuntimeException("Credit card not accepted");
    }
  }
}

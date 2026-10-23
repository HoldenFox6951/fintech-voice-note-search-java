package fintech;

public final class RiskDecisionTest {
  static String decision(boolean callerVerified, long amountCents) {
    return callerVerified && amountCents < 100_000 ? "ALLOW_REFUND_REVIEW" : "HOLD_FOR_COMPLIANCE";
  }
  public static void main(String[] args) {
    if (!"HOLD_FOR_COMPLIANCE".equals(decision(false, 1200))) throw new AssertionError();
    if (!"ALLOW_REFUND_REVIEW".equals(decision(true, 5000))) throw new AssertionError();
    System.out.println("risk decisions passed");
  }
}
